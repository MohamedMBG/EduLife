package com.edulife.advisor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AdvisorLlmResult(
        String message,
        List<Pick> picks
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Pick(
            String courseId,
            String reason,
            Double confidence,
            List<String> matchedSkills
    ) {
        public Pick(String courseId, String reason) {
            this(courseId, reason, null, null);
        }
    }
}
