package com.edulife.advisor.dto;

import java.util.UUID;

public record AdvisorRecommendationDto(UUID courseId, String reason, double score) {}
