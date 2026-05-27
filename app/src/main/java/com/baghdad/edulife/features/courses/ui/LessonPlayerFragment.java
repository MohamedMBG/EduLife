package com.baghdad.edulife.features.courses.ui;

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
import com.baghdad.edulife.features.courses.model.LessonDetail;
import com.baghdad.edulife.features.courses.model.LessonDetailUiState;
import com.baghdad.edulife.features.courses.viewmodel.LessonDetailViewModel;

import java.util.Locale;

public class LessonPlayerFragment extends Fragment {

    private LessonDetailViewModel lessonDetailViewModel;
    private String courseId = "";
    private String lessonId = "";
    private Button markCompleteButton;

    public LessonPlayerFragment() {
        super(R.layout.fragment_lesson_player);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        lessonDetailViewModel = new ViewModelProvider(this).get(LessonDetailViewModel.class);

        Bundle args = getArguments();
        courseId = args != null ? args.getString("courseId", "") : "";
        lessonId = args != null ? args.getString("lessonId", "") : "";
        markCompleteButton = view.findViewById(R.id.lessonMarkCompleteButton);

        bindLoadingState();

        view.findViewById(R.id.lessonBackButton).setOnClickListener(v ->
                Navigation.findNavController(view).popBackStack());

        view.findViewById(R.id.playerPlayButton).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Video playback coming next sprint!", Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.lessonPrevButton).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Navigate to previous lesson — coming next sprint!", Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.lessonNextButton).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Navigate to next lesson — coming next sprint!", Toast.LENGTH_SHORT).show());

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
        ((TextView) view.findViewById(R.id.lessonTitle)).setText(safeText(lessonDetail.title, "Lesson"));
        ((TextView) view.findViewById(R.id.lessonSectionContext))
                .setText(lessonDetail.sectionTitle == null || lessonDetail.sectionTitle.isBlank()
                        ? ""
                        : "From: " + lessonDetail.sectionTitle);

        String lessonBody = firstNonBlank(lessonDetail.contentBody, lessonDetail.summary,
                getString(R.string.lesson_player_notes_placeholder));
        ((TextView) view.findViewById(R.id.lessonSummary)).setText(lessonBody);
        ((TextView) view.findViewById(R.id.lessonTypeBadge)).setText(normalizeLabel(lessonDetail.lessonType));
        ((TextView) view.findViewById(R.id.lessonOrderText)).setText(
                "Lesson " + (lessonDetail.displayOrder != null ? lessonDetail.displayOrder : 1));
        ((TextView) view.findViewById(R.id.lessonDurationText)).setText(
                (lessonDetail.durationMinutes != null ? lessonDetail.durationMinutes : 0) + " min");

        view.findViewById(R.id.lessonPreviewBadge)
                .setVisibility(lessonDetail.preview ? View.VISIBLE : View.GONE);

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
        ((TextView) view.findViewById(R.id.lessonTitle)).setText("Loading lesson...");
        ((TextView) view.findViewById(R.id.lessonSectionContext)).setText("");
        ((TextView) view.findViewById(R.id.lessonSummary)).setText("Fetching lesson content from the backend...");
        ((TextView) view.findViewById(R.id.lessonTypeBadge)).setText("");
        ((TextView) view.findViewById(R.id.lessonOrderText)).setText("");
        ((TextView) view.findViewById(R.id.lessonDurationText)).setText("0 min");
        view.findViewById(R.id.lessonPreviewBadge).setVisibility(View.GONE);
        markCompleteButton.setVisibility(View.GONE);
    }

    private void bindErrorState(String message) {
        View view = requireView();
        ((TextView) view.findViewById(R.id.lessonTitle)).setText("Lesson unavailable");
        ((TextView) view.findViewById(R.id.lessonSectionContext)).setText("");
        ((TextView) view.findViewById(R.id.lessonSummary)).setText(message);
        ((TextView) view.findViewById(R.id.lessonTypeBadge)).setText("");
        ((TextView) view.findViewById(R.id.lessonOrderText)).setText("");
        ((TextView) view.findViewById(R.id.lessonDurationText)).setText("0 min");
        view.findViewById(R.id.lessonPreviewBadge).setVisibility(View.GONE);
        markCompleteButton.setVisibility(View.GONE);
    }

    private void bindMissingIdsState() {
        bindErrorState("Lesson detail could not open because the course or lesson ID was missing.");
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
}
