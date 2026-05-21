package com.baghdad.edulife.features.auth.ui;

import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
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
        ImageButton passwordVisibilityButton = view.findViewById(R.id.passwordVisibilityButton);

        passwordVisibilityButton.setOnClickListener(v -> togglePasswordVisibility(passwordEditText));

        loginButton.setOnClickListener(v -> handleLogin());

        view.findViewById(R.id.googleButton).setOnClickListener(v ->
                // Google sign-in needs OAuth configuration, so this screen only exposes a visual placeholder.
                Toast.makeText(requireContext(), "Google sign-in coming soon", Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.forgotPasswordText).setOnClickListener(v ->
                // Password recovery will be wired when the auth module receives reset endpoints.
                Toast.makeText(requireContext(), "Password recovery coming soon", Toast.LENGTH_SHORT).show());

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
            emailEditText.setError("Email is required");
            return;
        }

        if (password.isEmpty()) {
            passwordEditText.setError("Password is required");
            return;
        }

        authViewModel.login(email, password);
    }

    private void renderAuthState(AuthUiState state) {
        if (state == null) return;

        loginButton.setEnabled(!state.loading);

        if (state.loading) {
            Toast.makeText(requireContext(), "Logging in...", Toast.LENGTH_SHORT).show();
            return;
        }

        if (state.success) {
            // Navigate to the catalog only after Firebase login + email verification + backend sync all succeed.
            // The popUpTo in the nav graph action clears the entire auth back stack so the user
            // cannot press back to return to the login or onboarding screens.
            Navigation.findNavController(requireView()).navigate(R.id.action_loginFragment_to_homeFragment);
            authViewModel.resetState();
            return;
        }

        if (state.emailVerificationRequired) {
            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show();
            return;
        }

        if (state.message != null && !state.message.isBlank() && !state.success) {
            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show();
        }
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
