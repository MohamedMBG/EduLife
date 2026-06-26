package com.baghdad.edulife.features.advisor.model;

import java.util.List;

/**
 * Response DTO from the advisor endpoint containing the assistant message and ranked course recommendations.
 */
public class AdvisorResponse {
    public String message;
    public List<AdvisorRecommendation> recommendations;
    public String source;
}
