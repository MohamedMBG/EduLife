# Enrollment Creates Initial CourseProgress

## Goal

Fix AGENTS.md violation: enrolling in a course must create an initial `CourseProgress` row so progress queries work immediately after enrollment without waiting for the first lesson completion.

## What Changed

### `ProgressService`
Added public `initializeCourseProgress(UUID userId, UUID courseId)` that delegates to the existing private `syncCourseProgress`. Annotated `@Transactional` to participate in the caller's transaction.

### `EnrollmentService`
- Injected `ProgressService` via constructor
- After both enrollment paths (new row + reactivation), calls `progressService.initializeCourseProgress(userId, courseId)`

**Behavior:**
- New enrollment: creates `CourseProgress(0, totalLessons)` where totalLessons = `lessonRepository.countByCourseId`
- Reactivation: recalculates `completedLessons` from existing `lesson_progress` rows and updates the record

## Files Touched

- `backend/src/main/java/com/edulife/enrollments/service/EnrollmentService.java`
- `backend/src/main/java/com/edulife/progress/service/ProgressService.java`

## Backend Impact

`POST /api/v1/enrollments` now guarantees a `CourseProgress` row exists. `GET /api/v1/progress/courses/{courseId}` no longer returns stale or missing data immediately after enrollment.

## Android Impact

None — no API contract change.

## Web Impact

None — no API contract change.

## Architecture Compliance

- Business logic stays in service layer
- No circular dependency: `EnrollmentService` → `ProgressService` → `EnrollmentRepository` (no cycle)
- Method is idempotent — safe to call on reactivation

## Risks / Notes

None. Existing `syncCourseProgress` logic was already battle-tested by `markLessonComplete`.
