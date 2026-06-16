# EduLife Analytics, Gamification, and AI Workflows

## Workflow: Student, Teacher, Group, and Platform Analytics

Role:
Learner, Teacher, Group Admin, Admin

Platform:
Android, Web, Backend, Database, Tests

Status:
Mostly fully working

Entry point:

- Android:
  - `StudentAnalyticsFragment`
  - `TeacherAnalyticsFragment`
  - `PlatformAnalyticsFragment`
- Web:
  - `/analytics`
  - `/admin/analytics`
- Backend:
  - `/api/v1/analytics/me/summary`
  - `/api/v1/analytics/me/progress-trend`
  - `/api/v1/analytics/teacher/courses`
  - `/api/v1/analytics/teacher/cohorts`
  - `/api/v1/analytics/group/{groupId}/cohorts`
  - `/api/v1/analytics/platform`
  - `/api/v1/analytics/platform/cohorts`

End result:

- Each role can view server-scoped learning analytics without sending raw user-scoping identifiers for self scope.

Step-by-step:

1. Learner analytics summarize enrollments, lessons, attempts, passes, and certificates.
2. Teacher analytics aggregate only owned-course performance.
3. Group analytics aggregate only owned-group cohorts.
4. Platform analytics expose global summary and month/funnel trends to admins.

Backend code:

- file path: `backend/src/main/java/com/edulife/analytics/controller/AnalyticsController.java`
- file path: `backend/src/main/java/com/edulife/analytics/controller/CohortAnalyticsController.java`
- file path: `backend/src/main/java/com/edulife/analytics/service/AnalyticsService.java`
- file path: `backend/src/main/java/com/edulife/analytics/service/CohortAnalyticsService.java`

Android code:

- file path: `app/src/main/java/com/baghdad/edulife/features/analytics/ui/StudentAnalyticsFragment.java`
- file path: `app/src/main/java/com/baghdad/edulife/features/analytics/ui/TeacherAnalyticsFragment.java`
- file path: `app/src/main/java/com/baghdad/edulife/features/analytics/ui/PlatformAnalyticsFragment.java`

Web code:

- file path: `guided-journey-lab/src/routes/analytics.tsx`
- file path: `guided-journey-lab/src/routes/admin.analytics.tsx`

Database:

- tables: derived reads over `enrollments`, `lesson_progress`, `course_progress`, `exam_attempts`, `certificates`, `groups`, `group_members`, `group_courses`

API contract:

- DTOs:
  - `StudentAnalyticsSummaryDto`
  - `StudentProgressTrendDto`
  - `TeacherAnalyticsDto`
  - `TeacherCourseAnalyticsDto`
  - `TeacherCohortAnalyticsDto`
  - `GroupCohortAnalyticsDto`
  - `PlatformAnalyticsDto`
  - `PlatformCohortAnalyticsDto`
  - `FunnelDto`
  - `MonthCountDto`

Security:

- authentication: required
- authorization:
  - learner self endpoints need only auth
  - teacher/group/platform endpoints add `@PreAuthorize`
- ownership checks:
  - teacher scope derives from `created_by_user_id`
  - group scope derives from owned group authorization
  - platform scope is admin-only

Problems found:

- Android has student, teacher, and platform analytics, but I did not find a dedicated Android group analytics screen equivalent to the web group analytics panel.

Recommended next fix:

- if group analytics is a real mobile requirement, add Android parity against the existing backend endpoint

## Workflow: Backend Gamification State, Leaderboard, and Badges

Role:
Learner and any verified user for leaderboard

Platform:
Android, Backend, Database, Tests

Status:
Backend and Android full; web partial

Entry point:

- Android: `GamificationFragment`
- Backend:
  - `GET /api/v1/gamification/me`
  - `GET /api/v1/gamification/leaderboard`
  - `GET /api/v1/gamification/badges`

End result:

- Backend awards XP for learning events and exposes stable level/streak/badge state.

Step-by-step:

1. Enrollment, lesson completion, course completion, exam pass, certificate issue, and daily login emit XP events.
2. Backend writes an append-only XP event ledger with `dedup_key`.
3. Backend updates `user_gamification_state` transactionally.
4. Android reads the state and renders level, streak, and earned badges.

Backend code:

- file path: `backend/src/main/java/com/edulife/gamification/controller/GamificationController.java`
- file path: `backend/src/main/java/com/edulife/gamification/service/GamificationService.java`

Android code:

- file path: `app/src/main/java/com/baghdad/edulife/features/gamification/ui/GamificationFragment.java`
- file path: `app/src/main/java/com/baghdad/edulife/features/gamification/viewmodel/GamificationViewModel.java`

Web code:

- no direct use of backend gamification endpoints found in the main web app

Database:

- tables:
  - `user_gamification_state`
  - `gamification_xp_events`
  - `user_badges`
