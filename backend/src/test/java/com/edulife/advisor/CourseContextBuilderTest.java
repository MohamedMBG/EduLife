package com.edulife.advisor;

import com.edulife.advisor.dto.CourseContextDto;
import com.edulife.advisor.service.CourseContextBuilder;
import com.edulife.courses.entity.Course;
import com.edulife.courses.model.CourseStatus;
import com.edulife.courses.repository.CourseSectionRepository;
import com.edulife.courses.repository.CourseRepository;
import com.edulife.courses.repository.LessonRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CourseContextBuilderTest {

    @Mock private CourseRepository courseRepository;
    @Mock private CourseSectionRepository sectionRepository;
    @Mock private LessonRepository lessonRepository;

    private CourseContextBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new CourseContextBuilder(courseRepository, sectionRepository, lessonRepository);
    }

    @Test
    void emptyWhenNoPublishedCourses() {
        given(courseRepository.findAllByStatus(CourseStatus.PUBLISHED)).willReturn(List.of());

        List<CourseContextDto> result = builder.build("learn java");

        assertThat(result).isEmpty();
    }

    @Test
    void queriesOnlyPublishedStatus() {
        given(courseRepository.findAllByStatus(CourseStatus.PUBLISHED))
                .willReturn(List.of(publishedCourse("Java Basics", "Intro", "BEGINNER")));
        given(sectionRepository.findAllByCourseIdOrderByDisplayOrderAsc(any())).willReturn(List.of());

        builder.build("java");

        verify(courseRepository).findAllByStatus(CourseStatus.PUBLISHED);
    }

    @Test
    void projectsFieldsCorrectly() {
        Course course = publishedCourse("Python ML", "Machine learning with Python", "ADVANCED");
        given(courseRepository.findAllByStatus(CourseStatus.PUBLISHED))
                .willReturn(List.of(course));
        given(sectionRepository.findAllByCourseIdOrderByDisplayOrderAsc(any())).willReturn(List.of());

        List<CourseContextDto> result = builder.build("python");

        assertThat(result).hasSize(1);
        CourseContextDto dto = result.get(0);
        assertThat(dto.id()).isEqualTo(course.getId());
        assertThat(dto.title()).isEqualTo("Python ML");
        assertThat(dto.shortDescription()).isEqualTo("Machine learning with Python");
        assertThat(dto.description()).isEqualTo("Full Machine learning with Python");
        assertThat(dto.level()).isEqualTo("ADVANCED");
        assertThat(dto.languageCode()).isEqualTo("en");
        assertThat(dto.tags()).isEmpty();
        assertThat(dto.lessonTitles()).isEmpty();
    }

    @Test
    void returnsAllPublishedCourses() {
        List<Course> courses = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            courses.add(publishedCourse("Course " + i, "Description " + i, "BEGINNER"));
        }
        given(courseRepository.findAllByStatus(CourseStatus.PUBLISHED)).willReturn(courses);
        given(sectionRepository.findAllByCourseIdOrderByDisplayOrderAsc(any())).willReturn(List.of());

        List<CourseContextDto> result = builder.build("learn");

        assertThat(result).hasSize(100);
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
