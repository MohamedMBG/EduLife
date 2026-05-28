package com.edulife.enrollments;

import com.edulife.common.error.ApiErrorWriter;
import com.edulife.common.error.GlobalApiExceptionHandler;
import com.edulife.enrollments.controller.EnrollmentController;
import com.edulife.enrollments.dto.EnrolledCourseDto;
import com.edulife.enrollments.dto.EnrollmentResponse;
import com.edulife.enrollments.service.EnrollmentService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EnrollmentController.class)
@Import({SecurityConfig.class, ApiErrorWriter.class, GlobalApiExceptionHandler.class})
class EnrollmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EnrollmentService enrollmentService;

    @MockBean
    private FirebaseAuth firebaseAuth;

    @MockBean
    private UserRepository userRepository;

    // ── POST /api/v1/enrollments ──────────────────────────────────────────────

    @Test
    void rejectsEnrollRequestWithNoToken() throws Exception {
        mockMvc.perform(post("/api/v1/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":\"11111111-1111-1111-1111-111111111111\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(enrollmentService);
    }

    @Test
    void returnsConflictWhenAlreadyEnrolled() throws Exception {
        mockValidFirebaseToken();
        given(enrollmentService.enroll(any(UUID.class)))
                .willThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Already enrolled in this course"));

        mockMvc.perform(post("/api/v1/enrollments")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":\"11111111-1111-1111-1111-111111111111\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Already enrolled in this course"));
    }

    @Test
    void returnsNotFoundWhenCourseDoesNotExist() throws Exception {
        mockValidFirebaseToken();
        given(enrollmentService.enroll(any(UUID.class)))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        mockMvc.perform(post("/api/v1/enrollments")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":\"99999999-9999-9999-9999-999999999999\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Course not found"));
    }

    @Test
    void createsEnrollmentSuccessfully() throws Exception {
        mockValidFirebaseToken();
        UUID courseId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID enrollmentId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        given(enrollmentService.enroll(courseId))
                .willReturn(new EnrollmentResponse(enrollmentId, courseId, Instant.parse("2026-05-24T10:00:00Z"), "ACTIVE"));

        mockMvc.perform(post("/api/v1/enrollments")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":\"11111111-1111-1111-1111-111111111111\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.enrollmentId").value("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
                .andExpect(jsonPath("$.courseId").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    // ── DELETE /api/v1/enrollments/{id} ──────────────────────────────────────

    @Test
    void rejectsUnenrollRequestWithNoToken() throws Exception {
        UUID enrollmentId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        mockMvc.perform(delete("/api/v1/enrollments/{id}", enrollmentId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(enrollmentService);
    }

    @Test
    void returnsForbiddenWhenUnenrollingAnotherUsersEnrollment() throws Exception {
        mockValidFirebaseToken();
        UUID enrollmentId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the owner of this enrollment"))
                .given(enrollmentService).unenroll(enrollmentId);

        mockMvc.perform(delete("/api/v1/enrollments/{id}", enrollmentId)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("You are not the owner of this enrollment"));
    }

    @Test
    void returnsNotFoundWhenEnrollmentDoesNotExist() throws Exception {
        mockValidFirebaseToken();
        UUID enrollmentId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Enrollment not found"))
                .given(enrollmentService).unenroll(enrollmentId);

        mockMvc.perform(delete("/api/v1/enrollments/{id}", enrollmentId)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Enrollment not found"));
    }

    @Test
    void unenrollsSuccessfullyReturningNoContent() throws Exception {
        mockValidFirebaseToken();
        UUID enrollmentId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        willDoNothing().given(enrollmentService).unenroll(enrollmentId);

        mockMvc.perform(delete("/api/v1/enrollments/{id}", enrollmentId)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNoContent());
    }

    // ── GET /api/v1/enrollments/me ────────────────────────────────────────────

    @Test
    void rejectsMyEnrollmentsRequestWithNoToken() throws Exception {
        mockMvc.perform(get("/api/v1/enrollments/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(enrollmentService);
    }

    @Test
    void returnsEnrolledCoursesWithImageUrlForAuthenticatedUser() throws Exception {
        mockValidFirebaseToken();
        UUID enrollmentId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID courseId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        given(enrollmentService.getMyEnrollments()).willReturn(List.of(
                new EnrolledCourseDto(
                        enrollmentId,
                        courseId,
                        "math-bac-sm-algebra-foundations",
                        "Math Bac SM - Algebra Foundations",
                        "A structured algebra refresher for Moroccan Bac Sciences Math students.",
                        "BEGINNER",
                        "fr",
                        "https://images.unsplash.com/photo-1635070041078-e363dbe005cb?w=800&q=80",
                        Instant.parse("2026-05-24T10:00:00Z")
                )
        ));

        mockMvc.perform(get("/api/v1/enrollments/me")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].enrollmentId").value("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
                .andExpect(jsonPath("$[0].courseId").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$[0].title").value("Math Bac SM - Algebra Foundations"))
                .andExpect(jsonPath("$[0].level").value("BEGINNER"))
                .andExpect(jsonPath("$[0].imageUrl").value("https://images.unsplash.com/photo-1635070041078-e363dbe005cb?w=800&q=80"))
                .andExpect(jsonPath("$[0].enrolledAt").exists());
    }

    @Test
    void returnsEmptyListWhenUserHasNoEnrollments() throws Exception {
        mockValidFirebaseToken();
        given(enrollmentService.getMyEnrollments()).willReturn(List.of());

        mockMvc.perform(get("/api/v1/enrollments/me")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    private void mockValidFirebaseToken() throws Exception {
        FirebaseToken decodedToken = org.mockito.Mockito.mock(FirebaseToken.class);
        given(firebaseAuth.verifyIdToken("valid-token")).willReturn(decodedToken);
        given(decodedToken.getUid()).willReturn("firebase-uid-123");
        given(decodedToken.getEmail()).willReturn("student@edulife.test");
        given(decodedToken.isEmailVerified()).willReturn(true);
    }
}
