package com.baghdad.edulife.features.courses.ui;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
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
            int sectionIndex = 1;
            for (CourseSection section : courseDetail.sections) {
                // Section index added to display a clean indicator ("SECTION 1", "SECTION 2", etc.)
                sectionContainer.addView(createSectionView(courseId, section, isEnrolled, sectionIndex++));
            }
        }
    }

    private View createSectionView(String courseId, CourseSection section, boolean isEnrolled, int sectionIndex) {
        LinearLayout sectionLayout = new LinearLayout(requireContext());
        sectionLayout.setOrientation(LinearLayout.VERTICAL);
        sectionLayout.setPadding(0, 0, 0, dp(12));

        // Add a divider line between sections to create clear visual boundaries.
        if (sectionIndex > 1) {
            View divider = new View(requireContext());
            divider.setBackgroundColor(requireContext().getColor(R.color.brand_border));
            LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(1)
            );
            dividerParams.topMargin = dp(8);
            dividerParams.bottomMargin = dp(20);
            sectionLayout.addView(divider);
        }

        // Section header row: container for section badge pill and bold title.
        LinearLayout headerRow = new LinearLayout(requireContext());
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        
        // Section index pill badge to indicate learning sequence.
        TextView badge = new TextView(requireContext());
        badge.setText(String.format(Locale.getDefault(), "SECTION %d", sectionIndex));
        badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        badge.setTextColor(requireContext().getColor(R.color.brand_primary));
        badge.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        badge.setBackgroundResource(R.drawable.bg_lesson_type_badge);
        badge.setPadding(dp(8), dp(4), dp(8), dp(4));
        
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        badgeParams.rightMargin = dp(12);
        badge.setLayoutParams(badgeParams);

        // Section title in bold text.
        TextView sectionTitle = new TextView(requireContext());
        sectionTitle.setText(section.title);
        sectionTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        sectionTitle.setTextColor(requireContext().getColor(R.color.catalog_text_primary));
        sectionTitle.setTypeface(sectionTitle.getTypeface(), Typeface.BOLD);
        sectionTitle.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        headerRow.addView(badge);
        headerRow.addView(sectionTitle);
        sectionLayout.addView(headerRow);

        // Subtitle section description to summarize learnings.
        if (section.description != null && !section.description.isBlank()) {
            TextView sectionDescription = new TextView(requireContext());
            sectionDescription.setText(section.description);
            sectionDescription.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            sectionDescription.setTextColor(requireContext().getColor(R.color.catalog_text_secondary));
            sectionDescription.setPadding(0, dp(6), 0, dp(14));
            sectionLayout.addView(sectionDescription);
        } else {
            // Padding buffer if description is omitted.
            headerRow.setPadding(0, 0, 0, dp(12));
        }

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
        lessonLayout.setOrientation(LinearLayout.HORIZONTAL);
        lessonLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        lessonLayout.setBackgroundResource(R.drawable.bg_catalog_lesson_row);
        lessonLayout.setPadding(dp(16), dp(16), dp(16), dp(16));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = dp(12);
        lessonLayout.setLayoutParams(params);

        // Add standard material click ripple effect to make items feel interactive.
        TypedValue outValue = new TypedValue();
        if (requireContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)) {
            lessonLayout.setForeground(requireContext().getDrawable(outValue.resourceId));
        }

        // Left icon area: displays circular shape representing the media category (video vs text).
        ImageView typeIcon = new ImageView(requireContext());
        int iconRes = "VIDEO".equalsIgnoreCase(lesson.lessonType)
                ? R.drawable.ic_play_circle
                : R.drawable.ic_description;
        typeIcon.setImageResource(iconRes);
        typeIcon.setBackgroundResource(R.drawable.bg_lesson_icon_container);
        typeIcon.setPadding(dp(10), dp(10), dp(10), dp(10));
        typeIcon.setImageTintList(ColorStateList.valueOf(requireContext().getColor(R.color.brand_primary)));

        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(42), dp(42));
        iconParams.rightMargin = dp(14);
        typeIcon.setLayoutParams(iconParams);
        lessonLayout.addView(typeIcon);

        // Middle content area: vertically lists title, summary, and duration meta.
        LinearLayout contentLayout = new LinearLayout(requireContext());
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        contentLayout.setLayoutParams(contentParams);

        TextView lessonTitle = new TextView(requireContext());
        lessonTitle.setText(lesson.title);
        lessonTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        lessonTitle.setTextColor(requireContext().getColor(R.color.catalog_text_primary));
        lessonTitle.setTypeface(lessonTitle.getTypeface(), Typeface.BOLD);
        contentLayout.addView(lessonTitle);

        if (lesson.summary != null && !lesson.summary.isBlank()) {
            TextView lessonSummaryView = new TextView(requireContext());
            lessonSummaryView.setText(lesson.summary);
            lessonSummaryView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            lessonSummaryView.setTextColor(requireContext().getColor(R.color.catalog_text_secondary));
            lessonSummaryView.setPadding(0, dp(4), 0, dp(4));
            lessonSummaryView.setMaxLines(2);
            lessonSummaryView.setEllipsize(TextUtils.TruncateAt.END);
            contentLayout.addView(lessonSummaryView);
        }

        TextView lessonMeta = new TextView(requireContext());
        lessonMeta.setText(getString(
                R.string.course_detail_lesson_meta,
                normalizeLabel(lesson.lessonType),
                lesson.estimatedDurationMinutes
        ));
        lessonMeta.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        lessonMeta.setTextColor(requireContext().getColor(R.color.brand_text_muted));
        contentLayout.addView(lessonMeta);

        lessonLayout.addView(contentLayout);

        // Right status indicator: Completed check, locked pad, preview chip, or standard play chevron.
        boolean accessible = isEnrolled || lesson.preview;
        boolean completed = Boolean.TRUE.equals(lessonCompletionMap.get(lesson.id));

        if (completed) {
            // Show a premium green check circle for completed lessons.
            ImageView checkIcon = new ImageView(requireContext());
            checkIcon.setImageResource(R.drawable.ic_check_circle);
            checkIcon.setImageTintList(ColorStateList.valueOf(requireContext().getColor(R.color.brand_primary)));
            LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(dp(22), dp(22));
            checkIcon.setLayoutParams(statusParams);
            lessonLayout.addView(checkIcon);
        } else if (!isEnrolled) {
            if (lesson.preview) {
                // Show custom preview pill for free sample content.
                TextView previewBadge = new TextView(requireContext());
                previewBadge.setText("PREVIEW");
                previewBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
                previewBadge.setTextColor(requireContext().getColor(R.color.brand_primary));
                previewBadge.setTypeface(previewBadge.getTypeface(), Typeface.BOLD);
                previewBadge.setBackgroundResource(R.drawable.bg_catalog_badge);
                previewBadge.setPadding(dp(8), dp(4), dp(8), dp(4));
                lessonLayout.addView(previewBadge);
            } else {
                // Show a lock icon for content requiring enrollment.
                ImageView lockIcon = new ImageView(requireContext());
                lockIcon.setImageResource(R.drawable.ic_lock);
                lockIcon.setImageTintList(ColorStateList.valueOf(requireContext().getColor(R.color.brand_text_secondary)));
                LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(dp(18), dp(18));
                lockIcon.setLayoutParams(statusParams);
                lessonLayout.addView(lockIcon);
            }
        } else {
            // Show standard entry arrow to indicate interactive row.
            ImageView arrowIcon = new ImageView(requireContext());
            arrowIcon.setImageResource(R.drawable.ic_chevron_right);
            arrowIcon.setImageTintList(ColorStateList.valueOf(requireContext().getColor(R.color.brand_text_secondary)));
            LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(dp(18), dp(18));
            arrowIcon.setLayoutParams(statusParams);
            lessonLayout.addView(arrowIcon);
        }

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
            // Fades locked lessons slightly to visually signify restricted status.
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
