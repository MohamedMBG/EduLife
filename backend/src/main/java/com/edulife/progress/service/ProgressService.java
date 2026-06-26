package com.edulife.progress.service;

import com.edulife.courses.entity.CourseSection;
import com.edulife.courses.entity.Lesson;
import com.edulife.courses.repository.CourseSectionRepository;
import com.edulife.courses.repository.LessonRepository;
import com.edulife.enrollments.model.EnrollmentStatus;
import com.edulife.enrollments.repository.EnrollmentRepository;
import com.edulife.gamification.service.GamificationService;
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
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Core service for tracking learner progress through courses and lessons.
 *
 * <p>Handles lesson completion (idempotent), course progress aggregation,
 * and gamification XP emission for lesson/course completion events.</p>
 */
@Service
public class ProgressService {

    private final LessonProgressRepository lessonProgressRepository;
    private final CourseProgressRepository courseProgressRepository;
    private final LessonRepository lessonRepository;
    private final CourseSectionRepository sectionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final GamificationService gamificationService;

    public ProgressService(
            LessonProgressRepository lessonProgressRepository,
            CourseProgressRepository courseProgressRepository,
            LessonRepository lessonRepository,
            CourseSectionRepository sectionRepository,
            EnrollmentRepository enrollmentRepository,
            UserRepository userRepository,
            GamificationService gamificationService) {
        this.lessonProgressRepository = lessonProgressRepository;
        this.courseProgressRepository = courseProgressRepository;
        this.lessonRepository         = lessonRepository;
        this.sectionRepository        = sectionRepository;
        this.enrollmentRepository     = enrollmentRepository;
        this.userRepository           = userRepository;
        this.gamificationService      = gamificationService;
    }

    /**
     * Marks a lesson as completed for the authenticated learner.
     *
     * <p>Idempotent: a second call for the same lesson is a no-op. On first completion,
     * syncs course progress, emits lesson-completion XP, and triggers course-completion
     * XP if all lessons are now done.</p>
     *
     * @param courseId the course containing the lesson
     * @param lessonId the lesson to mark complete
     * @throws ResponseStatusException 404 if lesson not found, 403 if not enrolled
     */
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

            gamificationService.onLessonCompleted(user.getId(), lessonId);

            // Course-completion XP fires exactly once per course because the dedup key inside
            // GamificationService is "course:" + courseId + ":" + userId.
            long total = lessonRepository.countByCourseId(courseId);
            long completed = lessonProgressRepository.countByUserIdAndCourseId(user.getId(), courseId);
            if (total > 0 && completed >= total) {
                gamificationService.onCourseCompleted(user.getId(), courseId);
            }
        }
    }

    /**
     * Builds a detailed progress view for the authenticated learner in the given course.
     *
     * @param courseId the course to retrieve progress for
     * @return section-by-section progress with per-lesson completion status
     * @throws ResponseStatusException 403 if not enrolled
     */
    public CourseProgressDto getCourseProgress(UUID courseId) {
        User user = resolveCurrentUser();

        boolean enrolled = enrollmentRepository.existsByUserIdAndCourseIdAndStatus(
                user.getId(), courseId, EnrollmentStatus.ACTIVE);
        if (!enrolled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Enroll in this course to view progress");
        }

        List<CourseSection> sections = sectionRepository.findAllByCourseIdOrderByDisplayOrderAsc(courseId);
        Map<UUID, LessonProgress> completedMap = lessonProgressRepository
                .findAllByUserIdAndCourseId(user.getId(), courseId)
                .stream()
                .collect(Collectors.toMap(LessonProgress::getLessonId, Function.identity()));

        List<CourseProgressDto.SectionProgressDto> sectionDtos = sections.stream().map(section -> {
            List<Lesson> lessons = lessonRepository
                    .findAllByCourseSectionIdOrderByDisplayOrderAsc(section.getId());

            List<CourseProgressDto.LessonProgressDto> lessonDtos = lessons.stream().map(l -> {
                LessonProgress lp = completedMap.get(l.getId());
                return new CourseProgressDto.LessonProgressDto(
                        l.getId(),
                        l.getTitle(),
                        l.getLessonType(),
                        l.getEstimatedDurationMinutes(),
                        l.getDisplayOrder(),
                        l.isPreview(),
                        lp != null,
                        lp != null ? lp.getCompletedAt() : null
                );
            }).toList();

            return new CourseProgressDto.SectionProgressDto(
                    section.getId(),
                    section.getTitle(),
                    section.getDisplayOrder(),
                    lessonDtos
            );
        }).toList();

        int total = sectionDtos.stream().mapToInt(s -> s.lessons().size()).sum();
        int completed = (int) sectionDtos.stream()
                .flatMap(s -> s.lessons().stream())
                .filter(CourseProgressDto.LessonProgressDto::completed)
                .count();
        double percentComplete = total == 0 ? 0.0 : Math.round((completed * 1000.0) / total) / 10.0;

        return new CourseProgressDto(courseId, completed, total, percentComplete, sectionDtos);
    }

    /** Creates or updates the course progress row during enrollment initialization. */
    @Transactional
    public void initializeCourseProgress(UUID userId, UUID courseId) {
        syncCourseProgress(userId, courseId);
    }

    /** Recalculates and persists the aggregate lesson counts for a user-course pair. */
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

    /** Resolves the internal user from the Firebase-authenticated security context. */
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
