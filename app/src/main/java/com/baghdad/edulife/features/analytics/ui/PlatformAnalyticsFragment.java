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
import com.baghdad.edulife.features.analytics.model.PlatformAnalytics;
import com.baghdad.edulife.features.analytics.model.PlatformAnalyticsUiState;
import com.baghdad.edulife.features.analytics.model.PlatformCohortAnalytics;
import com.baghdad.edulife.features.analytics.model.PlatformCohortUiState;
import com.baghdad.edulife.features.analytics.viewmodel.PlatformAnalyticsViewModel;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.Collections;
import java.util.List;

/**
 * Global platform analytics overview. ADMIN-only access is enforced server-side; if a non-admin
 * somehow reaches this screen the backend returns 403, which surfaces here as the error state.
 * Renders loading / error / success (the grid has no empty state — counts are always present).
 */
public class PlatformAnalyticsFragment extends Fragment {

    private PlatformAnalyticsViewModel viewModel;

    private View stateCard;
    private CircularProgressIndicator loadingIndicator;
    private TextView stateText;
    private TextView retryButton;
    private View content;

    // Phase C cohort section views.
    private TextView cohortStatus;
    private TextView funnelHeader;
    private LinearLayout funnelContainer;
    private TextView cohortsHeader;
    private LinearLayout cohortsContainer;
    private TextView certTrendHeader;
    private LinearLayout certTrendContainer;

