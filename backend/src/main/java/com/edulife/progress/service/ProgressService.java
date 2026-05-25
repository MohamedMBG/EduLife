package com.edulife.progress.service;

import com.edulife.courses.entity.CourseSection;
import com.edulife.courses.entity.Lesson;
import com.edulife.courses.repository.CourseSectionRepository;
import com.edulife.courses.repository.LessonRepository;
import com.edulife.enrollments.model.EnrollmentStatus;
import com.edulife.enrollments.repository.EnrollmentRepository;
import com.edulife.progress.dto.CourseProgressDto;
import com.edulife.progress.entity.CourseProgress;
import com.edulife.progress.entity.LessonProgress;
import com.edulife.progress.repository.CourseProgressRepository;
import com.edulife.progress.repository.LessonProgressRepository;
import com.edulife.security.FirebaseAuthentication;
import com.edulife.users.entity.User;
import com.edulife.users.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProgressService {

    private final LessonProgressRepository lessonProgressRepository;
    private final CourseProgressRepository courseProgressRepository;
    private final LessonRepository lessonRepository;
    private final CourseSectionRepository sectionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    public ProgressService(
            LessonProgressRepository lessonProgressRepository,
            CourseProgressRepository courseProgressRepository,
            LessonRepository lessonRepository,
            CourseSectionRepository sectionRepository,
            EnrollmentRepository enrollmentRepository,
            UserRepository userRepository) {
        this.lessonProgressRepository = lessonProgressRepository;
        this.courseProgressRepository = courseProgressRepository;
        this.lessonRepository         = lessonRepository;
        this.sectionRepository        = sectionRepository;
        this.enrollmentRepository     = enrollmentRepository;
        this.userRepository           = userRepository;
    }

    @Transactional
    public void markLessonComplete(UUID courseId, UUID lessonId) {
        User user = resolveCurrentUser();

        Lesson lesson = lessonRepository.findByIdAndCourseId(lessonId, courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));

        if (!lesson.isPreview()) {
            boolean enrolled = enrollmentRepository.existsByUserIdAndCourseIdAndStatus(
                    user.getId(), courseId, EnrollmentStatus.ACTIVE);
            if (!enrolled) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Enroll in this course to mark lessons complete");
            }
        }

        // Idempotent — second call for the same lesson is a no-op
        if (!lessonProgressRepository.existsByUserIdAndLessonId(user.getId(), lessonId)) {
            lessonProgressRepository.save(new LessonProgress(user.getId(), lessonId, courseId));
            syncCourseProgress(user.getId(), courseId);
        }
    }

    public CourseProgressDto getCourseProgress(UUID courseId) {
        User user = resolveCurrentUser();

        boolean enrolled = enrollmentRepository.existsByUserIdAndCourseIdAndStatus(
                user.getId(), courseId, EnrollmentStatus.ACTIVE);
        if (!enrolled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Enroll in this course to view progress");
        }

        List<CourseSection> sections = sectionRepository.findAllByCourseIdOrderByDisplayOrderAsc(courseId);
        Set<UUID> completedIds = lessonProgressRepository
                .findCompletedLessonIdsByUserIdAndCourseId(user.getId(), courseId);

        int totalLessons = 0;
        int completedLessons = 0;

        List<CourseProgressDto.SectionProgressDto> sectionDtos = sections.stream().map(section -> {
            List<Lesson> lessons = lessonRepository
                    .findAllByCourseSectionIdOrderByDisplayOrderAsc(section.getId());

            List<CourseProgressDto.LessonProgressDto> lessonDtos = lessons.stream().map(l ->
                    new CourseProgressDto.LessonProgressDto(
                            l.getId(),
                            l.getTitle(),
                            l.getLessonType(),
                            l.getEstimatedDurationMinutes(),
                            l.getDisplayOrder(),
                            l.isPreview(),
                            completedIds.contains(l.getId())
                    )
            ).toList();

            return new CourseProgressDto.SectionProgressDto(
                    section.getId(),
                    section.getTitle(),
                    section.getDisplayOrder(),
                    lessonDtos
            );
        }).toList();

        // Recount from the built DTO to stay consistent with what we return
        int total = sectionDtos.stream()
                .mapToInt(s -> s.lessons().size())
                .sum();
        int completed = sectionDtos.stream()
                .flatMap(s -> s.lessons().stream())
                .mapToInt(l -> l.completed() ? 1 : 0)
                .sum();

        int percentage = total == 0 ? 0 : (int) Math.round((completed * 100.0) / total);

        return new CourseProgressDto(courseId, completed, total, percentage, sectionDtos);
    }

    private void syncCourseProgress(UUID userId, UUID courseId) {
        long total     = lessonRepository.countByCourseId(courseId);
        long completed = lessonProgressRepository.countByUserIdAndCourseId(userId, courseId);

        courseProgressRepository.findByUserIdAndCourseId(userId, courseId)
                .ifPresentOrElse(
                        cp -> {
                            cp.update((int) completed, (int) total);
                            courseProgressRepository.save(cp);
                        },
                        () -> courseProgressRepository.save(
                                new CourseProgress(userId, courseId, (int) completed, (int) total))
                );
    }

    private User resolveCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof FirebaseAuthentication firebaseAuth)) {
            throw new IllegalStateException("Firebase authentication required");
        }
        return userRepository.findByFirebaseUid(firebaseAuth.getFirebaseUid())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "User not found. Call /auth/sync first."));
    }
}
