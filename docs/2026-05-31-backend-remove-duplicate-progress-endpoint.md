# Backend: Remove Duplicate Progress Endpoint

## Goal

Remove the redundant `GET /api/v1/courses/{courseId}/progress` endpoint from `ProgressController` — its logic is identical to `GET /api/v1/progress/courses/{courseId}` in `ProgressQueryController`, which is the path the Android client uses.

## What Changed

`ProgressController` contained two endpoints:
1. `POST /api/v1/courses/{courseId}/lessons/{lessonId}/complete` — mark lesson complete (kept)
2. `GET /api/v1/courses/{courseId}/progress` — get course progress summary (removed)

The `GET` endpoint was dead code: `ApiService.java` calls `/progress/courses/{courseId}` (served by `ProgressQueryController`), not the `/courses/{courseId}/progress` variant. Having both routes serving identical data from the same service method is a maintenance hazard and creates unnecessary ambiguity about which path is canonical.

## Files Touched

- `backend/.../progress/controller/ProgressController.java` — removed `getCourseProgress` method and unused `CourseProgressDto` / `GetMapping` imports

## Backend Impact

`GET /api/v1/courses/{courseId}/progress` no longer exists. `GET /api/v1/progress/courses/{courseId}` is unchanged and is the only progress query endpoint. No client code targeted the removed route.

## Android Impact

None — `ApiService.java` already uses the canonical `/progress/courses/{courseId}` path.

## Web Impact

None.

## Architecture Compliance

Single responsibility: one endpoint per resource operation. No duplicate routes.

## Tests / Verification

Backend compiles. No runtime regression — the removed route was unreachable from any client.

## Risks / Notes

None.
