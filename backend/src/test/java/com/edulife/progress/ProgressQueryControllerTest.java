package com.edulife.progress;

import com.edulife.common.error.ApiErrorWriter;
import com.edulife.common.error.GlobalApiExceptionHandler;
import com.edulife.progress.controller.ProgressQueryController;
import com.edulife.progress.dto.CourseProgressDto;
import com.edulife.progress.service.ProgressService;
import com.edulife.security.SecurityConfig;
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
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProgressQueryController.class)
@Import({SecurityConfig.class, ApiErrorWriter.class, GlobalApiExceptionHandler.class})
class ProgressQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProgressService progressService;

    @MockBean
    private FirebaseAuth firebaseAuth;

    private static final UUID COURSE_ID  = UUID.fromString("aaaaaaaa-0000-0000-0000-aaaaaaaaaaaa");
    private static final UUID SECTION_ID = UUID.fromString("cccccccc-0000-0000-0000-cccccccccccc");
    private static final UUID LESSON_1   = UUID.fromString("11111111-0000-0000-0000-111111111111");
    private static final UUID LESSON_2   = UUID.fromString("22222222-0000-0000-0000-222222222222");

    @Test
    void rejectsGetCourseProgressWithNoToken() throws Exception {
        mockMvc.perform(get("/api/v1/progress/courses/{courseId}", COURSE_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void returnsForbiddenWhenNotEnrolled() throws Exception {
        mockValidFirebaseToken();
        given(progressService.getCourseProgress(COURSE_ID))
                .willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Enroll in this course to view progress"));

        mockMvc.perform(get("/api/v1/progress/courses/{courseId}", COURSE_ID)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Enroll in this course to view progress"));
    }

    @Test
    void returnsCourseProgressWithPerLessonCompletionFlags() throws Exception {
        mockValidFirebaseToken();
        Instant completedAt = Instant.parse("2026-05-25T10:00:00Z");

        CourseProgressDto dto = new CourseProgressDto(
                COURSE_ID, 1, 2, 50.0,
                List.of(new CourseProgressDto.SectionProgressDto(
                        SECTION_ID, "Algebra Basics", 1,
                        List.of(
                                new CourseProgressDto.LessonProgressDto(
                                        LESSON_1, "Lesson One", "VIDEO", 12, 1, false, true, completedAt),
                                new CourseProgressDto.LessonProgressDto(
                                        LESSON_2, "Lesson Two", "ARTICLE", 5, 2, false, false, null)
                        )
                ))
        );
        given(progressService.getCourseProgress(COURSE_ID)).willReturn(dto);

        mockMvc.perform(get("/api/v1/progress/courses/{courseId}", COURSE_ID)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(COURSE_ID.toString()))
                .andExpect(jsonPath("$.completedLessons").value(1))
                .andExpect(jsonPath("$.totalLessons").value(2))
                .andExpect(jsonPath("$.percentComplete").value(50.0))
                .andExpect(jsonPath("$.sections[0].sectionId").value(SECTION_ID.toString()))
                .andExpect(jsonPath("$.sections[0].title").value("Algebra Basics"))
                .andExpect(jsonPath("$.sections[0].lessons[0].lessonId").value(LESSON_1.toString()))
                .andExpect(jsonPath("$.sections[0].lessons[0].completed").value(true))
                .andExpect(jsonPath("$.sections[0].lessons[0].completedAt").exists())
                .andExpect(jsonPath("$.sections[0].lessons[1].lessonId").value(LESSON_2.toString()))
                .andExpect(jsonPath("$.sections[0].lessons[1].completed").value(false));
    }

    @Test
    void returnsZeroPercentWhenNoLessonsCompleted() throws Exception {
        mockValidFirebaseToken();

        CourseProgressDto dto = new CourseProgressDto(
                COURSE_ID, 0, 2, 0.0,
                List.of(new CourseProgressDto.SectionProgressDto(
                        SECTION_ID, "Algebra Basics", 1,
                        List.of(
                                new CourseProgressDto.LessonProgressDto(
                                        LESSON_1, "L1", "VIDEO", 10, 1, false, false, null),
                                new CourseProgressDto.LessonProgressDto(
                                        LESSON_2, "L2", "VIDEO", 10, 2, false, false, null)
                        )
                ))
        );
        given(progressService.getCourseProgress(COURSE_ID)).willReturn(dto);

        mockMvc.perform(get("/api/v1/progress/courses/{courseId}", COURSE_ID)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.percentComplete").value(0.0))
                .andExpect(jsonPath("$.completedLessons").value(0))
                .andExpect(jsonPath("$.sections[0].lessons[0].completed").value(false))
                .andExpect(jsonPath("$.sections[0].lessons[1].completed").value(false));
    }

    private void mockValidFirebaseToken() throws Exception {
        FirebaseToken decodedToken = mock(FirebaseToken.class);
        given(firebaseAuth.verifyIdToken("valid-token")).willReturn(decodedToken);
        given(decodedToken.getUid()).willReturn("firebase-uid-123");
        given(decodedToken.getEmail()).willReturn("student@edulife.test");
        given(decodedToken.isEmailVerified()).willReturn(true);
    }
}
