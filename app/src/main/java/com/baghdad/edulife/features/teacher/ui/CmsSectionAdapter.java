package com.baghdad.edulife.features.teacher.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.teacher.model.CmsSection;

public class CmsSectionAdapter extends ListAdapter<CmsSection, CmsSectionAdapter.ViewHolder> {

    public interface OnSectionLongClickListener {
        void onSectionLongClick(CmsSection section);
    }

    private final OnSectionLongClickListener longClickListener;

    public CmsSectionAdapter(OnSectionLongClickListener longClickListener) {
        super(DIFF);
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cms_section, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), longClickListener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView orderCircle;
        private final TextView sectionTitle;
        private final TextView deleteIcon;

        ViewHolder(@NonNull View view) {
            super(view);
            orderCircle = view.findViewById(R.id.cmsSectionOrder);
            sectionTitle = view.findViewById(R.id.cmsSectionTitle);
            deleteIcon = view.findViewById(R.id.cmsSectionDeleteIcon);
        }

        void bind(@NonNull CmsSection item, OnSectionLongClickListener longClickListener) {
            orderCircle.setText(String.valueOf(item.displayOrder));
            sectionTitle.setText(item.title != null ? item.title : "Untitled section");
            deleteIcon.setOnClickListener(v -> longClickListener.onSectionLongClick(item));
            itemView.setOnLongClickListener(v -> {
                longClickListener.onSectionLongClick(item);
                return true;
            });
        }
    }

    private static final DiffUtil.ItemCallback<CmsSection> DIFF =
            new DiffUtil.ItemCallback<CmsSection>() {
                @Override
                public boolean areItemsTheSame(@NonNull CmsSection a, @NonNull CmsSection b) {
                    return a.id != null && a.id.equals(b.id);
                }

                @Override
                public boolean areContentsTheSame(@NonNull CmsSection a, @NonNull CmsSection b) {
                    return safeEq(a.title, b.title) && a.displayOrder == b.displayOrder;
                }

                private boolean safeEq(String x, String y) {
                    return x == null ? y == null : x.equals(y);
                }
            };
}
