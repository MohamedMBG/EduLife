package com.edulife.analytics.dto;

/**
 * Global platform counts. Only ADMIN may reach the endpoint that returns this. Values are
 * aggregate counts derived from existing tables — no per-user PII is exposed.
 */
public record PlatformAnalyticsDto(
        long learners,
        long teachers,
        long groupAdmins,
        long admins,
        long coursesDraft,
        long coursesPublished,
        long coursesArchived,
        long activeEnrollments,
        long totalExamAttempts,
        long totalExamsPassed,
        long totalCertificates
) {}
