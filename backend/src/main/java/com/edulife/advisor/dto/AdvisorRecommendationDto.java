package com.edulife.advisor.dto;

import java.util.List;
import java.util.UUID;

public record AdvisorRecommendationDto(
        UUID courseId,
        String reason,
        double score,
        List<String> matchedSkills
) {
    public AdvisorRecommendationDto(UUID courseId, String reason, double score) {
        this(courseId, reason, score, List.of());
    }
}
