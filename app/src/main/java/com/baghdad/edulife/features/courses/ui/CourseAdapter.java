package com.baghdad.edulife.features.courses.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.courses.model.CourseSummary;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter that displays a list of course summaries using either a featured
 * or standard card layout, with click handling to open a course's detail screen.
 */
public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.CourseViewHolder> {

    public interface OnCourseClickListener {
        void onCourseClick(CourseSummary course);
    }

    private final List<CourseSummary> courses = new ArrayList<>();
    private final boolean featured;
    private OnCourseClickListener listener;

    public CourseAdapter(boolean featured) {
        this.featured = featured;
    }

    public void setOnCourseClickListener(OnCourseClickListener listener) {
        this.listener = listener;
    }

    public void setCourses(List<CourseSummary> newCourses) {
        courses.clear();
        courses.addAll(newCourses);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = featured
                ? R.layout.item_course_card_featured
                : R.layout.item_course_card;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new CourseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        holder.bind(courses.get(position));
    }

    @Override
    public int getItemCount() {
        return courses.size();
    }

    class CourseViewHolder extends RecyclerView.ViewHolder {
        final TextView levelBadge;
        final TextView title;
        final TextView desc;
        final TextView language;

        CourseViewHolder(@NonNull View itemView) {
            super(itemView);
            levelBadge = itemView.findViewById(R.id.levelBadge);
            title = itemView.findViewById(R.id.courseTitle);
            desc = itemView.findViewById(R.id.courseDesc);
            language = itemView.findViewById(R.id.courseLanguage);
        }

        void bind(CourseSummary course) {
            title.setText(course.title);
            desc.setText(course.shortDescription);
            String lang = course.languageCode != null ? course.languageCode : "EN";
            language.setText(lang.equalsIgnoreCase("en") ? "English" : lang);

            String level = course.level != null ? course.level.toUpperCase() : "BEGINNER";
            levelBadge.setText(level);
            applyLevelStyle(level);

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onCourseClick(course);
            });
        }

        /**
         * Applies level-specific background drawable and text color to the level badge
         * (beginner=green, intermediate=orange, advanced=red).
         */
        private void applyLevelStyle(String level) {
            int bgRes;
            int textColor;
            switch (level) {
                case "INTERMEDIATE":
                    bgRes = R.drawable.bg_level_intermediate;
                    textColor = 0xFFC97000;
                    break;
                case "ADVANCED":
                    bgRes = R.drawable.bg_level_advanced;
                    textColor = 0xFFC03232;
                    break;
                default:
                    bgRes = R.drawable.bg_level_beginner;
                    textColor = 0xFF0F8A68;
                    break;
            }
            levelBadge.setBackgroundResource(bgRes);
            levelBadge.setTextColor(textColor);
        }
    }
}
