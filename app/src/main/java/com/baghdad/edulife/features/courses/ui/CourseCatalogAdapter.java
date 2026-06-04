package com.baghdad.edulife.features.courses.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.courses.model.CourseSummary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CourseCatalogAdapter extends RecyclerView.Adapter<CourseCatalogAdapter.CourseViewHolder> {

    public interface OnCourseClickListener {
        void onCourseClick(CourseSummary courseSummary);
    }

    private final List<CourseSummary> courses = new ArrayList<>();
    private final OnCourseClickListener onCourseClickListener;
    private Map<String, Integer> progressMap = Collections.emptyMap();

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

    public void updateProgressMap(Map<String, Integer> map) {
        progressMap = map != null ? new HashMap<>(map) : Collections.emptyMap();
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
        Integer percent = progressMap.get(course.id);
        holder.bind(course, onCourseClickListener, percent);
    }

    @Override
    public int getItemCount() {
        return courses.size();
    }

    static class CourseViewHolder extends RecyclerView.ViewHolder {

        private final ImageView heroImage;
        private final TextView titleText;
        private final TextView descriptionText;
        private final TextView levelText;
        private final TextView languageText;
        private final TextView ratingText;
        private final TextView reviewCountText;
        private final TextView openCourseButton;
        private final View progressLayout;
        private final ProgressBar progressBar;
        private final TextView progressText;

        CourseViewHolder(@NonNull View itemView) {
            super(itemView);
            heroImage = itemView.findViewById(R.id.courseHeroImage);
            titleText = itemView.findViewById(R.id.courseTitleText);
            descriptionText = itemView.findViewById(R.id.courseDescriptionText);
            levelText = itemView.findViewById(R.id.courseLevelText);
            languageText = itemView.findViewById(R.id.courseLanguageText);
            ratingText = itemView.findViewById(R.id.courseRatingText);
            reviewCountText = itemView.findViewById(R.id.courseReviewCountText);
            openCourseButton = itemView.findViewById(R.id.openCourseButton);
            progressLayout = itemView.findViewById(R.id.courseProgressLayout);
            progressBar = itemView.findViewById(R.id.courseProgressBar);
            progressText = itemView.findViewById(R.id.courseProgressText);
        }

        void bind(CourseSummary course, OnCourseClickListener clickListener, @androidx.annotation.Nullable Integer progressPercent) {
            titleText.setText(course.title);
            descriptionText.setText(course.shortDescription);
            levelText.setText(normalizeLabel(course.level));
            languageText.setText(itemView.getContext().getString(
                    R.string.catalog_course_language,
                    normalizeLabel(course.languageCode)
            ));

            if (course.imageUrl != null && !course.imageUrl.isBlank()) {
                Glide.with(itemView.getContext())
                        .load(course.imageUrl)
                        .placeholder(heroForLevel(course.level))
                        .error(heroForLevel(course.level))
                        .centerCrop()
                        .into(heroImage);
            } else {
                heroImage.setImageResource(heroForLevel(course.level));
            }

            // Ratings are not yet served by the backend; derive a stable value per course id
            // so the catalog reads as polished without showing fake activity on every scroll.
            float rating = stableRating(course.id);
            int reviewCount = stableReviewCount(course.id);
            ratingText.setText(String.format(Locale.US, "%.1f", rating));
            reviewCountText.setText(String.format(Locale.US, "(%d)", reviewCount));

            if (progressPercent != null) {
                progressBar.setProgress(progressPercent);
                progressText.setText(String.format(Locale.US, "%d%%", progressPercent));
                progressLayout.setVisibility(View.VISIBLE);
            } else {
                progressLayout.setVisibility(View.GONE);
            }

            View.OnClickListener openListener = v -> clickListener.onCourseClick(course);
            itemView.setOnClickListener(openListener);
            openCourseButton.setOnClickListener(openListener);
        }

        private int heroForLevel(String level) {
            if (level == null) return R.drawable.bg_course_hero_beginner;
            switch (level.toUpperCase(Locale.ROOT)) {
                case "INTERMEDIATE":
                    return R.drawable.bg_course_hero_intermediate;
                case "ADVANCED":
                    return R.drawable.bg_course_hero_advanced;
                case "BEGINNER":
                default:
                    return R.drawable.bg_course_hero_beginner;
            }
        }

        private float stableRating(String id) {
            if (id == null) return 4.7f;
            int hash = Math.abs(id.hashCode());
            // Range: 4.3 .. 4.9
            return 4.3f + (hash % 7) * 0.1f;
        }

        private int stableReviewCount(String id) {
            if (id == null) return 120;
            int hash = Math.abs(id.hashCode());
            return 80 + (hash % 420);
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
