package com.edulife.advisor.dto;

import java.util.List;

public record AdvisorResponse(
        String message,
        List<AdvisorRecommendationDto> recommendations
) {}
