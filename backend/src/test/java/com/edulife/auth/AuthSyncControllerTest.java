package com.edulife.auth;

import com.edulife.groups.repository.GroupCourseRepository;
import com.edulife.groups.repository.GroupJoinRequestRepository;
import com.edulife.groups.repository.GroupMemberRepository;
import com.edulife.groups.repository.GroupRepository;
import com.edulife.users.repository.UserRepository;
import com.google.firebase.ErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthSyncControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupJoinRequestRepository groupJoinRequestRepository;

    @Autowired
    private GroupCourseRepository groupCourseRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private FirebaseAuth firebaseAuth;

    private static final String SEED_UID = "seed-instructor-edulife";

    @BeforeEach
    void cleanDatabase() {
        groupJoinRequestRepository.deleteAll();
        groupCourseRepository.deleteAll();
        groupMemberRepository.deleteAll();
        groupRepository.deleteAll();
        // Non-CASCADE FKs referencing users — clean before deleting test users.
        jdbcTemplate.execute("DELETE FROM enrollments WHERE user_id IN "
                + "(SELECT id FROM users WHERE firebase_uid IS DISTINCT FROM '" + SEED_UID + "')");
        jdbcTemplate.execute("DELETE FROM teacher_requests WHERE user_id IN "
                + "(SELECT id FROM users WHERE firebase_uid IS DISTINCT FROM '" + SEED_UID + "')");
        jdbcTemplate.execute("DELETE FROM advisor_log WHERE user_id IN "
                + "(SELECT id FROM users WHERE firebase_uid IS DISTINCT FROM '" + SEED_UID + "')");
        // Delete only test-created users. The Flyway V24 seed instructor is referenced
        // by courses.created_by_user_id and must survive.
        jdbcTemplate.execute("DELETE FROM users WHERE firebase_uid IS DISTINCT FROM '" + SEED_UID + "'");
    }

    private long testUserCount() {
        return userRepository.findAll().stream()
                .filter(u -> !SEED_UID.equals(u.getFirebaseUid()))
                .count();
    }

    @Test
    void syncCreatesUserWhenFirebaseTokenIsValid() throws Exception {
        FirebaseToken decodedToken = org.mockito.Mockito.mock(FirebaseToken.class);

        given(firebaseAuth.verifyIdToken("valid-token")).willReturn(decodedToken);
        given(decodedToken.getUid()).willReturn("firebase-uid-123");
        given(decodedToken.getEmail()).willReturn("student@edulife.test");
        given(decodedToken.isEmailVerified()).willReturn(true);

        mockMvc.perform(post("/api/v1/auth/sync")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", notNullValue()))
                .andExpect(jsonPath("$.role").value("LEARNER"))
                .andExpect(jsonPath("$.firebaseUid").doesNotExist());

        assert userRepository.findByFirebaseUid("firebase-uid-123").isPresent();
    }

    @Test
    void syncReusesExistingUserOnRepeatLogin() throws Exception {
        FirebaseToken decodedToken = org.mockito.Mockito.mock(FirebaseToken.class);

        given(firebaseAuth.verifyIdToken("valid-token")).willReturn(decodedToken);
        given(decodedToken.getUid()).willReturn("firebase-uid-repeat");
        given(decodedToken.getEmail()).willReturn("repeat@edulife.test");
        given(decodedToken.isEmailVerified()).willReturn(true);

        String firstResponse = mockMvc.perform(post("/api/v1/auth/sync")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String secondResponse = mockMvc.perform(post("/api/v1/auth/sync")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assert firstResponse.equals(secondResponse);
        assert testUserCount() == 1;
    }

    @Test
    void syncResolvesStaffTeacherEmailToTeacherFromAllowlist() throws Exception {
        // teacher@edulife.test is promoted by the trusted staff allowlist (verified email), NOT by
        // any client-supplied intendedRole. Sending no body proves the role is allowlist-driven.
        FirebaseToken decodedToken = org.mockito.Mockito.mock(FirebaseToken.class);

        given(firebaseAuth.verifyIdToken("teacher-token")).willReturn(decodedToken);
        given(decodedToken.getUid()).willReturn("firebase-uid-teacher");
        given(decodedToken.getEmail()).willReturn("teacher@edulife.test");
        given(decodedToken.isEmailVerified()).willReturn(true);

        mockMvc.perform(post("/api/v1/auth/sync")
                        .header("Authorization", "Bearer teacher-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TEACHER"));

        // Trusted DB role is stable across repeat logins.
        mockMvc.perform(post("/api/v1/auth/sync")
                        .header("Authorization", "Bearer teacher-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TEACHER"));
    }

    @Test
    void syncIgnoresTeacherIntentForNonStaffEmail() throws Exception {
        // P0 regression: a normal (non-staff) registrant must never self-promote to TEACHER.
        FirebaseToken decodedToken = org.mockito.Mockito.mock(FirebaseToken.class);

        given(firebaseAuth.verifyIdToken("escalate-teacher-token")).willReturn(decodedToken);
        given(decodedToken.getUid()).willReturn("firebase-uid-escalate-teacher");
        given(decodedToken.getEmail()).willReturn("escalate-teacher@edulife.test");
        given(decodedToken.isEmailVerified()).willReturn(true);

        mockMvc.perform(post("/api/v1/auth/sync")
                        .header("Authorization", "Bearer escalate-teacher-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"intendedRole\":\"TEACHER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("LEARNER"));
    }

    @Test
    void syncIgnoresGroupAdminIntentForNonStaffEmail() throws Exception {
        // P0 regression: a normal (non-staff) registrant must never self-promote to GROUP_ADMIN.
        FirebaseToken decodedToken = org.mockito.Mockito.mock(FirebaseToken.class);

        given(firebaseAuth.verifyIdToken("escalate-ga-token")).willReturn(decodedToken);
        given(decodedToken.getUid()).willReturn("firebase-uid-escalate-ga");
        given(decodedToken.getEmail()).willReturn("escalate-ga@edulife.test");
        given(decodedToken.isEmailVerified()).willReturn(true);

        mockMvc.perform(post("/api/v1/auth/sync")
                        .header("Authorization", "Bearer escalate-ga-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"intendedRole\":\"GROUP_ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("LEARNER"));
    }

    @Test
    void syncPreventsAdminSelfAssignment() throws Exception {
        FirebaseToken decodedToken = org.mockito.Mockito.mock(FirebaseToken.class);

        given(firebaseAuth.verifyIdToken("admin-intent-token")).willReturn(decodedToken);
        given(decodedToken.getUid()).willReturn("firebase-uid-admin-intent");
        given(decodedToken.getEmail()).willReturn("admin-intent@edulife.test");
        given(decodedToken.isEmailVerified()).willReturn(true);

        mockMvc.perform(post("/api/v1/auth/sync")
                        .header("Authorization", "Bearer admin-intent-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"intendedRole\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("LEARNER"));
    }

    @Test
    void syncRejectsInvalidOrExpiredToken() throws Exception {
        given(firebaseAuth.verifyIdToken("expired-token")).willThrow(
                new FirebaseAuthException(
                        ErrorCode.UNAUTHENTICATED,
                        "Token verification failed",
                        null,
                        null,
                        null
                )
        );

        mockMvc.perform(post("/api/v1/auth/sync")
                        .header("Authorization", "Bearer expired-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid or expired Firebase token"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void syncRejectsUnverifiedEmail() throws Exception {
        FirebaseToken decodedToken = org.mockito.Mockito.mock(FirebaseToken.class);

        given(firebaseAuth.verifyIdToken("unverified-token")).willReturn(decodedToken);
        given(decodedToken.getUid()).willReturn("firebase-uid-unverified");
        given(decodedToken.getEmail()).willReturn("unverified@edulife.test");
        given(decodedToken.isEmailVerified()).willReturn(false);

        mockMvc.perform(post("/api/v1/auth/sync")
                        .header("Authorization", "Bearer unverified-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Email is not verified"))
                .andExpect(jsonPath("$.timestamp").exists());

        assert userRepository.findByFirebaseUid("firebase-uid-unverified").isEmpty();
    }

    @Test
    void syncRejectsMissingToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/sync"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void syncAssignsAdminStaffRoleByVerifiedEmailWithoutIntent() throws Exception {
        // admin@edulife.test logs in with NO intendedRole. ADMIN can never be self-assigned, and
        // the seed migrations no-op on a fresh DB, so without the staff allowlist this would be
        // LEARNER. The allowlist must promote it to ADMIN purely from the verified email.
        FirebaseToken decodedToken = org.mockito.Mockito.mock(FirebaseToken.class);

        given(firebaseAuth.verifyIdToken("admin-staff-token")).willReturn(decodedToken);
        given(decodedToken.getUid()).willReturn("firebase-uid-admin-staff");
        given(decodedToken.getEmail()).willReturn("admin@edulife.test");
        given(decodedToken.isEmailVerified()).willReturn(true);

        mockMvc.perform(post("/api/v1/auth/sync")
                        .header("Authorization", "Bearer admin-staff-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void syncAssignsGroupAdminStaffRoleByVerifiedEmail() throws Exception {
        // groupadmin@edulife.test with no intendedRole must resolve to GROUP_ADMIN, proving the
        // staff role no longer depends on migration ordering or a first-login intendedRole.
        FirebaseToken decodedToken = org.mockito.Mockito.mock(FirebaseToken.class);

        given(firebaseAuth.verifyIdToken("ga-staff-token")).willReturn(decodedToken);
        given(decodedToken.getUid()).willReturn("firebase-uid-ga-staff");
        given(decodedToken.getEmail()).willReturn("groupadmin@edulife.test");
        given(decodedToken.isEmailVerified()).willReturn(true);

        mockMvc.perform(post("/api/v1/auth/sync")
                        .header("Authorization", "Bearer ga-staff-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("GROUP_ADMIN"));

        // Re-asserted on repeat login (no body) — stays GROUP_ADMIN, single row.
        mockMvc.perform(post("/api/v1/auth/sync")
                        .header("Authorization", "Bearer ga-staff-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("GROUP_ADMIN"));

        assert testUserCount() == 1;
    }

    @Test
    void syncLeavesNonStaffEmailAsLearner() throws Exception {
        // A normal learner email is not in the allowlist and must stay LEARNER.
        FirebaseToken decodedToken = org.mockito.Mockito.mock(FirebaseToken.class);

        given(firebaseAuth.verifyIdToken("learner-token")).willReturn(decodedToken);
        given(decodedToken.getUid()).willReturn("firebase-uid-plain-learner");
        given(decodedToken.getEmail()).willReturn("someone@edulife.test");
        given(decodedToken.isEmailVerified()).willReturn(true);

        mockMvc.perform(post("/api/v1/auth/sync")
                        .header("Authorization", "Bearer learner-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("LEARNER"));
    }
}
