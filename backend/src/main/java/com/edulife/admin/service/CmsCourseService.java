package com.edulife.admin.service;

import com.edulife.admin.dto.CourseAdminDto;
import com.edulife.admin.dto.CreateCourseRequest;
import com.edulife.admin.dto.UpdateCourseRequest;
import com.edulife.courses.dto.CourseCoverUploadResponse;
import com.edulife.courses.entity.Course;
import com.edulife.courses.repository.CourseRepository;
import com.edulife.courses.storage.CloudinaryStorageService;
import com.edulife.courses.storage.CloudinaryUploadResult;
import com.edulife.groups.repository.GroupMemberRepository;
import com.edulife.security.FirebaseAuthentication;
import com.edulife.users.entity.User;
import com.edulife.users.model.UserRole;
import com.edulife.users.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * CMS course lifecycle management. TEACHERs can create and edit their own courses;
 * GROUP_ADMINs review and publish courses authored by teachers inside their groups;
 * standalone teachers remain outside any group review queue, so only platform ADMINs
 * can approve or reject their course publication requests.
 */
@Service
public class CmsCourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final CloudinaryStorageService cloudinaryStorage;

    public CmsCourseService(
            CourseRepository courseRepository,
            UserRepository userRepository,
            GroupMemberRepository groupMemberRepository,
            CloudinaryStorageService cloudinaryStorage
    ) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.cloudinaryStorage = cloudinaryStorage;
    }

    @Transactional(readOnly = true)
    // Teachers see their own courses; group admins see courses authored by teachers in their
    // groups; platform admins see all pending uploads, including standalone teachers with no
    // institute group. Scoping stays in the service so controllers avoid business logic.
    public List<CourseAdminDto> listMyCourses() {
        User currentUser = resolveCurrentUser();
        List<Course> courses = switch (currentUser.getRole()) {
            case ADMIN -> courseRepository.findAll();
            case GROUP_ADMIN -> courseRepository.findAllByCreatedByUserIdIn(
                    groupMemberRepository.findMemberUserIdsManagedBy(currentUser.getId()));
            default -> courseRepository.findAllByCreatedByUserId(currentUser.getId());
        };
        return toDtos(courses);
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
    // Teachers cannot self-publish. ADMIN can publish anything; a GROUP_ADMIN approves only
    // courses authored by teachers who are members of one of their groups. A teacher who
    // chooses to stay independent is therefore reviewed only by the platform admin.
    public CourseAdminDto publishCourse(UUID courseId) {
        User currentUser = resolveCurrentUser();
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        requirePublishAuthority(currentUser, course);
        course.publish();
        return toDto(course);
    }

    private void requirePublishAuthority(User user, Course course) {
        if (user.getRole() == UserRole.ADMIN) {
            return;
        }
        boolean managesAuthor = user.getRole() == UserRole.GROUP_ADMIN
                && course.getCreatedByUserId() != null
                && groupMemberRepository.existsMemberManagedBy(user.getId(), course.getCreatedByUserId());
        if (!managesAuthor) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only approve courses from teachers in your groups");
        }
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

    @Transactional
    public CourseCoverUploadResponse uploadCoverImage(UUID courseId, MultipartFile file) {
        User currentUser = resolveCurrentUser();
        Course course = loadCourseForCoverUpload(courseId, currentUser);

        String oldPublicId = course.getCoverImagePublicId();
        CloudinaryUploadResult result = cloudinaryStorage.store(courseId, file);

        course.updateMetadata(
                course.getTitle(), course.getShortDescription(), course.getDescription(),
                course.getLanguageCode(), course.getLevel(), result.secureUrl()
        );
        course.setCoverImagePublicId(result.publicId());

        cloudinaryStorage.deleteByPublicId(oldPublicId);

        return new CourseCoverUploadResponse(
                courseId, result.secureUrl(), result.publicId(),
                "Course cover image updated successfully");
    }

    private Course loadCourseForCoverUpload(UUID courseId, User currentUser) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        if (currentUser.getRole() == UserRole.ADMIN) {
            return course;
        }

        boolean isOwner = course.getCreatedByUserId() != null
                && course.getCreatedByUserId().equals(currentUser.getId());
        if (isOwner) {
            return course;
        }

        boolean managesAuthor = currentUser.getRole() == UserRole.GROUP_ADMIN
                && course.getCreatedByUserId() != null
                && groupMemberRepository.existsMemberManagedBy(currentUser.getId(), course.getCreatedByUserId());
        if (managesAuthor) {
            return course;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "You do not have permission to update this course's cover image");
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

    private List<CourseAdminDto> toDtos(List<Course> courses) {
        // Batch-resolve author emails so the list endpoint stays one query instead of N+1.
        Map<UUID, String> emailsById = userRepository
                .findAllById(courses.stream()
                        .map(Course::getCreatedByUserId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(User::getId, User::getEmail, (a, b) -> a));
        return courses.stream()
                .map(c -> toDto(c, emailsById.get(c.getCreatedByUserId())))
                .toList();
    }

    private CourseAdminDto toDto(Course c) {
        String createdByEmail = c.getCreatedByUserId() == null
                ? null
                : userRepository.findById(c.getCreatedByUserId()).map(User::getEmail).orElse(null);
        return toDto(c, createdByEmail);
    }

    private CourseAdminDto toDto(Course c, String createdByEmail) {
        return new CourseAdminDto(
                c.getId(), c.getSlug(), c.getTitle(), c.getShortDescription(),
                c.getDescription(), c.getLanguageCode(), c.getLevel(), c.getImageUrl(),
                c.getStatus(), c.getPublishedAt(), c.getCreatedByUserId(), createdByEmail,
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
