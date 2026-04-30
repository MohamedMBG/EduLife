package com.edulife.auth.service;

import com.edulife.auth.dto.AuthSyncResponse;
import com.edulife.security.FirebaseAuthentication;
import com.edulife.users.entity.User;
import com.edulife.users.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthSyncService {

    private final UserRepository userRepository;

    public AuthSyncService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public AuthSyncResponse syncCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Only the Firebase filter is allowed to populate identity for auth sync, so reject any unexpected auth type.
        if (!(authentication instanceof FirebaseAuthentication firebaseAuth)) {
            throw new IllegalStateException("Firebase authentication is required for auth sync.");
        }

        String firebaseUid = firebaseAuth.getFirebaseUid();
        String email = firebaseAuth.getEmail();

        // These values must come from Firebase Admin token verification; missing values indicate a broken security context.
        if (firebaseUid == null || firebaseUid.isBlank()) {
            throw new IllegalStateException("Firebase UID is missing from authentication context.");
        }

        if (email == null || email.isBlank()) {
            throw new IllegalStateException("Firebase email is missing from authentication context.");
        }

        // Keep sync idempotent: repeated logins for the same Firebase user must reuse the same internal UUID.
        User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseGet(() -> userRepository.save(new User(firebaseUid, email)));

        // Never expose firebaseUid in API responses; Android should use only EduLife's internal UUID and role.
        return new AuthSyncResponse(user.getId(), user.getRole());
    }
}
