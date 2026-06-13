package com.edulife.auth;

import com.edulife.auth.config.StaffRoleProperties;
import com.edulife.auth.dto.AuthSyncResponse;
import com.edulife.auth.service.AuthSyncService;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthSyncServiceTest {

    private static final UUID USER_ID = UUID.fromString("bbbbbbbb-2222-2222-2222-bbbbbbbbbbbb");

    private final UserRepository userRepository = mock(UserRepository.class);
    private final StaffRoleProperties staffRoleProperties = mock(StaffRoleProperties.class);
    private final AuthSyncService service = new AuthSyncService(userRepository, staffRoleProperties);

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

    private static void authenticate(String firebaseUid, String email) {
        SecurityContextHolder.getContext().setAuthentication(new FirebaseAuthentication(firebaseUid, email));
    }
}
