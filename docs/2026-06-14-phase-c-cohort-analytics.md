# Task Audit - Phase C Cohort / Progress Analytics

## Date
2026-06-14

## Task Summary
Implement Phase C of docs/2026-06-14-advanced-analytics-planning.md: cohort/progress analytics (enrollment-month cohorts, completion funnels, progress/certificate trends) on top of the Phase A/B analytics. Read-only, modular monolith, all RBAC preserved. **No new tables added** — justified below. Android UI added only to existing analytics screens (student, platform); teacher/group cohort UI deferred.

## Snapshot/Materialized Table Decision (risk analysis)
**No materialized or snapshot tables, and no new migration, were added.** Justification:
- Current DB holds only seed data (V3 courses, V9 exam, V18/V19 staff) — the product is pre-launch, so enrollment/progress/exam/certificate row counts are trivial.
- All Phase C queries are single-pass aggregates: `GROUP BY date_trunc('month', ...)` and one funnel query per scope using `count(*) FILTER (WHERE ...)` over a derived table of `EXISTS` flags. They run over already-indexed FK columns (`enrollments.user_id/course_id`, `lesson_progress(user_id,course_id)`, `idx_exam_attempt_passed`, `certificates.user_id`).
- The planning doc gates snapshot tables on aggregation being "measurably too slow." That condition is neither measured nor obvious here, so adding them now would be premature/overengineering and would violate the task constraint.
- **Documented trigger for later:** if enrollment volume grows enough that month-bucketing is slow, the first cheap step is `CREATE INDEX idx_enrollments_enrolled_at` (new Flyway migration), and only then `analytics_daily_snapshot`-style tables. Existing migrations are never edited.

## Files Created
Backend (`com.edulife.analytics`):
- repository/FunnelProjection.java, repository/MonthCountProjection.java
- repository/CohortAnalyticsRepository.java (native projection queries)
- dto/FunnelDto.java, dto/MonthCountDto.java, dto/StudentProgressTrendDto.java, dto/TeacherCohortAnalyticsDto.java, dto/GroupCohortAnalyticsDto.java, dto/PlatformCohortAnalyticsDto.java
- service/CohortAnalyticsService.java
- controller/CohortAnalyticsController.java
- test: CohortAnalyticsServiceTest.java, CohortAnalyticsControllerTest.java

Android (`features/analytics`):
- model/Funnel.java, model/MonthCount.java, model/StudentProgressTrend.java, model/PlatformCohortAnalytics.java, model/StudentTrendUiState.java, model/PlatformCohortUiState.java
- ui/AnalyticsRows.java (shared row-inflation helper)

## Files Modified
Backend: none of the Phase A files changed (Phase C is additive).
Android:
- core/network/ApiService.java — added `analytics/me/progress-trend`, `analytics/platform/cohorts`.
- features/analytics/data/AnalyticsRepository.java — two new load methods + callbacks.
- features/analytics/viewmodel/StudentAnalyticsViewModel.java — `loadTrend()` + trend LiveData.
- features/analytics/viewmodel/PlatformAnalyticsViewModel.java — `loadCohorts()` + cohort LiveData.
- features/analytics/ui/StudentAnalyticsFragment.java — monthly trend section.
- features/analytics/ui/PlatformAnalyticsFragment.java — funnel + cohorts + cert-trend sections.
- res/layout/fragment_student_analytics.xml, res/layout/fragment_platform_analytics.xml — cohort sections.
- res/values/strings.xml — Phase C strings.

## Endpoints (read-only)
- `GET /analytics/me/progress-trend` — student's own lessons-completed-by-month. Any authenticated user; scoped to resolved user id.
- `GET /analytics/teacher/cohorts` — TEACHER/ADMIN; funnel + enrollment cohorts across the caller's **owned** courses.
- `GET /analytics/group/{groupId}/cohorts` — GROUP_ADMIN/ADMIN; funnel scoped to enrollments where course ∈ group AND learner ∈ group members; service enforces group ownership (creator or platform admin). **New group-admin scope (Phase B had deferred group analytics).**
- `GET /analytics/platform/cohorts` — ADMIN; global funnel + enrollment cohorts + certificate trend.

