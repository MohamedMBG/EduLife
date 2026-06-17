package com.edulife.advisor.dto;

import java.util.List;
import java.util.UUID;

public record CourseContextDto(
        UUID id,
        String title,
        String shortDescription,
        String description,
        String level,
        String languageCode,
        List<String> tags,
        List<String> lessonTitles
) {}
