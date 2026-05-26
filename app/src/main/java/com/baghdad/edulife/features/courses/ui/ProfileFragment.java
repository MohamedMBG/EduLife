package com.baghdad.edulife.features.courses.ui;

import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

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
        final int origProfileHeaderTop = profileHeader.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            profileHeader.setPadding(profileHeader.getPaddingLeft(), origProfileHeaderTop + top,
                    profileHeader.getPaddingRight(), profileHeader.getPaddingBottom());
            return WindowInsetsCompat.CONSUMED;
        });

        bindStaticUserInfo(view, sessionStorage);
        observeProfile(view);
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

    private void bindStaticUserInfo(View view, SessionStorage session) {
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

        // Show placeholder zeros until API responds
        view.<TextView>findViewById(R.id.profileStatPrimaryValue).setText("—");
        view.<TextView>findViewById(R.id.profileStatSecondaryValue).setText("—");
        view.<TextView>findViewById(R.id.profileStatTertiaryValue).setText("—");
    }

    private void observeProfile(View view) {
        TextView nameView = view.findViewById(R.id.profileName);
        TextView avatarInitials = view.findViewById(R.id.avatarInitials);
        TextView enrolledView = view.findViewById(R.id.profileStatPrimaryValue);
        TextView lessonsView = view.findViewById(R.id.profileStatSecondaryValue);
        TextView certView = view.findViewById(R.id.profileStatTertiaryValue);

        profileViewModel.profile.observe(getViewLifecycleOwner(), (ProfileResponse profile) -> {
            if (profile == null) return;

            // Prefer server-side displayName over Firebase fallback
            if (profile.displayName != null && !profile.displayName.isBlank()) {
                nameView.setText(profile.displayName);
                avatarInitials.setText(getInitials(profile.displayName));
            }

            enrolledView.setText(String.valueOf(profile.enrolledCourses));
            lessonsView.setText(String.valueOf(profile.completedLessons));
            certView.setText(String.valueOf(profile.certificates));
        });
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
        if (atIdx > 0) {
            String local = email.substring(0, atIdx);
            return local.substring(0, 1).toUpperCase() + local.substring(1);
        }
        return "Learner";
    }

    private String getInitials(String name) {
        if (name == null || name.isEmpty()) return "?";
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
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> renderCertificates(view, certificates));
            }

            @Override
            public void onError(String message) {
                // Silent fail — certs section stays empty
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
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            p.bottomMargin = dp(8);
            empty.setLayoutParams(p);
            container.addView(empty);
            return;
        }

        for (CertificateDto cert : certificates) {
            container.addView(buildCertCard(cert));
        }
    }

    private View buildCertCard(CertificateDto cert) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_settings_item);
        card.setPadding(dp(20), dp(16), dp(20), dp(16));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(10);
        card.setLayoutParams(params);

        TextView numberLabel = new TextView(requireContext());
        numberLabel.setText(getString(R.string.cert_number_label).toUpperCase());
        numberLabel.setTextColor(requireContext().getColor(R.color.brand_text_secondary));
        numberLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
        numberLabel.setTypeface(numberLabel.getTypeface(), Typeface.BOLD);

        TextView numberValue = new TextView(requireContext());
        numberValue.setText(cert.certificateNumber != null ? cert.certificateNumber : "—");
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
        issuedValue.setText(formatIsoDate(cert.issuedAt));
        issuedValue.setTextColor(requireContext().getColor(R.color.brand_text_body));
        issuedValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        issuedValue.setPadding(0, dp(4), 0, 0);

        card.addView(numberLabel);
        card.addView(numberValue);
        card.addView(issuedLabel);
        card.addView(issuedValue);
        return card;
    }

    private String formatIsoDate(String iso) {
        if (iso == null || iso.length() < 10) return "—";
        // ISO 8601: 2026-05-26T17:13:45.123Z → "2026-05-26"
        return iso.substring(0, 10);
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value,
                requireContext().getResources().getDisplayMetrics());
    }
}
