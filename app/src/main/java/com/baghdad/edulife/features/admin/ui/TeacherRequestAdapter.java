package com.baghdad.edulife.features.admin.ui;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.admin.model.AdminTeacherRequest;

public class TeacherRequestAdapter extends ListAdapter<AdminTeacherRequest, TeacherRequestAdapter.ViewHolder> {

    public interface ActionListener {
        void onApprove(String requestId);
        void onReject(String requestId);
    }

    private final ActionListener listener;

    public TeacherRequestAdapter(ActionListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_teacher_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView statusPill;
        private final TextView dateText;
        private final TextView emailText;
        private final TextView motivationText;
        private final LinearLayout actionRow;
        private final TextView approveButton;
        private final TextView rejectButton;
        private final LinearLayout adminNoteRow;
        private final TextView adminNoteText;

        ViewHolder(@NonNull View view) {
            super(view);
            statusPill = view.findViewById(R.id.requestStatusPill);
            dateText = view.findViewById(R.id.requestDate);
            emailText = view.findViewById(R.id.requestEmail);
            motivationText = view.findViewById(R.id.requestMotivation);
            actionRow = view.findViewById(R.id.requestActionRow);
            approveButton = view.findViewById(R.id.requestApproveButton);
            rejectButton = view.findViewById(R.id.requestRejectButton);
            adminNoteRow = view.findViewById(R.id.requestAdminNoteRow);
            adminNoteText = view.findViewById(R.id.requestAdminNote);
        }

        void bind(@NonNull AdminTeacherRequest item, ActionListener listener) {
            emailText.setText(item.userEmail != null ? item.userEmail : "Unknown");
            motivationText.setText(item.motivation != null ? item.motivation : "No motivation provided.");
            dateText.setText(formatDate(item.requestedAt));
            statusPill.setText(item.status != null ? item.status : "PENDING");

            int statusColor = resolveStatusColor(item.status);
            statusPill.setTextColor(statusColor);
            statusPill.setBackgroundTintList(ColorStateList.valueOf(
                    resolveStatusSurface(item.status, itemView)));

            boolean isPending = "PENDING".equals(item.status);
            actionRow.setVisibility(isPending ? View.VISIBLE : View.GONE);

            boolean hasNote = item.adminNote != null && !item.adminNote.isBlank();
            adminNoteRow.setVisibility(hasNote ? View.VISIBLE : View.GONE);
            if (hasNote) {
                adminNoteText.setText(item.adminNote);
            }

            approveButton.setOnClickListener(v -> {
                if (item.id != null) listener.onApprove(item.id);
            });
            rejectButton.setOnClickListener(v -> {
                if (item.id != null) listener.onReject(item.id);
            });
        }

        private String formatDate(String iso) {
            if (iso == null || iso.length() < 10) return "";
            return iso.substring(0, 10);
        }

        private int resolveStatusColor(String status) {
            if ("APPROVED".equals(status)) {
                return itemView.getContext().getColor(R.color.brand_primary);
            }
            if ("REJECTED".equals(status)) {
                return itemView.getContext().getColor(R.color.brand_error);
            }
            return itemView.getContext().getColor(R.color.admin_accent);
        }

        private int resolveStatusSurface(String status, View root) {
            if ("APPROVED".equals(status)) {
                return root.getContext().getColor(R.color.brand_primary_surface);
            }
            if ("REJECTED".equals(status)) {
                return root.getContext().getColor(R.color.brand_error_surface);
            }
            return root.getContext().getColor(R.color.admin_accent_surface);
        }
    }

    private static final DiffUtil.ItemCallback<AdminTeacherRequest> DIFF =
            new DiffUtil.ItemCallback<AdminTeacherRequest>() {
                @Override
                public boolean areItemsTheSame(@NonNull AdminTeacherRequest a, @NonNull AdminTeacherRequest b) {
                    return a.id != null && a.id.equals(b.id);
                }

                @Override
                public boolean areContentsTheSame(@NonNull AdminTeacherRequest a, @NonNull AdminTeacherRequest b) {
                    return a.status != null && a.status.equals(b.status)
                            && safeEq(a.adminNote, b.adminNote);
                }

                private boolean safeEq(String x, String y) {
                    return x == null ? y == null : x.equals(y);
                }
            };
}