### Cohort calculations
- **Funnel** counts distinct (user, course) enrollment grains reaching each stage: enrolled → started (≥1 lesson) → completed (all lessons via `course_progress total>0 and completed>=total`) → passed (a passing `exam_attempts` row for the course's exam) → certified (a `certificates` row). Stages are monotonically non-increasing. One SQL pass via `count(*) FILTER`.
- **Enrollment cohorts** group active enrollments by `to_char(date_trunc('month', enrolled_at),'YYYY-MM')`.
- **Certificate trend** groups certificates by `issued_at` month.
- **Student trend** groups the caller's `lesson_progress.completed_at` by month.

## Architecture Compliance
- Modular monolith preserved: Phase C lives in the existing `analytics/` module (own controller/service/repository/dto). No microservices, events/Kafka, warehouse, AI/predictions, payment or social analytics.
- Read-only: all service methods `@Transactional(readOnly = true)`; repository exposes interface projections, never entities. No writes, no snapshots.
- Cross-module repository reuse (courses/groups) follows the AdminMetrics/Phase A precedent.
- Android: feature-first MVVM, Fragment → ViewModel → Repository → ApiService; cohort sections load on independent LiveData so one section's failure does not blank the other. 401/token-refresh inherited from the global OkHttp `FirebaseTokenAuthenticator`.

## RBAC / Security
- Every endpoint validates Firebase token + email_verified (global filter) and gates role via `@PreAuthorize`; the service derives scope from the resolved user only — never from client `userId`/`teacherId`/`groupId`.
- Students: own id only. Teachers: own course ids only. Group admins: own group only (ownership re-checked in the service even though groupId is in the path; a valid-but-foreign groupId returns 403). Platform admins: global.
- No `firebase_uid` and no exam correct answers are read or exposed.
- Empty scopes (teacher with no courses, empty group) short-circuit to a zero funnel without hitting the DB.

## Code Comments Added
- Repository: header explaining read-only/no-entity contract, why scope comes from the service, and the `count(*) FILTER` single-pass funnel technique; per-query scope notes.
- Service: comments on each scope derivation, the group ownership check (mirrors GroupService), divide-by-zero/empty-scope guards, and trusted-identity resolution.
- Controller: comments on the global token enforcement + two-layer (role then ownership) security.
- Android: comments on independent section loading, runtime row inflation, and server-side scoping.

## Validation / Testing
- Backend: `CohortAnalyticsServiceTest` (7) — scope derivation for student/teacher/group/platform, group ownership 403 for non-owner, platform-admin cross-group read, empty-scope short-circuit, unsynced-user 401. `CohortAnalyticsControllerTest` (12) — every endpoint: no-token 401, role gating 403, allowed 200.
- Full backend suite: **`Tests run: 157, Failures: 0, Errors: 0` — BUILD SUCCESS** (includes BackendApplicationTests context load with the new repository bean).
- Android: `./gradlew :app:compileDebugJavaWithJavac :app:testDebugUnitTest` — **BUILD SUCCESSFUL** (passing `-Dorg.gradle.java.home` to a full JDK; the default toolchain resolved a jlink-less JRE — environment-only).
- Native Postgres SQL (date_trunc, FILTER, to_char) is validated at runtime, consistent with the existing native FTS query (V13/CourseRepository); unit tests mock the repository per the established Phase A/B pattern.

## Deferred (documented, not built)
- **Teacher cohort Android UI** and **Group cohort Android UI**: backend endpoints + tests are delivered, but no UI was added. The teacher analytics screen is a non-scroll RecyclerView layout (adding sections risks scroll regressions) and there is no group-admin analytics screen/navigation. Per "add Android UI only for views already supported by navigation and roles" + "smallest implementation," these are left backend-ready. They can reuse `AnalyticsRows` + the established state pattern when their screens are added.

## Risks / Notes
- Pass/funnel figures use enrollment-grain distinct counts; if a per-learner-across-courses view is later needed it is a new query, not a schema change.
- Platform "all data" queries are unfiltered global scans — fine at MVP volume; the index/snapshot path above is the documented escalation.
- No new navigation flows were introduced; all Android additions extend existing screens.
