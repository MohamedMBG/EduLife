package com.edulife.courses;

import com.edulife.common.error.ApiErrorWriter;
import com.edulife.common.error.GlobalApiExceptionHandler;
import com.edulife.courses.controller.CourseController;
import com.edulife.courses.dto.CourseDetailDto;
import com.edulife.courses.dto.CourseSectionDto;
import com.edulife.courses.dto.CourseSummaryDto;
import com.edulife.courses.dto.LessonSummaryDto;
import com.edulife.courses.service.CourseService;
import com.edulife.security.SecurityConfig;
import com.edulife.users.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CourseController.class)
@Import({SecurityConfig.class, ApiErrorWriter.class, GlobalApiExceptionHandler.class})
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CourseService courseService;

    @MockBean
    private FirebaseAuth firebaseAuth;

    @MockBean
    private UserRepository userRepository;

    @Test
    void returnsPaginatedPublishedCoursesWhenFirebaseTokenIsValid() throws Exception {
        mockValidFirebaseToken();

        CourseSummaryDto course = new CourseSummaryDto(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "math-bac-sm-algebra-foundations",
                "Math Bac SM - Algebra Foundations",
                "A structured algebra refresher for Moroccan Bac Sciences Math students.",
                "BEGINNER",
                "fr",
                "https://images.unsplash.com/photo-1635070041078-e363dbe005cb?w=800&q=80",
                Instant.parse("2026-05-01T10:00:00Z")
        );

        // Keep the response paginated so Android can rely on the same structure for list rendering.
        given(courseService.getPublishedCourses(eq("BEGINNER"), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(course), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/courses")
                        .param("category", "BEGINNER")
                        .param("page", "0")
                        .param("size", "20")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.content[0].slug").value("math-bac-sm-algebra-foundations"))
                .andExpect(jsonPath("$.content[0].title").value("Math Bac SM - Algebra Foundations"))
                .andExpect(jsonPath("$.content[0].level").value("BEGINNER"))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void rejectsMissingTokenOnCourseListBecauseCoursesStayProtected() throws Exception {
        // Sprint 2 and the current SecurityConfig keep course discovery inside the authenticated learner flow.
        mockMvc.perform(get("/api/v1/courses"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(firebaseAuth);
        verifyNoInteractions(courseService);
    }

    @Test
    void returnsEmptyPaginatedListWhenNoCoursesExist() throws Exception {
        mockValidFirebaseToken();

        given(courseService.getPublishedCourses(eq(null), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/courses")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void returnsCourseDetailWithSectionsAndLessons() throws Exception {
        mockValidFirebaseToken();

        UUID courseId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        CourseDetailDto detail = new CourseDetailDto(
                courseId,
                "math-bac-sm-algebra-foundations",
                "Math Bac SM - Algebra Foundations",
                "A structured algebra refresher for Moroccan Bac Sciences Math students.",
                "Build core confidence in equations, functions, and algebraic methods used across Bac Sciences Math coursework.",
                "BEGINNER",
                "fr",
                "https://images.unsplash.com/photo-1635070041078-e363dbe005cb?w=800&q=80",
                Instant.parse("2026-05-01T10:00:00Z"),
                List.of(
                        new CourseSectionDto(
                                UUID.fromString("11111111-aaaa-aaaa-aaaa-111111111111"),
                                "Algebra Basics",
                                "Start with core algebra language and operations.",
                                1,
                                List.of(
                                        new LessonSummaryDto(
                                                UUID.fromString("11111111-aaaa-0000-0000-111111111111"),
                                                "Understanding Algebraic Expressions",
                                                "Identify variables, constants, and operations in simple expressions.",
                                                "VIDEO",
                                                12,
                                                1,
                                                true
                                        )
                                )
                        )
                )
        );

        given(courseService.getPublishedCourseDetail(courseId)).willReturn(detail);

        mockMvc.perform(get("/api/v1/courses/{courseId}", courseId)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.slug").value("math-bac-sm-algebra-foundations"))
                .andExpect(jsonPath("$.sections[0].title").value("Algebra Basics"))
                .andExpect(jsonPath("$.sections[0].displayOrder").value(1))
                .andExpect(jsonPath("$.sections[0].lessons[0].title").value("Understanding Algebraic Expressions"))
                .andExpect(jsonPath("$.sections[0].lessons[0].lessonType").value("VIDEO"))
                .andExpect(jsonPath("$.sections[0].lessons[0].preview").value(true));
    }

    @Test
    void returnsNotFoundWhenCourseDetailUsesFakeId() throws Exception {
        mockValidFirebaseToken();

        UUID fakeCourseId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        given(courseService.getPublishedCourseDetail(fakeCourseId))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        mockMvc.perform(get("/api/v1/courses/{courseId}", fakeCourseId)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Course not found"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    private void mockValidFirebaseToken() throws Exception {
        FirebaseToken decodedToken = org.mockito.Mockito.mock(FirebaseToken.class);
        given(firebaseAuth.verifyIdToken("valid-token")).willReturn(decodedToken);
        given(decodedToken.getUid()).willReturn("firebase-uid-123");
        given(decodedToken.getEmail()).willReturn("student@edulife.test");
        given(decodedToken.isEmailVerified()).willReturn(true);
    }
}
