package com.baghdad.edulife.features.auth.ui;

import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.auth.model.AuthUiState;
import com.baghdad.edulife.features.auth.viewmodel.AuthViewModel;

public class RegisterFragment extends Fragment {

    private AuthViewModel authViewModel;

    private EditText fullNameInput;
    private EditText emailInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;
    private CheckBox termsCheckbox;
    private View createAccountButton;

    public RegisterFragment() {
        super(R.layout.fragment_register);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        fullNameInput = view.findViewById(R.id.fullNameInput);
        emailInput = view.findViewById(R.id.emailInput);
        passwordInput = view.findViewById(R.id.passwordInput);
        confirmPasswordInput = view.findViewById(R.id.confirmPasswordInput);
        termsCheckbox = view.findViewById(R.id.termsCheckbox);
        createAccountButton = view.findViewById(R.id.createAccountButton);

        View googleRegisterButton = view.findViewById(R.id.googleRegisterButton);
        View loginText = view.findViewById(R.id.loginText);
        ImageButton passwordVisibilityToggle = view.findViewById(R.id.passwordVisibilityToggle);
        ImageButton confirmPasswordVisibilityToggle =
                view.findViewById(R.id.confirmPasswordVisibilityToggle);

        passwordVisibilityToggle.setOnClickListener(
                v -> togglePasswordVisibility(passwordInput));
        confirmPasswordVisibilityToggle.setOnClickListener(
                v -> togglePasswordVisibility(confirmPasswordInput));

        createAccountButton.setOnClickListener(v -> handleRegister());

        googleRegisterButton.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Google sign-up coming soon", Toast.LENGTH_SHORT).show());

        loginText.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Login flow coming soon", Toast.LENGTH_SHORT).show());

        authViewModel.getAuthState().observe(getViewLifecycleOwner(), this::renderAuthState);

        fullNameInput.clearFocus();
        emailInput.clearFocus();
    }

    private void handleRegister() {
        String fullName = fullNameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();

        if (fullName.isEmpty()) {
            fullNameInput.setError("Full name is required");
            return;
        }

        if (email.isEmpty()) {
            emailInput.setError("Email is required");
            return;
        }

        if (password.isEmpty()) {
            passwordInput.setError("Password is required");
            return;
        }

        if (password.length() < 6) {
            passwordInput.setError("Password must be at least 6 characters");
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError("Passwords do not match");
            return;
        }

        if (!termsCheckbox.isChecked()) {
            Toast.makeText(requireContext(), "Please accept the terms before continuing.", Toast.LENGTH_SHORT).show();
            return;
        }

        authViewModel.register(email, password);
    }

    private void renderAuthState(AuthUiState state) {
        if (state == null) return;

        createAccountButton.setEnabled(!state.loading);

        if (state.loading) {
            Toast.makeText(requireContext(), "Creating account...", Toast.LENGTH_SHORT).show();
            return;
        }

        if (state.emailVerificationRequired) {
            Toast.makeText(
                    requireContext(),
                    "Account created. Please verify your email before logging in.",
                    Toast.LENGTH_LONG
            ).show();
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
}