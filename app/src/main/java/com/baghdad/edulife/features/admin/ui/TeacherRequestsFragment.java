package com.baghdad.edulife.features.admin.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.admin.model.TeacherRequestsUiState;
import com.baghdad.edulife.features.admin.viewmodel.TeacherRequestsViewModel;
import com.google.android.material.progressindicator.CircularProgressIndicator;

public class TeacherRequestsFragment extends Fragment {

    private TeacherRequestsViewModel viewModel;
    private TeacherRequestAdapter adapter;

    private View stateCard;
    private CircularProgressIndicator loadingIndicator;
    private TextView stateText;
    private TextView retryButton;
    private RecyclerView recyclerView;
    private TextView actionMessage;
    private TextView filterPending;
    private TextView filterApproved;
    private TextView filterRejected;

    public TeacherRequestsFragment() {
        super(R.layout.fragment_admin_teacher_requests);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(TeacherRequestsViewModel.class);

        stateCard = view.findViewById(R.id.teacherRequestsStateCard);
        loadingIndicator = view.findViewById(R.id.teacherRequestsLoadingIndicator);
        stateText = view.findViewById(R.id.teacherRequestsStateText);
        retryButton = view.findViewById(R.id.teacherRequestsRetryButton);
        recyclerView = view.findViewById(R.id.teacherRequestsRecyclerView);
        actionMessage = view.findViewById(R.id.teacherRequestsActionMessage);
        filterPending = view.findViewById(R.id.filterPending);
        filterApproved = view.findViewById(R.id.filterApproved);
        filterRejected = view.findViewById(R.id.filterRejected);

        view.findViewById(R.id.teacherRequestsBackButton).setOnClickListener(v ->
                Navigation.findNavController(view).popBackStack());

        adapter = new TeacherRequestAdapter(new TeacherRequestAdapter.ActionListener() {
            @Override
            public void onApprove(String requestId) {
                new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.admin_approve_confirm_title)
                        .setMessage(R.string.admin_approve_confirm_message)
                        .setPositiveButton(R.string.admin_approve, (d, w) -> viewModel.approveRequest(requestId))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }

            @Override
            public void onReject(String requestId) {
                View dialogView = getLayoutInflater().inflate(R.layout.dialog_reject_request, null);
                EditText noteInput = dialogView.findViewById(R.id.rejectNoteInput);
                new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.admin_reject_confirm_title)
                        .setView(dialogView)
                        .setPositiveButton(R.string.admin_reject, (d, w) -> {
                            String note = noteInput.getText().toString().trim();
                            viewModel.rejectRequest(requestId, note.isEmpty() ? null : note);
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        retryButton.setOnClickListener(v -> viewModel.loadRequests(viewModel.getCurrentFilter()));

        filterPending.setOnClickListener(v -> {
            viewModel.loadRequests("PENDING");
            updateFilterTabs("PENDING");
        });
        filterApproved.setOnClickListener(v -> {
            viewModel.loadRequests("APPROVED");
            updateFilterTabs("APPROVED");
        });
        filterRejected.setOnClickListener(v -> {
            viewModel.loadRequests("REJECTED");
            updateFilterTabs("REJECTED");
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);

        if (viewModel.getUiState().getValue() == null
                || viewModel.getUiState().getValue().loading) {
            viewModel.loadRequests("PENDING");
        }
    }

    private void render(@Nullable TeacherRequestsUiState state) {
        if (state == null) return;

        actionMessage.setVisibility(state.actionMessage != null ? View.VISIBLE : View.GONE);
        if (state.actionMessage != null) {
            actionMessage.setText(state.actionMessage);
        }

        if (state.loading) {
            stateCard.setVisibility(View.VISIBLE);
            loadingIndicator.setVisibility(View.VISIBLE);
            stateText.setText(R.string.admin_loading);
            retryButton.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);
            return;
        }

        if (state.errorMessage != null) {
            stateCard.setVisibility(View.VISIBLE);
            loadingIndicator.setVisibility(View.GONE);
            stateText.setText(state.errorMessage);
            retryButton.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            return;
        }

        if (state.requests != null && !state.requests.isEmpty()) {
            stateCard.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.submitList(state.requests);
        } else {
            stateCard.setVisibility(View.VISIBLE);
            loadingIndicator.setVisibility(View.GONE);
            stateText.setText(R.string.admin_requests_empty);
            retryButton.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);
        }
    }

    private void updateFilterTabs(String active) {
        styleTab(filterPending, "PENDING".equals(active));
        styleTab(filterApproved, "APPROVED".equals(active));
        styleTab(filterRejected, "REJECTED".equals(active));
    }

    private void styleTab(TextView tab, boolean selected) {
        if (tab == null) return;
        tab.setBackgroundResource(selected
                ? R.drawable.bg_catalog_filter_button_active
                : R.drawable.bg_catalog_filter_button);
        tab.setTextColor(requireContext().getColor(selected
                ? android.R.color.white
                : R.color.brand_primary));
    }
}
