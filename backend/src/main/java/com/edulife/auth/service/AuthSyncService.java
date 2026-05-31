package com.edulife.auth.service;

import com.edulife.auth.dto.AuthSyncRequest;
import com.edulife.auth.dto.AuthSyncResponse;
import com.edulife.security.FirebaseAuthentication;
import com.edulife.users.entity.User;
import com.edulife.users.model.UserRole;
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
    public AuthSyncResponse syncCurrentUser(AuthSyncRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof FirebaseAuthentication firebaseAuth)) {
            throw new IllegalStateException("Firebase authentication is required for auth sync.");
        }

        String firebaseUid = firebaseAuth.getFirebaseUid();
        String email = firebaseAuth.getEmail();

        if (firebaseUid == null || firebaseUid.isBlank()) {
            throw new IllegalStateException("Firebase UID is missing from authentication context.");
        }

        if (email == null || email.isBlank()) {
            throw new IllegalStateException("Firebase email is missing from authentication context.");
        }

        User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseGet(() -> {
                    User newUser = new User(firebaseUid, email);
                    // intendedRole is only applied on first sync. ADMIN cannot be self-assigned.
                    UserRole role = resolveIntendedRole(request);
                    newUser.setRole(role);
                    return userRepository.save(newUser);
                });

        return new AuthSyncResponse(user.getId(), user.getRole());
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
