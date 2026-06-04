package com.baghdad.edulife.features.profile.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.baghdad.edulife.R;
import com.baghdad.edulife.core.storage.SessionStorage;
import com.baghdad.edulife.features.auth.viewmodel.AuthViewModel;
import com.baghdad.edulife.features.profile.model.ProfileResponse;
import com.baghdad.edulife.features.profile.model.TeacherRequestResponse;
import com.baghdad.edulife.features.profile.viewmodel.ProfileViewModel;
import com.bumptech.glide.Glide;
import com.baghdad.edulife.features.profile.viewmodel.TeacherRequestViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private static final int AVATAR_MAX_PX = 1024;
    private static final int AVATAR_JPEG_QUALITY = 88;

    private AuthViewModel authViewModel;
    private ProfileViewModel profileViewModel;
    private ActivityResultLauncher<PickVisualMediaRequest> pickMediaLauncher;
    private TeacherRequestViewModel teacherRequestViewModel;
    private String currentRoleCode = "LEARNER";

    public ProfileFragment() {
        super(R.layout.fragment_profile);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pickMediaLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri != null) handleAvatarSelected(uri);
                }
        );
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        teacherRequestViewModel = new ViewModelProvider(this).get(TeacherRequestViewModel.class);
        SessionStorage sessionStorage = new SessionStorage(requireContext());
        currentRoleCode = sessionStorage.getRole() != null
                ? sessionStorage.getRole().toUpperCase(Locale.ROOT)
                : "LEARNER";

        View profileHeader = view.findViewById(R.id.profileHeaderLayout);
        final int origProfileHeaderTop = profileHeader.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            profileHeader.setPadding(profileHeader.getPaddingLeft(), origProfileHeaderTop + top,
                    profileHeader.getPaddingRight(), profileHeader.getPaddingBottom());
            return WindowInsetsCompat.CONSUMED;
        });

        bindStaticUserInfo(view, sessionStorage);
        observeProfile(view);
        observeAvatarUpload(view);
        profileViewModel.loadProfile();
        bindTeacherRequestRow(view);
        observeTeacherRequests(view);
        if (isTeacherRequestEligible(currentRoleCode)) {
            teacherRequestViewModel.loadLatestRequest();
        }

        view.findViewById(R.id.avatarContainer).setOnClickListener(v ->
                pickMediaLauncher.launch(
                        new PickVisualMediaRequest.Builder()
                                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                                .build()
                ));

        view.findViewById(R.id.settingsEditProfile).setOnClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.action_profileFragment_to_editProfileFragment));

        view.findViewById(R.id.profileCertificatesRow).setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_profileFragment_to_certificatesFragment));

        String role = sessionStorage.getRole();
        View becomeTeacherRow = view.findViewById(R.id.profileBecomeTeacherRow);
        if ("LEARNER".equalsIgnoreCase(role)) {
            becomeTeacherRow.setVisibility(View.VISIBLE);
            becomeTeacherRow.setOnClickListener(v ->
                    Navigation.findNavController(v)
                            .navigate(R.id.action_profileFragment_to_teacherRequestFragment));
        }

        view.findViewById(R.id.logoutButton).setOnClickListener(v -> {
            authViewModel.signOut();
            NavOptions options = new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph, true)
                    .build();
            Navigation.findNavController(view)
                    .navigate(R.id.action_profileFragment_to_loginFragment, null, options);
        });

        view.findViewById(R.id.deleteAccountButton)
                .setOnClickListener(v -> showDeleteAccountDialog(view));

        observeAccountDeletion(view);
    }

    private void handleAvatarSelected(Uri uri) {
        File compressed = compressImage(requireContext(), uri);
        if (compressed == null) {
            Toast.makeText(requireContext(), R.string.avatar_upload_error, Toast.LENGTH_SHORT).show();
            return;
        }
        profileViewModel.uploadAvatar(compressed);
    }

    @Nullable
    private File compressImage(Context context, Uri uri) {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            if (inputStream == null) return null;

            Bitmap original = BitmapFactory.decodeStream(inputStream);
            if (original == null) return null;

            Bitmap scaled = scaleBitmap(original, AVATAR_MAX_PX);

            File outFile = File.createTempFile("avatar_", ".jpg", context.getCacheDir());
            try (FileOutputStream out = new FileOutputStream(outFile)) {
                scaled.compress(Bitmap.CompressFormat.JPEG, AVATAR_JPEG_QUALITY, out);
            }

            if (!scaled.equals(original)) scaled.recycle();
            original.recycle();

            return outFile;
        } catch (IOException e) {
            return null;
        }
    }

    private Bitmap scaleBitmap(Bitmap src, int maxPx) {
        int w = src.getWidth();
        int h = src.getHeight();
        if (w <= maxPx && h <= maxPx) return src;

        float ratio = w > h ? (float) maxPx / w : (float) maxPx / h;
        return Bitmap.createScaledBitmap(src, Math.round(w * ratio), Math.round(h * ratio), true);
    }

    private void observeAvatarUpload(View view) {
        ImageView avatarImage = view.findViewById(R.id.avatarImage);
        TextView avatarInitials = view.findViewById(R.id.avatarInitials);

        profileViewModel.uploading.observe(getViewLifecycleOwner(), uploading -> {
            if (Boolean.TRUE.equals(uploading)) {
                Toast.makeText(requireContext(), R.string.avatar_uploading, Toast.LENGTH_SHORT).show();
            }
        });

        profileViewModel.uploadedAvatarUrl.observe(getViewLifecycleOwner(), url -> {
            if (url == null || url.isBlank()) return;
            Toast.makeText(requireContext(), R.string.avatar_upload_success, Toast.LENGTH_SHORT).show();
            loadAvatarImage(avatarImage, avatarInitials, url);
        });

        profileViewModel.uploadError.observe(getViewLifecycleOwner(), msg -> {
            if (msg == null || msg.isBlank()) return;
            profileViewModel.clearUploadError();
            Toast.makeText(requireContext(), R.string.avatar_upload_error, Toast.LENGTH_SHORT).show();
        });
    }

    private void loadAvatarImage(ImageView imageView, TextView initials, String url) {
        Glide.with(this)
                .load(url)
                .circleCrop()
                .into(imageView);
        imageView.setVisibility(View.VISIBLE);
        initials.setVisibility(View.GONE);
    }

    private void bindTeacherRequestRow(View view) {
        View requestRow = view.findViewById(R.id.profileTeacherRequestRow);
        if (!isTeacherRequestEligible(currentRoleCode)) {
            requestRow.setVisibility(View.GONE);
            return;
        }

        requestRow.setOnClickListener(v -> {
            TeacherRequestResponse request = teacherRequestViewModel.getLatestRequest().getValue();
            if (request != null && "PENDING".equalsIgnoreCase(request.status)) {
                Toast.makeText(requireContext(),
                        getString(R.string.profile_teacher_request_status_pending),
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (request != null && "APPROVED".equalsIgnoreCase(request.status)) {
                Toast.makeText(requireContext(),
                        getString(R.string.profile_teacher_request_status_approved),
                        Toast.LENGTH_SHORT).show();
                return;
            }
            showTeacherRequestDialog();
        });
    }

    private void showTeacherRequestDialog() {
        EditText motivationInput = new EditText(requireContext());
        motivationInput.setHint(R.string.profile_teacher_request_dialog_hint);
        motivationInput.setMinLines(4);
        motivationInput.setMaxLines(6);
        motivationInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        motivationInput.setFilters(new android.text.InputFilter[] {
                new android.text.InputFilter.LengthFilter(1000)
        });
        int padPx = (int) (requireContext().getResources().getDisplayMetrics().density * 24);
        motivationInput.setPadding(padPx, padPx / 2, padPx, padPx / 2);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.profile_teacher_request_dialog_title)
                .setMessage(R.string.profile_teacher_request_dialog_body)
                .setView(motivationInput)
                .setNegativeButton(R.string.profile_delete_account_dialog_cancel, null)
                .setPositiveButton(R.string.profile_teacher_request_dialog_submit, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(btn -> {
                    String motivation = motivationInput.getText() == null
                            ? ""
                            : motivationInput.getText().toString().trim();
                    if (motivation.isBlank()) {
                        Toast.makeText(requireContext(),
                                R.string.profile_teacher_request_motivation_required,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    dialog.dismiss();
                    teacherRequestViewModel.submitTeacherRequest(motivation);
                }));

        dialog.show();
    }

    private void showDeleteAccountDialog(View view) {
        EditText confirmInput = new EditText(requireContext());
        confirmInput.setHint(R.string.profile_delete_account_dialog_hint);
        confirmInput.setFilters(new android.text.InputFilter[] {
                new android.text.InputFilter.LengthFilter(16)
        });
        confirmInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        confirmInput.setSingleLine(true);
        int padPx = (int) (requireContext().getResources().getDisplayMetrics().density * 24);
        confirmInput.setPadding(padPx, padPx / 2, padPx, padPx / 2);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.profile_delete_account_dialog_title)
                .setMessage(R.string.profile_delete_account_dialog_body)
                .setView(confirmInput)
                .setNegativeButton(R.string.profile_delete_account_dialog_cancel, null)
                .setPositiveButton(R.string.profile_delete_account_dialog_confirm, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(btn -> {
                    String typed = confirmInput.getText() == null ? "" : confirmInput.getText().toString();
                    if (!"DELETE".equals(typed)) {
                        Toast.makeText(requireContext(),
                                R.string.profile_delete_account_confirm_required,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    dialog.dismiss();
                    profileViewModel.deleteAccount();
                }));

        dialog.show();
    }

    private void observeAccountDeletion(View view) {
        profileViewModel.accountDeleted.observe(getViewLifecycleOwner(), deleted -> {
            if (!Boolean.TRUE.equals(deleted)) return;
            authViewModel.signOut();
            Toast.makeText(requireContext(),
                    R.string.profile_delete_account_success, Toast.LENGTH_SHORT).show();
            NavOptions options = new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph, true)
                    .build();
            Navigation.findNavController(view)
                    .navigate(R.id.action_profileFragment_to_loginFragment, null, options);
            profileViewModel.clearAccountDeleted();
        });

        profileViewModel.deleteError.observe(getViewLifecycleOwner(), msg -> {
            if (msg == null || msg.isBlank()) return;
            Toast.makeText(requireContext(),
                    R.string.profile_delete_account_error, Toast.LENGTH_SHORT).show();
            profileViewModel.clearDeleteError();
        });
    }

    private void bindStaticUserInfo(View view, SessionStorage session) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        TextView avatarInitials = view.findViewById(R.id.avatarInitials);
        ImageView avatarImage = view.findViewById(R.id.avatarImage);
        TextView nameView = view.findViewById(R.id.profileName);
        TextView emailView = view.findViewById(R.id.profileEmail);
        TextView roleView = view.findViewById(R.id.profileRole);
        TextView metaStatusView = view.findViewById(R.id.profileMetaStatus);
        TextView internalIdValueView = view.findViewById(R.id.profileInternalIdValue);
        TextView verificationValueView = view.findViewById(R.id.profileVerificationValue);

        String displayName = user != null && user.getDisplayName() != null
                ? user.getDisplayName()
                : extractNameFromEmail(user);
        String email = user != null && user.getEmail() != null ? user.getEmail() : "—";
        String role = session.getRole() != null ? session.getRole().toUpperCase() : "STUDENT";
        boolean emailVerified = user != null && user.isEmailVerified();
        String internalUserId = session.getUserId();
        boolean syncReady = internalUserId != null && !internalUserId.isBlank();

        nameView.setText(displayName);
        emailView.setText(email);
        roleView.setText(formatRole(role).toUpperCase());
        avatarInitials.setText(getInitials(displayName));
        metaStatusView.setText(buildMetaStatus(emailVerified, syncReady));
        internalIdValueView.setText(syncReady
                ? internalUserId
                : getString(R.string.profile_internal_id_missing));
        verificationValueView.setText(getString(emailVerified
                ? R.string.profile_access_verified
                : R.string.profile_access_pending));

        view.<TextView>findViewById(R.id.profileStatPrimaryValue).setText("—");
        view.<TextView>findViewById(R.id.profileStatSecondaryValue).setText("—");
        view.<TextView>findViewById(R.id.profileStatTertiaryValue).setText("—");

        // Load existing avatar if available from a previous session's cached profile
        ProfileResponse cached = profileViewModel.profile.getValue();
        if (cached != null && cached.avatarUrl != null && !cached.avatarUrl.isBlank()) {
            loadAvatarImage(avatarImage, avatarInitials, cached.avatarUrl);
        }
    }

    private void observeProfile(View view) {
        TextView nameView = view.findViewById(R.id.profileName);
        TextView avatarInitials = view.findViewById(R.id.avatarInitials);
        ImageView avatarImage = view.findViewById(R.id.avatarImage);
        TextView enrolledView = view.findViewById(R.id.profileStatPrimaryValue);
        TextView lessonsView = view.findViewById(R.id.profileStatSecondaryValue);
        TextView certView = view.findViewById(R.id.profileStatTertiaryValue);

        profileViewModel.profile.observe(getViewLifecycleOwner(), (ProfileResponse profile) -> {
            if (profile == null) return;

            if (profile.displayName != null && !profile.displayName.isBlank()) {
                nameView.setText(profile.displayName);
                avatarInitials.setText(getInitials(profile.displayName));
            }

            enrolledView.setText(String.valueOf(profile.enrolledCourses));
            lessonsView.setText(String.valueOf(profile.completedLessons));
            certView.setText(String.valueOf(profile.certificates));

            if (profile.avatarUrl != null && !profile.avatarUrl.isBlank()) {
                loadAvatarImage(avatarImage, avatarInitials, profile.avatarUrl);
            }
        });
    }

    private void observeTeacherRequests(View view) {
        TextView statusView = view.findViewById(R.id.profileTeacherRequestStatus);
        TextView metaView = view.findViewById(R.id.profileTeacherRequestMeta);
        View row = view.findViewById(R.id.profileTeacherRequestRow);

        if (!isTeacherRequestEligible(currentRoleCode)) {
            row.setVisibility(View.GONE);
            return;
        }

        teacherRequestViewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            if (Boolean.TRUE.equals(loading)) {
                statusView.setText(R.string.profile_teacher_request_status_idle);
                metaView.setText(R.string.profile_teacher_request_meta_loading);
            }
        });

        teacherRequestViewModel.getLatestRequest().observe(getViewLifecycleOwner(), request ->
                bindTeacherRequestState(statusView, metaView, request));

        teacherRequestViewModel.getSubmitMessage().observe(getViewLifecycleOwner(), message -> {
            if (message == null || message.isBlank()) return;
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            teacherRequestViewModel.clearSubmitMessage();
        });

        teacherRequestViewModel.getError().observe(getViewLifecycleOwner(), message -> {
            if (message == null || message.isBlank()) return;
            Toast.makeText(requireContext(),
                    R.string.profile_teacher_request_error, Toast.LENGTH_SHORT).show();
            teacherRequestViewModel.clearError();
        });
    }

    private void bindTeacherRequestState(TextView statusView, TextView metaView, TeacherRequestResponse request) {
        if (request == null) {
            statusView.setText(R.string.profile_teacher_request_status_idle);
            metaView.setText(R.string.profile_teacher_request_meta_idle);
            return;
        }

        String status = request.status != null ? request.status.toUpperCase(Locale.ROOT) : "";
        switch (status) {
            case "PENDING":
                statusView.setText(R.string.profile_teacher_request_status_pending);
                metaView.setText(getString(R.string.profile_teacher_request_meta_pending,
                        formatTeacherRequestDate(request.requestedAt)));
                break;
            case "APPROVED":
                statusView.setText(R.string.profile_teacher_request_status_approved);
                metaView.setText(getString(R.string.profile_teacher_request_meta_approved,
                        formatTeacherRequestDate(request.reviewedAt)));
                break;
            case "REJECTED":
                statusView.setText(R.string.profile_teacher_request_status_rejected);
                metaView.setText(getString(R.string.profile_teacher_request_meta_rejected,
                        formatTeacherRequestDate(request.reviewedAt)));
                break;
            default:
                statusView.setText(R.string.profile_teacher_request_status_idle);
                metaView.setText(R.string.profile_teacher_request_meta_idle);
                break;
        }
    }

    private String buildMetaStatus(boolean emailVerified, boolean syncReady) {
        String verification = getString(emailVerified
                ? R.string.profile_meta_verified
                : R.string.profile_meta_pending);
        String sync = getString(syncReady
                ? R.string.profile_meta_sync_ready
                : R.string.profile_meta_sync_pending);
        return verification + " • " + sync;
    }

    private String formatRole(String role) {
        if (role == null || role.isBlank()) return "Student";
        String normalized = role.replace('_', ' ').trim().toLowerCase();
        String[] parts = normalized.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) continue;
            if (builder.length() > 0) builder.append(' ');
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.length() == 0 ? "Student" : builder.toString();
    }

    private String extractNameFromEmail(FirebaseUser user) {
        if (user == null || user.getEmail() == null) return "Learner";
        String email = user.getEmail();
        int atIdx = email.indexOf('@');
        if (atIdx <= 0) return "Learner";
        String local = email.substring(0, atIdx);
        if (local.isEmpty()) return "Learner";
        if (local.length() == 1) return local.toUpperCase();
        return local.substring(0, 1).toUpperCase() + local.substring(1);
    }

    private String getInitials(String name) {
        if (name == null) return "?";
        String trimmed = name.trim();
        if (trimmed.isEmpty()) return "?";
        String[] parts = trimmed.split("\\s+");
        StringBuilder out = new StringBuilder(2);
        for (String part : parts) {
            if (part.isEmpty()) continue;
            out.append(Character.toUpperCase(part.charAt(0)));
            if (out.length() == 2) break;
        }
        return out.length() == 0 ? "?" : out.toString();
    }

    private boolean isTeacherRequestEligible(String roleCode) {
        return "LEARNER".equalsIgnoreCase(roleCode) || "STUDENT".equalsIgnoreCase(roleCode);
    }

    private String formatTeacherRequestDate(String isoInstant) {
        if (isoInstant == null || isoInstant.isBlank()) {
            return "recently";
        }
        try {
            Instant instant = Instant.parse(isoInstant);
            return DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
                    .withZone(ZoneId.systemDefault())
                    .format(instant);
        } catch (Exception e) {
            return isoInstant;
        }
    }
}
