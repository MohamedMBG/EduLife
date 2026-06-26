package com.edulife.admin.dto;

/** Snapshot of platform-wide counts returned by the admin metrics endpoint. */
public record AdminMetricsDto(
        long totalLearners,
        long totalTeachers,
        long totalGroupAdmins,
        long totalCoursesDraft,
        long totalCoursesPublished,
        long totalCoursesArchived,
        long totalEnrollmentsActive,
        long totalCertificates,
        long pendingTeacherRequests
) {}
