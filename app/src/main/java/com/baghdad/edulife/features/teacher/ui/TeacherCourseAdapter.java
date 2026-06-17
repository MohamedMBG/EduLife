package com.baghdad.edulife.features.teacher.ui;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.teacher.model.CmsCourse;

public class TeacherCourseAdapter extends ListAdapter<CmsCourse, TeacherCourseAdapter.ViewHolder> {

    public interface OnCourseClickListener {
        void onCourseClick(CmsCourse course);
    }

    private final OnCourseClickListener listener;

    public TeacherCourseAdapter(OnCourseClickListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cms_course, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView courseImage;
        private final TextView courseTitle;
        private final TextView courseDesc;
        private final TextView statusChip;

        ViewHolder(@NonNull View view) {
            super(view);
            courseImage = view.findViewById(R.id.cmsCourseImage);
            courseTitle = view.findViewById(R.id.cmsCourseTitle);
            courseDesc = view.findViewById(R.id.cmsCourseDesc);
            statusChip = view.findViewById(R.id.cmsCourseStatusChip);
        }

        void bind(@NonNull CmsCourse item, OnCourseClickListener listener) {
            courseTitle.setText(item.title != null ? item.title : "Untitled");
            courseDesc.setText(item.shortDescription != null ? item.shortDescription : "");
            String status = item.status != null ? item.status : "DRAFT";
            statusChip.setText(resolveStatusLabel(status, itemView));
            applyStatusStyle(status);

            int fallback = R.drawable.bg_course_hero_beginner;
            if (item.imageUrl != null && !item.imageUrl.isBlank()) {
                Glide.with(itemView)
                        .load(item.imageUrl)
                        .placeholder(fallback)
                        .error(fallback)
                        .centerCrop()
                        .into(courseImage);
            } else {
                courseImage.setImageResource(fallback);
            }

            itemView.setOnClickListener(v -> listener.onCourseClick(item));
        }

        private String resolveStatusLabel(String status, View root) {
            switch (status) {
                case "PUBLISHED":
                    return root.getContext().getString(R.string.teacher_status_published);
                case "ARCHIVED":
                    return root.getContext().getString(R.string.teacher_status_archived);
                default:
                    return root.getContext().getString(R.string.teacher_status_draft);
            }
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

    private static final DiffUtil.ItemCallback<CmsCourse> DIFF =
            new DiffUtil.ItemCallback<CmsCourse>() {
                @Override
                public boolean areItemsTheSame(@NonNull CmsCourse a, @NonNull CmsCourse b) {
                    return a.id != null && a.id.equals(b.id);
                }

                @Override
                public boolean areContentsTheSame(@NonNull CmsCourse a, @NonNull CmsCourse b) {
                    return safeEq(a.title, b.title)
                            && safeEq(a.status, b.status)
                            && safeEq(a.shortDescription, b.shortDescription);
                }

                private boolean safeEq(String x, String y) {
                    return x == null ? y == null : x.equals(y);
                }
            };
}
