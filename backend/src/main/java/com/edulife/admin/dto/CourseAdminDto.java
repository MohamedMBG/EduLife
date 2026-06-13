package com.edulife.admin.dto;

import com.edulife.courses.model.CourseStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * CMS-facing course projection that includes status and createdByUserId fields
 * not present in the learner-facing CourseSummaryDto.
 */
public record CourseAdminDto(
        UUID id,
        String slug,
        String title,
        String shortDescription,
        String description,
        String languageCode,
        String level,
        String imageUrl,
        CourseStatus status,
        Instant publishedAt,
        UUID createdByUserId,
        // Lets reviewers (group admins / admins) see which teacher authored the course.
        String createdByEmail,
        Instant createdAt,
        Instant updatedAt
) {}
