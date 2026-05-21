package com.baghdad.edulife.features.courses.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.courses.model.CourseSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CourseCatalogAdapter extends RecyclerView.Adapter<CourseCatalogAdapter.CourseViewHolder> {

    public interface OnCourseClickListener {
        void onCourseClick(CourseSummary courseSummary);
    }

    private final List<CourseSummary> courses = new ArrayList<>();
    private final OnCourseClickListener onCourseClickListener;

    public CourseCatalogAdapter(OnCourseClickListener onCourseClickListener) {
        this.onCourseClickListener = onCourseClickListener;
    }

    public void submitList(List<CourseSummary> updatedCourses) {
        courses.clear();
        if (updatedCourses != null) {
            courses.addAll(updatedCourses);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_course_summary, parent, false);
        return new CourseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        CourseSummary course = courses.get(position);
        holder.bind(course, onCourseClickListener);
    }

    @Override
    public int getItemCount() {
        return courses.size();
    }

    static class CourseViewHolder extends RecyclerView.ViewHolder {

        private final TextView titleText;
        private final TextView descriptionText;
        private final TextView levelText;
        private final TextView languageText;
        private final Button openCourseButton;

        CourseViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.courseTitleText);
            descriptionText = itemView.findViewById(R.id.courseDescriptionText);
            levelText = itemView.findViewById(R.id.courseLevelText);
            languageText = itemView.findViewById(R.id.courseLanguageText);
            openCourseButton = itemView.findViewById(R.id.openCourseButton);
        }

        void bind(CourseSummary course, OnCourseClickListener clickListener) {
            titleText.setText(course.title);
            descriptionText.setText(course.shortDescription);
            levelText.setText(normalizeLabel(course.level));
            languageText.setText(itemView.getContext().getString(
                    R.string.catalog_course_language,
                    normalizeLabel(course.languageCode)
            ));

            View.OnClickListener openListener = v -> clickListener.onCourseClick(course);
            itemView.setOnClickListener(openListener);
            openCourseButton.setOnClickListener(openListener);
        }

        private String normalizeLabel(String rawValue) {
            if (rawValue == null || rawValue.isBlank()) {
                return "Unknown";
            }

            String normalized = rawValue.replace('_', ' ').toLowerCase(Locale.ROOT);
            return normalized.substring(0, 1).toUpperCase(Locale.ROOT) + normalized.substring(1);
        }
    }
}
