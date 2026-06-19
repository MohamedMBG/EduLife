package com.edulife.auth;

import com.edulife.auth.config.StaffRoleProperties;
import com.edulife.auth.dto.AuthSyncRequest;
import com.edulife.auth.dto.AuthSyncResponse;
import com.edulife.auth.service.AuthSyncService;
import com.edulife.profiles.repository.ProfileRepository;
import com.edulife.security.FirebaseAuthentication;
import com.edulife.users.entity.User;
import com.edulife.users.model.UserRole;
import com.edulife.users.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthSyncServiceTest {

    private static final UUID USER_ID = UUID.fromString("bbbbbbbb-2222-2222-2222-bbbbbbbbbbbb");

    private final UserRepository userRepository = mock(UserRepository.class);
    private final ProfileRepository profileRepository = mock(ProfileRepository.class);
    private final StaffRoleProperties staffRoleProperties = mock(StaffRoleProperties.class);
    private final AuthSyncService service = new AuthSyncService(userRepository, profileRepository, staffRoleProperties);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void syncRelinksExistingGroupAdminEmailWhenFirebaseUidChanged() {
        User existing = new User("old-firebase-uid", "groupadmin@edulife.test");
        ReflectionTestUtils.setField(existing, "id", USER_ID);
        existing.setRole(UserRole.LEARNER);

        given(userRepository.findByFirebaseUid("new-firebase-uid")).willReturn(Optional.empty());
        given(userRepository.findByEmail("groupadmin@edulife.test")).willReturn(Optional.of(existing));
        given(userRepository.save(existing)).willReturn(existing);
        given(staffRoleProperties.roleFor("groupadmin@edulife.test")).willReturn(UserRole.GROUP_ADMIN);
        authenticate("new-firebase-uid", "groupadmin@edulife.test");

        AuthSyncResponse response = service.syncCurrentUser(null);

        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.role()).isEqualTo(UserRole.GROUP_ADMIN);
        assertThat(existing.getFirebaseUid()).isEqualTo("new-firebase-uid");
        assertThat(existing.getRole()).isEqualTo(UserRole.GROUP_ADMIN);
        verify(userRepository, times(2)).save(existing);
    }

    @Test
    void newUserCannotSelfAssignTeacherViaIntendedRole() {
        assertNewNonStaffUserIsCreatedAsLearner(UserRole.TEACHER);
    }

    @Test
    void newUserCannotSelfAssignGroupAdminViaIntendedRole() {
        assertNewNonStaffUserIsCreatedAsLearner(UserRole.GROUP_ADMIN);
    }

    @Test
    void newUserCannotSelfAssignAdminViaIntendedRole() {
        assertNewNonStaffUserIsCreatedAsLearner(UserRole.ADMIN);
    }

    @Test
    void existingTeacherKeepsRoleOnResync() {
        User existing = new User("teacher-uid", "realteacher@edulife.test");
        ReflectionTestUtils.setField(existing, "id", USER_ID);
        existing.setRole(UserRole.TEACHER);

        given(userRepository.findByFirebaseUid("teacher-uid")).willReturn(Optional.of(existing));
        // Not a staff allowlist email, so reconcileStaffRole must not change the persisted role.
        given(staffRoleProperties.roleFor("realteacher@edulife.test")).willReturn(null);
        authenticate("teacher-uid", "realteacher@edulife.test");

        // Even a malicious LEARNER intent in the body must not demote a trusted DB role.
        AuthSyncResponse response = service.syncCurrentUser(new AuthSyncRequest(UserRole.LEARNER));

        assertThat(response.role()).isEqualTo(UserRole.TEACHER);
        assertThat(existing.getRole()).isEqualTo(UserRole.TEACHER);
    }

    private void assertNewNonStaffUserIsCreatedAsLearner(UserRole intendedRole) {
        String uid = "new-uid";
        String email = "newbie@edulife.test";
        User createdRow = new User(uid, email);
        ReflectionTestUtils.setField(createdRow, "id", USER_ID);
        createdRow.setRole(UserRole.LEARNER);

        // First lookup (top of sync) misses; second lookup (after insert) returns the new row.
        given(userRepository.findByFirebaseUid(uid)).willReturn(Optional.empty(), Optional.of(createdRow));
        given(userRepository.findByEmail(email)).willReturn(Optional.empty());
        given(staffRoleProperties.roleFor(email)).willReturn(null);
        authenticate(uid, email);

        AuthSyncResponse response = service.syncCurrentUser(new AuthSyncRequest(intendedRole));

        assertThat(response.role()).isEqualTo(UserRole.LEARNER);
        // The row is always inserted with the LEARNER role regardless of the requested intent.
        verify(userRepository).insertForAuthSyncIfAbsent(any(), eq(uid), eq(email), eq("LEARNER"));
    }

    private static void authenticate(String firebaseUid, String email) {
        SecurityContextHolder.getContext().setAuthentication(new FirebaseAuthentication(firebaseUid, email, null));
    }
}
