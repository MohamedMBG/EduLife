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

import com.baghdad.edulife.R;

public class RegisterFragment extends Fragment {

    public RegisterFragment() {
        super(R.layout.fragment_register);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText fullNameInput = view.findViewById(R.id.fullNameInput);
        EditText emailInput = view.findViewById(R.id.emailInput);
        EditText passwordInput = view.findViewById(R.id.passwordInput);
        EditText confirmPasswordInput = view.findViewById(R.id.confirmPasswordInput);
        CheckBox termsCheckbox = view.findViewById(R.id.termsCheckbox);
        View createAccountButton = view.findViewById(R.id.createAccountButton);
        View googleRegisterButton = view.findViewById(R.id.googleRegisterButton);
        View loginText = view.findViewById(R.id.loginText);
        ImageButton passwordVisibilityToggle = view.findViewById(R.id.passwordVisibilityToggle);
        ImageButton confirmPasswordVisibilityToggle =
                view.findViewById(R.id.confirmPasswordVisibilityToggle);

        passwordVisibilityToggle.setOnClickListener(
                v -> togglePasswordVisibility(passwordInput));
        confirmPasswordVisibilityToggle.setOnClickListener(
                v -> togglePasswordVisibility(confirmPasswordInput));

        createAccountButton.setOnClickListener(v ->
                // Registration is intentionally UI-only until the MVP auth API/Firebase decision is finalized.
                Toast.makeText(requireContext(), "Registration coming soon", Toast.LENGTH_SHORT).show());

        googleRegisterButton.setOnClickListener(v ->
                // Google sign-up needs OAuth configuration, so this screen only exposes a visual placeholder.
                Toast.makeText(requireContext(), "Google sign-up coming soon", Toast.LENGTH_SHORT).show());

        loginText.setOnClickListener(v ->
                // Login navigation stays as a placeholder until the app navigation graph is finalized.
                Toast.makeText(requireContext(), "Login flow coming soon", Toast.LENGTH_SHORT).show());

        termsCheckbox.setOnClickListener(v -> {
            // The checkbox records consent intent locally; validation will belong in the auth ViewModel later.
        });

        // Keep references explicit for the future ViewModel handoff without adding backend behavior yet.
        fullNameInput.clearFocus();
        emailInput.clearFocus();
    }

    private void togglePasswordVisibility(@NonNull EditText passwordInput) {
        boolean isHidden = passwordInput.getTransformationMethod() instanceof PasswordTransformationMethod;

        // Keep password visibility local to the UI so no sensitive registration state is persisted before auth exists.
        passwordInput.setTransformationMethod(isHidden
                ? HideReturnsTransformationMethod.getInstance()
                : PasswordTransformationMethod.getInstance());
        passwordInput.setSelection(passwordInput.length());
    }
}
