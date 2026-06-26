package com.baghdad.edulife.features.advisor.model;

import java.util.List;

/**
 * A single course recommendation returned by the AI advisor, including relevance score and reasoning.
 */
public class AdvisorRecommendation {
    public String courseId;
    public String reason;
    public double score;
    public List<String> matchedSkills;
}
