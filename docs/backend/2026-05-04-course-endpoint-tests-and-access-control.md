# Task Audit - Course Endpoint Tests And Access Control

## Date
2026-05-04

## Task Summary
Added tests for published course list and course detail endpoints and aligned access control with the current Sprint 2 security decision to keep course endpoints protected by Firebase authentication.

## Files Created
- backend/src/test/java/com/edulife/courses/CourseControllerTest.java
- docs/2026-05-04-course-endpoint-tests-and-access-control.md

## Files Modified
- backend/src/main/java/com/edulife/common/error/GlobalApiExceptionHandler.java
- backend/src/main/java/com/edulife/security/SecurityConfig.java

## What Was Done
Created `CourseControllerTest` to verify:
- `GET /api/v1/courses` returns `200` with a valid Firebase token
- `GET /api/v1/courses` returns a paginated response shape
- `GET /api/v1/courses` returns an empty paginated list when no courses exist
- `GET /api/v1/courses/{id}` returns `200` with nested sections and lessons
- `GET /api/v1/courses/{id}` returns `404` for fake course IDs

Resolved the access-control decision from issue `#230` by keeping course endpoints protected. This matches the current EduLife execution plan, where course discovery is part of the authenticated learner flow.

Because of that decision, the old expectation from issue `#212` for `200` with no token was not implemented. The verified behavior is:
- no token -> `401 Authentication required`

Updated `SecurityConfig` comments to make the protected-course decision explicit.

Updated `GlobalApiExceptionHandler` so `ResponseStatusException` returns the clean public-facing reason, such as `Course not found`, instead of Spring's verbose `404 NOT_FOUND "Course not found"` message.

## Architecture Compliance
The test work stays within the backend testing structure:
- endpoint tests under `backend/src/test/java/com/edulife/courses`
- security policy remains centralized in `SecurityConfig`
- API error formatting remains centralized in `GlobalApiExceptionHandler`

No business logic was moved into controllers or tests.

## Code Comments Added
Added comments in:
- `CourseControllerTest.java` to explain the protected-course decision and why pagination structure matters
- `SecurityConfig.java` to document why course endpoints remain authenticated
- `GlobalApiExceptionHandler.java` to explain why `ResponseStatusException` reasons are preferred over framework-generated messages

These comments explain the non-obvious behavior decisions behind the implementation.

## Validation / Testing
Ran:
- `./mvnw -Dtest=CourseControllerTest test`

Result:
- passed successfully

Note:
- The full backend suite still has the previously known Flyway checksum issue against the local `edulife` PostgreSQL database, so this task used focused MVC tests that do not depend on local migration history.

## Risks / Notes
Spring logs a warning that serializing `PageImpl` directly does not guarantee long-term JSON stability. The current endpoint still uses Spring `Page` because that is the existing API contract. If a more stable list contract is needed later, the project should introduce a dedicated pagination response DTO.
