package com.baghdad.edulife.features.groupadmin.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.groupadmin.model.GroupCourse;

/** Read-only row for a course attached to the group, with a status chip. */
public class GroupCourseAdapter extends ListAdapter<GroupCourse, GroupCourseAdapter.ViewHolder> {

    public GroupCourseAdapter() {
        super(DIFF);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_group_course, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView statusChip;

        ViewHolder(@NonNull View view) {
            super(view);
            title = view.findViewById(R.id.groupCourseTitle);
            statusChip = view.findViewById(R.id.groupCourseStatus);
        }

        void bind(@NonNull GroupCourse item) {
            title.setText(item.title != null ? item.title : "Untitled");
            String status = item.status != null ? item.status : "DRAFT";
            statusChip.setText(status);
            applyStatusStyle(status);
        }

        private void applyStatusStyle(String status) {
            int textColor;
            int bgDrawable;
            switch (status) {
                case "PUBLISHED":
                    textColor = itemView.getContext().getColor(R.color.teacher_status_published_text);
                    bgDrawable = R.drawable.bg_teacher_status_published;
                    break;
                case "ARCHIVED":
                    textColor = itemView.getContext().getColor(R.color.teacher_status_archived_text);
                    bgDrawable = R.drawable.bg_teacher_status_archived;
                    break;
                default:
                    textColor = itemView.getContext().getColor(R.color.teacher_status_draft_text);
                    bgDrawable = R.drawable.bg_teacher_status_draft;
                    break;
            }
            statusChip.setTextColor(textColor);
            statusChip.setBackgroundResource(bgDrawable);
        }
    }

    private static final DiffUtil.ItemCallback<GroupCourse> DIFF =
            new DiffUtil.ItemCallback<GroupCourse>() {
                @Override
                public boolean areItemsTheSame(@NonNull GroupCourse a, @NonNull GroupCourse b) {
                    return a.courseId != null && a.courseId.equals(b.courseId);
                }

                @Override
                public boolean areContentsTheSame(@NonNull GroupCourse a, @NonNull GroupCourse b) {
                    return safeEq(a.title, b.title) && safeEq(a.status, b.status);
                }

                private boolean safeEq(String x, String y) {
                    return x == null ? y == null : x.equals(y);
                }
            };
}
