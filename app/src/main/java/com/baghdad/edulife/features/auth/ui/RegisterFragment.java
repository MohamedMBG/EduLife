package com.baghdad.edulife.features.auth.ui;

import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.auth.model.AuthUiState;
import com.baghdad.edulife.features.auth.model.RegisterRequest;
import com.baghdad.edulife.features.auth.viewmodel.AuthViewModel;

public class RegisterFragment extends Fragment {
    private static final String STATE_SELECTED_ROLE = "selectedRole";
    private static final String STATE_ROLE_STEP_ACTIVE = "roleStepActive";

    private AuthViewModel authViewModel;

    private TextView eyebrowText;
    private TextView titleText;
    private TextView infoMessageText;
    private TextView stepIndicatorText;
    private View roleStepContainer;
    private View credentialsStepContainer;
    private View continueRoleButton;
    private View backToRoleButton;
    private View learnerRoleCard;
    private View teacherRoleCard;
    private View groupAdminRoleCard;
    private EditText fullNameInput;
    private EditText emailInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;
    private CheckBox termsCheckbox;
    private View createAccountButton;
    private String selectedRole;
    private boolean roleStepActive = true;

    public RegisterFragment() {
        super(R.layout.fragment_register);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        eyebrowText = view.findViewById(R.id.eyebrowText);
        titleText = view.findViewById(R.id.titleText);
        infoMessageText = view.findViewById(R.id.infoMessageText);
        stepIndicatorText = view.findViewById(R.id.stepIndicatorText);
        roleStepContainer = view.findViewById(R.id.roleStepContainer);
        credentialsStepContainer = view.findViewById(R.id.credentialsStepContainer);
        continueRoleButton = view.findViewById(R.id.continueRoleButton);
        backToRoleButton = view.findViewById(R.id.backToRoleButton);
        learnerRoleCard = view.findViewById(R.id.learnerRoleCard);
        teacherRoleCard = view.findViewById(R.id.teacherRoleCard);
        groupAdminRoleCard = view.findViewById(R.id.groupAdminRoleCard);
        fullNameInput = view.findViewById(R.id.fullNameInput);
        emailInput = view.findViewById(R.id.emailInput);
        passwordInput = view.findViewById(R.id.passwordInput);
        confirmPasswordInput = view.findViewById(R.id.confirmPasswordInput);
        termsCheckbox = view.findViewById(R.id.termsCheckbox);
        createAccountButton = view.findViewById(R.id.createAccountButton);

        View loginText = view.findViewById(R.id.loginText);
        ImageButton passwordVisibilityToggle = view.findViewById(R.id.passwordVisibilityToggle);
        ImageButton confirmPasswordVisibilityToggle =
                view.findViewById(R.id.confirmPasswordVisibilityToggle);

        passwordVisibilityToggle.setOnClickListener(
                v -> togglePasswordVisibility(passwordInput));
        confirmPasswordVisibilityToggle.setOnClickListener(
                v -> togglePasswordVisibility(confirmPasswordInput));

        learnerRoleCard.setOnClickListener(v -> selectRole(RegisterRequest.ROLE_LEARNER));
        teacherRoleCard.setOnClickListener(v -> selectRole(RegisterRequest.ROLE_TEACHER));
        groupAdminRoleCard.setOnClickListener(v -> selectRole(RegisterRequest.ROLE_GROUP_ADMIN));
        continueRoleButton.setOnClickListener(v -> openCredentialsStep());
        backToRoleButton.setOnClickListener(v -> showRoleStep());
        createAccountButton.setOnClickListener(v -> handleRegister());

        loginText.setOnClickListener(v ->
                // Navigate back to the login screen; popUpTo in the nav graph action removes the register screen.
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_registerFragment_to_loginFragment));

        authViewModel.getAuthState().observe(getViewLifecycleOwner(), this::renderAuthState);

        if (savedInstanceState != null) {
            selectedRole = savedInstanceState.getString(STATE_SELECTED_ROLE);
            roleStepActive = savedInstanceState.getBoolean(STATE_ROLE_STEP_ACTIVE, true);
        }

        updateRoleSelectionUi();
        updateStepUi();