    public PlatformAnalyticsFragment() {
        super(R.layout.fragment_platform_analytics);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(PlatformAnalyticsViewModel.class);

        stateCard = view.findViewById(R.id.platformStateCard);
        loadingIndicator = view.findViewById(R.id.platformLoading);
        stateText = view.findViewById(R.id.platformStateText);
        retryButton = view.findViewById(R.id.platformRetry);
        content = view.findViewById(R.id.platformContent);
        cohortStatus = view.findViewById(R.id.platformCohortStatus);
        funnelHeader = view.findViewById(R.id.platformFunnelHeader);
        funnelContainer = view.findViewById(R.id.platformFunnelContainer);
        cohortsHeader = view.findViewById(R.id.platformCohortsHeader);
        cohortsContainer = view.findViewById(R.id.platformCohortsContainer);
        certTrendHeader = view.findViewById(R.id.platformCertTrendHeader);
        certTrendContainer = view.findViewById(R.id.platformCertTrendContainer);

        // Static tile labels; values bound on success.
        labelOf(view, R.id.tileLearners).setText(R.string.analytics_platform_learners);
        labelOf(view, R.id.tileTeachers).setText(R.string.analytics_platform_teachers);
        labelOf(view, R.id.tileGroupAdmins).setText(R.string.analytics_platform_group_admins);
        labelOf(view, R.id.tileAdmins).setText(R.string.analytics_platform_admins);
        labelOf(view, R.id.tilePublished).setText(R.string.analytics_platform_courses_published);
        labelOf(view, R.id.tileDraft).setText(R.string.analytics_platform_courses_draft);
        labelOf(view, R.id.tileEnrollments).setText(R.string.analytics_platform_enrollments);
        labelOf(view, R.id.tileCertificates).setText(R.string.analytics_platform_certificates);
        labelOf(view, R.id.tileAttempts).setText(R.string.analytics_platform_attempts);
        labelOf(view, R.id.tilePassed).setText(R.string.analytics_platform_passed);

        // Retry reloads both the headline counts and the cohort section.
        retryButton.setOnClickListener(v -> {
            viewModel.load();
            viewModel.loadCohorts();
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);
        viewModel.getCohortState().observe(getViewLifecycleOwner(), this::renderCohorts);

        PlatformAnalyticsUiState current = viewModel.getUiState().getValue();
        if (current == null || (current.loading && current.data == null)) {
            viewModel.load();
        }
        PlatformCohortUiState cohort = viewModel.getCohortState().getValue();
        if (cohort == null || (cohort.loading && cohort.data == null)) {
            viewModel.loadCohorts();
        }
    }

    private void render(@Nullable PlatformAnalyticsUiState state) {
        if (state == null) return;

        if (state.loading) {
            stateCard.setVisibility(View.VISIBLE);
            loadingIndicator.setVisibility(View.VISIBLE);
            stateText.setText(R.string.analytics_platform_loading);
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
        bind(state.data);
    }

    private void bind(@Nullable PlatformAnalytics d) {
        if (d == null) return;
        valueOf(R.id.tileLearners).setText(AnalyticsFormat.count(d.learners));
        valueOf(R.id.tileTeachers).setText(AnalyticsFormat.count(d.teachers));
        valueOf(R.id.tileGroupAdmins).setText(AnalyticsFormat.count(d.groupAdmins));
        valueOf(R.id.tileAdmins).setText(AnalyticsFormat.count(d.admins));
        valueOf(R.id.tilePublished).setText(AnalyticsFormat.count(d.coursesPublished));
        valueOf(R.id.tileDraft).setText(AnalyticsFormat.count(d.coursesDraft));
        valueOf(R.id.tileEnrollments).setText(AnalyticsFormat.count(d.activeEnrollments));
        valueOf(R.id.tileCertificates).setText(AnalyticsFormat.count(d.totalCertificates));
        valueOf(R.id.tileAttempts).setText(AnalyticsFormat.count(d.totalExamAttempts));
        valueOf(R.id.tilePassed).setText(AnalyticsFormat.count(d.totalExamsPassed));
    }

    /**
     * Renders the cohort section (funnel + enrollment cohorts + certificate trend) independently of
     * the headline counts. Section headers stay hidden until data arrives so loading/error/empty
     * never shows dangling titles.
     */
    private void renderCohorts(@Nullable PlatformCohortUiState state) {
        if (state == null) return;

        if (state.loading) {
            showCohortStatus(R.string.analytics_trend_loading);
            return;
        }
        if (state.errorMessage != null) {
            cohortStatus.setVisibility(View.VISIBLE);
            cohortStatus.setText(state.errorMessage);
            hideCohortSections();
            clearCohortContainers();
            return;
        }

        PlatformCohortAnalytics d = state.data;
        if (d == null) {
            showCohortStatus(R.string.analytics_trend_empty);
            return;
        }

        cohortStatus.setVisibility(View.GONE);

        // Funnel always present (zero funnel is meaningful as "no progress yet").
        funnelHeader.setVisibility(View.VISIBLE);
        if (d.funnel != null) {
            AnalyticsRows.renderFunnel(funnelContainer, d.funnel);
        }

        List<com.baghdad.edulife.features.analytics.model.MonthCount> cohorts =
                d.enrollmentCohorts != null ? d.enrollmentCohorts : Collections.emptyList();
        cohortsHeader.setVisibility(cohorts.isEmpty() ? View.GONE : View.VISIBLE);
        AnalyticsRows.renderMonths(cohortsContainer, cohorts);

        List<com.baghdad.edulife.features.analytics.model.MonthCount> certs =
                d.certificateTrend != null ? d.certificateTrend : Collections.emptyList();
        certTrendHeader.setVisibility(certs.isEmpty() ? View.GONE : View.VISIBLE);
        AnalyticsRows.renderMonths(certTrendContainer, certs);
    }

    private void showCohortStatus(int textRes) {
        cohortStatus.setVisibility(View.VISIBLE);
        cohortStatus.setText(textRes);
        hideCohortSections();
        clearCohortContainers();
    }

    private void hideCohortSections() {
        funnelHeader.setVisibility(View.GONE);
        cohortsHeader.setVisibility(View.GONE);
        certTrendHeader.setVisibility(View.GONE);
    }

    private void clearCohortContainers() {
        AnalyticsRows.clear(funnelContainer);
        AnalyticsRows.clear(cohortsContainer);
        AnalyticsRows.clear(certTrendContainer);
    }

    // Scoped lookups: each included tile carries its own tileLabel / tileValue.
    private TextView labelOf(View root, int tileId) {
        return root.findViewById(tileId).findViewById(R.id.tileLabel);
    }

    private TextView valueOf(int tileId) {
        return requireView().findViewById(tileId).findViewById(R.id.tileValue);
    }
}
