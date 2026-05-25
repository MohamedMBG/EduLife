package com.baghdad.edulife.features.courses.ui;

import android.os.Bundle;
import android.view.View;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends Fragment {

    private AuthViewModel authViewModel;

    public ProfileFragment() {
        super(R.layout.fragment_profile);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        SessionStorage sessionStorage = new SessionStorage(requireContext());

        View profileHeader = view.findViewById(R.id.profileHeaderLayout);
        final int origProfileHeaderTop = profileHeader.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            profileHeader.setPadding(profileHeader.getPaddingLeft(), origProfileHeaderTop + top,
                    profileHeader.getPaddingRight(), profileHeader.getPaddingBottom());
            return WindowInsetsCompat.CONSUMED;
        });

        bindUserInfo(view, sessionStorage);

        view.findViewById(R.id.logoutButton).setOnClickListener(v -> {
            authViewModel.signOut();
            NavOptions options = new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph, true)
                    .build();
            Navigation.findNavController(view)
                    .navigate(R.id.action_profileFragment_to_loginFragment, null, options);
        });
    }

    private void bindUserInfo(View view, SessionStorage session) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        TextView avatarInitials = view.findViewById(R.id.avatarInitials);
        TextView nameView = view.findViewById(R.id.profileName);
        TextView emailView = view.findViewById(R.id.profileEmail);
        TextView roleView = view.findViewById(R.id.profileRole);
        TextView metaStatusView = view.findViewById(R.id.profileMetaStatus);
        TextView primaryValueView = view.findViewById(R.id.profileStatPrimaryValue);
        TextView secondaryValueView = view.findViewById(R.id.profileStatSecondaryValue);
        TextView tertiaryValueView = view.findViewById(R.id.profileStatTertiaryValue);
        TextView internalIdValueView = view.findViewById(R.id.profileInternalIdValue);
        TextView verificationValueView = view.findViewById(R.id.profileVerificationValue);

        String displayName = user != null && user.getDisplayName() != null
                ? user.getDisplayName()
                : extractNameFromEmail(user);
        String email = user != null && user.getEmail() != null ? user.getEmail() : "—";
        String role = session.getRole() != null ? session.getRole().toUpperCase() : "STUDENT";
        String displayRole = formatRole(role);
        boolean emailVerified = user != null && user.isEmailVerified();
        String internalUserId = session.getUserId();
        boolean syncReady = internalUserId != null && !internalUserId.isBlank();

        nameView.setText(displayName);
        emailView.setText(email);
        roleView.setText(displayRole.toUpperCase());
        avatarInitials.setText(getInitials(displayName));
        // Surface real account state instead of placeholder progress numbers so the profile
        // remains trustworthy before enrollments, progress, and certificates are fully wired.
        metaStatusView.setText(buildMetaStatus(emailVerified, syncReady));
        primaryValueView.setText(displayRole);
        secondaryValueView.setText(getString(emailVerified
                ? R.string.profile_value_verified
                : R.string.profile_value_pending));
        tertiaryValueView.setText(getString(syncReady
                ? R.string.profile_value_ready
                : R.string.profile_value_not_ready));
        internalIdValueView.setText(syncReady
                ? internalUserId
                : getString(R.string.profile_internal_id_missing));
        verificationValueView.setText(getString(emailVerified
                ? R.string.profile_access_verified
                : R.string.profile_access_pending));
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
}
