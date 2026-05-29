package com.edulife.admin.service;

import com.edulife.admin.dto.CreateSectionRequest;
import com.edulife.admin.dto.SectionAdminDto;
import com.edulife.admin.dto.UpdateSectionRequest;
import com.edulife.courses.entity.Course;
import com.edulife.courses.entity.CourseSection;
import com.edulife.courses.repository.CourseRepository;
import com.edulife.courses.repository.CourseSectionRepository;
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

/** CMS section management. Section mutations require ownership of the parent course. */
@Service
public class CmsSectionService {

    private final CourseRepository courseRepository;
    private final CourseSectionRepository sectionRepository;
    private final UserRepository userRepository;

    public CmsSectionService(
            CourseRepository courseRepository,
            CourseSectionRepository sectionRepository,
            UserRepository userRepository
    ) {
        this.courseRepository = courseRepository;
        this.sectionRepository = sectionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<SectionAdminDto> listSections(UUID courseId) {
        User currentUser = resolveCurrentUser();
        loadCourseForRead(courseId, currentUser);
        return sectionRepository.findAllByCourseIdOrderByDisplayOrderAsc(courseId)
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public SectionAdminDto createSection(UUID courseId, CreateSectionRequest request) {
        User currentUser = resolveCurrentUser();
        loadCourseForMutation(courseId, currentUser);

        CourseSection section = new CourseSection(
                UUID.randomUUID(),
                courseId,
                request.title(),
                request.description(),
                request.displayOrder()
        );

        return toDto(sectionRepository.save(section));
    }

    @Transactional
    public SectionAdminDto updateSection(UUID courseId, UUID sectionId, UpdateSectionRequest request) {
        User currentUser = resolveCurrentUser();
        loadCourseForMutation(courseId, currentUser);

        CourseSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));

        // Guard against cross-course section manipulation via a mismatched courseId path segment.
        if (!section.getCourseId().equals(courseId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found in this course");
        }

        section.update(
                request.title() != null ? request.title() : section.getTitle(),
                request.description() != null ? request.description() : section.getDescription(),
                request.displayOrder()
        );
        return toDto(section);
    }

    @Transactional
    public void deleteSection(UUID courseId, UUID sectionId) {
        User currentUser = resolveCurrentUser();
        loadCourseForMutation(courseId, currentUser);

        CourseSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));

        if (!section.getCourseId().equals(courseId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found in this course");
        }

        // ON DELETE CASCADE in V2 migration will remove child lessons automatically.
        sectionRepository.delete(section);
    }

    private Course loadCourseForRead(UUID courseId, User currentUser) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    }

    private Course loadCourseForMutation(UUID courseId, User currentUser) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;
        boolean isOwner = course.getCreatedByUserId() != null
                && course.getCreatedByUserId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not the course owner");
        }
        return course;
    }

    private SectionAdminDto toDto(CourseSection s) {
        return new SectionAdminDto(
                s.getId(), s.getCourseId(), s.getTitle(), s.getDescription(),
                s.getDisplayOrder(), s.getCreatedAt(), s.getUpdatedAt()
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
