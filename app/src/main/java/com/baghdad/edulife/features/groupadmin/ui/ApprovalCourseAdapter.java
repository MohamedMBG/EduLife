package com.baghdad.edulife.features.groupadmin.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.teacher.model.CmsCourse;

/**
 * Course row for the approvals screen. When an approve listener is supplied the row shows an
 * "Approve & publish" button (pending queue); otherwise it is display-only (published list).
 */
public class ApprovalCourseAdapter extends ListAdapter<CmsCourse, ApprovalCourseAdapter.ViewHolder> {

    public interface OnApproveListener {
        void onApprove(CmsCourse course);
    }

    @Nullable
    private final OnApproveListener listener;

    public ApprovalCourseAdapter(@Nullable OnApproveListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_approval_course, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView courseImage;
        private final TextView title;
        private final TextView author;
        private final TextView approveButton;

        ViewHolder(@NonNull View view) {
            super(view);
            courseImage = view.findViewById(R.id.approvalCourseImage);
            title = view.findViewById(R.id.approvalTitle);
            author = view.findViewById(R.id.approvalAuthor);
            approveButton = view.findViewById(R.id.approvalButton);
        }

        void bind(@NonNull CmsCourse item, @Nullable OnApproveListener listener) {
            title.setText(item.title != null ? item.title : "Untitled");
            String byEmail = item.createdByEmail != null ? item.createdByEmail : "unknown teacher";
            author.setText(author.getContext().getString(R.string.approvals_authored_by, byEmail));

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

            if (listener != null) {
                approveButton.setVisibility(View.VISIBLE);
                approveButton.setOnClickListener(v -> listener.onApprove(item));
            } else {
                approveButton.setVisibility(View.GONE);
                approveButton.setOnClickListener(null);
            }
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
                            && safeEq(a.createdByEmail, b.createdByEmail);
                }

                private boolean safeEq(String x, String y) {
                    return x == null ? y == null : x.equals(y);
                }
            };
}
