package com.edulife.courses.service;

import com.edulife.courses.dto.LessonDetailDto;
import com.edulife.courses.entity.CourseSection;
import com.edulife.courses.entity.Lesson;
import com.edulife.courses.repository.CourseSectionRepository;
import com.edulife.courses.repository.LessonRepository;
import com.edulife.enrollments.model.EnrollmentStatus;
import com.edulife.enrollments.repository.EnrollmentRepository;
import com.edulife.progress.repository.LessonProgressRepository;
import com.edulife.security.FirebaseAuthentication;
import com.edulife.users.entity.User;
import com.edulife.users.repository.UserRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LessonService {

    private final LessonRepository lessonRepository;
    private final CourseSectionRepository sectionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final UserRepository userRepository;

    public LessonService(LessonRepository lessonRepository,
                         CourseSectionRepository sectionRepository,
                         EnrollmentRepository enrollmentRepository,
                         LessonProgressRepository lessonProgressRepository,
                         UserRepository userRepository) {
        this.lessonRepository         = lessonRepository;
        this.sectionRepository        = sectionRepository;
        this.enrollmentRepository     = enrollmentRepository;
        this.lessonProgressRepository = lessonProgressRepository;
        this.userRepository           = userRepository;
    }

    public LessonDetailDto getLessonDetail(UUID courseId, UUID lessonId) {
        Lesson lesson = lessonRepository.findByIdAndCourseId(lessonId, courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));

        CourseSection section = sectionRepository.findById(lesson.getCourseSectionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));

        User user = resolveCurrentUser();

        if (!lesson.isPreview()) {
            boolean enrolled = enrollmentRepository.existsByUserIdAndCourseIdAndStatus(
                    user.getId(), courseId, EnrollmentStatus.ACTIVE);
            if (!enrolled) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Enroll in this course to access the lesson");
            }
        }

        boolean completed = lessonProgressRepository.existsByUserIdAndLessonId(user.getId(), lessonId);

        return new LessonDetailDto(
                lesson.getId(),
                courseId,
                section.getId(),
                section.getTitle(),
                lesson.getTitle(),
                lesson.getSummary(),
                lesson.getLessonType(),
                lesson.getContentUrl(),
                lesson.getContentBody(),
                lesson.getEstimatedDurationMinutes(),
                lesson.getDisplayOrder(),
                lesson.isPreview(),
                completed
        );
    }

    private User resolveCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof FirebaseAuthentication firebaseAuth)) {
            throw new IllegalStateException("Firebase authentication required");
        }
        return userRepository.findByFirebaseUid(firebaseAuth.getFirebaseUid())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found. Call /auth/sync first."));
    }
}
