package com.edulife.advisor;

import com.edulife.advisor.dto.CourseContextDto;
import com.edulife.advisor.service.CourseContextBuilder;
import com.edulife.courses.entity.Course;
import com.edulife.courses.model.CourseStatus;
import com.edulife.courses.repository.CourseRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CourseContextBuilderTest {

    @Mock
    private CourseRepository courseRepository;

    private CourseContextBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new CourseContextBuilder(courseRepository);
    }

    @Test
    void emptyWhenNoPublishedCourses() {
        given(courseRepository.countByStatus(CourseStatus.PUBLISHED)).willReturn(0L);

        List<CourseContextDto> result = builder.build("learn java");

        assertThat(result).isEmpty();
    }

    @Test
    void queriesOnlyPublishedStatus() {
        given(courseRepository.countByStatus(CourseStatus.PUBLISHED)).willReturn(1L);
        given(courseRepository.findAllByStatus(eq(CourseStatus.PUBLISHED), any()))
                .willReturn(new PageImpl<>(List.of(publishedCourse("Java Basics", "Intro", "BEGINNER"))));

        builder.build("java");

        verify(courseRepository).findAllByStatus(eq(CourseStatus.PUBLISHED), any());
    }

    @Test
    void projectsFieldsCorrectly() {
        Course course = publishedCourse("Python ML", "Machine learning with Python", "ADVANCED");
        given(courseRepository.countByStatus(CourseStatus.PUBLISHED)).willReturn(1L);
        given(courseRepository.findAllByStatus(eq(CourseStatus.PUBLISHED), any()))
                .willReturn(new PageImpl<>(List.of(course)));

        List<CourseContextDto> result = builder.build("python");

        assertThat(result).hasSize(1);
        CourseContextDto dto = result.get(0);
        assertThat(dto.id()).isEqualTo(course.getId());
        assertThat(dto.title()).isEqualTo("Python ML");
        assertThat(dto.shortDescription()).isEqualTo("Machine learning with Python");
        assertThat(dto.level()).isEqualTo("ADVANCED");
        assertThat(dto.languageCode()).isEqualTo("en");
        assertThat(dto.tags()).isEmpty();
    }

    @Test
    void returnsUpToCapWhenUnderLimit() {
        List<Course> courses = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            courses.add(publishedCourse("Course " + i, "Description " + i, "BEGINNER"));
        }
        given(courseRepository.countByStatus(CourseStatus.PUBLISHED)).willReturn(30L);
        given(courseRepository.findAllByStatus(eq(CourseStatus.PUBLISHED), any()))
                .willReturn(new PageImpl<>(courses));

        List<CourseContextDto> result = builder.build("learn");

        assertThat(result).hasSize(30);
    }

    @Test
    void capsAtFiftyWhenOverLimit() {
        List<Course> courses = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            courses.add(publishedCourse("Course " + i, "Description " + i, "BEGINNER"));
        }
        given(courseRepository.countByStatus(CourseStatus.PUBLISHED)).willReturn(100L);
        given(courseRepository.findAllByStatus(eq(CourseStatus.PUBLISHED), any()))
                .willReturn(new PageImpl<>(courses));

        List<CourseContextDto> result = builder.build("learn");

        assertThat(result).hasSize(CourseContextBuilder.CATALOG_CAP);
    }

    @Test
    void keywordFilteringRanksMatchingCoursesFirst() {
        List<Course> courses = new ArrayList<>();
        for (int i = 0; i < 80; i++) {
            courses.add(publishedCourse("Generic Course " + i, "A course about general topics", "BEGINNER"));
        }
        for (int i = 0; i < 20; i++) {
            courses.add(publishedCourse("Python Course " + i, "Learn Python programming", "INTERMEDIATE"));
        }
        given(courseRepository.countByStatus(CourseStatus.PUBLISHED)).willReturn(100L);
        given(courseRepository.findAllByStatus(eq(CourseStatus.PUBLISHED), any()))
                .willReturn(new PageImpl<>(courses));

        List<CourseContextDto> result = builder.build("python programming");

        assertThat(result).hasSize(CourseContextBuilder.CATALOG_CAP);
        long pythonCount = result.stream().filter(c -> c.title().contains("Python")).count();
        assertThat(pythonCount).isEqualTo(20);
    }

    @Test
    void keywordFilteringFallsBackToNaturalOrderWhenNoMatch() {
        List<Course> courses = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            courses.add(publishedCourse("Course " + i, "Description " + i, "BEGINNER"));
        }
        given(courseRepository.countByStatus(CourseStatus.PUBLISHED)).willReturn(100L);
        given(courseRepository.findAllByStatus(eq(CourseStatus.PUBLISHED), any()))
                .willReturn(new PageImpl<>(courses));

        List<CourseContextDto> result = builder.build("xyzzy");

        assertThat(result).hasSize(CourseContextBuilder.CATALOG_CAP);
    }

    private Course publishedCourse(String title, String description, String level) {
        Course course = new Course(
                UUID.randomUUID(),
                title.toLowerCase().replace(' ', '-') + "-" + UUID.randomUUID(),
                title,
                description,
                "Full " + description,
                "en",
                level,
                null,
                UUID.randomUUID()
        );
        course.publish();
        return course;
    }
}
