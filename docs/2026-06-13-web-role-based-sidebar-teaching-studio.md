# Web: Role-Based Sidebar + Teaching Studio

## Goal

The side panel must differ per role: learners see the learning flow, teachers/group admins see course management, admins keep their separate console. Teachers previously got the learner sidebar with no way to act on their CMS rights.

## What Changed

- `AppShell` sidebar is now role-aware (role read from the auth session synced via `/api/v1/auth/sync`):
  - LEARNER → "Learner portal": Home, My Courses, Explore, Certificates, Level & Progress (unchanged).
  - TEACHER → "Teacher portal": Teaching Studio, Course Catalog.
  - GROUP_ADMIN → "Group admin portal": same teaching nav (same CMS rights server-side).
  - ADMIN → keeps the separate `AdminShell`; on learner routes the shell labels itself "Admin preview" with the full learner nav.
- New Teaching Studio at `/teach` (TEACHER/GROUP_ADMIN/ADMIN only via new `RequireTeacher` guard):
  - Lists own courses from `GET /api/v1/cms/courses` with DRAFT/PUBLISHED/ARCHIVED badges.
  - Creates draft courses via `POST /api/v1/cms/courses`.
- New course manager at `/teach/$courseId`:
  - Sections: list/create/delete via `/api/v1/cms/courses/{courseId}/sections`.
  - Lessons: list/create/delete via `/api/v1/cms/sections/{sectionId}/lessons` (ARTICLE body vs VIDEO/RESOURCE URL respected).
- `/dashboard` now redirects TEACHER and GROUP_ADMIN to `/teach` (mirrors the existing ADMIN → `/admin/dashboard` redirect).
- API layer: CMS types (`CmsCourse`, `CmsSection`, `CmsLesson`, create requests) and client functions added.
- Pre-existing TypeScript errors fixed: demo `AdminMetrics` stub missing `pendingTeacherRequests`; unguarded `query.data` access in `certificates.index.tsx` and `courses.$courseId.index.tsx`.

## Files Touched

- `guided-journey-lab/src/components/app/AppShell.tsx`
- `guided-journey-lab/src/lib/auth/auth-context.tsx` (new `RequireTeacher`)
- `guided-journey-lab/src/lib/api/types.ts`
- `guided-journey-lab/src/lib/api/client.ts`
- `guided-journey-lab/src/routes/teach.tsx` (new layout)
- `guided-journey-lab/src/routes/teach.index.tsx` (new)
- `guided-journey-lab/src/routes/teach.$courseId.tsx` (new)
- `guided-journey-lab/src/routes/dashboard.tsx` (teacher redirect)
- `guided-journey-lab/src/routes/certificates.index.tsx` (TS fix)
- `guided-journey-lab/src/routes/courses.$courseId.index.tsx` (TS fix)
- `guided-journey-lab/src/routeTree.gen.ts` (auto-generated)

## Backend Endpoints Used

- `GET/POST /api/v1/cms/courses`
- `GET/POST/DELETE /api/v1/cms/courses/{courseId}/sections[/{sectionId}]`
- `GET/POST/DELETE /api/v1/cms/sections/{sectionId}/lessons[/{lessonId}]`

No backend changes — endpoints existed (already used by the Android teacher CMS module).

## Design Tokens Used

Existing tokens only: `bg-primary/10`, `bg-secondary`, `bg-muted`, `text-destructive`, `shadow-soft/elevated`, `rounded-3xl`, `text-display`. No hardcoded colors.

## States Handled

- [x] Loading (course list, sections, lessons)
- [x] Error (query + mutation errors surfaced inline with role=alert)
- [x] Empty (no courses, no sections, no lessons)
- [x] Success

## Dark Mode Tested

Token-based styling only, so both modes are covered by the design system. Not visually inspected in dark mode.

## TypeScript Errors

None — `tsc --noEmit` exits 0 (repo previously had 10 pre-existing errors; now fixed).

## Tests / Verification

- Headless browser: teacher login → redirected to `/teach`, sidebar shows "Teacher portal / Teaching Studio / Course Catalog" (no learner items), empty state renders, no console errors.
- Admin login verified earlier → `/admin/dashboard` with AdminShell (unchanged).

## Risks / Notes

- Create/delete CMS mutations not exercised end-to-end from the web UI (would write test data into the production Neon DB); they reuse the same request layer as verified endpoints and the same contracts as the Android CMS module.
- Learner sidebar content is unchanged by construction (same nav array as before), verified by code path, not by a fresh learner login.
- No course update/publish UI yet — publish/archive are ADMIN-only endpoints and belong to the admin console; course metadata editing can be added later via `PUT /api/v1/cms/courses/{id}`.
