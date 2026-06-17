package com.edulife.auth.service;

import com.edulife.auth.config.StaffRoleProperties;
import com.edulife.auth.dto.AuthSyncRequest;
import com.edulife.auth.dto.AuthSyncResponse;
import com.edulife.profiles.entity.Profile;
import com.edulife.profiles.repository.ProfileRepository;
import com.edulife.security.FirebaseAuthentication;
import com.edulife.users.entity.User;
import com.edulife.users.model.UserRole;
import com.edulife.users.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthSyncService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final StaffRoleProperties staffRoleProperties;

    public AuthSyncService(
            UserRepository userRepository,
            ProfileRepository profileRepository,
            StaffRoleProperties staffRoleProperties) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.staffRoleProperties = staffRoleProperties;
    }

    @Transactional
    public AuthSyncResponse syncCurrentUser(AuthSyncRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof FirebaseAuthentication firebaseAuth)) {
            throw new IllegalStateException("Firebase authentication is required for auth sync.");
        }

        String firebaseUid = firebaseAuth.getFirebaseUid();
        String email = firebaseAuth.getEmail();
        String displayName = firebaseAuth.getDisplayName();

        if (firebaseUid == null || firebaseUid.isBlank()) {
            throw new IllegalStateException("Firebase UID is missing from authentication context.");
        }

        if (email == null || email.isBlank()) {
            throw new IllegalStateException("Firebase email is missing from authentication context.");
        }

        User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseGet(() -> createUserIfAbsent(firebaseUid, email, request));

        ensureProfileDisplayName(user.getId(), displayName);

        // Seeded staff accounts are identified by their verified Firebase email and promoted to
        // the configured role here. This self-heals rows that were created as LEARNER and is
        // independent of Flyway migration ordering, so staff always reach their correct portal.
        // It is driven only off the trusted token email — never client input — so it cannot be
        // used to self-assign a privileged role.
        user = reconcileStaffRole(user, email);

        return new AuthSyncResponse(user.getId(), user.getRole());
    }

    private void ensureProfileDisplayName(UUID userId, String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return;
        }
        Profile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> profileRepository.save(new Profile(userId)));
        if (profile.getDisplayName() == null || profile.getDisplayName().isBlank()) {
            profile.update(displayName, profile.getBio());
            profileRepository.save(profile);
        }
    }

    private User reconcileStaffRole(User user, String email) {
        UserRole staffRole = staffRoleProperties.roleFor(email);
        if (staffRole != null && staffRole != user.getRole()) {
            user.setRole(staffRole);
            return userRepository.save(user);
        }
        return user;
    }

    private User createUserIfAbsent(String firebaseUid, String email, AuthSyncRequest request) {
        return userRepository.findByEmail(email)
                .map(user -> relinkExistingEmailUser(user, firebaseUid))
                .orElseGet(() -> insertNewUser(firebaseUid, email, request));
    }

    private User insertNewUser(String firebaseUid, String email, AuthSyncRequest request) {
        // Browser auth listeners and multiple tabs can call /auth/sync at the same time.
        // The database upsert keeps first sync idempotent instead of leaking a unique-key error.
        UserRole role = resolveIntendedRole(request);
        try {
            userRepository.insertForAuthSyncIfAbsent(UUID.randomUUID(), firebaseUid, email, role.name());
        } catch (DataIntegrityViolationException ex) {
            // A concurrent sync or a recreated Firebase account may have already claimed the
            // verified email. Recover by loading the trusted email row instead of surfacing a 500.
            return userRepository.findByFirebaseUid(firebaseUid)
                    .or(() -> userRepository.findByEmail(email)
                            .map(user -> relinkExistingEmailUser(user, firebaseUid)))
                    .orElseThrow(() -> ex);
        }

        return userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new IllegalStateException("Auth sync failed to create or load the internal user."));
    }

    private User relinkExistingEmailUser(User user, String firebaseUid) {
        if (!firebaseUid.equals(user.getFirebaseUid())) {
            // Firebase verified emails are unique in the project, so email is the stable account
            // identity when a Firebase user is deleted/recreated and gets a new uid.
            user.relinkFirebaseUid(firebaseUid);
            return userRepository.save(user);
        }
        return user;
    }

    private UserRole resolveIntendedRole(AuthSyncRequest request) {
        if (request == null || request.intendedRole() == null) {
            return UserRole.LEARNER;
        }
        // ADMIN cannot be self-assigned through registration.
        if (request.intendedRole() == UserRole.ADMIN) {
            return UserRole.LEARNER;
        }
        return request.intendedRole();
    }
}
