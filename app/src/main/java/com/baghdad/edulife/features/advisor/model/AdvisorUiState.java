package com.baghdad.edulife.features.advisor.model;

import java.util.Collections;
import java.util.List;

/**
 * Immutable UI state for the advisor screen, representing idle, loading, success, and error states.
 */
public final class AdvisorUiState {
    public final boolean loading;
    public final String learnerGoal;
    public final String assistantMessage;
    public final String errorMessage;
    public final boolean isRateLimit;
    public final String retryGoal;
    public final List<AdvisorRecommendation> recommendations;

    private AdvisorUiState(
            boolean loading,
            String learnerGoal,
            String assistantMessage,
            String errorMessage,
            boolean isRateLimit,
            String retryGoal,
            List<AdvisorRecommendation> recommendations
    ) {
        this.loading = loading;
        this.learnerGoal = learnerGoal;
        this.assistantMessage = assistantMessage;
        this.errorMessage = errorMessage;
        this.isRateLimit = isRateLimit;
        this.retryGoal = retryGoal;
        this.recommendations = recommendations;
    }

    public static AdvisorUiState idle() {
        return new AdvisorUiState(
                false,
                "",
                "Tell me what you want to achieve, and I will compare it with the current EduLife courses.",
                null,
                false,
                null,
                Collections.emptyList()
        );
    }

    public static AdvisorUiState loading(String learnerGoal) {
        return new AdvisorUiState(
                true,
                learnerGoal,
                "Consulting the AI advisor...",
                null,
                false,
                null,
                Collections.emptyList()
        );
    }

    public static AdvisorUiState success(
            String learnerGoal,
            String assistantMessage,
            List<AdvisorRecommendation> recommendations
    ) {
        return new AdvisorUiState(false, learnerGoal, assistantMessage, null, false, null, recommendations);
    }

    public static AdvisorUiState error(String learnerGoal, String message, boolean isRateLimit) {
        return new AdvisorUiState(
                false,
                learnerGoal,
                "",
                message,
                isRateLimit,
                learnerGoal.isBlank() ? null : learnerGoal,
                Collections.emptyList()
        );
    }
}
