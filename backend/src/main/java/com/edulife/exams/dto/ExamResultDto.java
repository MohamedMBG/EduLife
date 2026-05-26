package com.edulife.exams.dto;

import java.util.UUID;

public record ExamResultDto(
        UUID examId,
        int score,
        int passScore,
        boolean passed,
        String certificateNumber
) {}
