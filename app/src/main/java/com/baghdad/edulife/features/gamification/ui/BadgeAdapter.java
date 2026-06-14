package com.baghdad.edulife.features.gamification.ui;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.widget.ImageViewCompat;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.gamification.model.Badge;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for showing badges in a grid on the Gamification dashboard.
 */
public class BadgeAdapter extends RecyclerView.Adapter<BadgeAdapter.ViewHolder> {

    private final List<Badge> badges = new ArrayList<>();

    public void submitList(List<Badge> newBadges) {
        badges.clear();
        if (newBadges != null) {
            badges.addAll(newBadges);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_badge, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(badges.get(position));
    }

    @Override
    public int getItemCount() {
        return badges.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout badgeCardRoot;
        private final ImageView badgeIcon;
        private final TextView badgeName;
        private final TextView badgeDescription;
        private final ImageView badgeLockIcon;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            badgeCardRoot = itemView.findViewById(R.id.badgeCardRoot);
            badgeIcon = itemView.findViewById(R.id.badgeIcon);
            badgeName = itemView.findViewById(R.id.badgeName);
            badgeDescription = itemView.findViewById(R.id.badgeDescription);
            badgeLockIcon = itemView.findViewById(R.id.badgeLockIcon);
        }

        void bind(Badge badge) {
            badgeIcon.setImageResource(badge.iconResId);
            badgeName.setText(badge.name);
            badgeDescription.setText(badge.description);

            if (badge.earned) {
                badgeCardRoot.setBackgroundResource(R.drawable.bg_badge_card);
                badgeIcon.setAlpha(1.0f);
                ImageViewCompat.setImageTintList(badgeIcon, null);
                badgeName.setTextColor(itemView.getContext().getColor(R.color.brand_text_primary));
                badgeDescription.setTextColor(itemView.getContext().getColor(R.color.brand_text_secondary));
                badgeLockIcon.setVisibility(View.GONE);
            } else {
                badgeCardRoot.setBackgroundResource(R.drawable.bg_badge_card_locked);
                badgeIcon.setAlpha(0.35f);
                ImageViewCompat.setImageTintList(badgeIcon,
                        ColorStateList.valueOf(itemView.getContext().getColor(R.color.brand_text_muted)));
                badgeName.setTextColor(itemView.getContext().getColor(R.color.brand_text_muted));
                badgeDescription.setTextColor(itemView.getContext().getColor(R.color.brand_text_muted));
                badgeLockIcon.setVisibility(View.VISIBLE);
                ImageViewCompat.setImageTintList(badgeLockIcon,
                        ColorStateList.valueOf(itemView.getContext().getColor(R.color.brand_text_muted)));
            }
        }
    }
}
