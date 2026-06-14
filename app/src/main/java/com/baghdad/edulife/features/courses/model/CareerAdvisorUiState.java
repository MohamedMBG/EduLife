package com.baghdad.edulife.features.courses.model;

import java.util.Collections;
import java.util.List;

public class CareerAdvisorUiState {
    public final boolean loading;
    public final String learnerGoal;
    public final String assistantMessage;
    public final String errorMessage;
    public final List<CareerCourseRecommendation> recommendations;

    private CareerAdvisorUiState(
            boolean loading,
            String learnerGoal,
            String assistantMessage,
            String errorMessage,
            List<CareerCourseRecommendation> recommendations
    ) {
        this.loading = loading;
        this.learnerGoal = learnerGoal;
        this.assistantMessage = assistantMessage;
        this.errorMessage = errorMessage;
        this.recommendations = recommendations;
    }

    public static CareerAdvisorUiState idle() {
        return new CareerAdvisorUiState(
                false,
                "",
                "Tell me what you want to achieve, and I will compare it with the current EduLife courses.",
                null,
                Collections.emptyList()
        );
    }

    public static CareerAdvisorUiState loading(String learnerGoal) {
        return new CareerAdvisorUiState(
                true,
                learnerGoal,
                "I am checking the current course catalog against your goal...",
                null,
                Collections.emptyList()
        );
    }

    public static CareerAdvisorUiState success(
            String learnerGoal,
            String assistantMessage,
            List<CareerCourseRecommendation> recommendations
    ) {
        return new CareerAdvisorUiState(false, learnerGoal, assistantMessage, null, recommendations);
    }

    public static CareerAdvisorUiState error(String learnerGoal, String message) {
        return new CareerAdvisorUiState(false, learnerGoal, "", message, Collections.emptyList());
    }
}
