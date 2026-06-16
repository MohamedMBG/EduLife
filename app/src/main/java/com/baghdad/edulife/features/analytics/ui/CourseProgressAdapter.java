package com.baghdad.edulife.features.analytics.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.analytics.model.CourseProgressAnalytics;

/**
 * Renders one progress card per enrolled course on the Study Analytics screen: title, progress
 * percentage, a slim progress bar, lessons completed/total, and last activity. Display only — all
 * values are pre-computed in the model.
 */
public class CourseProgressAdapter
        extends ListAdapter<CourseProgressAnalytics, CourseProgressAdapter.ViewHolder> {

    public CourseProgressAdapter() {
        super(DIFF);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_course_progress_analytics, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView percent;
        private final TextView lessons;
        private final TextView lastActivity;
        private final ProgressBar progress;

        ViewHolder(@NonNull View view) {
            super(view);
            title = view.findViewById(R.id.courseProgressTitle);
            percent = view.findViewById(R.id.courseProgressPercent);
            lessons = view.findViewById(R.id.courseProgressLessons);
            lastActivity = view.findViewById(R.id.courseProgressLastActivity);
            progress = view.findViewById(R.id.courseProgressBar);
        }

        void bind(@NonNull CourseProgressAnalytics item) {
            int pct = item.progressPercent();
            title.setText(item.courseTitle);
            percent.setText(percent.getResources().getString(R.string.analytics_percent_value, pct));
            progress.setProgress(pct);
            lessons.setText(lessons.getResources().getString(
                    R.string.analytics_course_lessons_of,
                    item.lessonsCompleted, item.totalLessons));
            lastActivity.setText(lastActivity.getResources().getString(
                    R.string.analytics_course_last_activity, item.lastActivity));
        }
    }

    private static final DiffUtil.ItemCallback<CourseProgressAnalytics> DIFF =
            new DiffUtil.ItemCallback<CourseProgressAnalytics>() {
                @Override
                public boolean areItemsTheSame(@NonNull CourseProgressAnalytics a,
                                               @NonNull CourseProgressAnalytics b) {
                    return a.courseTitle != null && a.courseTitle.equals(b.courseTitle);
                }

                @Override
                public boolean areContentsTheSame(@NonNull CourseProgressAnalytics a,
                                                  @NonNull CourseProgressAnalytics b) {
                    return a.lessonsCompleted == b.lessonsCompleted
                            && a.totalLessons == b.totalLessons
                            && equal(a.lastActivity, b.lastActivity);
                }

                private boolean equal(String x, String y) {
                    return x == null ? y == null : x.equals(y);
                }
            };
}
