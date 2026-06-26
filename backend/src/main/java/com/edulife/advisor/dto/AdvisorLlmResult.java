package com.edulife.advisor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** Deserialized JSON response from the LLM containing a message and a list of course picks. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AdvisorLlmResult(
        String message,
        List<Pick> picks
) {
    /** A single course recommendation returned by the LLM. */
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
