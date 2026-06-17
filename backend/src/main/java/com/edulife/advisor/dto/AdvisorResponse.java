package com.edulife.advisor.dto;

import java.util.List;

public record AdvisorResponse(
        String message,
        List<AdvisorRecommendationDto> recommendations,
        String source
) {
    public AdvisorResponse(String message, List<AdvisorRecommendationDto> recommendations) {
        this(message, recommendations, "deterministic-fallback");
    }
}