- migration files: `V22__gamification.sql`

API contract:

- response DTOs:
  - `GamificationStateDto`
  - `LeaderboardEntryDto`
  - `BadgeDto`

Security:

- authentication: required
- authorization:
  - `/me` and `/badges` are owner-scoped
  - `/leaderboard` is shared to authenticated users
- ownership checks:
  - XP is emitted server-side only
  - dedup keys prevent double-awarding

Problems found:

- Web `level` does not call these endpoints and instead re-derives XP locally from other data.

Recommended next fix:

- switch the web level page to backend gamification state so both clients show the same truth

## Workflow: Web Level Progress Page

Role:
Learner

Platform:
Web only

Status:
Local/derived

Entry point:

- `/level`

End result:

- Web user sees a polished level/progress page, but it is not backed by `/api/v1/gamification/*`.

Step-by-step:

1. Web loads profile, enrollments, progress, and certificates.
2. It derives XP, level thresholds, badges, streaks, and activity in the route itself.
3. The page renders a pseudo-gamification dashboard.

Web code:

- file path: `guided-journey-lab/src/routes/level.tsx`

Problems found:

- this can drift from backend/Android gamification rules
- badge IDs are manually mirrored and could diverge

Recommended next fix:

- convert this route to use backend gamification endpoints as the single source of truth

## Workflow: Study Planner

Role:
Learner

Platform:
Android, Web

Status:
Local only

Entry point:

- Android: `PlannerFragment`
- Web: `/planner`

End result:

- Learner can track goals, study days, hours, and tasks locally on the device/browser.

Step-by-step:

1. Planner loads local state from SharedPreferences or `localStorage`.
2. It optionally reads current enrollments to show focus-course labels.
3. Task changes, study day toggles, and hour logs are saved locally.

Android code:

- file path: `app/src/main/java/com/baghdad/edulife/features/courses/ui/PlannerFragment.java`
- file path: `app/src/main/java/com/baghdad/edulife/features/courses/viewmodel/PlannerViewModel.java`

Web code:

- file path: `guided-journey-lab/src/routes/planner.tsx`

Database:

- none

Problems found:

- no backend persistence
- no cross-device sync
- no role-aware study planning data model

Recommended next fix:

- explicitly decide whether planner is intentionally personal/local or should become a persisted feature

## Workflow: Career Advisor / AI Advisor

Role:
Learner

Platform:
Android, Web, Backend, Database, Tests

Status:
Partial

Entry point:

- Android: `AdvisorFragment`
- Web: `/advisor`
- Backend: `POST /api/v1/advisor/recommend`

End result:

- Learner enters a free-form goal and receives recommended courses plus reasoning.

Step-by-step:

1. Client loads catalog/enrollments to present and contextualize results.
2. Client sends `goal` to advisor endpoint.
3. Backend builds a filtered published-course context and calls the configured provider.
4. Backend logs the response in `advisor_log`.
5. Web can fall back to a local rule-based matcher when AI is disabled or the API fails.

Backend code:

- file path: `backend/src/main/java/com/edulife/advisor/controller/AdvisorController.java`
- file path: `backend/src/main/java/com/edulife/advisor/service/AdvisorService.java`
- file path: `backend/src/main/java/com/edulife/advisor/service/CourseContextBuilder.java`

Android code:

- file path: `app/src/main/java/com/baghdad/edulife/features/advisor/ui/AdvisorFragment.java`
- note: older dead duplicate exists at `features/courses/ui/CareerAdvisorFragment.java`

Web code:

- file path: `guided-journey-lab/src/routes/advisor.tsx`
- file path: `guided-journey-lab/src/lib/career/advisor.ts`

Database:

- tables: `advisor_log`
- migration files: `V23__advisor_log.sql`

API contract:

- request DTO: `AdvisorRequest`
  - `goal`
- response DTO: `AdvisorResponse`
  - `message`
  - `recommendations[] { courseId, reason, score }`

Security:

- authentication: required
- authorization: any verified user
- ownership checks: backend only exposes published-course context

Problems found:

- provider defaults can be stub/fallback-oriented depending on environment
- web fallback can hide backend failures in some paths

Recommended next fix:

- make provider mode explicit in environment and admin-facing ops docs so behavior is predictable

## Workflow: Public Teacher Profile, Notifications, and Discussions

Role:
Planned / not implemented

Platform:
None active

Status:
Documented but not implemented

Entry point:

- none found in backend controllers, Android navigation, or web routes

End result:

- no current runtime workflow

Evidence:

- no `discussions` module
- no `notifications` controller/feature
- no public teacher profile route or controller

Problems found:

- docs mention these as future or deferred concepts, but there is no executable code path yet

Recommended next fix:

- keep them out of MVP parity work until the current learner/staff gaps above are resolved

