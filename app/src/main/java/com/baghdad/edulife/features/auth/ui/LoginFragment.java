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

import com.baghdad.edulife.R;

public class LoginFragment extends Fragment {

    public LoginFragment() {
        super(R.layout.fragment_login);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText passwordEditText = view.findViewById(R.id.passwordEditText);
        ImageButton passwordVisibilityButton = view.findViewById(R.id.passwordVisibilityButton);

        passwordVisibilityButton.setOnClickListener(v -> togglePasswordVisibility(passwordEditText));

        view.findViewById(R.id.loginButton).setOnClickListener(v ->
                // Backend authentication is intentionally deferred until the auth API is connected.
                Toast.makeText(requireContext(), "Login action coming soon", Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.googleButton).setOnClickListener(v ->
                // Google sign-in needs OAuth configuration, so this screen only exposes a visual placeholder.
                Toast.makeText(requireContext(), "Google sign-in coming soon", Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.forgotPasswordText).setOnClickListener(v ->
                // Password recovery will be wired when the auth module receives reset endpoints.
                Toast.makeText(requireContext(), "Password recovery coming soon", Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.registerRow).setOnClickListener(v ->
                // Registration navigation stays as a placeholder until the app navigation graph is finalized.
                Toast.makeText(requireContext(), "Register flow coming soon", Toast.LENGTH_SHORT).show());
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
