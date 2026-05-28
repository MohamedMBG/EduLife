package com.baghdad.edulife.features.auth.ui;

import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
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

        // The MVP ships only email/password auth, so unsupported OAuth buttons stay hidden instead of behaving like dead links.
        view.findViewById(R.id.loginDividerRow).setVisibility(View.GONE);
        view.findViewById(R.id.googleButton).setVisibility(View.GONE);

        view.findViewById(R.id.forgotPasswordText).setOnClickListener(v ->
                sendPasswordResetEmail());

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
        loginButtonText.setText(state.loading
                ? R.string.auth_signing_in
                : R.string.auth_log_in);

        if (state.loading) {
            hideError();
            return;
        }

        if (state.success) {
            Navigation.findNavController(requireView()).navigate(R.id.action_loginFragment_to_homeFragment);
            authViewModel.resetState();
            return;
        }

        if (state.emailVerificationRequired) {
            showError("Email not verified. Check your inbox and click the verification link, then try again.");
            return;
        }

        if (state.message != null && !state.message.isBlank()) {
            showError(friendlyMessage(state.message));
        }
    }

    private void sendPasswordResetEmail() {
        String email = emailEditText.getText().toString().trim();
        if (email.isEmpty()) {
            emailEditText.setError(getString(R.string.auth_email_required));
            Toast.makeText(requireContext(), R.string.auth_reset_email_prompt, Toast.LENGTH_SHORT).show();
            return;
        }

        authViewModel.sendPasswordResetEmail(email, (success, message) -> {
            if (!isAdded()) {
                return;
            }

            Toast.makeText(
                    requireContext(),
                    success ? R.string.auth_reset_email_sent : R.string.auth_reset_email_failed,
                    Toast.LENGTH_SHORT
            ).show();
        });
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
            return "Cannot reach the server. Make sure your phone is on the right network and the backend is running.";
        }
        if (raw.startsWith("Backend sync timed out.")) {
            return "The server did not answer in time. Check that the backend is running and reachable at the configured IP address.";
        }
        if (raw.startsWith("Backend sync failed.")) {
            return "Server rejected the request (" + raw.replace("Backend sync failed. Status: ", "HTTP ") + "). Contact support if this persists.";
        }
        if (raw.contains("password") || raw.contains("credential") || raw.contains("no user")) {
            return "Incorrect email or password. Please try again.";
        }
        if (raw.contains("verify")) {
            return "Email not verified. Check your inbox and click the verification link.";
        }
        if (raw.contains("network") || raw.contains("Unable to resolve") || raw.contains("timeout")) {
            return "Network error. Check your internet connection and try again.";
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
