package com.baghdad.edulife.features.courses.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.courses.data.CourseRepository;
import com.baghdad.edulife.features.courses.model.CourseDetail;
import com.baghdad.edulife.features.courses.model.CourseSection;
import com.baghdad.edulife.features.courses.model.LessonDetail;
import com.baghdad.edulife.features.courses.model.LessonDetailUiState;
import com.baghdad.edulife.features.courses.model.LessonSummary;
import com.baghdad.edulife.features.courses.viewmodel.LessonDetailViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LessonPlayerFragment extends Fragment {

    private LessonDetailViewModel lessonDetailViewModel;
    private CourseRepository courseRepository;
    private String courseId = "";
    private String lessonId = "";
    private Button markCompleteButton;
    private Button previousLessonButton;
    private Button nextLessonButton;
    private TextView playerActionLabel;
    private String previousLessonId = "";
    private String nextLessonId = "";
    private String currentContentUrl = "";

    public LessonPlayerFragment() {
        super(R.layout.fragment_lesson_player);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        lessonDetailViewModel = new ViewModelProvider(this).get(LessonDetailViewModel.class);
        courseRepository = new CourseRepository();

        Bundle args = getArguments();
        courseId = args != null ? args.getString("courseId", "") : "";
        lessonId = args != null ? args.getString("lessonId", "") : "";
        markCompleteButton = view.findViewById(R.id.lessonMarkCompleteButton);
        previousLessonButton = view.findViewById(R.id.lessonPrevButton);
        nextLessonButton = view.findViewById(R.id.lessonNextButton);
        playerActionLabel = view.findViewById(R.id.playerActionLabel);

        bindLoadingState();

        view.findViewById(R.id.lessonBackButton).setOnClickListener(v ->
                Navigation.findNavController(view).popBackStack());

        view.findViewById(R.id.playerPlayButton).setOnClickListener(v ->
                openLessonResource());

        previousLessonButton.setOnClickListener(v -> navigateToSiblingLesson(previousLessonId, true));

        nextLessonButton.setOnClickListener(v -> navigateToSiblingLesson(nextLessonId, false));

        lessonDetailViewModel.getUiState().observe(getViewLifecycleOwner(), this::renderLessonState);

        if (courseId.isBlank() || lessonId.isBlank()) {
            // Lesson access depends on stable backend IDs, so avoid fake UI state if navigation broke.
            bindMissingIdsState();
            return;
        }

        lessonDetailViewModel.loadLessonDetail(courseId, lessonId);
    }

    private void renderLessonState(LessonDetailUiState state) {
        if (state == null) {
            return;
        }

        if (state.loading) {
            bindLoadingState();
            return;
        }

        if (state.errorMessage != null && !state.errorMessage.isBlank()) {
            bindErrorState(state.errorMessage);
            return;
        }

        if (state.lessonDetail != null) {
            bindLessonDetail(state.lessonDetail);
        }
    }

    private void bindLessonDetail(@NonNull LessonDetail lessonDetail) {
        View view = requireView();
        ((TextView) view.findViewById(R.id.lessonTitle)).setText(safeText(lessonDetail.title, getString(R.string.lesson_player_default_title)));
        ((TextView) view.findViewById(R.id.lessonSectionContext))
                .setText(lessonDetail.sectionTitle == null || lessonDetail.sectionTitle.isBlank()
                        ? ""
                        : getString(R.string.lesson_player_section_format, lessonDetail.sectionTitle));

        String lessonBody = firstNonBlank(lessonDetail.contentBody, lessonDetail.summary,
                getString(R.string.lesson_player_notes_placeholder));
        ((TextView) view.findViewById(R.id.lessonSummary)).setText(lessonBody);
        ((TextView) view.findViewById(R.id.lessonTypeBadge)).setText(normalizeLabel(lessonDetail.lessonType));
        ((TextView) view.findViewById(R.id.lessonOrderText)).setText(
                getString(R.string.lesson_player_order_format, lessonDetail.displayOrder != null ? lessonDetail.displayOrder : 1));
        ((TextView) view.findViewById(R.id.lessonDurationText)).setText(
                getString(R.string.lesson_player_duration_format, lessonDetail.durationMinutes != null ? lessonDetail.durationMinutes : 0));

        view.findViewById(R.id.lessonPreviewBadge)
                .setVisibility(lessonDetail.preview ? View.VISIBLE : View.GONE);
        currentContentUrl = lessonDetail.contentUrl != null ? lessonDetail.contentUrl.trim() : "";
        playerActionLabel.setText(currentContentUrl.isBlank()
                ? R.string.lesson_player_resource_missing
                : R.string.lesson_player_resource_ready);
        loadLessonNavigationState();

        // Only enrolled, non-preview lessons should record progress on the backend.
        if (lessonDetail.preview) {
            markCompleteButton.setVisibility(View.GONE);
            return;
        }

        markCompleteButton.setVisibility(View.VISIBLE);
        if (lessonDetail.completed) {
            markCompleteButton.setEnabled(false);
            markCompleteButton.setText(R.string.lesson_player_completed);
            return;
        }

        markCompleteButton.setEnabled(true);
        markCompleteButton.setText(R.string.lesson_player_mark_complete);
        markCompleteButton.setOnClickListener(v -> markLessonComplete());
    }

    private void bindLoadingState() {
        if (!isAdded()) {
            return;
        }

        View view = requireView();
        ((TextView) view.findViewById(R.id.lessonTitle)).setText(R.string.lesson_player_loading_title);
        ((TextView) view.findViewById(R.id.lessonSectionContext)).setText("");
        ((TextView) view.findViewById(R.id.lessonSummary)).setText(R.string.lesson_player_loading_body);
        ((TextView) view.findViewById(R.id.lessonTypeBadge)).setText("");
        ((TextView) view.findViewById(R.id.lessonOrderText)).setText("");
        ((TextView) view.findViewById(R.id.lessonDurationText)).setText(getString(R.string.lesson_player_duration_format, 0));
        view.findViewById(R.id.lessonPreviewBadge).setVisibility(View.GONE);
        markCompleteButton.setVisibility(View.GONE);
        playerActionLabel.setText(R.string.lesson_player_tap_to_play);
        previousLessonButton.setEnabled(false);
        nextLessonButton.setEnabled(false);
    }

    private void bindErrorState(String message) {
        View view = requireView();
        ((TextView) view.findViewById(R.id.lessonTitle)).setText(R.string.lesson_player_unavailable_title);
        ((TextView) view.findViewById(R.id.lessonSectionContext)).setText("");
        ((TextView) view.findViewById(R.id.lessonSummary)).setText(message);
        ((TextView) view.findViewById(R.id.lessonTypeBadge)).setText("");
        ((TextView) view.findViewById(R.id.lessonOrderText)).setText("");
        ((TextView) view.findViewById(R.id.lessonDurationText)).setText(getString(R.string.lesson_player_duration_format, 0));
        view.findViewById(R.id.lessonPreviewBadge).setVisibility(View.GONE);
        markCompleteButton.setVisibility(View.GONE);
        playerActionLabel.setText(R.string.lesson_player_tap_to_play);
        previousLessonButton.setEnabled(false);
        nextLessonButton.setEnabled(false);
    }

    private void bindMissingIdsState() {
        bindErrorState(getString(R.string.lesson_player_missing_ids));
    }

    private void markLessonComplete() {
        markCompleteButton.setEnabled(false);
        markCompleteButton.setText(R.string.lesson_player_completed);

        lessonDetailViewModel.markLessonComplete(courseId, lessonId, new LessonDetailViewModel.CompletionCallback() {
            @Override
            public void onSuccess() {
                // Button already shows the completed state, so no extra UI change is required.
            }

            @Override
            public void onForbidden() {
                if (!isAdded()) {
                    return;
                }

                resetButton(markCompleteButton);
                Toast.makeText(requireContext(),
                        "You need to enroll in this course first.",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) {
                    return;
                }

                resetButton(markCompleteButton);
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resetButton(Button button) {
        button.setEnabled(true);
        button.setText(R.string.lesson_player_mark_complete);
    }

    private String normalizeLabel(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String s = raw.replace('_', ' ').toLowerCase(Locale.ROOT);
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String firstNonBlank(String primary, String secondary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        if (secondary != null && !secondary.isBlank()) {
            return secondary;
        }
        return fallback;
    }

    private void openLessonResource() {
        if (currentContentUrl == null || currentContentUrl.isBlank()) {
            Toast.makeText(requireContext(), R.string.lesson_player_resource_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(currentContentUrl));
        startActivity(intent);
    }

    private void loadLessonNavigationState() {
        courseRepository.loadCourseDetail(courseId, new CourseRepository.CourseDetailCallback() {
            @Override
            public void onSuccess(CourseDetail courseDetail) {
                if (!isAdded()) {
                    return;
                }

                List<LessonSummary> lessons = flattenLessons(courseDetail.sections);
                previousLessonId = "";
                nextLessonId = "";

                for (int index = 0; index < lessons.size(); index++) {
                    LessonSummary lesson = lessons.get(index);
                    if (lesson != null && lesson.id != null && lesson.id.equals(lessonId)) {
                        previousLessonId = index > 0 && lessons.get(index - 1) != null && lessons.get(index - 1).id != null
                                ? lessons.get(index - 1).id
                                : "";
                        nextLessonId = index < lessons.size() - 1 && lessons.get(index + 1) != null && lessons.get(index + 1).id != null
                                ? lessons.get(index + 1).id
                                : "";
                        break;
                    }
                }

                previousLessonButton.setEnabled(!previousLessonId.isBlank());
                nextLessonButton.setEnabled(!nextLessonId.isBlank());
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) {
                    return;
                }

                previousLessonId = "";
                nextLessonId = "";
                previousLessonButton.setEnabled(false);
                nextLessonButton.setEnabled(false);
            }
        });
    }

    private List<LessonSummary> flattenLessons(List<CourseSection> sections) {
        List<LessonSummary> lessons = new ArrayList<>();
        if (sections == null) {
            return lessons;
        }

        for (CourseSection section : sections) {
            if (section == null || section.lessons == null) {
                continue;
            }
            lessons.addAll(section.lessons);
        }
        return lessons;
    }

    private void navigateToSiblingLesson(String targetLessonId, boolean previous) {
        if (targetLessonId == null || targetLessonId.isBlank()) {
            Toast.makeText(requireContext(),
                    previous ? R.string.lesson_player_prev_unavailable : R.string.lesson_player_next_unavailable,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Bundle navArgs = new Bundle();
        navArgs.putString("courseId", courseId);
        navArgs.putString("lessonId", targetLessonId);
        Navigation.findNavController(requireView())
                .navigate(R.id.lessonPlayerFragment, navArgs);
    }
}
