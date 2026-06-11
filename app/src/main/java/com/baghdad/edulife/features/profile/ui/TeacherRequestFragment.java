package com.baghdad.edulife.features.profile.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.profile.model.TeacherRequestResponse;
import com.baghdad.edulife.features.profile.viewmodel.TeacherRequestViewModel;
import com.google.android.material.textfield.TextInputEditText;

public class TeacherRequestFragment extends Fragment {

    private TeacherRequestViewModel viewModel;

    public TeacherRequestFragment() {
        super(R.layout.fragment_teacher_request);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(TeacherRequestViewModel.class);

        View header = view.findViewById(R.id.teacherRequestHeaderLayout);
        final int origTop = header.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            header.setPadding(header.getPaddingLeft(), origTop + top,
                    header.getPaddingRight(), header.getPaddingBottom());
            return WindowInsetsCompat.CONSUMED;
        });

        view.findViewById(R.id.teacherRequestBackButton).setOnClickListener(v ->
                Navigation.findNavController(v).popBackStack());

        view.findViewById(R.id.teacherSubmitButton).setOnClickListener(v -> {
            TextInputEditText motivInput = view.findViewById(R.id.teacherMotivationInput);
            String motivation = motivInput.getText() != null
                    ? motivInput.getText().toString() : "";
            viewModel.submitTeacherRequest(motivation);
        });

        view.findViewById(R.id.teacherRetryButton).setOnClickListener(v -> viewModel.loadLatestRequest());

        observeState(view);
        viewModel.loadLatestRequest();
    }

    private void observeState(View view) {
        ProgressBar loading = view.findViewById(R.id.teacherRequestLoading);
        View errorView    = view.findViewById(R.id.teacherRequestErrorView);
        TextView errorText = view.findViewById(R.id.teacherErrorText);
        View formView     = view.findViewById(R.id.teacherRequestForm);
        View pendingCard  = view.findViewById(R.id.teacherPendingCard);
        View approvedCard = view.findViewById(R.id.teacherApprovedCard);
        View rejectionCard = view.findViewById(R.id.teacherRejectionCard);
        TextView adminNoteView = view.findViewById(R.id.teacherAdminNote);
        TextView requestedAtView = view.findViewById(R.id.teacherRequestedAt);
        View submitButton = view.findViewById(R.id.teacherSubmitButton);
        TextView submitLabel = view.findViewById(R.id.teacherSubmitLabel);

        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            loading.setVisibility(Boolean.TRUE.equals(isLoading) ? View.VISIBLE : View.GONE);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null && !err.isBlank()) {
                errorView.setVisibility(View.VISIBLE);
                errorText.setText(err);
                formView.setVisibility(View.GONE);
                pendingCard.setVisibility(View.GONE);
                approvedCard.setVisibility(View.GONE);
            } else {
                errorView.setVisibility(View.GONE);
            }
        });

        viewModel.getLatestRequest().observe(getViewLifecycleOwner(), req -> {
            errorView.setVisibility(View.GONE);
            if (req == null) {
                pendingCard.setVisibility(View.GONE);
                approvedCard.setVisibility(View.GONE);
                rejectionCard.setVisibility(View.GONE);
                formView.setVisibility(View.VISIBLE);
                return;
            }
            switch (req.status) {
                case "PENDING":
                    showPendingState(formView, pendingCard, approvedCard, requestedAtView, req);
                    break;
                case "REJECTED":
                    showRejectedState(formView, rejectionCard, pendingCard, approvedCard,
                            adminNoteView, req);
                    break;
                case "APPROVED":
                    formView.setVisibility(View.GONE);
                    pendingCard.setVisibility(View.GONE);
                    approvedCard.setVisibility(View.VISIBLE);
                    break;
            }
        });

        viewModel.getSubmitting().observe(getViewLifecycleOwner(), isSubmitting -> {
            boolean active = Boolean.TRUE.equals(isSubmitting);
            submitButton.setEnabled(!active);
            submitLabel.setText(active
                    ? getString(R.string.teacher_request_submitting)
                    : getString(R.string.teacher_request_submit));
        });

        viewModel.getSubmitMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg == null || msg.isBlank()) return;
            viewModel.clearSubmitMessage();
            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
        });
    }

    private void showPendingState(View form, View pending, View approved,
                                  TextView requestedAtView, TeacherRequestResponse req) {
        form.setVisibility(View.GONE);
        pending.setVisibility(View.VISIBLE);
        approved.setVisibility(View.GONE);
        if (req.requestedAt != null && req.requestedAt.length() >= 10) {
            requestedAtView.setText(req.requestedAt.substring(0, 10));
        }
    }

    private void showRejectedState(View form, View rejCard, View pending, View approved,
                                   TextView adminNoteView, TeacherRequestResponse req) {
        form.setVisibility(View.VISIBLE);
        pending.setVisibility(View.GONE);
        approved.setVisibility(View.GONE);
        rejCard.setVisibility(View.VISIBLE);
        if (req.adminNote != null && !req.adminNote.isBlank()) {
            adminNoteView.setText(req.adminNote);
            adminNoteView.setVisibility(View.VISIBLE);
        } else {
            adminNoteView.setVisibility(View.GONE);
        }
    }
}
