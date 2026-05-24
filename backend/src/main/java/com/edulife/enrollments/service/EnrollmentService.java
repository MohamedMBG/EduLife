package com.edulife.enrollments.service;

import com.edulife.courses.entity.Course;
import com.edulife.courses.model.CourseStatus;
import com.edulife.courses.repository.CourseRepository;
import com.edulife.enrollments.dto.EnrolledCourseDto;
import com.edulife.enrollments.dto.EnrollmentResponse;
import com.edulife.enrollments.entity.Enrollment;
import com.edulife.enrollments.model.EnrollmentStatus;
import com.edulife.enrollments.repository.EnrollmentRepository;
import com.edulife.security.FirebaseAuthentication;
import com.edulife.users.entity.User;
import com.edulife.users.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
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

    public EnrollmentService(
            EnrollmentRepository enrollmentRepository,
            CourseRepository courseRepository,
            UserRepository userRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public EnrollmentResponse enroll(UUID courseId) {
        User user = resolveCurrentUser();

        courseRepository.findByIdAndStatus(courseId, CourseStatus.PUBLISHED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        if (enrollmentRepository.existsByUserIdAndCourseId(user.getId(), courseId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already enrolled in this course");
        }

        Enrollment enrollment = enrollmentRepository.save(new Enrollment(user.getId(), courseId));

        return new EnrollmentResponse(
                enrollment.getId(),
                enrollment.getCourseId(),
                enrollment.getEnrolledAt(),
                enrollment.getStatus().name()
        );
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
