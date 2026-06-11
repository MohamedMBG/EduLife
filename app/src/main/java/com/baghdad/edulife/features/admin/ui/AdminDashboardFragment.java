package com.baghdad.edulife.features.admin.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.admin.model.AdminStats;
import com.baghdad.edulife.features.admin.model.AdminUiState;
import com.baghdad.edulife.features.admin.viewmodel.AdminDashboardViewModel;
import com.baghdad.edulife.features.auth.viewmodel.AuthViewModel;
import com.google.android.material.progressindicator.CircularProgressIndicator;

public class AdminDashboardFragment extends Fragment {

    private AdminDashboardViewModel viewModel;
    private AuthViewModel authViewModel;

    private View stateCard;
    private CircularProgressIndicator loadingIndicator;
    private TextView stateText;
    private TextView retryButton;
    private View content;
    private TextView statLearners;
    private TextView statTeachers;
    private TextView statCourses;
    private TextView statPending;
    private TextView statCertificates;
    private TextView statEnrollments;
    private TextView pendingBadge;

    public AdminDashboardFragment() {
        super(R.layout.fragment_admin_dashboard);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(AdminDashboardViewModel.class);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        stateCard = view.findViewById(R.id.adminStateCard);
        loadingIndicator = view.findViewById(R.id.adminLoadingIndicator);
        stateText = view.findViewById(R.id.adminStateText);
        retryButton = view.findViewById(R.id.adminRetryButton);
        content = view.findViewById(R.id.adminContent);
        statLearners = view.findViewById(R.id.statLearnersValue);
        statTeachers = view.findViewById(R.id.statTeachersValue);
        statCourses = view.findViewById(R.id.statCoursesValue);
        statPending = view.findViewById(R.id.statPendingValue);
        statCertificates = view.findViewById(R.id.statCertificatesValue);
        statEnrollments = view.findViewById(R.id.statEnrollmentsValue);
        pendingBadge = view.findViewById(R.id.adminPendingBadge);

        retryButton.setOnClickListener(v -> viewModel.loadStats());

        view.findViewById(R.id.adminLogoutButton).setOnClickListener(v -> {
            authViewModel.signOut();
            Navigation.findNavController(view).navigate(
                    R.id.action_adminDashboardFragment_to_loginFragment);
        });

        view.findViewById(R.id.adminTeacherRequestsCta).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(
                        R.id.action_adminDashboardFragment_to_teacherRequestsFragment));

        view.findViewById(R.id.adminUsersCta).setOnClickListener(v ->
                // User management screen is a post-MVP item — acknowledged but not built yet.
                android.widget.Toast.makeText(requireContext(),
                        R.string.admin_users_coming_soon, android.widget.Toast.LENGTH_SHORT).show());

        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);

        if (viewModel.getUiState().getValue() == null
                || viewModel.getUiState().getValue().loading) {
            viewModel.loadStats();
        }
    }

    private void render(@Nullable AdminUiState state) {
        if (state == null) return;

        if (state.loading) {
            stateCard.setVisibility(View.VISIBLE);
            loadingIndicator.setVisibility(View.VISIBLE);
            stateText.setText(R.string.admin_loading);
            retryButton.setVisibility(View.GONE);
            content.setVisibility(View.GONE);
            return;
        }

        if (state.errorMessage != null) {
            stateCard.setVisibility(View.VISIBLE);
            loadingIndicator.setVisibility(View.GONE);
            stateText.setText(state.errorMessage);
            retryButton.setVisibility(View.VISIBLE);
            content.setVisibility(View.GONE);
            return;
        }

        stateCard.setVisibility(View.GONE);
        content.setVisibility(View.VISIBLE);
        bindStats(state.stats);
    }

    private void bindStats(@Nullable AdminStats s) {
        if (s == null) return;
        statLearners.setText(String.valueOf(s.totalLearners));
        statTeachers.setText(String.valueOf(s.totalTeachers));
        statCourses.setText(String.valueOf(s.totalCoursesPublished));
        statPending.setText(String.valueOf(s.pendingTeacherRequests));
        statCertificates.setText(String.valueOf(s.totalCertificates));
        statEnrollments.setText(String.valueOf(s.totalEnrollmentsActive));
        pendingBadge.setText(s.pendingTeacherRequests > 0
                ? String.valueOf(s.pendingTeacherRequests)
                : "0");
    }
}
