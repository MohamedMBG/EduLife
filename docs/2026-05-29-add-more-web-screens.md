# Task Audit - Add More Web Screens

## Date
2026-05-29

## Task Summary
Expanded the EduLife website with additional real backend-backed learner screens: certificates, enrolled course detail, and lesson view. Updated existing dashboard and courses screens to link into those routes.

## Files Created
- `guided-journey-lab/src/routes/certificates.tsx`
- `guided-journey-lab/src/routes/courses.$courseId.tsx`
- `guided-journey-lab/src/routes/learn.$courseId.$lessonId.tsx`
- `docs/2026-05-29-add-more-web-screens.md`

## Files Modified
- `guided-journey-lab/src/components/app/AppShell.tsx`
- `guided-journey-lab/src/lib/api/client.ts`
- `guided-journey-lab/src/lib/api/types.ts`
- `guided-journey-lab/src/routes/courses.tsx`
- `guided-journey-lab/src/routes/dashboard.tsx`
- `guided-journey-lab/src/routeTree.gen.ts`

## What Was Done
Added a new `/certificates` route that loads certificate records from `GET /api/v1/certificates` and presents them as a learner-facing certificate history.

Added a new `/courses/$courseId` route that loads a real course outline from `GET /api/v1/courses/{courseId}`, checks the learner's enrollments, loads progress when enrolled, and exposes lesson entry points section by section.

Added a new `/learn/$courseId/$lessonId` route that loads lesson content from `GET /api/v1/courses/{courseId}/lessons/{lessonId}`, shows course progress, and marks the lesson complete through `POST /api/v1/courses/{courseId}/lessons/{lessonId}/complete`.

Extended the shared website API layer with DTOs and client helpers for:
- `LessonDetail`
- `Certificate`
- `getLessonDetail`
- `markLessonComplete`
- `listMyCertificates`

Updated the shared learner shell navigation to include a Certificates entry so the new route is reachable as a first-class screen.

Updated existing screens so the new routes are connected:
- `dashboard.tsx` now links active courses to the course detail screen and surfaces certificates navigation
- `courses.tsx` now links each enrolled course card to its course detail screen

Ran a production build so TanStack regenerated `routeTree.gen.ts` for the expanded route set.

## Architecture Compliance
The work stays inside the website deployable under `guided-journey-lab/src/routes`, `guided-journey-lab/src/lib/api`, and `guided-journey-lab/src/components/app`, which matches the repository web structure guidance. The new screens consume the existing backend contracts instead of inventing separate website-only data behavior, which keeps Android and web aligned to one backend API.

## Code Comments Added
Added focused comments in the new lesson flow for:
- why lesson completion invalidates multiple learner queries together
- why the course detail and lesson views depend on backend progress state

Comments explain the state and backend consistency reasoning rather than restating obvious JSX.

## Validation / Testing
Validated by running:

- `cmd /c npm run build`

The website build completed successfully for both client and SSR outputs. The existing CSS `@import` ordering warning remained, but it did not block the build and was not introduced by this task.

## Risks / Notes
The new screens assume the existing backend endpoints stay available and protected by the Firebase session already added in the previous website-backend integration task.

Certificates currently display backend certificate metadata only. There is no certificate PDF download route exposed in the current web app task because the backend controller inspected for this task only provides listing.

The `level` screen remains a separate, older screen and is still not aligned to the backend learner-flow data. This task focused on adding real learner-flow screens rather than rewriting the pre-existing gamification surface.
