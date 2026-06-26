package com.edulife.advisor.dto;

import java.util.List;

/** Outbound response DTO containing the advisor message, recommendations, and the source strategy used. */
public record AdvisorResponse(
        String message,
        List<AdvisorRecommendationDto> recommendations,
        String source
) {
    public AdvisorResponse(String message, List<AdvisorRecommendationDto> recommendations) {
        this(message, recommendations, "deterministic-fallback");
    }
}
