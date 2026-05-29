package com.edulife.courses.service;

import com.edulife.courses.dto.CourseDetailDto;
import com.edulife.courses.dto.CourseSectionDto;
import com.edulife.courses.dto.CourseSummaryDto;
import com.edulife.courses.dto.LessonSummaryDto;
import com.edulife.courses.entity.Course;
import com.edulife.courses.entity.CourseSection;
import com.edulife.courses.entity.Lesson;
import com.edulife.courses.model.CourseStatus;
import com.edulife.courses.repository.CourseRepository;
import com.edulife.courses.repository.CourseSectionRepository;
import com.edulife.courses.repository.LessonRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class CourseService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final CourseRepository courseRepository;
    private final CourseSectionRepository courseSectionRepository;
    private final LessonRepository lessonRepository;

    public CourseService(
            CourseRepository courseRepository,
            CourseSectionRepository courseSectionRepository,
            LessonRepository lessonRepository
    ) {
        this.courseRepository = courseRepository;
        this.courseSectionRepository = courseSectionRepository;
        this.lessonRepository = lessonRepository;
    }

    @Transactional(readOnly = true)
    // Read-only transaction keeps the discovery slice safe from accidental writes while still
    // allowing the service to orchestrate repositories and DTO mapping in one place.
    public Page<CourseSummaryDto> getPublishedCourses(String category, String query, Pageable pageable) {
        Pageable sanitizedPageable = sanitizePageable(pageable);

        // Full-text search takes priority over category filter when both are provided.
        // Using plainto_tsquery on the Postgres side handles multi-word phrases correctly.
        if (query != null && !query.isBlank()) {
            return courseRepository.searchPublished(query.trim(), sanitizedPageable)
                    .map(this::toCourseSummary);
        }

        // The current Sprint 2 schema stores this catalog bucket in the `level` column.
        // The API uses `category` now so Android can move forward without waiting for a
        // dedicated course-category table and migration.
        Page<Course> courses = (category != null && !category.isBlank())
                ? courseRepository.findAllByStatusAndLevel(
                        CourseStatus.PUBLISHED,
                        category.trim(),
                        sanitizedPageable
                )
                : courseRepository.findAllByStatus(CourseStatus.PUBLISHED, sanitizedPageable);

        // Spring Page already provides content plus pagination metadata, so no custom wrapper
        // is needed yet for the Android course list contract.
        return courses.map(this::toCourseSummary);
    }

    @Transactional(readOnly = true)
    // Detail reads remain published-only so learners cannot enumerate draft content simply by
    // guessing IDs before teacher/admin flows are added later.
    public CourseDetailDto getPublishedCourseDetail(UUID courseId) {
        Course course = courseRepository.findByIdAndStatus(courseId, CourseStatus.PUBLISHED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        List<CourseSectionDto> sections = courseSectionRepository.findAllByCourseIdOrderByDisplayOrderAsc(courseId)
                .stream()
                .map(this::toCourseSectionDto)
                .toList();

        return new CourseDetailDto(
                course.getId(),
                course.getSlug(),
                course.getTitle(),
                course.getShortDescription(),
                course.getDescription(),
                course.getLevel(),
                course.getLanguageCode(),
                course.getImageUrl(),
                course.getPublishedAt(),
                sections
        );
    }

    private CourseSummaryDto toCourseSummary(Course course) {
        // DTO mapping keeps entity shape decoupled from the public API contract expected by Android.
        return new CourseSummaryDto(
                course.getId(),
                course.getSlug(),
                course.getTitle(),
                course.getShortDescription(),
                course.getLevel(),
                course.getLanguageCode(),
                course.getImageUrl(),
                course.getPublishedAt()
        );
    }

    private CourseSectionDto toCourseSectionDto(CourseSection section) {
        // Loading lessons section by section keeps the implementation simple and easy to
        // reason about for Sprint 2 while the catalog size remains intentionally small.
        List<LessonSummaryDto> lessons = lessonRepository
                .findAllByCourseSectionIdOrderByDisplayOrderAsc(section.getId())
                .stream()
                .map(this::toLessonSummaryDto)
                .toList();

        return new CourseSectionDto(
                section.getId(),
                section.getTitle(),
                section.getDescription(),
                section.getDisplayOrder(),
                lessons
        );
    }

    private LessonSummaryDto toLessonSummaryDto(Lesson lesson) {
        return new LessonSummaryDto(
                lesson.getId(),
                lesson.getTitle(),
                lesson.getSummary(),
                lesson.getLessonType(),
                lesson.getEstimatedDurationMinutes(),
                lesson.getDisplayOrder(),
                lesson.isPreview()
        );
    }

    private Pageable sanitizePageable(Pageable pageable) {
        int pageNumber = pageable == null ? 0 : Math.max(pageable.getPageNumber(), 0);
        int requestedSize = pageable == null ? DEFAULT_PAGE_SIZE : pageable.getPageSize();
        int safeSize = requestedSize <= 0 ? DEFAULT_PAGE_SIZE : Math.min(requestedSize, MAX_PAGE_SIZE);

        // Discovery order should stay stable for learners, so published date remains the
        // enforced sort even if callers omit sort parameters or request something inconsistent.
        return PageRequest.of(pageNumber, safeSize, Sort.by("publishedAt").descending());
    }
}
