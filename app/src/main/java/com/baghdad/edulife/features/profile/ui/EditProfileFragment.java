package com.baghdad.edulife.features.profile.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.profile.model.ProfileResponse;
import com.baghdad.edulife.features.profile.viewmodel.ProfileViewModel;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class EditProfileFragment extends Fragment {

    private ProfileViewModel profileViewModel;
    private TextInputLayout displayNameLayout;
    private TextInputEditText displayNameInput;
    private TextInputEditText bioInput;
    private Button saveButton;
    private ProgressBar savingIndicator;

    public EditProfileFragment() {
        super(R.layout.fragment_edit_profile);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);

        displayNameLayout = view.findViewById(R.id.displayNameLayout);
        displayNameInput = view.findViewById(R.id.displayNameInput);
        bioInput = view.findViewById(R.id.bioInput);
        saveButton = view.findViewById(R.id.saveButton);
        savingIndicator = view.findViewById(R.id.savingIndicator);

        view.findViewById(R.id.backButton).setOnClickListener(v ->
                Navigation.findNavController(view).popBackStack());

        prefillFromCurrentProfile();

        saveButton.setOnClickListener(v -> attemptSave());

        observeSaveState();
    }

    private void prefillFromCurrentProfile() {
        ProfileResponse current = profileViewModel.profile.getValue();
        if (current == null) return;

        if (current.displayName != null) {
            displayNameInput.setText(current.displayName);
        }
        if (current.bio != null) {
            bioInput.setText(current.bio);
        }
    }

    private void attemptSave() {
        displayNameLayout.setError(null);

        String name = displayNameInput.getText() != null
                ? displayNameInput.getText().toString().trim() : "";
        String bio = bioInput.getText() != null
                ? bioInput.getText().toString().trim() : "";

        if (name.isEmpty()) {
            displayNameLayout.setError(getString(R.string.edit_profile_name_required));
            return;
        }
        if (name.length() > 100) {
            displayNameLayout.setError(getString(R.string.edit_profile_name_too_long));
            return;
        }

        profileViewModel.updateProfile(name, bio);
    }

    private void observeSaveState() {
        profileViewModel.saving.observe(getViewLifecycleOwner(), saving -> {
            boolean isSaving = Boolean.TRUE.equals(saving);
            savingIndicator.setVisibility(isSaving ? View.VISIBLE : View.GONE);
            saveButton.setEnabled(!isSaving);
        });

        profileViewModel.saveSuccess.observe(getViewLifecycleOwner(), success -> {
            if (!Boolean.TRUE.equals(success)) return;
            profileViewModel.clearSaveSuccess();
            Toast.makeText(requireContext(), R.string.edit_profile_success, Toast.LENGTH_SHORT).show();
            Navigation.findNavController(requireView()).popBackStack();
        });

        profileViewModel.saveError.observe(getViewLifecycleOwner(), msg -> {
            if (msg == null || msg.isBlank()) return;
            profileViewModel.clearSaveError();
            Toast.makeText(requireContext(), R.string.edit_profile_error, Toast.LENGTH_SHORT).show();
        });
    }
}
