package com.baghdad.edulife.features.courses.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.courses.model.CareerCourseRecommendation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CareerRecommendationAdapter
        extends RecyclerView.Adapter<CareerRecommendationAdapter.RecommendationViewHolder> {

    public interface OnRecommendationClickListener {
        void onRecommendationClick(CareerCourseRecommendation recommendation);
    }

    private final List<CareerCourseRecommendation> recommendations = new ArrayList<>();
    private final OnRecommendationClickListener listener;

    public CareerRecommendationAdapter(OnRecommendationClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<CareerCourseRecommendation> updatedRecommendations) {
        recommendations.clear();
        if (updatedRecommendations != null) {
            recommendations.addAll(updatedRecommendations);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecommendationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_career_recommendation, parent, false);
        return new RecommendationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecommendationViewHolder holder, int position) {
        holder.bind(position + 1, recommendations.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return recommendations.size();
    }

    static class RecommendationViewHolder extends RecyclerView.ViewHolder {

        private final TextView rankText;
        private final TextView titleText;
        private final TextView metaText;
        private final TextView reasonText;
        private final TextView scoreText;

        RecommendationViewHolder(@NonNull View itemView) {
            super(itemView);
            rankText = itemView.findViewById(R.id.recommendationRankText);
            titleText = itemView.findViewById(R.id.recommendationTitleText);
            metaText = itemView.findViewById(R.id.recommendationMetaText);
            reasonText = itemView.findViewById(R.id.recommendationReasonText);
            scoreText = itemView.findViewById(R.id.recommendationScoreText);
        }

        void bind(
                int rank,
                CareerCourseRecommendation recommendation,
                OnRecommendationClickListener listener
        ) {
            // Apply different background badge shapes and labels based on priority rank to establish visual hierarchy.
            if (rank == 1) {
                rankText.setText("BEST MATCH");
                rankText.setBackgroundResource(R.drawable.bg_badge_best);
                scoreText.setText("Start here");
            } else {
                rankText.setText("ALTERNATIVE");
                rankText.setBackgroundResource(R.drawable.bg_badge_next);
                scoreText.setText("Supporting skill");
            }

            titleText.setText(recommendation.course.title != null ? recommendation.course.title : "Untitled course");
            
            // Format metadata with a clean bullet separator for professional design standard.
            metaText.setText(String.format(Locale.US, "%s • %s",
                    normalizeLabel(recommendation.course.level),
                    normalizeLabel(recommendation.course.languageCode)));
            reasonText.setText(recommendation.reason);

            // The click opens the existing course detail screen so the advisor stays a discovery
            // helper and does not create a parallel enrollment path.
            itemView.setOnClickListener(v -> listener.onRecommendationClick(recommendation));
        }

        // Map short ISO codes to user-friendly display labels so language codes (e.g. 'en') read naturally.
        private String normalizeLabel(String rawValue) {
            if (rawValue == null || rawValue.isBlank()) {
                return "Unknown";
            }
            if ("en".equalsIgnoreCase(rawValue)) {
                return "English";
            }
            if ("fr".equalsIgnoreCase(rawValue)) {
                return "French";
            }
            if ("ar".equalsIgnoreCase(rawValue)) {
                return "Arabic";
            }
            String normalized = rawValue.replace('_', ' ').toLowerCase(Locale.ROOT);
            return normalized.substring(0, 1).toUpperCase(Locale.ROOT) + normalized.substring(1);
        }
    }
}
