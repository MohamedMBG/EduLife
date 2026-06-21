package com.baghdad.edulife.features.gamification.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.gamification.model.LevelMilestone;

import java.util.ArrayList;
import java.util.List;

public class LevelMilestoneAdapter extends RecyclerView.Adapter<LevelMilestoneAdapter.ViewHolder> {

    private final List<LevelMilestone> items = new ArrayList<>();

    public void submitList(List<LevelMilestone> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_level_milestone, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final View indicator;
        private final TextView levelText;
        private final TextView titleText;
        private final TextView xpText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            indicator = itemView.findViewById(R.id.milestoneIndicator);
            levelText = itemView.findViewById(R.id.milestoneLevelText);
            titleText = itemView.findViewById(R.id.milestoneTitleText);
            xpText = itemView.findViewById(R.id.milestoneXpText);
        }

        void bind(LevelMilestone milestone) {
            levelText.setText(String.valueOf(milestone.level));
            titleText.setText(milestone.title);
            xpText.setText(itemView.getContext().getString(
                    R.string.gamification_level_row_xp, milestone.xpThreshold));

            switch (milestone.state) {
                case DONE:
                    indicator.setBackgroundResource(R.drawable.bg_milestone_done);
                    levelText.setTextColor(itemView.getContext().getColor(R.color.white));
                    titleText.setTextColor(itemView.getContext().getColor(R.color.brand_text_primary));
                    xpText.setTextColor(itemView.getContext().getColor(R.color.brand_text_secondary));
                    itemView.setAlpha(1f);
                    break;
                case CURRENT:
                    indicator.setBackgroundResource(R.drawable.bg_milestone_current);
                    levelText.setTextColor(itemView.getContext().getColor(R.color.white));
                    titleText.setTextColor(itemView.getContext().getColor(R.color.brand_text_primary));
                    xpText.setTextColor(itemView.getContext().getColor(R.color.gamification_green));
                    itemView.setAlpha(1f);
                    break;
                case LOCKED:
                    indicator.setBackgroundResource(R.drawable.bg_milestone_locked);
                    levelText.setTextColor(itemView.getContext().getColor(R.color.brand_text_muted));
                    titleText.setTextColor(itemView.getContext().getColor(R.color.brand_text_muted));
                    xpText.setTextColor(itemView.getContext().getColor(R.color.brand_text_muted));
                    itemView.setAlpha(0.6f);
                    break;
            }
        }
    }
}
