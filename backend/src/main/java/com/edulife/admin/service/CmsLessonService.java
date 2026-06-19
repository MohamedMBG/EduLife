package com.edulife.admin.service;

import com.edulife.admin.dto.CreateLessonRequest;
import com.edulife.admin.dto.LessonAdminDto;
import com.edulife.admin.dto.UpdateLessonRequest;
import com.edulife.courses.entity.CourseSection;
import com.edulife.courses.entity.Lesson;
import com.edulife.courses.repository.CourseSectionRepository;
import com.edulife.courses.repository.CourseRepository;
import com.edulife.courses.repository.LessonRepository;
import com.edulife.security.FirebaseAuthentication;
import com.edulife.users.entity.User;
import com.edulife.users.model.UserRole;
import com.edulife.users.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** CMS lesson management under a section. Ownership is resolved through the section → course chain. */
@Service
public class CmsLessonService {

    private final LessonRepository lessonRepository;
    private final CourseSectionRepository sectionRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CmsCourseAccessGuard courseAccessGuard;

    public CmsLessonService(
            LessonRepository lessonRepository,
            CourseSectionRepository sectionRepository,
            CourseRepository courseRepository,
            UserRepository userRepository,
            CmsCourseAccessGuard courseAccessGuard
    ) {
        this.lessonRepository = lessonRepository;
        this.sectionRepository = sectionRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.courseAccessGuard = courseAccessGuard;
    }

    @Transactional(readOnly = true)
    public List<LessonAdminDto> listLessons(UUID sectionId) {
        User currentUser = resolveCurrentUser();
        loadSectionForRead(sectionId, currentUser);
        return lessonRepository.findAllByCourseSectionIdOrderByDisplayOrderAsc(sectionId)
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public LessonAdminDto createLesson(UUID sectionId, CreateLessonRequest request) {
        User currentUser = resolveCurrentUser();
        loadSectionForMutation(sectionId, currentUser);

        Lesson lesson = new Lesson(
                UUID.randomUUID(),
                sectionId,
                request.title(),
                request.summary(),
                request.lessonType(),
                request.estimatedDurationMinutes(),
                request.displayOrder(),
                request.preview(),
                request.contentUrl(),
                request.contentBody()
        );

        return toDto(lessonRepository.save(lesson));
    }

    @Transactional
    public LessonAdminDto updateLesson(UUID sectionId, UUID lessonId, UpdateLessonRequest request) {
        User currentUser = resolveCurrentUser();
        loadSectionForMutation(sectionId, currentUser);

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));

        // Guard against cross-section lesson manipulation via a mismatched sectionId path segment.
        if (!lesson.getCourseSectionId().equals(sectionId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found in this section");
        }

        lesson.update(
                request.title(), request.summary(), request.lessonType(),
                request.estimatedDurationMinutes(), request.displayOrder(),
                request.preview(), request.contentUrl(), request.contentBody()
        );
        return toDto(lesson);
    }

    @Transactional
    public void deleteLesson(UUID sectionId, UUID lessonId) {
        User currentUser = resolveCurrentUser();
        loadSectionForMutation(sectionId, currentUser);

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));

        if (!lesson.getCourseSectionId().equals(sectionId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found in this section");
        }

        lessonRepository.delete(lesson);
    }

    /** Resolves the parent course from the section and verifies the caller owns it. */
    private CourseSection loadSectionForMutation(UUID sectionId, User currentUser) {
        CourseSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));

        courseRepository.findById(section.getCourseId()).ifPresent(course -> {
            boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;
            boolean isOwner = course.getCreatedByUserId() != null
                    && course.getCreatedByUserId().equals(currentUser.getId());
            if (!isAdmin && !isOwner) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not the course owner");
            }
        });

        return section;
    }

    private CourseSection loadSectionForRead(UUID sectionId, User currentUser) {
        CourseSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));
        // Listing a section's lessons exposes unpublished content (contentBody/contentUrl), so
        // reads require the same ownership scope as mutations, resolved through section → course.
        courseRepository.findById(section.getCourseId())
                .ifPresent(course -> courseAccessGuard.requireReadAccess(currentUser, course));
        return section;
    }

    private LessonAdminDto toDto(Lesson l) {
        return new LessonAdminDto(
                l.getId(), l.getCourseSectionId(), l.getTitle(), l.getSummary(),
                l.getLessonType(), l.getEstimatedDurationMinutes(), l.getDisplayOrder(),
                l.isPreview(), l.getContentUrl(), l.getContentBody(),
                l.getCreatedAt(), l.getUpdatedAt()
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
