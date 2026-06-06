package com.baghdad.edulife.features.courses.ui;

import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.courses.model.CourseDetail;
import com.baghdad.edulife.features.courses.model.CourseDetailUiState;
import com.baghdad.edulife.features.courses.model.CourseSection;
import com.baghdad.edulife.features.courses.model.LessonSummary;
import com.baghdad.edulife.features.courses.viewmodel.CourseDetailViewModel;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CourseDetailFragment extends Fragment {

    private CourseDetailViewModel courseDetailViewModel;
    private View loadingIndicator;
    private TextView statusText;
    private ScrollView detailScrollView;
    private LinearLayout sectionContainer;
    private LinearLayout progressSummaryLayout;
    private TextView progressSummaryText;
    private ProgressBar progressSummaryBar;

    private String courseId = "";
    private boolean isEnrolled;
    private Map<String, Boolean> lessonCompletionMap = new HashMap<>();

    public CourseDetailFragment() {
        super(R.layout.fragment_course_detail);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        NavController navController = Navigation.findNavController(view);
        courseDetailViewModel = new ViewModelProvider(
                navController.getCurrentBackStackEntry()
        ).get(CourseDetailViewModel.class);

        loadingIndicator = view.findViewById(R.id.detailLoadingIndicator);
        statusText = view.findViewById(R.id.detailStatusText);
        detailScrollView = view.findViewById(R.id.detailScrollView);
        sectionContainer = view.findViewById(R.id.sectionContainer);
        progressSummaryLayout = view.findViewById(R.id.progressSummaryLayout);
        progressSummaryText = view.findViewById(R.id.progressSummaryText);
        progressSummaryBar = view.findViewById(R.id.progressSummaryBar);

        view.findViewById(R.id.backButton).setOnClickListener(v ->
                Navigation.findNavController(view).popBackStack());

        // Nav graph is the only entry; requireArguments turns a missing-bundle wiring bug into
        // an ISE rather than the silent "missing id" UI path below. The id-present branch still
        // renders an error state because a blank value can leak through programmatic navigation.
        Bundle args = requireArguments();
        String argId = args.getString("courseId");
        if (argId == null || argId.isBlank()) {
            renderError(getString(R.string.course_detail_missing_id));
            return;
        }
        courseId = argId;
        isEnrolled = args.getBoolean("isEnrolled", false);

        courseDetailViewModel.getUiState().observe(getViewLifecycleOwner(), this::renderState);
        courseDetailViewModel.getLessonCompletionState().observe(getViewLifecycleOwner(), completionMap -> {
            lessonCompletionMap = completionMap != null ? completionMap : Collections.emptyMap();
            CourseDetailUiState current = courseDetailViewModel.getUiState().getValue();
            if (current != null && current.courseDetail != null) {
                bindCourseDetail(current.courseDetail);
            }
        });

        CourseDetailUiState currentState = courseDetailViewModel.getUiState().getValue();
        if (currentState == null || currentState.courseDetail == null) {
            courseDetailViewModel.loadCourseDetail(courseId);
        }
        if (isEnrolled) {
            courseDetailViewModel.loadLessonCompletion(courseId);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isEnrolled && !courseId.isBlank()) {
            // Returning from the lesson player should refresh completion markers immediately so
            // the course outline reflects the learner's latest progress without a full reload.
            courseDetailViewModel.loadLessonCompletion(courseId);
        }
    }

    private void renderState(CourseDetailUiState state) {
        if (state == null) return;

        loadingIndicator.setVisibility(state.loading ? View.VISIBLE : View.GONE);

        if (state.loading) {
            detailScrollView.setVisibility(View.GONE);
            statusText.setVisibility(View.VISIBLE);
            statusText.setText(R.string.course_detail_loading);
            return;
        }

        if (state.errorMessage != null && !state.errorMessage.isBlank()) {
            renderError(state.errorMessage);
            return;
        }

        if (state.courseDetail != null) {
            bindCourseDetail(state.courseDetail);
        }
    }

    private void renderError(String message) {
        detailScrollView.setVisibility(View.GONE);
        requireView().findViewById(R.id.enrollCtaFooter).setVisibility(View.GONE);
        statusText.setVisibility(View.VISIBLE);
        statusText.setText(message);
    }

    private void bindCourseDetail(@NonNull CourseDetail courseDetail) {
        View view = requireView();
        TextView titleText = view.findViewById(R.id.courseTitleText);
        TextView shortDescriptionText = view.findViewById(R.id.courseShortDescriptionText);
        TextView levelText = view.findViewById(R.id.courseLevelText);
        TextView languageText = view.findViewById(R.id.courseLanguageText);
        TextView sectionCountText = view.findViewById(R.id.courseSectionCountText);
        TextView descriptionText = view.findViewById(R.id.courseDescriptionText);

        titleText.setText(courseDetail.title);
        shortDescriptionText.setText(courseDetail.shortDescription);
        levelText.setText(normalizeLabel(courseDetail.level));
        languageText.setText(getString(R.string.catalog_course_language, normalizeLabel(courseDetail.languageCode)));
        int sectionCount = courseDetail.sections != null ? courseDetail.sections.size() : 0;
        sectionCountText.setText(getString(R.string.course_detail_section_count, sectionCount));
        descriptionText.setText(courseDetail.description);

        statusText.setVisibility(View.GONE);
        detailScrollView.setVisibility(View.VISIBLE);

        rebuildSections(courseDetail);


        int lessonCount = 0;
        if (courseDetail.sections != null) {
            for (CourseSection section : courseDetail.sections) {
                if (section.lessons != null) lessonCount += section.lessons.size();
            }
        }
        final int finalSectionCount = sectionCount;
        final int finalLessonCount = lessonCount;

        View footer = view.findViewById(R.id.enrollCtaFooter);
        footer.setVisibility(View.VISIBLE);

        Button enrollBtn = view.findViewById(R.id.enrollCtaButton);
        Button takeExamBtn = view.findViewById(R.id.takeExamButton);

        if (isEnrolled) {
            enrollBtn.setVisibility(View.GONE);
            takeExamBtn.setVisibility(View.VISIBLE);
            takeExamBtn.setOnClickListener(v -> {
                Bundle examArgs = new Bundle();
                examArgs.putString("courseId", courseDetail.id != null ? courseDetail.id : "");
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_courseDetailFragment_to_examFragment, examArgs);
            });
        } else {
            takeExamBtn.setVisibility(View.GONE);
            enrollBtn.setVisibility(View.VISIBLE);
            enrollBtn.setOnClickListener(v -> {
                Bundle navArgs = new Bundle();
                navArgs.putString("courseId", courseDetail.id != null ? courseDetail.id : "");
                navArgs.putString("courseTitle", courseDetail.title != null ? courseDetail.title : "");
                navArgs.putString("courseLevel", courseDetail.level != null ? courseDetail.level : "");
                navArgs.putString("courseLanguage", courseDetail.languageCode != null ? courseDetail.languageCode : "");
                navArgs.putString("courseDesc", courseDetail.description != null ? courseDetail.description : "");
                navArgs.putInt("sectionCount", finalSectionCount);
                navArgs.putInt("lessonCount", finalLessonCount);
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_courseDetailFragment_to_enrollCourseFragment, navArgs);
            });
        }
    }

    private void rebuildSections(@NonNull CourseDetail courseDetail) {
        boolean isEnrolled = getArguments() != null && getArguments().getBoolean("isEnrolled", false);
        sectionContainer.removeAllViews();
        if (courseDetail.sections != null) {
            for (CourseSection section : courseDetail.sections) {
                sectionContainer.addView(createSectionView(courseId, section, isEnrolled));
            }
        }
    }

    private View createSectionView(String courseId, CourseSection section, boolean isEnrolled) {
        LinearLayout sectionLayout = new LinearLayout(requireContext());
        sectionLayout.setOrientation(LinearLayout.VERTICAL);
        sectionLayout.setPadding(0, 0, 0, dp(20));

        TextView sectionTitle = new TextView(requireContext());
        sectionTitle.setText(section.title);
        sectionTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        sectionTitle.setTextColor(requireContext().getColor(R.color.catalog_text_primary));
        sectionTitle.setTypeface(sectionTitle.getTypeface(), Typeface.BOLD);

        TextView sectionDescription = new TextView(requireContext());
        sectionDescription.setText(section.description);
        sectionDescription.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        sectionDescription.setTextColor(requireContext().getColor(R.color.catalog_text_secondary));
        sectionDescription.setPadding(0, dp(6), 0, dp(12));

        sectionLayout.addView(sectionTitle);
        sectionLayout.addView(sectionDescription);

        List<LessonSummary> lessons = section.lessons;
        if (lessons != null) {
            for (LessonSummary lesson : lessons) {
                sectionLayout.addView(createLessonView(courseId, lesson, section.title, isEnrolled));
            }
        }

        return sectionLayout;
    }

    private View createLessonView(String courseId, LessonSummary lesson, String sectionTitle, boolean isEnrolled) {
        LinearLayout lessonLayout = new LinearLayout(requireContext());
        lessonLayout.setOrientation(LinearLayout.VERTICAL);
        lessonLayout.setBackgroundResource(R.drawable.bg_catalog_lesson_row);
        lessonLayout.setPadding(dp(16), dp(14), dp(16), dp(14));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = dp(12);
        lessonLayout.setLayoutParams(params);

        // Header row: title + completion badge
        LinearLayout headerRow = new LinearLayout(requireContext());
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView lessonTitle = new TextView(requireContext());
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lessonTitle.setLayoutParams(titleParams);
        lessonTitle.setText(lesson.title);
        lessonTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        lessonTitle.setTextColor(requireContext().getColor(R.color.catalog_text_primary));
        lessonTitle.setTypeface(lessonTitle.getTypeface(), Typeface.BOLD);

        boolean completed = Boolean.TRUE.equals(lessonCompletionMap.get(lesson.id));
        TextView completedBadge = new TextView(requireContext());
        completedBadge.setText(R.string.progress_lesson_completed);
        completedBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        completedBadge.setTextColor(requireContext().getColor(R.color.catalog_primary));
        completedBadge.setTypeface(completedBadge.getTypeface(), Typeface.BOLD);
        completedBadge.setPadding(dp(8), 0, 0, 0);
        completedBadge.setVisibility(completed ? View.VISIBLE : View.GONE);

        headerRow.addView(lessonTitle);
        headerRow.addView(completedBadge);

        TextView lessonSummaryView = new TextView(requireContext());
        lessonSummaryView.setText(lesson.summary);
        lessonSummaryView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        lessonSummaryView.setTextColor(requireContext().getColor(R.color.catalog_text_secondary));
        lessonSummaryView.setPadding(0, dp(6), 0, dp(8));

        TextView lessonMeta = new TextView(requireContext());
        lessonMeta.setText(getString(
                R.string.course_detail_lesson_meta,
                normalizeLabel(lesson.lessonType),
                lesson.estimatedDurationMinutes
        ));
        lessonMeta.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        lessonMeta.setTextColor(requireContext().getColor(R.color.catalog_text_secondary));

        boolean accessible = isEnrolled || lesson.preview;

        TextView accessText = new TextView(requireContext());
        boolean isCompleted = lesson.id != null && Boolean.TRUE.equals(lessonCompletionMap.get(lesson.id));
        if (isEnrolled) {
            // Enrolled lessons stay accessible, but completed lessons need a clear visual marker
            // so learners can scan the outline without reopening each lesson.
            accessText.setText(isCompleted
                    ? R.string.lesson_player_completed
                    : R.string.course_detail_open_lesson);
            accessText.setTextColor(requireContext().getColor(R.color.catalog_primary));
        } else {
            accessText.setText(lesson.preview ? R.string.course_detail_preview : R.string.course_detail_locked);
            accessText.setTextColor(requireContext().getColor(lesson.preview
                    ? R.color.catalog_primary
                    : R.color.catalog_warning));
        }
        accessText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        accessText.setPadding(0, dp(8), 0, 0);
        accessText.setTypeface(accessText.getTypeface(), Typeface.BOLD);

        lessonLayout.addView(headerRow);
        lessonLayout.addView(lessonSummaryView);
        lessonLayout.addView(lessonMeta);
        lessonLayout.addView(accessText);

        if (accessible) {
            lessonLayout.setOnClickListener(v -> {
                Bundle navArgs = new Bundle();
                navArgs.putString("courseId",        courseId);
                navArgs.putString("lessonId",        lesson.id != null ? lesson.id : "");
                navArgs.putString("lessonTitle",     lesson.title != null ? lesson.title : "");
                navArgs.putString("lessonSummary",   lesson.summary != null ? lesson.summary : "");
                navArgs.putString("lessonType",      lesson.lessonType != null ? lesson.lessonType : "");
                navArgs.putInt("durationMinutes",    lesson.estimatedDurationMinutes);
                navArgs.putBoolean("isPreview",      lesson.preview);
                navArgs.putString("sectionTitle",    sectionTitle != null ? sectionTitle : "");
                navArgs.putInt("orderInSection",     lesson.displayOrder);
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_courseDetailFragment_to_lessonPlayerFragment, navArgs);
            });
        } else {
            lessonLayout.setAlpha(0.55f);
        }

        return lessonLayout;
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                requireContext().getResources().getDisplayMetrics()
        );
    }

    private String normalizeLabel(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return "Unknown";
        }
        String normalized = rawValue.replace('_', ' ').toLowerCase(Locale.ROOT);
        return normalized.substring(0, 1).toUpperCase(Locale.ROOT) + normalized.substring(1);
    }
}
