package com.baghdad.edulife.features.groupadmin.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.groupadmin.model.ApprovalsUiState;
import com.baghdad.edulife.features.groupadmin.viewmodel.CourseApprovalsViewModel;
import com.baghdad.edulife.features.teacher.model.CmsCourse;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.Collections;
import java.util.List;

/**
 * Course approval queue for group admins: drafts authored by teachers in their groups can be
 * approved (published); already-published courses are shown read-only. Mirrors web /approvals.
 */
public class CourseApprovalsFragment extends Fragment {

    private CourseApprovalsViewModel viewModel;
    private ApprovalCourseAdapter pendingAdapter;
    private ApprovalCourseAdapter publishedAdapter;

    private View stateCard;
    private CircularProgressIndicator loadingIndicator;
    private TextView stateText;
    private TextView retryButton;
    private View content;
    private TextView pendingEmpty;
    private TextView publishedEmpty;

    public CourseApprovalsFragment() {
        super(R.layout.fragment_course_approvals);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(CourseApprovalsViewModel.class);

        view.findViewById(R.id.approvalsBack).setOnClickListener(v ->
                Navigation.findNavController(view).navigateUp());

        stateCard = view.findViewById(R.id.approvalsStateCard);
        loadingIndicator = view.findViewById(R.id.approvalsLoading);
        stateText = view.findViewById(R.id.approvalsStateText);
        retryButton = view.findViewById(R.id.approvalsRetry);
        content = view.findViewById(R.id.approvalsContent);
        pendingEmpty = view.findViewById(R.id.approvalsPendingEmpty);
        publishedEmpty = view.findViewById(R.id.approvalsPublishedEmpty);

        // Pending rows get an approve button; published rows are display-only (listener null).
        pendingAdapter = new ApprovalCourseAdapter(this::confirmApprove);
        RecyclerView pendingRecycler = view.findViewById(R.id.approvalsPendingRecycler);
        pendingRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        pendingRecycler.setAdapter(pendingAdapter);

        publishedAdapter = new ApprovalCourseAdapter(null);
        RecyclerView publishedRecycler = view.findViewById(R.id.approvalsPublishedRecycler);
        publishedRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        publishedRecycler.setAdapter(publishedAdapter);

        retryButton.setOnClickListener(v -> viewModel.load());

        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);
        viewModel.getMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg == null) return;
            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
            viewModel.clearMessage();
        });

        viewModel.load();
    }

    private void confirmApprove(CmsCourse course) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.approvals_confirm_title)
                .setMessage(getString(R.string.approvals_confirm_body,
                        course.title != null ? course.title : "this course"))
                .setPositiveButton(R.string.approvals_confirm_approve,
                        (d, w) -> viewModel.approve(course.id))
                .setNegativeButton(R.string.group_admin_cancel_button, null)
                .show();
    }

    private void render(@Nullable ApprovalsUiState state) {
        if (state == null) return;

        if (state.loading) {
            stateCard.setVisibility(View.VISIBLE);
            loadingIndicator.setVisibility(View.VISIBLE);
            stateText.setText(R.string.group_admin_loading);
            retryButton.setVisibility(View.GONE);
            content.setVisibility(View.GONE);
            return;
        }

        if (state.error != null) {
            stateCard.setVisibility(View.VISIBLE);
            loadingIndicator.setVisibility(View.GONE);
            stateText.setText(state.error);
            retryButton.setVisibility(View.VISIBLE);
            content.setVisibility(View.GONE);
            return;
        }

        stateCard.setVisibility(View.GONE);
        content.setVisibility(View.VISIBLE);

        List<CmsCourse> pending = state.pending != null ? state.pending : Collections.emptyList();
        List<CmsCourse> published = state.published != null ? state.published : Collections.emptyList();

        pendingAdapter.submitList(pending);
        pendingEmpty.setVisibility(pending.isEmpty() ? View.VISIBLE : View.GONE);

        publishedAdapter.submitList(published);
        publishedEmpty.setVisibility(published.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
