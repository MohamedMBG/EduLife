package com.edulife.enrollments.service;

import com.edulife.courses.entity.Course;
import com.edulife.courses.model.CourseStatus;
import com.edulife.courses.repository.CourseRepository;
import com.edulife.enrollments.dto.EnrolledCourseDto;
import com.edulife.enrollments.dto.EnrollmentResponse;
import com.edulife.enrollments.entity.Enrollment;
import com.edulife.enrollments.model.EnrollmentStatus;
import com.edulife.enrollments.repository.EnrollmentRepository;
import com.edulife.gamification.service.GamificationService;
import com.edulife.progress.service.ProgressService;
import com.edulife.security.FirebaseAuthentication;
import com.edulife.users.entity.User;
import com.edulife.users.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final ProgressService progressService;
    private final GamificationService gamificationService;

    public EnrollmentService(
            EnrollmentRepository enrollmentRepository,
            CourseRepository courseRepository,
            UserRepository userRepository,
            ProgressService progressService,
            GamificationService gamificationService) {
        this.enrollmentRepository = enrollmentRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.progressService = progressService;
        this.gamificationService = gamificationService;
    }

    @Transactional
    public EnrollmentResponse enroll(UUID courseId) {
        User user = resolveCurrentUser();

        courseRepository.findByIdAndStatus(courseId, CourseStatus.PUBLISHED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        if (enrollmentRepository.existsByUserIdAndCourseIdAndStatus(user.getId(), courseId, EnrollmentStatus.ACTIVE)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already enrolled in this course");
        }

        // Reactivate a previously cancelled enrollment rather than inserting a duplicate row —
        // the table enforces UNIQUE (user_id, course_id).
        Optional<Enrollment> existing = enrollmentRepository.findByUserIdAndCourseId(user.getId(), courseId);
        Enrollment enrollment;
        if (existing.isPresent()) {
            enrollment = existing.get();
            enrollment.reactivate();
            enrollment = enrollmentRepository.save(enrollment);
        } else {
            enrollment = enrollmentRepository.save(new Enrollment(user.getId(), courseId));
        }

        progressService.initializeCourseProgress(user.getId(), courseId);

        // Dedup key is the enrollment id, so a reactivated row will not double-award. XP
        // emission runs in REQUIRES_NEW so a gamification failure cannot roll back the
        // enrollment — backend remains the source of truth, but enrollment is authoritative.
        gamificationService.onEnrollment(user.getId(), enrollment.getId());

        return new EnrollmentResponse(
                enrollment.getId(),
                enrollment.getCourseId(),
                enrollment.getEnrolledAt(),
                enrollment.getStatus().name()
        );
    }

    @Transactional
    public void unenroll(UUID enrollmentId) {
        User user = resolveCurrentUser();

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Enrollment not found"));

        if (!enrollment.getUserId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the owner of this enrollment");
        }

        enrollment.cancel();
        enrollmentRepository.save(enrollment);
    }

    public List<EnrolledCourseDto> getMyEnrollments() {
        User user = resolveCurrentUser();

        List<Enrollment> enrollments = enrollmentRepository
                .findAllByUserIdAndStatus(user.getId(), EnrollmentStatus.ACTIVE);

        if (enrollments.isEmpty()) {
            return List.of();
        }

        List<UUID> courseIds = enrollments.stream()
                .map(Enrollment::getCourseId)
                .toList();

        Map<UUID, Course> coursesById = courseRepository.findAllById(courseIds).stream()
                .collect(Collectors.toMap(Course::getId, Function.identity()));

        return enrollments.stream()
                .filter(e -> coursesById.containsKey(e.getCourseId()))
                .map(e -> {
                    Course course = coursesById.get(e.getCourseId());
                    return new EnrolledCourseDto(
                            e.getId(),
                            course.getId(),
                            course.getSlug(),
                            course.getTitle(),
                            course.getShortDescription(),
                            course.getLevel(),
                            course.getLanguageCode(),
                            course.getImageUrl(),
                            e.getEnrolledAt()
                    );
                })
                .toList();
    }

    private User resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof FirebaseAuthentication firebaseAuth)) {
            throw new IllegalStateException("Firebase authentication is required.");
        }

        String firebaseUid = firebaseAuth.getFirebaseUid();
        return userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found. Call /auth/sync first."));
    }
}
