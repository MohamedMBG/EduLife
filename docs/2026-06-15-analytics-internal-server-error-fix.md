# Analytics Internal Server Error — Fix

## Goal

Resolve `Analytics unavailable / Internal server error` shown on the web app when a learner
opens the `/analytics` route.

## Root Cause

Two layers contributed to the same 500:

1. **Stale backend deploy.** The analytics package (controllers, services, repositories) was
   committed on `codex/gamification-course-card-polish` (PR #351) at `51f3baf` but never merged
   to `main`. The deployed backend therefore had no handler for `GET /api/v1/analytics/me/summary`
   (and siblings) and was returning a generic error to the web client.
2. **404 masquerading as 500.** Spring Boot 3.5 raises `NoHandlerFoundException` for unmapped
   paths, and `GlobalApiExceptionHandler.handleUnexpected(Exception)` was the only handler that
   matched it — so a missing endpoint surfaced to the client as
   `{"status":500,"message":"Internal server error"}` instead of the documented 404 contract.
   This swallowed the real diagnostic ("the endpoint isn't deployed yet") behind a generic crash.

## What Changed

- Branched off `main` (no analytics) and cherry-picked `51f3baf` to ship the analytics module
  on its own PR, independent of the gamification work still in flight on PR #351.
- Added a dedicated `@ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})`
  in `GlobalApiExceptionHandler` returning a 404 `ApiError` so future stale deploys are
  diagnosable from the client message.
- Enabled `spring.mvc.throw-exception-if-no-handler-found=true` and disabled the default
  `spring.web.resources.add-mappings` fallback so unknown `/api/v1/**` paths reach the handler
  instead of being captured by the static-resource chain.
- Unit test `NotFoundExceptionHandlerTest` locks in the new 404 contract for both
  `NoHandlerFoundException` and `NoResourceFoundException`.

## Files Touched

- backend/src/main/java/com/edulife/common/error/GlobalApiExceptionHandler.java
- backend/src/main/resources/application.yaml
- backend/src/test/java/com/edulife/common/error/NotFoundExceptionHandlerTest.java
- (cherry-pick from `51f3baf`) full `backend/src/main/java/com/edulife/analytics/**`,
  additive query methods on `CertificateRepository`, `EnrollmentRepository`,
  `ExamAttemptRepository`, `CourseProgressRepository`, web routes, Android UI.

## Backend Impact

- Analytics endpoints are now reachable on the merged backend:
  `GET /api/v1/analytics/me/summary`, `/me/progress-trend`, `/teacher/courses`,
  `/teacher/cohorts`, `/group/{groupId}/cohorts`, `/platform`, `/platform/cohorts`.
- All read-only; no Flyway migration; ownership scoping enforced server-side per existing pattern.
- Generic exception handler no longer eats `NoHandlerFoundException` / `NoResourceFoundException`.

## Web Impact

- Web routes `/analytics` and `/admin/analytics` already shipped in PR #351's cherry-picked
  commit and now resolve against a real backend.

## Android Impact

- Phase A/C learner + platform analytics fragments + ViewModels shipped from the cherry-pick.

## Tests / Verification

- `mvnw -o test` → 159 tests, all green (157 prior + 2 new in `NotFoundExceptionHandlerTest`).
- `mvnw -o test -Dtest=*Analytics*` → 32 tests across the analytics module pass.

## Risks / Notes

- `spring.web.resources.add-mappings=false` disables the default classpath static-resource
  handler. The avatar serving endpoint uses an explicit `WebMvcConfigurer.addResourceHandlers`
  registration (`AvatarStorageConfig`), so the avatar contract is unaffected; the existing
  `ProfileAvatarControllerTest` still passes.
- Gamification work on PR #351 is parked in a stash on `codex/gamification-course-card-polish`;
  that PR can keep iterating without blocking the analytics deploy.
