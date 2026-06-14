# Task Audit - Phase A Analytics Implementation

## Date
2026-06-14

## Task Summary
Implement Phase A of docs/2026-06-14-advanced-analytics-planning.md: a backend-only, read-only analytics module inside the existing Spring Boot modular monolith. Three audiences — student own summary, teacher owned-course analytics, platform admin global counts — derived from existing MVP tables. No new tables, no events/Kafka/microservices/warehouse/AI/predictions.

## Files Created
- backend/src/main/java/com/edulife/analytics/package-info.java
- backend/src/main/java/com/edulife/analytics/dto/StudentAnalyticsSummaryDto.java
- backend/src/main/java/com/edulife/analytics/dto/TeacherCourseAnalyticsDto.java
- backend/src/main/java/com/edulife/analytics/dto/TeacherAnalyticsDto.java
- backend/src/main/java/com/edulife/analytics/dto/PlatformAnalyticsDto.java
- backend/src/main/java/com/edulife/analytics/service/AnalyticsService.java
- backend/src/main/java/com/edulife/analytics/controller/AnalyticsController.java
- backend/src/test/java/com/edulife/analytics/AnalyticsServiceTest.java
- backend/src/test/java/com/edulife/analytics/AnalyticsControllerTest.java

## Files Modified
- backend/src/main/java/com/edulife/enrollments/repository/EnrollmentRepository.java — added `countByCourseIdAndStatus`
- backend/src/main/java/com/edulife/certificates/repository/CertificateRepository.java — added `countByCourseId`
- backend/src/main/java/com/edulife/exams/repository/ExamAttemptRepository.java — added `countByUserId`, `countByUserIdAndPassedTrue`, `countByExamId`, `countByExamIdAndPassedTrue`, `countByPassedTrue`
- backend/src/main/java/com/edulife/progress/repository/CourseProgressRepository.java — added `countByCourseId`, `countCompletedByCourseId` (`@Query`)

## What Was Done

### Endpoints (all under /api/v1/analytics, read-only)
- `GET /me/summary` — student's own summary: active enrollments, lessons completed, exam attempts, exams passed, certificates earned. Any authenticated user; scoped to the resolved user id (no role gate needed — there is no parameter to request another user).
- `GET /teacher/courses` — `@PreAuthorize hasAnyRole('TEACHER','ADMIN')`. Per owned course: active enrollments, learners-with-progress, learners-completed, completion rate %, exam attempts, exams passed, attempt-based pass rate %, certificates issued. Scoped to courses authored by the resolved user (`findAllByCreatedByUserId`).
- `GET /platform` — `@PreAuthorize hasRole('ADMIN')`. Global counts: users by role, courses by status, active enrollments, total/passed exam attempts, total certificates.

### Source of metrics (existing tables only)
users, courses, enrollments, course_progress, exams, exam_attempts, certificates. No schema change — only read-only count/aggregate repository methods were added. The single `@Query` (`countCompletedByCourseId`) guards `totalLessons > 0` so an empty course is not counted as completed.

### Aggregation decisions
- Rates are percentages rounded to one decimal; zero denominator returns 0.0 (no divide-by-zero).
- `passRatePercent` is attempt-based (passed attempts / total attempts) and is documented as such in the DTO Javadoc; per-learner dedup was intentionally deferred (Phase A simplicity).
- A course with no exam yet returns zero attempts/passed instead of failing the whole dashboard.

## Architecture Compliance
- Modular monolith preserved: one new `analytics/` domain module with controller/service/dto, owning no entities and writing no data. No microservices, no event-driven/Kafka, no warehouse, no AI/predictions (matches AGENTS.md §3/§20 and the planning doc's Phase A scope).
- Thin controller; all aggregation and scoping logic in the service; repositories only run read queries. DTOs (records) used for all output — no JPA entity is exposed.
- Cross-module repository reuse follows the existing precedent in `AdminMetricsService`.
- Learner flow untouched; no MVP sprint behavior changed.

## Code Comments Added
- `package-info.java` documents the module's read-only, no-entity, server-side-scoping contract.
- DTOs note scoping guarantees and the attempt-based pass-rate definition.
- `AnalyticsService`: comments on ownership scoping (resolved id only, never client-supplied), missing-exam handling, divide-by-zero guard, and the trusted-identity resolution path.
- `AnalyticsController`: comments on the global Firebase/email_verified enforcement, the two-layer role gate, and why `/me/summary` needs no role gate.
- Repository additions each carry a one-line read-only-analytics purpose comment; the `@Query` notes the `totalLessons > 0` guard.

## Security / Ownership
- Firebase token validation and `email_verified` are enforced globally by `FirebaseTokenFilter`; role authorities come from the trusted users table (`ROLE_<role>`), never the client token claims.
- Role gating via `@PreAuthorize` (`@EnableMethodSecurity` already on `SecurityConfig`).
- Scope is always derived from the server-resolved internal user. No endpoint accepts `userId`, `teacherId`, `groupId`, or `role` from the client for scoping.
- `firebase_uid` and exam correct answers are never read or serialized by this module.

## Validation / Testing
- `AnalyticsServiceTest` (Mockito, 4 tests): student summary keyed only by resolved id; teacher analytics queries only own-authored courses with correct rate math; empty-courses case; missing/unsynced user → 401.
- `AnalyticsControllerTest` (`@WebMvcTest`, 9 tests): no-token → 401 on all three endpoints; learner/teacher → 403 on `/platform`; learner → 403 on `/teacher/courses`; admin → 200 `/platform`; teacher → 200 `/teacher/courses`; learner → 200 `/me/summary`.
- Result: `Tests run: 13, Failures: 0, Errors: 0` — BUILD SUCCESS. Whole backend `main` compiled (repository edits to four modules included).

## Risks / Notes
- Attempt-based pass rate is coarser than per-learner pass rate; if a per-learner figure is later required, add a distinct-user count query (still no schema change).
- All aggregation is computed on read. Fine at MVP data volume; if it becomes slow at scale, the planning doc's Phase C snapshot tables (new Flyway migration) are the path — not added now.
- No Android/web work in this task (Phase A is backend-only per scope).
- No new endpoints are wired into any client yet; they are additive and do not affect existing routes.
