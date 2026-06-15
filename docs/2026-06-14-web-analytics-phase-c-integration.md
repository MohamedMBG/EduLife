# Web Analytics — Phase C Integration

## Goal
Implement Phase C of advanced analytics (cohort / progress analytics) on the website.

Phase C per `docs/2026-06-14-advanced-analytics-planning.md` §12: cohort grouping, learner-loop
funnel, and deeper trend analysis — read-only, no new tables, RBAC-scoped server-side.

## What Changed
The full Phase A + Phase C web UI already existed, finished and build-verified, on branch
`codex/analytics-phases-a-c` but was never merged onto `main` or the current working branch
(`codex/android-responsive-icon-polish`). Per the user's decision, the finished work was
**integrated** rather than rewritten.

Brought the 7 web files from `codex/analytics-phases-a-c` onto the current branch:

- `/analytics` route — role-aware learner / teacher / group-admin dashboards:
  - **Learner:** own summary metrics + lesson-completion trend by month (Phase C trend).
  - **Teacher:** owned-course summary, learner funnel (enrolled → started → completed → passed →
    certified), enrollment cohorts by month, per-course performance table (Phase C cohorts/funnel).
  - **Group admin:** owned-group cards each with a group-scoped funnel (Phase C group cohorts).
- `/admin/analytics` route — platform console: global counts, global learner funnel, enrollment
  cohort trend, certificate-issuance trend (Phase C platform cohorts).
- Typed API contracts + client functions for the analytics endpoints.
- Nav entries added to both `AppShell` and `AdminShell`; generated route tree updated.

The four shared files (`client.ts`, `types.ts`, `AppShell.tsx`, `AdminShell.tsx`) were byte-identical
to the shared base `bc8e002` on the current branch, so taking the branch versions was a clean,
conflict-free overlay (verified with `git diff bc8e002 HEAD`).

## Files Touched
- guided-journey-lab/src/routes/analytics.tsx (new)
- guided-journey-lab/src/routes/admin.analytics.tsx (new)
- guided-journey-lab/src/lib/api/client.ts
- guided-journey-lab/src/lib/api/types.ts
- guided-journey-lab/src/components/app/AppShell.tsx
- guided-journey-lab/src/components/app/AdminShell.tsx
- guided-journey-lab/src/routeTree.gen.ts

## Backend Endpoints Used
- `GET /api/v1/analytics/me/summary` (Phase A)
- `GET /api/v1/analytics/me/progress-trend` (Phase C)
- `GET /api/v1/analytics/teacher/courses` (Phase A)
- `GET /api/v1/analytics/teacher/cohorts` (Phase C)
- `GET /api/v1/analytics/group/{groupId}/cohorts` (Phase C)
- `GET /api/v1/analytics/platform` (Phase A)
- `GET /api/v1/analytics/platform/cohorts` (Phase C)

All scope is resolved server-side from the Firebase session. The web client sends no trusted
`userId` / `teacherId` / `role`; `groupId` is passed only as a resource id and the backend re-checks
group ownership.

## Design Tokens Used
No new tokens. Reuses existing: `bg-surface-elevated`, `border-border`, `bg-primary`,
`text-muted-foreground`, `text-foreground`, `shadow-soft`, `text-display`, `rounded-3xl`,
`bg-destructive/5`. No hardcoded colors.

## States Handled
- [x] Loading — per-query loading panels
- [x] Error — error panel + retry per query
- [x] Empty — "No data yet" / empty-text rows for trends, cohorts, course table, groups
- [x] Success — dashboard content

## Dark Mode Tested
N/A (token-based only; all colors are theme variables, both modes covered by tokens).

## TypeScript Errors
None. `tsc --noEmit` → exit 0. `vite build` → exit 0 (`analytics` 21.0 kB + `admin.analytics`
10.7 kB chunks emitted).

## Backend Integration (follow-up — fixes runtime fetch error)
The web routes 404'd because the running backend lacked the analytics endpoints. Integrated the
backend Phase A + Phase C module from `codex/analytics-phases-a-c` (fully additive, **no Flyway
migration** — no new tables):
- New package `backend/src/main/java/com/edulife/analytics/**` (controllers, services, repositories,
  projections, DTOs).
- Additive query methods on existing repos: `CertificateRepository`, `EnrollmentRepository`,
  `ExamAttemptRepository`, `CourseProgressRepository` (all were byte-identical to base, clean overlay).
- Tests: `AnalyticsServiceTest`, `AnalyticsControllerTest`, `CohortAnalyticsServiceTest`,
  `CohortAnalyticsControllerTest`.
- No SecurityConfig change (existing chain gates the new endpoints; `@PreAuthorize` handles roles).
- Verified: `./mvnw -o -q compile` → exit 0.

**Required to clear the error:** rebuild + restart the backend so the new endpoints are served. If
the web app points at a deployed backend (`VITE_API_BASE_URL`), deploy the backend too.

## Risks / Notes
- Android Phase C UI (learner trend, platform cohorts) also lives on the same branch; teacher/group
  Android cohort UI was deferred there (backend-ready). Not part of this web task.
- No new tables; all metrics are on-read aggregates. Snapshot/materialized tables remain the
  documented escalation only if aggregation is measured too slow at scale.
- This change mixes web files onto an Android-named branch. If keeping deployables separate matters,
  cherry-pick these 7 files onto a dedicated web branch before the subtree push to the `web` remote.
