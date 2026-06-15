# Task Audit - Phase B Analytics Dashboards (Android)

## Date
2026-06-14

## Task Summary
Implement Phase B of docs/2026-06-14-advanced-analytics-planning.md: Android feature-first MVVM analytics dashboards consuming the existing Phase A read-only backend. Role-specific screens for Student, Teacher, and Platform Admin. Group Admin deferred (no backend endpoint exists in Phase A). All scope decisions stay server-side; Android renders whatever the backend returns.

## Files Created
Android feature `features/analytics/`:
- model/StudentAnalyticsSummary.java
- model/TeacherCourseAnalytics.java
- model/TeacherAnalytics.java
- model/PlatformAnalytics.java
- model/StudentAnalyticsUiState.java
- model/TeacherAnalyticsUiState.java
- model/PlatformAnalyticsUiState.java
- model/AnalyticsFormat.java  (pure, unit-tested)
- data/AnalyticsRepository.java
- viewmodel/StudentAnalyticsViewModel.java
- viewmodel/TeacherAnalyticsViewModel.java
- viewmodel/PlatformAnalyticsViewModel.java
- ui/StudentAnalyticsFragment.java
- ui/TeacherAnalyticsFragment.java
- ui/TeacherCourseAnalyticsAdapter.java
- ui/PlatformAnalyticsFragment.java

Layouts + drawable:
- res/layout/fragment_student_analytics.xml
- res/layout/fragment_teacher_analytics.xml
- res/layout/fragment_platform_analytics.xml
- res/layout/item_teacher_course_analytics.xml
- res/layout/item_analytics_stat_row.xml
- res/layout/item_analytics_tile.xml
- res/drawable/ic_analytics.xml

Test:
- app/src/test/java/com/baghdad/edulife/features/analytics/AnalyticsFormatTest.java

## Files Modified
- core/network/ApiService.java — added 3 GET endpoints (analytics/me/summary, analytics/teacher/courses, analytics/platform) + imports.
- res/navigation/nav_graph.xml — 3 new destinations + 3 actions (profile→student, teacherDashboard→teacher, adminDashboard→platform).
- res/values/strings.xml — analytics dashboard strings.
- features/profile/ui/ProfileFragment.java + res/layout/fragment_profile.xml — "My Learning Stats" CTA row (added xmlns:app for app:tint).
- features/teacher/ui/TeacherDashboardFragment.java + res/layout/fragment_teacher_dashboard.xml — "Course Analytics" CTA card.
- features/admin/ui/AdminDashboardFragment.java + res/layout/fragment_admin_dashboard.xml — "Platform Analytics" CTA card.

## What Was Done

### Screens (all four states each)
- **Student** (`StudentAnalyticsFragment`) — own active courses, lessons completed, exam attempts, exams passed, certificates. Reachable from Profile. Empty folds into success (all-zero summary renders as zeros).
- **Teacher** (`TeacherAnalyticsFragment` + adapter) — per owned-course card: enrolled, completion %, pass rate %, certificates, passed/total attempts. Reachable from Teacher dashboard. Distinct empty state when the teacher owns no courses.
- **Platform Admin** (`PlatformAnalyticsFragment`) — users by role, published/draft courses, active enrollments, exam attempts/passed, certificates. Reachable from Admin dashboard (admin navigation already existed, so UI was wired rather than deferred). Adds exam attempt/pass signal not present in the existing /admin/metrics screen.

### State handling
Each screen renders exactly one of loading / error (with retry) / empty / success via an immutable `*UiState` and a single `render()` switch, matching the existing Admin/Teacher dashboard pattern. Retry re-enters loading then refetches.

### Networking / auth
`AnalyticsRepository` uses the shared `ApiClient` Retrofit instance. The Firebase Bearer token is injected by `FirebaseAuthInterceptor` and a single 401 refresh-and-retry is performed globally by `FirebaseTokenAuthenticator` at the OkHttp layer — so the new calls inherit the established token-refresh behavior with no extra code. Persistent 401/403 surfaces as the error state.

### Group Admin — deferred (documented)
Phase A backend exposes no group-scoped analytics endpoint. Per task instructions, the Group Admin dashboard is **not** built. When a backend `analytics/group/...` endpoint is added (planning doc Phase B "should-have"), an analogous screen can reuse this feature's pattern. No placeholder UI was added to avoid a fake-complete screen.

## Architecture Compliance
- Feature-first MVVM under `features/analytics/{model,data,viewmodel,ui}`; flow is Fragment → ViewModel → Repository → ApiService → backend. No API calls in fragments, no business logic in UI.
- Java + XML only; Retrofit/OkHttp; manual DI (repository instantiated in ViewModel) — consistent with the rest of the app.
- **Android never decides access scope.** Endpoints are scoped server-side (Phase A); the client sends no userId/teacherId/role and renders the response verbatim. Platform screen relies on the server's ADMIN-only 403 if reached by a non-admin.
- No new analytics tables, no Kafka/events/microservices/warehouse/AI, no third-party analytics SDK.
- Colors/dimens via existing theme tokens (brand_*/teacher_*/admin_*); no hardcoded colors. Reused existing card drawables.

## Code Comments Added
- ApiService: block comment on analytics endpoints noting server-side scoping + global token/401 handling.
- AnalyticsRepository: class comment on the auth/retry inheritance and error surfacing; per-method scope notes.
- ViewModels: comments on the loading→success/error transitions and postValue usage.
- Fragments: comments on the four-state rendering, why student/platform have no separate empty state, scoped-include findViewById, and that scope is server-enforced.
- Adapter: comment that it is display-only and never computes/filters analytics.
- Reusable layouts: comments explaining the included label/value rows resolve per-instance via scoped findViewById.
- CTA wiring in Profile/Teacher/Admin fragments: comments on the entry point and server-side scoping.

## Validation / Testing
- `./gradlew :app:compileDebugJavaWithJavac :app:testDebugUnitTest --tests "*AnalyticsFormatTest"` → **BUILD SUCCESSFUL**. (Required passing `-Dorg.gradle.java.home` to a full JDK because the default toolchain resolved a jlink-less JRE — environment-only, not a code issue.)
- `AnalyticsFormatTest` covers percent formatting (locale-stable one-decimal), count clamping, and passed/attempts dashing — the host-JVM, Android-free logic. The app has no ViewModel/instrumentation test harness, so deeper UI tests were out of scope per "where the project already has test patterns".
- Resource, navigation, and R-file tasks compiled cleanly, validating the new layouts, ids, strings, and nav graph.

## Risks / Notes
- Group Admin analytics intentionally absent pending a backend endpoint — the only deferred role.
- The Platform screen overlaps partially with the existing Admin metrics screen but adds exam attempt/pass data; both consume distinct endpoints and coexist.
- No ViewModel/Repository unit tests because the project lacks a Robolectric/MockWebServer setup; adding one is a separate infra task.
- Pass rate shown is attempt-based (backend-defined); the UI displays it as-is.
- IDE reported "not on classpath" warnings during editing — benign Gradle-sync noise; the Gradle build confirms correctness.
