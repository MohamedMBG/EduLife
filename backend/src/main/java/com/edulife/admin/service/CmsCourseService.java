package com.edulife.admin.service;

import com.edulife.admin.dto.CourseAdminDto;
import com.edulife.admin.dto.CreateCourseRequest;
import com.edulife.admin.dto.UpdateCourseRequest;
import com.edulife.courses.entity.Course;
import com.edulife.courses.repository.CourseRepository;
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

/**
 * CMS course lifecycle management. TEACHERs can create and edit their own courses;
 * ADMINs can edit any course and transition status to PUBLISHED or ARCHIVED.
 */
@Service
public class CmsCourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public CmsCourseService(CourseRepository courseRepository, UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    // Teachers only see their own courses; admins see all. Ownership check stays in service
    // so the controller remains free of business logic.
    public List<CourseAdminDto> listMyCourses() {
        User currentUser = resolveCurrentUser();
        List<Course> courses = (currentUser.getRole() == UserRole.ADMIN)
                ? courseRepository.findAll()
                : courseRepository.findAllByCreatedByUserId(currentUser.getId());
        return courses.stream().map(this::toDto).toList();
    }

    @Transactional
    public CourseAdminDto createCourse(CreateCourseRequest request) {
        User currentUser = resolveCurrentUser();

        // Slug is always server-generated to prevent callers from injecting arbitrary values.
        // A short UUID suffix guarantees global uniqueness even for identical titles.
        String slug = slugify(request.title()) + "-" + UUID.randomUUID().toString().substring(0, 8);

        Course course = new Course(
                UUID.randomUUID(),
                slug,
                request.title(),
                request.shortDescription(),
                request.description(),
                request.languageCode(),
                request.level(),
                request.imageUrl(),
                currentUser.getId()
        );

        return toDto(courseRepository.save(course));
    }

    @Transactional
    public CourseAdminDto updateCourse(UUID courseId, UpdateCourseRequest request) {
        User currentUser = resolveCurrentUser();
        Course course = loadCourseForMutation(courseId, currentUser);

        // Only non-null request fields are applied; callers can send a partial update.
        String title = request.title() != null ? request.title() : course.getTitle();
        String shortDesc = request.shortDescription() != null ? request.shortDescription() : course.getShortDescription();
        String desc = request.description() != null ? request.description() : course.getDescription();
        String lang = request.languageCode() != null ? request.languageCode() : course.getLanguageCode();
        String level = request.level() != null ? request.level() : course.getLevel();
        String imageUrl = request.imageUrl() != null ? request.imageUrl() : course.getImageUrl();

        course.updateMetadata(title, shortDesc, desc, lang, level, imageUrl);
        return toDto(course);
    }

    @Transactional
    // Only ADMIN can publish; this prevents teachers from self-publishing without review.
    public CourseAdminDto publishCourse(UUID courseId) {
        User currentUser = resolveCurrentUser();
        requireAdmin(currentUser);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        course.publish();
        return toDto(course);
    }

    @Transactional
    // Only ADMIN can archive; archived courses disappear from learner discovery immediately.
    public CourseAdminDto archiveCourse(UUID courseId) {
        User currentUser = resolveCurrentUser();
        requireAdmin(currentUser);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        course.archive();
        return toDto(course);
    }

    private Course loadCourseForMutation(UUID courseId, User currentUser) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;
        boolean isOwner = course.getCreatedByUserId() != null
                && course.getCreatedByUserId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            // A TEACHER cannot mutate another teacher's course — ownership is the gate.
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not the course owner");
        }
        return course;
    }

    private void requireAdmin(User user) {
        if (user.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required");
        }
    }

    private CourseAdminDto toDto(Course c) {
        return new CourseAdminDto(
                c.getId(), c.getSlug(), c.getTitle(), c.getShortDescription(),
                c.getDescription(), c.getLanguageCode(), c.getLevel(), c.getImageUrl(),
                c.getStatus(), c.getPublishedAt(), c.getCreatedByUserId(),
                c.getCreatedAt(), c.getUpdatedAt()
        );
    }

    /** Converts an arbitrary title into a URL-safe lowercase slug. */
    private static String slugify(String title) {
        return title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("-{2,}", "-");
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
