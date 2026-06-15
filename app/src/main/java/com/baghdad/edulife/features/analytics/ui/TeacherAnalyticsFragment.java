package com.baghdad.edulife.features.analytics.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.analytics.model.TeacherAnalytics;
import com.baghdad.edulife.features.analytics.model.TeacherAnalyticsUiState;
import com.baghdad.edulife.features.analytics.model.TeacherCourseAnalytics;
import com.baghdad.edulife.features.analytics.viewmodel.TeacherAnalyticsViewModel;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.Collections;
import java.util.List;

/**
 * Teacher owned-course analytics. Renders all four states: loading, error (with retry), empty
 * (teacher owns no courses), and success (the course list). The backend returns only the caller's
 * own courses, so this screen performs no scoping itself.
 */
public class TeacherAnalyticsFragment extends Fragment {

    private TeacherAnalyticsViewModel viewModel;
    private TeacherCourseAnalyticsAdapter adapter;

    private View stateCard;
    private CircularProgressIndicator loadingIndicator;
    private TextView stateText;
    private TextView retryButton;
    private RecyclerView recyclerView;
    private View emptyView;

    public TeacherAnalyticsFragment() {
        super(R.layout.fragment_teacher_analytics);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(TeacherAnalyticsViewModel.class);

        stateCard = view.findViewById(R.id.teacherAnalyticsStateCard);
        loadingIndicator = view.findViewById(R.id.teacherAnalyticsLoading);
        stateText = view.findViewById(R.id.teacherAnalyticsStateText);
        retryButton = view.findViewById(R.id.teacherAnalyticsRetry);
        recyclerView = view.findViewById(R.id.teacherAnalyticsRecycler);
        emptyView = view.findViewById(R.id.teacherAnalyticsEmpty);

        adapter = new TeacherCourseAnalyticsAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        retryButton.setOnClickListener(v -> viewModel.load());

        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);

        TeacherAnalyticsUiState current = viewModel.getUiState().getValue();
        if (current == null || (current.loading && current.data == null)) {
            viewModel.load();
        }
    }

    private void render(@Nullable TeacherAnalyticsUiState state) {
        if (state == null) return;

        if (state.loading) {
            stateCard.setVisibility(View.VISIBLE);
            loadingIndicator.setVisibility(View.VISIBLE);
            stateText.setText(R.string.analytics_teacher_loading);
            retryButton.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);
            return;
        }

        if (state.errorMessage != null) {
            stateCard.setVisibility(View.VISIBLE);
            loadingIndicator.setVisibility(View.GONE);
            stateText.setText(state.errorMessage);
            retryButton.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);
            return;
        }

        stateCard.setVisibility(View.GONE);

        TeacherAnalytics data = state.data;
        List<TeacherCourseAnalytics> courses =
                (data != null && data.courses != null) ? data.courses : Collections.emptyList();

        if (courses.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.submitList(courses);
        }
    }
}
