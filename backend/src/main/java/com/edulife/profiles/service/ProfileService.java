package com.edulife.profiles.service;

import com.edulife.certificates.repository.CertificateRepository;
import com.edulife.enrollments.model.EnrollmentStatus;
import com.edulife.enrollments.repository.EnrollmentRepository;
import com.edulife.profiles.dto.AvatarUploadResponse;
import com.edulife.profiles.dto.ProfileDto;
import com.edulife.profiles.dto.UpdateProfileRequest;
import com.edulife.profiles.entity.Profile;
import com.edulife.profiles.repository.ProfileRepository;
import com.edulife.profiles.storage.AvatarStorage;
import com.edulife.progress.repository.LessonProgressRepository;
import com.edulife.security.FirebaseAuthentication;
import com.edulife.users.entity.User;
import com.edulife.users.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service handling profile retrieval, updates, and avatar uploads for the authenticated user.
 * Profiles are lazily created on first access.
 */
@Service
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final CertificateRepository certificateRepository;
    private final AvatarStorage avatarStorage;

    public ProfileService(
            ProfileRepository profileRepository,
            UserRepository userRepository,
            EnrollmentRepository enrollmentRepository,
            LessonProgressRepository lessonProgressRepository,
            CertificateRepository certificateRepository,
            AvatarStorage avatarStorage) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.lessonProgressRepository = lessonProgressRepository;
        this.certificateRepository = certificateRepository;
        this.avatarStorage = avatarStorage;
    }

    /** Returns the authenticated user's profile, creating one lazily if it does not exist. */
    public ProfileDto getProfile() {
        User user = resolveCurrentUser();
        Profile profile = findOrCreateProfile(user.getId());
        return toDto(user, profile);
    }

    /** Updates display name and bio for the authenticated user's profile. */
    @Transactional
    public ProfileDto updateProfile(UpdateProfileRequest request) {
        User user = resolveCurrentUser();
        Profile profile = findOrCreateProfile(user.getId());
        profile.update(request.displayName(), request.bio());
        profileRepository.save(profile);
        return toDto(user, profile);
    }

    /** Stores a new avatar image, updates the profile URL, and deletes the previous avatar if any. */
    @Transactional
    public AvatarUploadResponse uploadAvatar(MultipartFile file) {
        User user = resolveCurrentUser();
        Profile profile = findOrCreateProfile(user.getId());
        String previousAvatarUrl = profile.getAvatarUrl();

        AvatarStorage.StoredAvatar stored = avatarStorage.store(user.getId(), file);
        profile.updateAvatarUrl(stored.publicUrl());
        profileRepository.save(profile);

        // Delete the prior file only after the new one is persisted so a write failure cannot
        // leave the profile pointing at a missing avatar.
        avatarStorage.deleteIfStored(previousAvatarUrl);

        return new AvatarUploadResponse(stored.publicUrl());
    }

    private Profile findOrCreateProfile(UUID userId) {
        return profileRepository.findByUserId(userId)
                .orElseGet(() -> profileRepository.save(new Profile(userId)));
    }

    private ProfileDto toDto(User user, Profile profile) {
        int enrolled = (int) enrollmentRepository.countByUserIdAndStatus(user.getId(), EnrollmentStatus.ACTIVE);
        int completed = (int) lessonProgressRepository.countByUserId(user.getId());
        int certificates = (int) certificateRepository.countByUserId(user.getId());

        return new ProfileDto(
                user.getId(),
                user.getEmail(),
                profile.getDisplayName(),
                profile.getBio(),
                profile.getAvatarUrl(),
                enrolled,
                completed,
                certificates
        );
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
