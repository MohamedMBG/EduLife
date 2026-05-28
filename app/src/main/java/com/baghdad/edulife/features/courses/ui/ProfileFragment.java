package com.baghdad.edulife.features.courses.ui;

import android.app.AlertDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.baghdad.edulife.R;
import com.baghdad.edulife.core.storage.SessionStorage;
import com.baghdad.edulife.features.auth.viewmodel.AuthViewModel;
import com.baghdad.edulife.features.certificates.data.CertificateRepository;
import com.baghdad.edulife.features.certificates.model.CertificateDto;
import com.baghdad.edulife.features.profile.model.ProfileResponse;
import com.baghdad.edulife.features.profile.viewmodel.ProfileViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class ProfileFragment extends Fragment {

    private AuthViewModel authViewModel;
    private ProfileViewModel profileViewModel;
    private CertificateRepository certificateRepository;

    public ProfileFragment() {
        super(R.layout.fragment_profile);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        certificateRepository = new CertificateRepository();
        SessionStorage sessionStorage = new SessionStorage(requireContext());

        View profileHeader = view.findViewById(R.id.profileHeaderLayout);
        final int originalProfileHeaderTop = profileHeader.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            profileHeader.setPadding(
                    profileHeader.getPaddingLeft(),
                    originalProfileHeaderTop + top,
                    profileHeader.getPaddingRight(),
                    profileHeader.getPaddingBottom()
            );
            return WindowInsetsCompat.CONSUMED;
        });

        bindStaticUserInfo(view, sessionStorage);
        observeProfile(view);
        wireActionRows(view);
        profileViewModel.loadProfile();
        loadCertificates(view);

        view.findViewById(R.id.logoutButton).setOnClickListener(v -> {
            authViewModel.signOut();
            NavOptions options = new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph, true)
                    .build();
            Navigation.findNavController(view)
                    .navigate(R.id.action_profileFragment_to_loginFragment, null, options);
        });
    }

    private void bindStaticUserInfo(View view, SessionStorage sessionStorage) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        TextView avatarInitials = view.findViewById(R.id.avatarInitials);
        TextView nameView = view.findViewById(R.id.profileName);
        TextView emailView = view.findViewById(R.id.profileEmail);
        TextView roleView = view.findViewById(R.id.profileRole);
        TextView metaStatusView = view.findViewById(R.id.profileMetaStatus);
        TextView internalIdValueView = view.findViewById(R.id.profileInternalIdValue);
        TextView verificationValueView = view.findViewById(R.id.profileVerificationValue);

        String displayName = user != null && user.getDisplayName() != null
                ? user.getDisplayName()
                : extractNameFromEmail(user);
        String email = user != null && user.getEmail() != null
                ? user.getEmail()
                : getString(R.string.profile_placeholder_value);
        String role = sessionStorage.getRole() != null ? sessionStorage.getRole().toUpperCase() : "STUDENT";
        boolean emailVerified = user != null && user.isEmailVerified();
        String internalUserId = sessionStorage.getUserId();
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

        view.<TextView>findViewById(R.id.profileStatPrimaryValue).setText(R.string.profile_placeholder_value);
        view.<TextView>findViewById(R.id.profileStatSecondaryValue).setText(R.string.profile_placeholder_value);
        view.<TextView>findViewById(R.id.profileStatTertiaryValue).setText(R.string.profile_placeholder_value);
    }

    private void observeProfile(View view) {
        TextView nameView = view.findViewById(R.id.profileName);
        TextView avatarInitials = view.findViewById(R.id.avatarInitials);
        TextView enrolledView = view.findViewById(R.id.profileStatPrimaryValue);
        TextView lessonsView = view.findViewById(R.id.profileStatSecondaryValue);
        TextView certificatesView = view.findViewById(R.id.profileStatTertiaryValue);

        profileViewModel.profile.observe(getViewLifecycleOwner(), profile -> {
            if (profile == null) {
                return;
            }

            if (profile.displayName != null && !profile.displayName.isBlank()) {
                nameView.setText(profile.displayName);
                avatarInitials.setText(getInitials(profile.displayName));
            }

            enrolledView.setText(String.valueOf(profile.enrolledCourses));
            lessonsView.setText(String.valueOf(profile.completedLessons));
            certificatesView.setText(String.valueOf(profile.certificates));
        });

        profileViewModel.error.observe(getViewLifecycleOwner(), message -> {
            if (message == null || message.isBlank() || !isAdded()) {
                return;
            }

            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        });

        profileViewModel.saveMessage.observe(getViewLifecycleOwner(), message -> {
            if (message == null || message.isBlank() || !isAdded()) {
                return;
            }

            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        });
    }

    private void wireActionRows(View view) {
        // Notifications are explicitly deferred in the EduLife MVP plan, so remove the dead row from the active UI.
        view.findViewById(R.id.settingsNotifications).setVisibility(View.GONE);

        view.findViewById(R.id.settingsEditProfile).setOnClickListener(v -> showEditProfileDialog());
        view.findViewById(R.id.settingsLanguage).setOnClickListener(v ->
                showInfoDialog(R.string.profile_language_title, R.string.profile_language_message));
        view.findViewById(R.id.settingsPrivacy).setOnClickListener(v ->
                showInfoDialog(R.string.profile_privacy_title, R.string.profile_privacy_message));
        view.findViewById(R.id.settingsAbout).setOnClickListener(v ->
                showInfoDialog(R.string.profile_about_title, R.string.profile_about_message));
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
        if (role == null || role.isBlank()) {
            return "Student";
        }

        String normalized = role.replace('_', ' ').trim().toLowerCase();
        String[] parts = normalized.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.length() == 0 ? "Student" : builder.toString();
    }

    private String extractNameFromEmail(FirebaseUser user) {
        if (user == null || user.getEmail() == null) {
            return "Learner";
        }

        String email = user.getEmail();
        int atIndex = email.indexOf('@');
        if (atIndex > 0) {
            String local = email.substring(0, atIndex);
            return local.substring(0, 1).toUpperCase() + local.substring(1);
        }
        return "Learner";
    }

    private String getInitials(String name) {
        if (name == null || name.isEmpty()) {
            return "?";
        }

        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return String.valueOf(parts[0].charAt(0)).toUpperCase()
                    + String.valueOf(parts[1].charAt(0)).toUpperCase();
        }
        return String.valueOf(name.charAt(0)).toUpperCase();
    }

    private void loadCertificates(View view) {
        certificateRepository.loadMyCertificates(new CertificateRepository.CertificatesCallback() {
            @Override
            public void onSuccess(List<CertificateDto> certificates) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> renderCertificates(view, certificates));
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), R.string.cert_loading_error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void renderCertificates(View view, List<CertificateDto> certificates) {
        LinearLayout container = view.findViewById(R.id.certificatesContainer);
        container.removeAllViews();

        if (certificates == null || certificates.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText(R.string.cert_empty);
            empty.setTextColor(requireContext().getColor(R.color.brand_text_secondary));
            empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
            empty.setLineSpacing(0f, 1.4f);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.bottomMargin = dp(8);
            empty.setLayoutParams(params);
            container.addView(empty);
            return;
        }

        for (CertificateDto certificate : certificates) {
            container.addView(buildCertificateCard(certificate));
        }
    }

    private View buildCertificateCard(CertificateDto certificate) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_settings_item);
        card.setPadding(dp(20), dp(16), dp(20), dp(16));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = dp(10);
        card.setLayoutParams(params);

        TextView numberLabel = new TextView(requireContext());
        numberLabel.setText(getString(R.string.cert_number_label).toUpperCase());
        numberLabel.setTextColor(requireContext().getColor(R.color.brand_text_secondary));
        numberLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
        numberLabel.setTypeface(numberLabel.getTypeface(), Typeface.BOLD);

        TextView numberValue = new TextView(requireContext());
        numberValue.setText(certificate.certificateNumber != null
                ? certificate.certificateNumber
                : getString(R.string.profile_placeholder_value));
        numberValue.setTextColor(requireContext().getColor(R.color.brand_text_primary));
        numberValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
        numberValue.setTypeface(numberValue.getTypeface(), Typeface.BOLD);
        numberValue.setPadding(0, dp(4), 0, dp(10));

        TextView issuedLabel = new TextView(requireContext());
        issuedLabel.setText(getString(R.string.cert_issued_label).toUpperCase());
        issuedLabel.setTextColor(requireContext().getColor(R.color.brand_text_secondary));
        issuedLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
        issuedLabel.setTypeface(issuedLabel.getTypeface(), Typeface.BOLD);

        TextView issuedValue = new TextView(requireContext());
        issuedValue.setText(formatIsoDate(certificate.issuedAt));
        issuedValue.setTextColor(requireContext().getColor(R.color.brand_text_body));
        issuedValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        issuedValue.setPadding(0, dp(4), 0, 0);

        card.addView(numberLabel);
        card.addView(numberValue);
        card.addView(issuedLabel);
        card.addView(issuedValue);
        return card;
    }

    private void showEditProfileDialog() {
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(24), dp(16), dp(24), 0);

        EditText displayNameInput = new EditText(requireContext());
        displayNameInput.setHint(R.string.profile_edit_name);

        ProfileResponse currentProfile = profileViewModel.profile.getValue();
        String currentDisplayName = currentProfile != null && currentProfile.displayName != null
                ? currentProfile.displayName
                : ((TextView) requireView().findViewById(R.id.profileName)).getText().toString();
        String currentBio = currentProfile != null && currentProfile.bio != null ? currentProfile.bio : "";

        displayNameInput.setText(currentDisplayName);

        EditText bioInput = new EditText(requireContext());
        bioInput.setHint(R.string.profile_edit_bio);
        bioInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        bioInput.setMinLines(3);
        bioInput.setText(currentBio);

        container.addView(displayNameInput);
        container.addView(bioInput);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.profile_edit_title)
                .setView(container)
                .setNegativeButton(R.string.profile_edit_cancel, null)
                .setPositiveButton(R.string.profile_edit_save, null)
                .create();

        dialog.setOnShowListener(unused -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String displayName = displayNameInput.getText().toString().trim();
            String bio = bioInput.getText().toString().trim();

            if (displayName.isBlank()) {
                displayNameInput.setError(getString(R.string.profile_edit_name_required));
                return;
            }

            // Profile updates must flow through the backend so learner identity stays consistent across devices.
            profileViewModel.updateProfile(displayName, bio);
            dialog.dismiss();
        }));

        dialog.show();
    }

    private void showInfoDialog(int titleResId, int messageResId) {
        new AlertDialog.Builder(requireContext())
                .setTitle(titleResId)
                .setMessage(messageResId)
                .setPositiveButton(R.string.profile_dialog_close, null)
                .show();
    }

    private String formatIsoDate(String iso) {
        if (iso == null || iso.length() < 10) {
            return getString(R.string.profile_placeholder_value);
        }
        return iso.substring(0, 10);
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                requireContext().getResources().getDisplayMetrics()
        );
    }
}
