package com.baghdad.edulife.features.courses.ui;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
import com.baghdad.edulife.features.profile.viewmodel.ProfileViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends Fragment {

    private AuthViewModel authViewModel;
    private ProfileViewModel profileViewModel;

    public ProfileFragment() {
        super(R.layout.fragment_profile);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
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

    private void showDeleteAccountDialog(View view) {
        EditText confirmInput = new EditText(requireContext());
        confirmInput.setHint(R.string.profile_delete_account_dialog_hint);
        // The confirmation only ever compares against the literal "DELETE", so capping the
        // input prevents a paste-bomb from reaching the network layer and short-circuits any
        // accidental over-length input before the equality check.
        confirmInput.setFilters(new android.text.InputFilter[] {
                new android.text.InputFilter.LengthFilter(16)
        });
        // Single-line + no suggestions so the typed string is not silently mangled into
        // "Delete" or autocompleted before the equality check.
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
                        // Stay on the dialog so the learner can correct; toast carries the exact
                        // requirement so we don't depend on the label alone.
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
            // Local sign-out clears Firebase + SessionStorage so the orphaned Bearer token
            // cannot be replayed even if it is still cached locally.
            authViewModel.signOut();
            Toast.makeText(requireContext(),
                    R.string.profile_delete_account_success, Toast.LENGTH_SHORT).show();
            NavOptions options = new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph, true)
                    .build();
            Navigation.findNavController(view)
                    .navigate(R.id.action_profileFragment_to_loginFragment, null, options);
            // Consume the signal so a re-attached observer cannot re-fire sign-out + nav.
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
        if (atIdx <= 0) return "Learner";
        // Defensive: a malformed Firebase email could leave local empty after the substring,
        // so guard substring(1) which would otherwise crash on a single-char local part.
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
        // Use the first two non-blank parts (split on a leading space can leave an empty
        // string in parts[0] on certain inputs, so guard before charAt).
        StringBuilder out = new StringBuilder(2);
        for (String part : parts) {
            if (part.isEmpty()) continue;
            out.append(Character.toUpperCase(part.charAt(0)));
            if (out.length() == 2) break;
        }
        return out.length() == 0 ? "?" : out.toString();
    }
}
