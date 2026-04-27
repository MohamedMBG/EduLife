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

    // Repository used to access the users table in PostgreSQL
    private final UserRepository userRepository;

    // Constructor injection (recommended for Spring services)
    public AuthSyncService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Main method used by /api/v1/auth/sync
     *
     * Responsibilities:
     * 1. Read authenticated user from Spring Security context
     * 2. Extract Firebase identity (UID + email)
     * 3. Validate identity presence (defensive coding)
     * 4. Upsert user in database:
     *    - If exists → reuse
     *    - If not → create new user
     * 5. Return safe response (internal UUID + role only)
     *
     * @return AuthSyncResponse containing internal userId and role
     */

    @Transactional
    public AuthSyncResponse syncCurrentUser(){

        /**
         * Get the current authentication object from Spring Security
         *
         * This was populated earlier by FirebaseTokenFilter
         * after verifying the Firebase ID token.
         */
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        /**
         * Ensure that authentication is of type FirebaseAuthentication
         *
         * Why:
         * - Prevents trusting any other authentication type
         * - Guarantees we are using validated Firebase identity
         */
        if (!(authentication instanceof FirebaseAuthentication firebaseAuth)) {
            throw new IllegalStateException("Unexpected authentication type: " + authentication.getClass());
        }

        /**
         * Extract Firebase identity fields
         *
         * These values come ONLY from the verified Firebase token,
         * NOT from client request body (important for security).
         */
        String firebaseUid = firebaseAuth.getFirebaseUid();
        String email = firebaseAuth.getEmail();

        /**
         * Defensive validation:
         * Ensure email exists
         */

        if (firebaseUid == null || firebaseUid.isBlank()) {
            throw new IllegalStateException("Firebase UID is missing from authentication context");
        }

        if (email == null || email.isBlank()) {
            throw new IllegalStateException("Firebase email is missing from authentication context");
        }

        /**
         * UPSERT LOGIC (core of this service)
         *
         * Try to find existing user by Firebase UID:
         * - If found → reuse existing user
         * - If not found → create a new user
         *
         * This ensures:
         * - No duplicate users
         * - Stable identity mapping
         */
        User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseGet(() ->
                    userRepository.save(new User(firebaseUid, email))
                );

        /**
         * Build response DTO
         * IMPORTANT:
         * - Only expose internal userId and role
         * - NEVER expose firebaseUid (security + abstraction)
         */
        return new AuthSyncResponse(
                user.getId(),     // internal UUID used across the system
                user.getRole()    // role used for authorization (RBAC)
        );
    }
}
