package com.baghdad.edulife.features.auth.ui;

import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Patterns;
import android.view.View;
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
import com.baghdad.edulife.core.storage.SessionStorage;
import com.baghdad.edulife.features.auth.model.AuthUiState;
import com.baghdad.edulife.features.auth.viewmodel.AuthViewModel;

public class LoginFragment extends Fragment {

    private AuthViewModel authViewModel;
    private View loginButton;
    private EditText emailEditText;
    private EditText passwordEditText;
    private View loginErrorCard;
    private TextView loginErrorText;
    private TextView loginButtonText;

    public LoginFragment() {
        super(R.layout.fragment_login);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        emailEditText = view.findViewById(R.id.emailEditText);
        passwordEditText = view.findViewById(R.id.passwordEditText);
        loginButton = view.findViewById(R.id.loginButton);
        loginErrorCard = view.findViewById(R.id.loginErrorCard);
        loginErrorText = view.findViewById(R.id.loginErrorText);
        loginButtonText = view.findViewById(R.id.loginButtonText);
        ImageButton passwordVisibilityButton = view.findViewById(R.id.passwordVisibilityButton);

        passwordVisibilityButton.setOnClickListener(v -> togglePasswordVisibility(passwordEditText));

        loginButton.setOnClickListener(v -> handleLogin());

        view.findViewById(R.id.forgotPasswordText).setOnClickListener(v ->
                // Password recovery will be wired when the auth module receives reset endpoints.
                Toast.makeText(requireContext(),
                        R.string.auth_forgot_password_unavailable, Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.registerRow).setOnClickListener(v ->
                // Navigate to the register screen so users who land on login can create an account.
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_loginFragment_to_registerFragment));

        authViewModel.getAuthState().observe(getViewLifecycleOwner(), this::renderAuthState);
    }

    private void handleLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();

        if (email.isEmpty()) {
            emailEditText.setError(getString(R.string.auth_email_required));
            return;
        }

        // Catch malformed addresses before the backend round-trip so users get a field-level
        // hint instead of a generic "Backend sync failed. Status: 400" message.
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError(getString(R.string.auth_email_invalid));
            return;
        }

        if (password.isEmpty()) {
            passwordEditText.setError(getString(R.string.auth_password_required));
            return;
        }

        hideError();
        authViewModel.login(email, password);
    }

    private void renderAuthState(AuthUiState state) {
        if (state == null) return;

        loginButton.setEnabled(!state.loading);
        loginButton.setAlpha(state.loading ? 0.65f : 1f);
        emailEditText.setEnabled(!state.loading);
        passwordEditText.setEnabled(!state.loading);
        // Mirror the backend-sync wait state in the button text so users know the request is still being processed.
        loginButtonText.setText(getString(state.loading
                ? R.string.auth_login_button_loading
                : R.string.auth_login_button_idle));

        if (state.loading) {
            hideError();
            return;
        }

        if (state.success) {
            SessionStorage session = new SessionStorage(requireContext());
            String role = session.getRole();
            int action;
            if ("ADMIN".equals(role)) {
                action = R.id.action_loginFragment_to_adminDashboardFragment;
            } else if ("TEACHER".equals(role)) {
                action = R.id.action_loginFragment_to_teacherDashboardFragment;
            } else {
                action = R.id.action_loginFragment_to_homeFragment;
            }
            Navigation.findNavController(requireView()).navigate(action);
            authViewModel.resetState();
            return;
        }

        if (state.emailVerificationRequired) {
            showError(getString(R.string.auth_login_email_not_verified));
            return;
        }

        if (state.message != null && !state.message.isBlank()) {
            showError(friendlyMessage(state.message));
        }
    }

    private void showError(String message) {
        loginErrorText.setText(message);
        loginErrorCard.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        loginErrorCard.setVisibility(View.GONE);
    }

    private String friendlyMessage(String raw) {
        if (raw.startsWith("Network error during sync:")) {
            return getString(R.string.auth_error_server_unreachable);
        }
        if (raw.startsWith("Backend sync timed out.")) {
            return getString(R.string.auth_error_server_timeout);
        }
        if (raw.startsWith("Backend sync failed.")) {
            return getString(R.string.auth_error_server_rejected,
                    raw.replace("Backend sync failed. Status: ", "HTTP "));
        }
        if (raw.contains("password") || raw.contains("credential") || raw.contains("no user")) {
            return getString(R.string.auth_error_bad_credentials);
        }
        if (raw.contains("verify")) {
            return getString(R.string.auth_error_email_not_verified);
        }
        if (raw.contains("network") || raw.contains("Unable to resolve") || raw.contains("timeout")) {
            return getString(R.string.auth_error_network);
        }
        return raw;
    }

    private void togglePasswordVisibility(@NonNull EditText passwordEditText) {
        boolean isHidden = passwordEditText.getTransformationMethod() instanceof PasswordTransformationMethod;

        // Keep password visibility local to the UI so no sensitive login state is persisted before auth exists.
        passwordEditText.setTransformationMethod(isHidden
                ? HideReturnsTransformationMethod.getInstance()
                : PasswordTransformationMethod.getInstance());
        passwordEditText.setSelection(passwordEditText.length());
    }
}
