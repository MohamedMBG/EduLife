package com.baghdad.edulife.features.gamification.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.gamification.model.LeaderboardEntryResponse;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    private final List<LeaderboardEntryResponse> entries = new ArrayList<>();
    private String currentUserId;

    public void submitList(List<LeaderboardEntryResponse> newEntries, @Nullable String userId) {
        entries.clear();
        currentUserId = userId;
        if (newEntries != null) {
            for (LeaderboardEntryResponse entry : newEntries) {
                if (entry.rank > 3) {
                    entries.add(entry);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(entries.get(position), currentUserId);

        holder.itemView.setAlpha(0f);
        holder.itemView.setTranslationY(20f);
        holder.itemView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(350)
                .setStartDelay(position * 50L)
                .start();
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout root;
        private final TextView rank;
        private final TextView initials;
        private final TextView name;
        private final TextView youBadge;
        private final TextView level;
        private final TextView xp;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.leaderboardEntryRoot);
            rank = itemView.findViewById(R.id.entryRank);
            initials = itemView.findViewById(R.id.entryInitials);
            name = itemView.findViewById(R.id.entryName);
            youBadge = itemView.findViewById(R.id.entryYouBadge);
            level = itemView.findViewById(R.id.entryLevel);
            xp = itemView.findViewById(R.id.entryXp);
        }

        void bind(LeaderboardEntryResponse entry, @Nullable String currentUserId) {
            Context context = itemView.getContext();
            boolean isCurrentUser = currentUserId != null && currentUserId.equals(entry.userId);

            rank.setText(context.getString(R.string.leaderboard_rank_label, entry.rank));
            initials.setText(getInitials(entry.displayName));
            name.setText(entry.displayName != null ? entry.displayName : "Learner");
            xp.setText(context.getString(R.string.leaderboard_xp_label, entry.totalXp));

            String levelText = "Lvl " + entry.level;
            if (entry.levelName != null && !entry.levelName.isBlank()) {
                levelText += " · " + entry.levelName;
            }
            level.setText(levelText);

            if (isCurrentUser) {
                root.setBackgroundResource(R.drawable.bg_leaderboard_current_user);
                youBadge.setVisibility(View.VISIBLE);
            } else {
                root.setBackgroundResource(R.drawable.bg_leaderboard_row);
                youBadge.setVisibility(View.GONE);
            }
        }

        private static String getInitials(@Nullable String name) {
            if (name == null || name.isBlank()) return "?";
            String[] parts = name.trim().split("\\s+");
            StringBuilder out = new StringBuilder(2);
            for (String part : parts) {
                if (part.isEmpty()) continue;
                out.append(Character.toUpperCase(part.charAt(0)));
                if (out.length() == 2) break;
            }
            return out.length() == 0 ? "?" : out.toString();
        }
    }
}
