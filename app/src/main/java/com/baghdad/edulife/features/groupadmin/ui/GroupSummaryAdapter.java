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
import com.baghdad.edulife.features.groupadmin.model.GroupSummary;

/** Renders the group admin's owned groups with member/course headline counts. */
public class GroupSummaryAdapter extends ListAdapter<GroupSummary, GroupSummaryAdapter.ViewHolder> {

    public interface OnGroupClickListener {
        void onGroupClick(GroupSummary group);
    }

    private final OnGroupClickListener listener;

    public GroupSummaryAdapter(OnGroupClickListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_group_summary, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView name;
        private final TextView counts;

        ViewHolder(@NonNull View view) {
            super(view);
            name = view.findViewById(R.id.groupName);
            counts = view.findViewById(R.id.groupCounts);
        }

        void bind(@NonNull GroupSummary item, OnGroupClickListener listener) {
            name.setText(item.name != null ? item.name : "Untitled group");
            counts.setText(counts.getContext().getString(
                    R.string.group_admin_counts, item.memberCount, item.courseCount));
            itemView.setOnClickListener(v -> listener.onGroupClick(item));
        }
    }

    private static final DiffUtil.ItemCallback<GroupSummary> DIFF =
            new DiffUtil.ItemCallback<GroupSummary>() {
                @Override
                public boolean areItemsTheSame(@NonNull GroupSummary a, @NonNull GroupSummary b) {
                    return a.id != null && a.id.equals(b.id);
                }

                @Override
                public boolean areContentsTheSame(@NonNull GroupSummary a, @NonNull GroupSummary b) {
                    return safeEq(a.name, b.name)
                            && a.memberCount == b.memberCount
                            && a.courseCount == b.courseCount;
                }

                private boolean safeEq(String x, String y) {
                    return x == null ? y == null : x.equals(y);
                }
            };
}
