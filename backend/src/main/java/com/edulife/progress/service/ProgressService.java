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

    @Transactional
    public void initializeCourseProgress(UUID userId, UUID courseId) {
        syncCourseProgress(userId, courseId);
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
