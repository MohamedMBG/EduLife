package com.edulife.admin.dto;

public record AdminMetricsDto(
        long totalLearners,
        long totalTeachers,
        long totalGroupAdmins,
        long totalCoursesDraft,
        long totalCoursesPublished,
        long totalCoursesArchived,
        long totalEnrollmentsActive,
        long totalCertificates
) {}
