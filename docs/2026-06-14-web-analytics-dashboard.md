# Task Audit - Web Analytics Dashboard

## Date
2026-06-14

## Task Summary
Verified the existing Advanced Analytics work and checked whether a web version existed. The previous work implemented backend analytics plus Android dashboards, but not the web UI. Added a web analytics version to `guided-journey-lab` using the existing Phase A and Phase C backend endpoints.

## Files Created
- guided-journey-lab/src/routes/analytics.tsx
- guided-journey-lab/src/routes/admin.analytics.tsx
- docs/2026-06-14-web-analytics-dashboard.md

## Files Modified
- guided-journey-lab/src/lib/api/types.ts
- guided-journey-lab/src/lib/api/client.ts
- guided-journey-lab/src/components/app/AppShell.tsx
- guided-journey-lab/src/components/app/AdminShell.tsx
- guided-journey-lab/src/routeTree.gen.ts

## What Was Done
Confirmed from the Phase B and Phase C audit files that Android analytics screens were built and that the web UI was not implemented. Added typed API contracts and client functions for the analytics endpoints:

- `GET /api/v1/analytics/me/summary`
- `GET /api/v1/analytics/me/progress-trend`
- `GET /api/v1/analytics/teacher/courses`
- `GET /api/v1/analytics/teacher/cohorts`
- `GET /api/v1/analytics/group/{groupId}/cohorts`
- `GET /api/v1/analytics/platform`
- `GET /api/v1/analytics/platform/cohorts`

Added `/analytics` for learner, teacher, and group-admin portals:

- Learner: own summary metrics and lesson completion trend.
- Teacher: owned-course performance, owned-course funnel, enrollment cohorts, and course table.
- Group admin: owned groups, member/course totals, and group-scoped funnels.

Added `/admin/analytics` inside the admin console:

- Platform-wide user/course/enrollment/exam/certificate counts.
- Global learner funnel.
- Enrollment cohort trend.
- Certificate issuance trend.

Updated the normal app shell and admin shell navigation so the analytics routes are reachable.

## Architecture Compliance
The web implementation stays in the existing TanStack Start web app and consumes the existing Spring Boot modular-monolith endpoints. No backend or Android files were modified for this task. The web app does not decide analytics scope: it sends no trusted `userId`, `teacherId`, or `role`; backend RBAC and ownership checks remain authoritative. The group-admin route passes `groupId` only as a resource identifier, and the backend still verifies group ownership.

No microservices, Kafka, event pipeline, warehouse, AI, predictions, payments, social features, or third-party analytics SDKs were added.

## Code Comments Added
Added API-client comments explaining that analytics scope is resolved server-side from the authenticated Firebase session and that foreign group IDs are still rejected by backend ownership checks. Added an inline comment in the group analytics route explaining that group ownership is rechecked by the backend.

## Validation / Testing
Ran the web production build:

```text
cmd /c pnpm build
```

Result: build succeeded. The generated output included `analytics` and `admin.analytics` chunks, and `guided-journey-lab/src/routeTree.gen.ts` now contains `/analytics` and `/admin/analytics`.

Initial `pnpm build` through PowerShell failed because script execution is disabled for `pnpm.ps1`; rerunning through `cmd /c pnpm build` solved that. The sandboxed run also hit an access-denied error while Vite/esbuild loaded config, so the successful build was run with approved escalation.

## Risks / Notes
The web analytics UI depends on the Phase A and Phase C backend endpoints being present and deployed with the web app. The pages include loading, error, empty, and success states through React Query state handling. No manual browser screenshot was taken because the production build validated routing, TypeScript, and bundle generation.
