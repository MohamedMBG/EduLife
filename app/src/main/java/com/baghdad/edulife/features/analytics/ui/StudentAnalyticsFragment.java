package com.baghdad.edulife.features.analytics.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.analytics.model.AnalyticsInsight;
import com.baghdad.edulife.features.analytics.model.StudyAnalytics;
import com.baghdad.edulife.features.analytics.model.StudyAnalyticsUiState;
import com.baghdad.edulife.features.analytics.ui.widget.WeeklyBarChartView;
import com.baghdad.edulife.features.analytics.viewmodel.StudyAnalyticsViewModel;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.List;

/**
 * Redesigned learner Study Analytics screen. Renders loading / error / success (empty folded into
 * success as zeroed values). All data comes from {@link StudyAnalyticsViewModel}; this fragment only
 * binds views — no business logic or API calls here.
 */
public class StudentAnalyticsFragment extends Fragment {

    private StudyAnalyticsViewModel viewModel;

    // State overlay
    private View stateCard;
    private CircularProgressIndicator loadingIndicator;
    private TextView stateText;
    private TextView retryButton;
    private View content;

    // Hero
    private CircularProgressIndicator overallRing;
    private TextView overallPercent;
    private TextView overallCaption;
    private TextView heroLessons;
    private TextView heroCourses;
    private TextView heroCertificates;

    // Weekly
    private WeeklyBarChartView weeklyChart;
    private TextView weeklySummary;

    // Courses
    private RecyclerView coursesList;
    private TextView coursesEmpty;
    private CourseProgressAdapter coursesAdapter;

    // Exam
    private TextView examAverage;
    private TextView examPassed;
    private TextView examBest;

    // Streak
    private TextView streakCurrent;
    private TextView streakBest;
    private TextView streakWeek;

    // Insights
    private LinearLayout insightsCard;

    public StudentAnalyticsFragment() {
        super(R.layout.fragment_student_analytics);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(StudyAnalyticsViewModel.class);

        stateCard = view.findViewById(R.id.studentStateCard);
        loadingIndicator = view.findViewById(R.id.studentLoadingIndicator);
        stateText = view.findViewById(R.id.studentStateText);
        retryButton = view.findViewById(R.id.studentRetryButton);
        content = view.findViewById(R.id.studentContent);

        overallRing = view.findViewById(R.id.studyOverallRing);
        overallPercent = view.findViewById(R.id.studyOverallPercent);
        overallCaption = view.findViewById(R.id.studyOverallCaption);
        heroLessons = view.findViewById(R.id.studyHeroLessons);
        heroCourses = view.findViewById(R.id.studyHeroCourses);
        heroCertificates = view.findViewById(R.id.studyHeroCertificates);

        weeklyChart = view.findViewById(R.id.studyWeeklyChart);
        weeklySummary = view.findViewById(R.id.studyWeeklySummary);

        coursesList = view.findViewById(R.id.studyCoursesList);
        coursesEmpty = view.findViewById(R.id.studyCoursesEmpty);
        coursesAdapter = new CourseProgressAdapter();
        coursesList.setLayoutManager(new LinearLayoutManager(requireContext()));
        coursesList.setAdapter(coursesAdapter);

        examAverage = view.findViewById(R.id.studyExamAverage);
        examPassed = view.findViewById(R.id.studyExamPassed);
        examBest = view.findViewById(R.id.studyExamBest);

        streakCurrent = view.findViewById(R.id.studyStreakCurrent);
        streakBest = view.findViewById(R.id.studyStreakBest);
        streakWeek = view.findViewById(R.id.studyStreakWeek);

        insightsCard = view.findViewById(R.id.studyInsightsCard);

        retryButton.setOnClickListener(v -> viewModel.load());

        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);

        // Only trigger the first load if nothing has been fetched yet, so rotation reuses state.
        StudyAnalyticsUiState current = viewModel.getUiState().getValue();
        if (current == null || (current.loading && current.analytics == null)) {
            viewModel.load();
        }
    }

    private void render(@Nullable StudyAnalyticsUiState state) {
        if (state == null) return;

        if (state.loading) {
            stateCard.setVisibility(View.VISIBLE);
            loadingIndicator.setVisibility(View.VISIBLE);
            stateText.setText(R.string.study_loading);
            retryButton.setVisibility(View.GONE);
            content.setVisibility(View.GONE);
            return;
        }

        if (state.errorMessage != null) {
            stateCard.setVisibility(View.VISIBLE);
            loadingIndicator.setVisibility(View.GONE);
            stateText.setText(R.string.study_error_generic);
            retryButton.setVisibility(View.VISIBLE);
            content.setVisibility(View.GONE);
            return;
        }

        stateCard.setVisibility(View.GONE);
        content.setVisibility(View.VISIBLE);
        bind(state.analytics);
    }

    private void bind(@Nullable StudyAnalytics a) {
        if (a == null) return;

        // Hero
        overallRing.setProgressCompat(a.overallProgressPercent, true);
        overallPercent.setText(getString(R.string.analytics_percent_value, a.overallProgressPercent));
        if (a.overallProgressPercent > 0 || a.activeCourses > 0) {
            overallCaption.setText(getString(
                    R.string.study_overall_caption, a.overallProgressPercent, a.currentPathTitle));
        } else {
            overallCaption.setText(R.string.study_overall_caption_empty);
        }
        heroLessons.setText(String.valueOf(a.completedLessons));
        heroCourses.setText(String.valueOf(a.activeCourses));
        heroCertificates.setText(String.valueOf(a.certificatesEarned));

        // Weekly chart
        if (a.weekly != null) {
            weeklyChart.setData(a.weekly.days);
            weeklySummary.setText(getString(R.string.study_weekly_summary,
                    a.weekly.totalLessonsThisWeek, a.weekly.daysStudiedThisWeek));
        }

        // Courses
        List<?> courses = a.courses;
        boolean hasCourses = courses != null && !courses.isEmpty();
        coursesList.setVisibility(hasCourses ? View.VISIBLE : View.GONE);
        coursesEmpty.setVisibility(hasCourses ? View.GONE : View.VISIBLE);
        coursesAdapter.submitList(a.courses);

        // Exam
        if (a.exam != null) {
            examAverage.setText(getString(R.string.analytics_percent_value, a.exam.averageScore));
            examPassed.setText(String.valueOf(a.exam.passedExams));
            examBest.setText(getString(R.string.analytics_percent_value, a.exam.bestScore));
        }

        // Streak
        if (a.streak != null) {
            streakCurrent.setText(getString(R.string.study_streak_days, a.streak.currentStreak));
            streakBest.setText(getString(R.string.study_streak_days, a.streak.bestStreak));
            streakWeek.setText(getString(R.string.study_streak_days_short, a.streak.daysStudiedThisWeek));
        }

        bindInsights(a.insights);
    }

    /** Inflates one insight row per message into the insights card; clears any previous rows. */
    private void bindInsights(@Nullable List<AnalyticsInsight> insights) {
        insightsCard.removeAllViews();
        if (insights == null || insights.isEmpty()) {
            insightsCard.setVisibility(View.GONE);
            return;
        }
        insightsCard.setVisibility(View.VISIBLE);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (AnalyticsInsight insight : insights) {
            View row = inflater.inflate(R.layout.item_study_insight, insightsCard, false);
            ((TextView) row.findViewById(R.id.insightMessage)).setText(insight.message);
            insightsCard.addView(row);
        }
    }
}
