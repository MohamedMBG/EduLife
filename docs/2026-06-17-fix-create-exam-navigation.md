# Fix: "Create final exam" button does nothing

## Goal
Fix the "Create final exam" / "Manage final exam" button in the teacher CMS course page that appeared to do nothing when clicked.

## What Changed
`teach.$courseId.tsx` contained the full course management UI inline, but TanStack Router treated it as a **layout route** (parent of `teach.$courseId.exam.tsx`). Without an `<Outlet />`, clicking the Link to `/teach/$courseId/exam` changed the URL but the child route (exam builder) never rendered — the parent UI stayed visible.

Fix: split into the standard layout/index pattern:
- `teach.$courseId.tsx` — now a passthrough `<Outlet />` (same as `courses.$courseId.tsx`, `certificates.tsx`, etc.)
- `teach.$courseId.index.tsx` — **new file**, contains all the course management UI previously in the parent

The route tree (`routeTree.gen.ts`) already had the `TeachCourseIdIndexRoute` entry, so no regeneration was needed.

## Files Touched
- `guided-journey-lab/src/routes/teach.$courseId.tsx` — replaced with Outlet passthrough
- `guided-journey-lab/src/routes/teach.$courseId.index.tsx` — new, contains moved content

## Backend Endpoints Used
No changes; same endpoints as before.

## Design Tokens Used
No new tokens.

## States Handled
- [x] Loading
- [x] Error
- [x] Empty
- [x] Success

## Dark Mode Tested
N/A — no UI changes, only route structure fix.

## TypeScript Errors
None.

## Risks / Notes
- Zero logic changes — code was moved verbatim.
- The route tree was already expecting this file to exist; it just didn't.
