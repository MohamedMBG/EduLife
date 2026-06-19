package com.edulife.account.service;

import com.edulife.profiles.entity.Profile;
import com.edulife.profiles.repository.ProfileRepository;
import com.edulife.security.FirebaseAuthentication;
import com.edulife.users.entity.User;
import com.edulife.users.repository.UserRepository;
import com.google.firebase.ErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import jakarta.transaction.Transactional;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final FirebaseAuth firebaseAuth;

    public AccountService(
            UserRepository userRepository,
            ProfileRepository profileRepository,
            FirebaseAuth firebaseAuth
    ) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.firebaseAuth = firebaseAuth;
    }

    /**
     * Deletes the currently authenticated user's account.
     *
     * The user row is anonymized in place (email + firebase_uid nulled) rather than hard-deleted
     * so audit-bound rows (certificates, exam attempts, group memberships) keep a stable
     * user_id reference. The Firebase account is removed via the Admin SDK so the same email
     * can be re-registered in the future and the current Bearer token is revoked.
     */
    @Transactional
    public void deleteCurrentAccount() {
        User currentUser = resolveCurrentUser();
        String firebaseUid = currentUser.getFirebaseUid();

        Optional<Profile> profile = profileRepository.findByUserId(currentUser.getId());
        profile.ifPresent(p -> {
            p.anonymize();
            profileRepository.save(p);
        });

        currentUser.anonymize();
        userRepository.save(currentUser);

        deleteFirebaseAccount(firebaseUid);

        // Revoke the in-flight session so the now-orphaned Bearer token cannot be reused on
        // any follow-up request in the same connection.
        SecurityContextHolder.clearContext();
    }

    private void deleteFirebaseAccount(String firebaseUid) {
        if (firebaseUid == null || firebaseUid.isBlank()) {
            return;
        }
        try {
            firebaseAuth.deleteUser(firebaseUid);
        } catch (FirebaseAuthException e) {
            // A missing Firebase user means the account is already gone; finish the local
            // anonymization quietly so the client still receives 204.
            if (e.getErrorCode() == ErrorCode.NOT_FOUND) {
                // Do not log the Firebase UID; it is a sensitive identifier.
                log.info("Firebase account already absent during account deletion.");
                return;
            }
            log.error("Firebase deleteUser failed during account deletion.", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not finalize account deletion");
        }
    }

    private User resolveCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof FirebaseAuthentication firebaseAuth)) {
            throw new IllegalStateException("Firebase authentication required");
        }
        return userRepository.findByFirebaseUid(firebaseAuth.getFirebaseUid())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "User not found. Call /auth/sync first."));
    }
}
