package com.edulife.advisor.client;

import com.edulife.advisor.dto.AdvisorLlmResult;
import com.edulife.advisor.dto.CourseContextDto;
import java.util.List;

public interface LlmClient {
    AdvisorLlmResult recommend(String goal, List<CourseContextDto> catalog);
}
