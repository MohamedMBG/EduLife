package com.baghdad.edulife.features.courses.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.baghdad.edulife.R;
import com.baghdad.edulife.core.network.ApiClient;
import com.baghdad.edulife.core.network.ApiService;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LessonPlayerFragment extends Fragment {

    public LessonPlayerFragment() {
        super(R.layout.fragment_lesson_player);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args           = getArguments();
        String courseId       = args != null ? args.getString("courseId", "")       : "";
        String lessonId       = args != null ? args.getString("lessonId", "")       : "";
        String lessonTitle    = args != null ? args.getString("lessonTitle", "")    : "";
        String lessonSummary  = args != null ? args.getString("lessonSummary", "")  : "";
        String lessonType     = args != null ? args.getString("lessonType", "")     : "";
        int    durationMin    = args != null ? args.getInt("durationMinutes", 0)    : 0;
        boolean isPreview     = args != null && args.getBoolean("isPreview", false);
        String sectionTitle   = args != null ? args.getString("sectionTitle", "")   : "";
        int    orderInSection = args != null ? args.getInt("orderInSection", 1)     : 1;

        ((TextView) view.findViewById(R.id.lessonTitle)).setText(lessonTitle);
        ((TextView) view.findViewById(R.id.lessonSectionContext))
                .setText(sectionTitle.isBlank() ? "" : "From: " + sectionTitle);
        ((TextView) view.findViewById(R.id.lessonSummary)).setText(lessonSummary);
        ((TextView) view.findViewById(R.id.lessonTypeBadge)).setText(normalizeLabel(lessonType));
        ((TextView) view.findViewById(R.id.lessonOrderText)).setText("Lesson " + orderInSection);
        ((TextView) view.findViewById(R.id.lessonDurationText)).setText(durationMin + " min");

        view.findViewById(R.id.lessonPreviewBadge)
                .setVisibility(isPreview ? View.VISIBLE : View.GONE);

        view.findViewById(R.id.lessonBackButton).setOnClickListener(v ->
                Navigation.findNavController(view).popBackStack());

        view.findViewById(R.id.playerPlayButton).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Video playback coming next sprint!", Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.lessonPrevButton).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Navigate to previous lesson — coming next sprint!", Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.lessonNextButton).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Navigate to next lesson — coming next sprint!", Toast.LENGTH_SHORT).show());

        Button markCompleteButton = view.findViewById(R.id.lessonMarkCompleteButton);

        // Hide "Mark as Done" for preview lessons — no enrollment = no progress tracking
        if (isPreview || courseId.isBlank() || lessonId.isBlank()) {
            markCompleteButton.setVisibility(View.GONE);
            return;
        }

        markCompleteButton.setOnClickListener(v -> {
            markCompleteButton.setEnabled(false);
            markCompleteButton.setText(R.string.lesson_player_completed);

            ApiClient.getClient()
                    .create(ApiService.class)
                    .markLessonComplete(courseId, lessonId)
                    .enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                            if (!isAdded()) return;
                            if (response.isSuccessful()) {
                                // Button already shows "✓ Completed" — nothing more needed
                            } else if (response.code() == 403) {
                                resetButton(markCompleteButton);
                                Toast.makeText(requireContext(),
                                        "You need to enroll in this course first.",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                resetButton(markCompleteButton);
                                Toast.makeText(requireContext(),
                                        "Could not save progress. Try again.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                            if (!isAdded()) return;
                            resetButton(markCompleteButton);
                            Toast.makeText(requireContext(),
                                    "Network error. Check your connection.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
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
}
