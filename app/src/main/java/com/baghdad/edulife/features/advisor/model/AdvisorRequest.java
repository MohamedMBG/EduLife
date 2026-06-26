package com.baghdad.edulife.features.advisor.model;

/**
 * Request DTO sent to the advisor endpoint containing the learner's career goal.
 */
public class AdvisorRequest {
    public final String goal;

    public AdvisorRequest(String goal) {
        this.goal = goal;
    }
}
