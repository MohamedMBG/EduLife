package com.edulife.advisor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AdvisorLlmResult(
        String message,
        List<Pick> picks
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Pick(String courseId, String reason) {}
}