        fullNameInput.clearFocus();
        emailInput.clearFocus();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_SELECTED_ROLE, selectedRole);
        outState.putBoolean(STATE_ROLE_STEP_ACTIVE, roleStepActive);
    }

    private void handleRegister() {
        if (selectedRole == null || selectedRole.isBlank()) {
            Toast.makeText(requireContext(),
                    R.string.register_role_required, Toast.LENGTH_SHORT).show();
            showRoleStep();
            return;
        }

        String fullName = fullNameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();

        if (fullName.isEmpty()) {
            fullNameInput.setError(getString(R.string.auth_full_name_required));
            return;
        }

        if (email.isEmpty()) {
            emailInput.setError(getString(R.string.auth_email_required));
            return;
        }

        if (password.isEmpty()) {
            passwordInput.setError(getString(R.string.auth_password_required));
            return;
        }

        if (password.length() < 6) {
            passwordInput.setError(getString(R.string.auth_password_min_length));
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError(getString(R.string.auth_passwords_do_not_match));
            return;
        }

        if (!termsCheckbox.isChecked()) {
            Toast.makeText(requireContext(),
                    R.string.auth_terms_required, Toast.LENGTH_SHORT).show();
            return;
        }

        // "Student" in the UI maps to the backend LEARNER role so Android stays aligned with the
        // locked enum used by /api/v1/auth/sync.
        authViewModel.register(new RegisterRequest(fullName, email, password, selectedRole));
    }

    private void renderAuthState(AuthUiState state) {
        if (state == null) return;

        continueRoleButton.setEnabled(!state.loading);
        backToRoleButton.setEnabled(!state.loading);
        createAccountButton.setEnabled(!state.loading);

        if (state.loading) {
            Toast.makeText(requireContext(),
                    R.string.auth_creating_account, Toast.LENGTH_SHORT).show();
            return;
        }

        if (state.emailVerificationRequired) {
            Toast.makeText(requireContext(),
                    R.string.auth_register_verify_email, Toast.LENGTH_LONG).show();
            return;
        }

        if (!state.success && state.message != null && !state.message.isBlank()) {
            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show();
        }
    }

    private void togglePasswordVisibility(@NonNull EditText passwordInput) {
        boolean isHidden = passwordInput.getTransformationMethod() instanceof PasswordTransformationMethod;

        passwordInput.setTransformationMethod(isHidden
                ? HideReturnsTransformationMethod.getInstance()
                : PasswordTransformationMethod.getInstance());
        passwordInput.setSelection(passwordInput.length());
    }

    private void selectRole(String role) {
        selectedRole = role;
        updateRoleSelectionUi();
    }

    private void openCredentialsStep() {
        if (selectedRole == null || selectedRole.isBlank()) {
            Toast.makeText(requireContext(),
                    R.string.register_role_required, Toast.LENGTH_SHORT).show();
            return;
        }
        roleStepActive = false;
        updateStepUi();
    }

    private void showRoleStep() {
        roleStepActive = true;
        updateStepUi();
    }

    private void updateStepUi() {
        roleStepContainer.setVisibility(roleStepActive ? View.VISIBLE : View.GONE);
        credentialsStepContainer.setVisibility(roleStepActive ? View.GONE : View.VISIBLE);

        if (roleStepActive) {
            stepIndicatorText.setText(R.string.register_role_step_indicator);
            eyebrowText.setText(R.string.register_role_eyebrow);
            titleText.setText(R.string.register_role_title);
            infoMessageText.setText(R.string.register_role_info);
        } else {
            stepIndicatorText.setText(R.string.register_credentials_step_indicator);
            eyebrowText.setText(getString(R.string.register_role_profile_eyebrow, getRoleLabel(selectedRole)));
            titleText.setText(R.string.register_create_title);
            // Show the selected role here because the backend only honors it on first sync and
            // users need a clear confirmation before they submit credentials.
            infoMessageText.setText(getString(
                    R.string.register_role_selected_info,
                    getRoleLabel(selectedRole)
            ));
        }
    }

    private void updateRoleSelectionUi() {
        updateRoleCardSelection(learnerRoleCard, RegisterRequest.ROLE_LEARNER.equals(selectedRole));
        updateRoleCardSelection(teacherRoleCard, RegisterRequest.ROLE_TEACHER.equals(selectedRole));
        updateRoleCardSelection(groupAdminRoleCard, RegisterRequest.ROLE_GROUP_ADMIN.equals(selectedRole));
        continueRoleButton.setEnabled(selectedRole != null && !selectedRole.isBlank());
    }

    private void updateRoleCardSelection(View roleCard, boolean selected) {
        roleCard.setSelected(selected);
        roleCard.setBackgroundResource(R.drawable.bg_auth_role_option);
    }

    private String getRoleLabel(String role) {
        if (RegisterRequest.ROLE_TEACHER.equals(role)) {
            return getString(R.string.register_role_teacher_title);
        }
        if (RegisterRequest.ROLE_GROUP_ADMIN.equals(role)) {
            return getString(R.string.register_role_group_admin_title);
        }
        return getString(R.string.register_role_student_title);
    }
}
