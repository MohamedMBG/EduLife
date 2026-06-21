package com.baghdad.edulife.features.gamification.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.courses.model.EnrolledCourse;
import com.baghdad.edulife.features.gamification.model.CourseProgressItem;

import java.util.ArrayList;
import java.util.List;

public class CourseProgressAdapter extends RecyclerView.Adapter<CourseProgressAdapter.ViewHolder> {

    public interface OnCourseClickListener {
        void onCourseClick(EnrolledCourse course);
    }

    private final List<CourseProgressItem> items = new ArrayList<>();
    private OnCourseClickListener clickListener;

    public void submitList(List<CourseProgressItem> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    public void setOnCourseClickListener(OnCourseClickListener listener) {
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_course_progress, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position), clickListener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView courseTitle;
        private final TextView progressText;
        private final ProgressBar progressBar;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            courseTitle = itemView.findViewById(R.id.courseProgressTitle);
            progressText = itemView.findViewById(R.id.courseProgressText);
            progressBar = itemView.findViewById(R.id.courseProgressBar);
        }

        void bind(CourseProgressItem item, OnCourseClickListener listener) {
            courseTitle.setText(item.course.title);

            if (item.progress != null) {
                int percent = (int) item.progress.percentComplete;
                progressBar.setProgress(percent);
                progressText.setText(itemView.getContext().getString(
                        R.string.courses_progress_format,
                        item.progress.completedLessons,
                        item.progress.totalLessons,
                        percent));
            } else if (item.failed) {
                progressBar.setProgress(0);
                progressText.setText(R.string.courses_progress_unavailable);
            } else {
                progressBar.setProgress(0);
                progressText.setText(R.string.courses_progress_loading);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onCourseClick(item.course);
            });
        }
    }
}
