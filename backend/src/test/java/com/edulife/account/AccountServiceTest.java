package com.edulife.account;

import com.edulife.account.service.AccountService;
import com.edulife.profiles.entity.Profile;
import com.edulife.profiles.repository.ProfileRepository;
import com.edulife.security.FirebaseAuthentication;
import com.edulife.users.entity.User;
import com.edulife.users.repository.UserRepository;
import com.google.firebase.ErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AccountServiceTest {

    private static final String FIREBASE_UID = "firebase-uid-123";
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private UserRepository userRepository;
    private ProfileRepository profileRepository;
    private FirebaseAuth firebaseAuth;
    private AccountService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        profileRepository = mock(ProfileRepository.class);
        firebaseAuth = mock(FirebaseAuth.class);
        service = new AccountService(userRepository, profileRepository, firebaseAuth);

        SecurityContextHolder.getContext().setAuthentication(
                new FirebaseAuthentication(FIREBASE_UID, "student@edulife.test", null));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deleteAnonymizesUserAndProfileThenDeletesFirebaseAccount() throws Exception {
        User user = userMock();
        Profile profile = profileMock();
        given(userRepository.findByFirebaseUid(FIREBASE_UID)).willReturn(Optional.of(user));
        given(profileRepository.findByUserId(USER_ID)).willReturn(Optional.of(profile));

        service.deleteCurrentAccount();

        verify(user).anonymize();
        verify(userRepository).save(user);
        verify(profile).anonymize();
        verify(profileRepository).save(profile);
        verify(firebaseAuth).deleteUser(FIREBASE_UID);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void deleteWithoutProfileStillAnonymizesUserAndDeletesFirebaseAccount() throws Exception {
        User user = userMock();
        given(userRepository.findByFirebaseUid(FIREBASE_UID)).willReturn(Optional.of(user));
        given(profileRepository.findByUserId(USER_ID)).willReturn(Optional.empty());

        service.deleteCurrentAccount();

        verify(user).anonymize();
        verify(userRepository).save(user);
        verify(profileRepository, never()).save(any());
        verify(firebaseAuth).deleteUser(FIREBASE_UID);
    }

    @Test
    void deleteSwallowsFirebaseNotFound() throws Exception {
        User user = userMock();
        given(userRepository.findByFirebaseUid(FIREBASE_UID)).willReturn(Optional.of(user));
        given(profileRepository.findByUserId(USER_ID)).willReturn(Optional.empty());
        willThrow(new FirebaseAuthException(ErrorCode.NOT_FOUND, "no such user", null, null, null))
                .given(firebaseAuth).deleteUser(FIREBASE_UID);

        service.deleteCurrentAccount();

        verify(user).anonymize();
        verify(userRepository).save(user);
    }

    @Test
    void deleteSurfacesFirebaseFailureAsApiError() throws Exception {
        User user = userMock();
        given(userRepository.findByFirebaseUid(FIREBASE_UID)).willReturn(Optional.of(user));
        given(profileRepository.findByUserId(USER_ID)).willReturn(Optional.empty());
        willThrow(new FirebaseAuthException(ErrorCode.INTERNAL, "boom", null, null, null))
                .given(firebaseAuth).deleteUser(FIREBASE_UID);

        assertThatThrownBy(() -> service.deleteCurrentAccount())
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Could not finalize account deletion");
    }

    @Test
    void deleteRejectsWhenLocalUserMissing() {
        given(userRepository.findByFirebaseUid(FIREBASE_UID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteCurrentAccount())
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Call /auth/sync first");
    }

    private User userMock() {
        User user = mock(User.class);
        given(user.getId()).willReturn(USER_ID);
        given(user.getFirebaseUid()).willReturn(FIREBASE_UID);
        return user;
    }

    private Profile profileMock() {
        return mock(Profile.class);
    }
}
