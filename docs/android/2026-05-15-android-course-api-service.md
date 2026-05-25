# Task Audit - Android Course API Service

## Date
2026-05-15

## Task Summary
Extended the shared Android Retrofit API service with course discovery endpoints for the live backend.

## Files Created
- docs/2026-05-15-android-course-api-service.md

## Files Modified
- app/src/main/java/com/baghdad/edulife/core/network/ApiService.java

## What Was Done
Added Retrofit endpoint declarations for:
- `GET /api/v1/courses`
- `GET /api/v1/courses/{courseId}`

The list endpoint includes the current backend query parameters for:
- `category`
- `page`
- `size`

This keeps the Android course feature aligned with the real Sprint 2 backend contract instead of relying on mock APIs.

## Architecture Compliance
The change stays inside `core/network/` because `ApiService` is the shared Retrofit contract surface used by feature repositories, while course-specific orchestration remains in `features/courses/data/`.

## Code Comments Added
Added comments in `ApiService` to explain why:
- the course list query maps to the current backend category contract
- the detail endpoint returns ordered course content for the learner flow

These comments clarify contract intent rather than restating annotations.

## Validation / Testing
Validated later as part of the full Android course catalog integration build.

## Risks / Notes
The backend currently uses `category` for what the Android UI presents as a learner-facing level filter. That naming mismatch is documented and should be revisited when backend category modeling expands.
