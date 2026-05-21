package com.baghdad.edulife.features.courses.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.baghdad.edulife.R;

import java.util.Locale;

public class LessonPlayerFragment extends Fragment {

    public LessonPlayerFragment() {
        super(R.layout.fragment_lesson_player);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args           = getArguments();
        String lessonId       = args != null ? args.getString("lessonId", "")      : "";
        String lessonTitle    = args != null ? args.getString("lessonTitle", "")   : "";
        String lessonSummary  = args != null ? args.getString("lessonSummary", "") : "";
        String lessonType     = args != null ? args.getString("lessonType", "")    : "";
        int    durationMin    = args != null ? args.getInt("durationMinutes", 0)   : 0;
        boolean isPreview     = args != null && args.getBoolean("isPreview", false);
        String sectionTitle   = args != null ? args.getString("sectionTitle", "")  : "";
        int    orderInSection = args != null ? args.getInt("orderInSection", 1)    : 1;

        ((TextView) view.findViewById(R.id.lessonTitle)).setText(lessonTitle);
        ((TextView) view.findViewById(R.id.lessonSectionContext))
                .setText(sectionTitle.isBlank() ? "" : "From: " + sectionTitle);
        ((TextView) view.findViewById(R.id.lessonSummary)).setText(lessonSummary);
        ((TextView) view.findViewById(R.id.lessonTypeBadge)).setText(normalizeLabel(lessonType));
        ((TextView) view.findViewById(R.id.lessonOrderText))
                .setText("Lesson " + orderInSection);
        ((TextView) view.findViewById(R.id.lessonDurationText))
                .setText(durationMin + " min");

        View previewBadge = view.findViewById(R.id.lessonPreviewBadge);
        previewBadge.setVisibility(isPreview ? View.VISIBLE : View.GONE);

        view.findViewById(R.id.lessonBackButton).setOnClickListener(v ->
                Navigation.findNavController(view).popBackStack());

        view.findViewById(R.id.playerPlayButton).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Video playback coming next sprint!", Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.lessonPrevButton).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Navigate to previous lesson — coming next sprint!", Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.lessonNextButton).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Navigate to next lesson — coming next sprint!", Toast.LENGTH_SHORT).show());
    }

    private String normalizeLabel(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String s = raw.replace('_', ' ').toLowerCase(Locale.ROOT);
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }
}
