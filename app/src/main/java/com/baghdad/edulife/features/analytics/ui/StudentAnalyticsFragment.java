package com.baghdad.edulife.features.analytics.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.analytics.model.AnalyticsFormat;
import com.baghdad.edulife.features.analytics.model.StudentAnalyticsSummary;
import com.baghdad.edulife.features.analytics.model.StudentAnalyticsUiState;
import com.baghdad.edulife.features.analytics.model.StudentProgressTrend;
import com.baghdad.edulife.features.analytics.model.StudentTrendUiState;
import com.baghdad.edulife.features.analytics.viewmodel.StudentAnalyticsViewModel;
import com.google.android.material.progressindicator.CircularProgressIndicator;

/**
 * Student's own analytics summary. Renders the four required states (loading / error / empty /
 * success). Empty is folded into success: a synced learner always receives a summary object, which
 * may be all-zero — that is shown as zeros rather than a separate empty screen.
 */
public class StudentAnalyticsFragment extends Fragment {

    private StudentAnalyticsViewModel viewModel;

    private View stateCard;
    private CircularProgressIndicator loadingIndicator;
    private TextView stateText;
    private TextView retryButton;
    private View content;

    // Phase C trend section views.
    private TextView trendStatus;
    private LinearLayout trendContainer;

    public StudentAnalyticsFragment() {
        super(R.layout.fragment_student_analytics);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(StudentAnalyticsViewModel.class);

        stateCard = view.findViewById(R.id.studentStateCard);
        loadingIndicator = view.findViewById(R.id.studentLoadingIndicator);
        stateText = view.findViewById(R.id.studentStateText);
        retryButton = view.findViewById(R.id.studentRetryButton);
        content = view.findViewById(R.id.studentContent);
        trendStatus = view.findViewById(R.id.studentTrendStatus);
        trendContainer = view.findViewById(R.id.studentTrendContainer);

        // Static labels on each reusable stat row; values are bound on success.
        labelOf(view, R.id.rowEnrollments).setText(R.string.analytics_student_enrollments);
        labelOf(view, R.id.rowLessons).setText(R.string.analytics_student_lessons);
        labelOf(view, R.id.rowAttempts).setText(R.string.analytics_student_attempts);
        labelOf(view, R.id.rowPassed).setText(R.string.analytics_student_passed);
        labelOf(view, R.id.rowCertificates).setText(R.string.analytics_student_certificates);

        // Retry re-issues the load; the ViewModel flips back to loading first.
        retryButton.setOnClickListener(v -> viewModel.load());

        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);
        viewModel.getTrendState().observe(getViewLifecycleOwner(), this::renderTrend);

        // Only trigger the first load if nothing has been fetched yet, so rotation reuses state.
        StudentAnalyticsUiState current = viewModel.getUiState().getValue();
        if (current == null || (current.loading && current.summary == null)) {
            viewModel.load();
        }
        StudentTrendUiState trend = viewModel.getTrendState().getValue();
        if (trend == null || (trend.loading && trend.trend == null)) {
            viewModel.loadTrend();
        }
    }

    private void render(@Nullable StudentAnalyticsUiState state) {
        if (state == null) return;

        if (state.loading) {
            stateCard.setVisibility(View.VISIBLE);
            loadingIndicator.setVisibility(View.VISIBLE);
            stateText.setText(R.string.analytics_student_loading);
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
        bind(state.summary);
    }

    private void bind(@Nullable StudentAnalyticsSummary s) {
        if (s == null) return;
        valueOf(R.id.rowEnrollments).setText(AnalyticsFormat.count(s.activeEnrollments));
        valueOf(R.id.rowLessons).setText(AnalyticsFormat.count(s.lessonsCompleted));
        valueOf(R.id.rowAttempts).setText(AnalyticsFormat.count(s.examAttempts));
        valueOf(R.id.rowPassed).setText(AnalyticsFormat.count(s.examsPassed));
        valueOf(R.id.rowCertificates).setText(AnalyticsFormat.count(s.certificatesEarned));
    }

    /**
     * Renders the monthly trend section independently of the summary: status text for
     * loading/error/empty, inflated rows for success.
     */
    private void renderTrend(@Nullable StudentTrendUiState state) {
        if (state == null) return;

        if (state.loading) {
            trendStatus.setVisibility(View.VISIBLE);
            trendStatus.setText(R.string.analytics_trend_loading);
            AnalyticsRows.clear(trendContainer);
            return;
        }
        if (state.errorMessage != null) {
            trendStatus.setVisibility(View.VISIBLE);
            trendStatus.setText(state.errorMessage);
            AnalyticsRows.clear(trendContainer);
            return;
        }

        StudentProgressTrend trend = state.trend;
        boolean empty = trend == null || trend.lessonsByMonth == null || trend.lessonsByMonth.isEmpty();
        if (empty) {
            trendStatus.setVisibility(View.VISIBLE);
            trendStatus.setText(R.string.analytics_trend_empty);
            AnalyticsRows.clear(trendContainer);
            return;
        }

        trendStatus.setVisibility(View.GONE);
        AnalyticsRows.renderMonths(trendContainer, trend.lessonsByMonth);
    }

    // Scoped lookups: each included row carries its own statRowLabel / statRowValue.
    private TextView labelOf(View root, int rowId) {
        return root.findViewById(rowId).findViewById(R.id.statRowLabel);
    }

    private TextView valueOf(int rowId) {
        return requireView().findViewById(rowId).findViewById(R.id.statRowValue);
    }
}
