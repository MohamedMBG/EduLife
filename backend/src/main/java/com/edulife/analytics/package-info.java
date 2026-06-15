/**
 * Read-only analytics module (Phase A).
 *
 * <p>This module derives operational metrics from existing MVP tables only. It owns no
 * entities and writes no data: every query is a read-only aggregate over the courses,
 * enrollments, progress, exams, and certificates already produced by the learner flow.</p>
 *
 * <p>Scope is always resolved server-side from the authenticated Firebase identity. A
 * client-supplied {@code userId}, {@code teacherId}, or {@code role} is never trusted for
 * scoping. {@code firebase_uid} and exam correct answers are never exposed.</p>
 *
 * <p>Phase A intentionally excludes event pipelines, warehouses, snapshot tables, AI, and
 * predictions per docs/2026-06-14-advanced-analytics-planning.md.</p>
 */
package com.edulife.analytics;
