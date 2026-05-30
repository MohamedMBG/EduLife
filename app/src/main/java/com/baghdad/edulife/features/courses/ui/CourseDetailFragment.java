package com.baghdad.edulife.features.courses.ui;

import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.courses.model.CourseDetail;
import com.baghdad.edulife.features.courses.model.CourseDetailUiState;
import com.baghdad.edulife.features.courses.model.CourseSection;
import com.baghdad.edulife.features.courses.model.LessonSummary;
import com.baghdad.edulife.features.courses.viewmodel.CourseDetailViewModel;

import java.util.List;
import java.util.Locale;

public class CourseDetailFragment extends Fragment {

    private CourseDetailViewModel courseDetailViewModel;
    private View loadingIndicator;
    private TextView statusText;
    private ScrollView detailScrollView;
    private LinearLayout sectionContainer;

    public CourseDetailFragment() {
        super(R.layout.fragment_course_detail);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        courseDetailViewModel = new ViewModelProvider(this).get(CourseDetailViewModel.class);
        loadingIndicator = view.findViewById(R.id.detailLoadingIndicator);
        statusText = view.findViewById(R.id.detailStatusText);
        detailScrollView = view.findViewById(R.id.detailScrollView);
        sectionContainer = view.findViewById(R.id.sectionContainer);

        view.findViewById(R.id.backButton).setOnClickListener(v ->
                Navigation.findNavController(view).popBackStack());

        String courseId = getArguments() != null ? getArguments().getString("courseId") : null;
        if (courseId == null || courseId.isBlank()) {
            renderError(getString(R.string.course_detail_missing_id));
            return;
        }

        courseDetailViewModel.getUiState().observe(getViewLifecycleOwner(), this::renderState);

        CourseDetailUiState currentState = courseDetailViewModel.getUiState().getValue();
        if (currentState == null || currentState.courseDetail == null) {
            courseDetailViewModel.loadCourseDetail(courseId);
        }
    }

    private void renderState(CourseDetailUiState state) {
        if (state == null) {
            return;
        }

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

        sectionContainer.removeAllViews();
        int lessonCount = 0;
        String courseId = getArguments() != null ? getArguments().getString("courseId", "") : "";
        if (courseDetail.sections != null) {
            for (CourseSection section : courseDetail.sections) {
                sectionContainer.addView(createSectionView(courseId, section));
                if (section.lessons != null) lessonCount += section.lessons.size();
            }
        }
        final int finalSectionCount = sectionCount;
        final int finalLessonCount = lessonCount;

        statusText.setVisibility(View.GONE);
        detailScrollView.setVisibility(View.VISIBLE);

        View footer = requireView().findViewById(R.id.enrollCtaFooter);
        footer.setVisibility(View.VISIBLE);
        Button enrollBtn = requireView().findViewById(R.id.enrollCtaButton);
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

        Button takeExamBtn = requireView().findViewById(R.id.takeExamButton);
        takeExamBtn.setOnClickListener(v -> {
            Bundle examArgs = new Bundle();
            examArgs.putString("courseId", courseDetail.id != null ? courseDetail.id : "");
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_courseDetailFragment_to_examFragment, examArgs);
        });
    }

    private View createSectionView(String courseId, CourseSection section) {
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
                sectionLayout.addView(createLessonView(courseId, lesson, section.title));
            }
        }

        return sectionLayout;
    }

    private View createLessonView(String courseId, LessonSummary lesson, String sectionTitle) {
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

        TextView lessonTitle = new TextView(requireContext());
        lessonTitle.setText(lesson.title);
        lessonTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        lessonTitle.setTextColor(requireContext().getColor(R.color.catalog_text_primary));
        lessonTitle.setTypeface(lessonTitle.getTypeface(), Typeface.BOLD);

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

        TextView accessText = new TextView(requireContext());
        accessText.setText(lesson.preview ? R.string.course_detail_preview : R.string.course_detail_locked);
        accessText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        accessText.setTextColor(requireContext().getColor(lesson.preview
                ? R.color.catalog_primary
                : R.color.catalog_warning));
        accessText.setPadding(0, dp(8), 0, 0);
        accessText.setTypeface(accessText.getTypeface(), Typeface.BOLD);

        lessonLayout.addView(lessonTitle);
        lessonLayout.addView(lessonSummaryView);
        lessonLayout.addView(lessonMeta);
        lessonLayout.addView(accessText);

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
