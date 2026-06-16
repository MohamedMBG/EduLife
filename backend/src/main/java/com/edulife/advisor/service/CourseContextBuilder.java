package com.edulife.advisor.service;

import com.edulife.advisor.dto.CourseContextDto;
import com.edulife.courses.entity.Course;
import com.edulife.courses.model.CourseStatus;
import com.edulife.courses.repository.CourseRepository;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class CourseContextBuilder {

    public static final int CATALOG_CAP = 50;
    private static final int FETCH_LIMIT = 200;

    private final CourseRepository courseRepository;

    public CourseContextBuilder(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    // TODO: Cache catalog for 5 minutes once Caffeine is added to pom.xml dependencies.
    public List<CourseContextDto> build(String goal) {
        long total = courseRepository.countByStatus(CourseStatus.PUBLISHED);

        if (total == 0) {
            return List.of();
        }

        if (total <= CATALOG_CAP) {
            return courseRepository
                    .findAllByStatus(CourseStatus.PUBLISHED, PageRequest.of(0, CATALOG_CAP))
                    .stream()
                    .map(this::toDto)
                    .toList();
        }

        List<Course> candidates = courseRepository
                .findAllByStatus(CourseStatus.PUBLISHED, PageRequest.of(0, FETCH_LIMIT))
                .getContent();

        return filterByKeywords(candidates, goal);
    }

    private List<CourseContextDto> filterByKeywords(List<Course> courses, String goal) {
        Set<String> keywords = tokenize(goal);

        if (keywords.isEmpty()) {
            return courses.stream().limit(CATALOG_CAP).map(this::toDto).toList();
        }

        return courses.stream()
                .map(c -> new ScoredCourse(c, score(c, keywords)))
                .sorted(Comparator.comparingInt(ScoredCourse::score).reversed())
                .limit(CATALOG_CAP)
                .map(sc -> toDto(sc.course()))
                .toList();
    }

    private int score(Course course, Set<String> keywords) {
        String searchText = buildSearchText(course);
        return (int) keywords.stream().filter(searchText::contains).count();
    }

    private String buildSearchText(Course course) {
        StringBuilder sb = new StringBuilder();
        if (course.getTitle() != null) sb.append(course.getTitle().toLowerCase()).append(' ');
        if (course.getShortDescription() != null) sb.append(course.getShortDescription().toLowerCase()).append(' ');
        if (course.getLevel() != null) sb.append(course.getLevel().toLowerCase());
        return sb.toString();
    }

    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) return Set.of();
        return Arrays.stream(text.toLowerCase().split("[^a-z0-9]+"))
                .filter(w -> w.length() > 2)
                .collect(Collectors.toSet());
    }

    private CourseContextDto toDto(Course course) {
        return new CourseContextDto(
                course.getId(),
                course.getTitle(),
                course.getShortDescription(),
                course.getLevel(),
                course.getLanguageCode(),
                List.of()
        );
    }

    private record ScoredCourse(Course course, int score) {}
}
