package com.baghdad.edulife.features.advisor.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.advisor.model.AdvisorRecommendation;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter that displays advisor course recommendations, styling the first
 * item as "Best Match" and subsequent items as alternatives.
 */
public class AdvisorRecommendationAdapter
        extends RecyclerView.Adapter<AdvisorRecommendationAdapter.ViewHolder> {

    /** Listener invoked when a recommendation card is tapped. */
    public interface OnRecommendationClickListener {
        void onRecommendationClick(AdvisorRecommendation recommendation);
    }

    private final List<AdvisorRecommendation> items = new ArrayList<>();
    private final OnRecommendationClickListener listener;

    public AdvisorRecommendationAdapter(OnRecommendationClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<AdvisorRecommendation> updated) {
        items.clear();
        if (updated != null) {
            items.addAll(updated);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_advisor_recommendation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(position + 1, items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView rankBadge;
        private final TextView scoreBadge;
        private final TextView reasonText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            rankBadge = itemView.findViewById(R.id.advisorRankBadge);
            scoreBadge = itemView.findViewById(R.id.advisorScoreBadge);
            reasonText = itemView.findViewById(R.id.advisorReasonText);
        }

        void bind(int rank, AdvisorRecommendation rec, OnRecommendationClickListener listener) {
            if (rank == 1) {
                rankBadge.setText("BEST MATCH");
                rankBadge.setBackgroundResource(R.drawable.bg_badge_best);
                scoreBadge.setText("Start here");
            } else {
                rankBadge.setText("ALTERNATIVE");
                rankBadge.setBackgroundResource(R.drawable.bg_badge_next);
                scoreBadge.setText("Supporting skill");
            }

            reasonText.setText(rec.reason != null ? rec.reason : "");
            itemView.setOnClickListener(v -> listener.onRecommendationClick(rec));
        }
    }
}
