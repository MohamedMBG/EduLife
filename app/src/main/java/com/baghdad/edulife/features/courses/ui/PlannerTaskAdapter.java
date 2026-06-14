package com.baghdad.edulife.features.courses.ui;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.courses.model.PlannerTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for the Study Planner's task checklist.
 */
public class PlannerTaskAdapter extends RecyclerView.Adapter<PlannerTaskAdapter.ViewHolder> {

    public interface PlannerTaskListener {
        void onTaskToggled(String taskId);
        void onTaskDeleted(String taskId);
    }

    private final List<PlannerTask> taskList = new ArrayList<>();
    private final PlannerTaskListener listener;

    public PlannerTaskAdapter(PlannerTaskListener listener) {
        this.listener = listener;
    }

    public void submitList(List<PlannerTask> newList) {
        taskList.clear();
        if (newList != null) {
            taskList.addAll(newList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_planner_task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PlannerTask task = taskList.get(position);
        holder.bind(task, listener);
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final CheckBox taskCheckBox;
        private final TextView taskTitleText;
        private final View deleteTaskButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            taskCheckBox = itemView.findViewById(R.id.taskCheckBox);
            taskTitleText = itemView.findViewById(R.id.taskTitleText);
            deleteTaskButton = itemView.findViewById(R.id.deleteTaskButton);
        }

        void bind(PlannerTask task, PlannerTaskListener listener) {
            taskTitleText.setText(task.getTitle());

            // Clear check listener first to prevent recursion during binding
            taskCheckBox.setOnCheckedChangeListener(null);
            taskCheckBox.setChecked(task.isCompleted());

            // Styling based on task completion
            if (task.isCompleted()) {
                taskTitleText.setPaintFlags(taskTitleText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                taskTitleText.setTextColor(itemView.getContext().getColor(R.color.brand_text_muted));
            } else {
                taskTitleText.setPaintFlags(taskTitleText.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                taskTitleText.setTextColor(itemView.getContext().getColor(R.color.brand_text_primary));
            }

            taskCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onTaskToggled(task.getId());
                }
            });

            deleteTaskButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTaskDeleted(task.getId());
                }
            });
        }
    }
}
