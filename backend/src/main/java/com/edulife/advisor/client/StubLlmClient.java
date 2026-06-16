package com.edulife.advisor.client;

import com.edulife.advisor.dto.AdvisorLlmResult;
import com.edulife.advisor.dto.CourseContextDto;
import java.util.List;

public class StubLlmClient implements LlmClient {

    @Override
    public AdvisorLlmResult recommend(String goal, List<CourseContextDto> catalog) {
        return new AdvisorLlmResult("", List.of());
    }
}
