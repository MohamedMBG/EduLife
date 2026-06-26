package com.edulife.advisor.client;

import com.edulife.advisor.dto.AdvisorLlmResult;
import com.edulife.advisor.dto.CourseContextDto;
import java.util.List;

/** Abstraction over an LLM provider used to generate course recommendations. */
public interface LlmClient {

    /**
     * Asks the LLM to pick the best course(s) from the catalog for the given learner goal.
     *
     * @param goal    the learner's stated learning objective
     * @param catalog pre-filtered courses to choose from
     * @return parsed LLM recommendation result
     */
    AdvisorLlmResult recommend(String goal, List<CourseContextDto> catalog);
}
