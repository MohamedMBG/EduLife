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
import com.baghdad.edulife.features.groupadmin.model.GroupMember;

/** Member row with email, role chip, and a remove action. */
public class GroupMemberAdapter extends ListAdapter<GroupMember, GroupMemberAdapter.ViewHolder> {

    public interface OnRemoveListener {
        void onRemove(GroupMember member);
    }

    private final OnRemoveListener listener;

    public GroupMemberAdapter(OnRemoveListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_group_member, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView email;
        private final TextView role;
        private final TextView removeButton;

        ViewHolder(@NonNull View view) {
            super(view);
            email = view.findViewById(R.id.memberEmail);
            role = view.findViewById(R.id.memberRole);
            removeButton = view.findViewById(R.id.memberRemove);
        }

        void bind(@NonNull GroupMember item, OnRemoveListener listener) {
            email.setText(item.email != null ? item.email : item.userId);
            role.setText(formatRole(item.role));
            removeButton.setOnClickListener(v -> listener.onRemove(item));
        }

        private String formatRole(String role) {
            if (role == null || role.isEmpty()) return "MEMBER";
            return role.replace('_', ' ');
        }
    }

    private static final DiffUtil.ItemCallback<GroupMember> DIFF =
            new DiffUtil.ItemCallback<GroupMember>() {
                @Override
                public boolean areItemsTheSame(@NonNull GroupMember a, @NonNull GroupMember b) {
                    return a.userId != null && a.userId.equals(b.userId);
                }

                @Override
                public boolean areContentsTheSame(@NonNull GroupMember a, @NonNull GroupMember b) {
                    return safeEq(a.email, b.email) && safeEq(a.role, b.role);
                }

                private boolean safeEq(String x, String y) {
                    return x == null ? y == null : x.equals(y);
                }
            };
}
