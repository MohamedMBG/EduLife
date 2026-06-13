# Group Admin Portal: Distinct UI + Groups Management

## Goal

GROUP_ADMIN must not share the teacher UI. Per the AGENTS.md role matrix, a group admin manages cohorts (members inside their own group, courses assigned to the group) and does not author courses. The previous iteration gave GROUP_ADMIN the teacher sidebar and Teaching Studio.

## What Changed

### Backend (groups module)

The module had write endpoints only (create group, add/remove member, attach course) — no way to read anything back, so no UI could exist. Added:

- `GET /api/v1/groups` — groups owned by the caller (ADMIN sees all), with member/course counts (`GroupSummaryDto`).
- `GET /api/v1/groups/{groupId}` — members (with email + role) and attached courses (with title + status), owner-or-ADMIN only (`GroupDetailDto`, `GroupMemberDetailDto`, `GroupCourseDetailDto`).
- `AddMemberRequest` now accepts `userId` **or** `email` (exactly one; service validates). Email lets group admins add people without knowing internal ids. Backward compatible — `userId` requests still work.
- Repos: `countByGroupId` on members and courses.
- No schema change, no migration.

### Web

- Sidebar per role (AppShell):
  - GROUP_ADMIN → "Group admin portal": **My Groups**, Course Catalog. No Teaching Studio.
  - TEACHER → "Teacher portal": Teaching Studio, **My Cohorts** (teachers also own cohorts per the backend rules), Course Catalog.
  - LEARNER unchanged; ADMIN keeps the separate admin console.
- New `/groups` page: list own groups with member/course counts, create group.
- New `/groups/$groupId` page: member list (add by email, remove with confirm), assigned courses (assign from published catalog, duplicates filtered out).
- Guards refactored into a shared `RequireRole`:
  - `RequireTeacher` (/teach) now allows TEACHER/ADMIN only — GROUP_ADMIN is bounced to /dashboard, which forwards to /groups (no redirect loop).
  - New `RequireGroupManager` (/groups) allows TEACHER/GROUP_ADMIN/ADMIN.
- `/dashboard` redirect: GROUP_ADMIN → `/groups`.

## Files Touched

Backend:
- `backend/.../groups/controller/GroupController.java` (2 GET endpoints)
- `backend/.../groups/service/GroupService.java` (list/detail/add-by-email)
- `backend/.../groups/dto/AddMemberRequest.java`, `GroupSummaryDto.java` (new), `GroupDetailDto.java` (new), `GroupMemberDetailDto.java` (new), `GroupCourseDetailDto.java` (new)
- `backend/.../groups/repository/GroupMemberRepository.java`, `GroupCourseRepository.java` (countByGroupId)
- `backend/src/test/java/com/edulife/groups/GroupControllerTest.java` (4 new tests)

Web:
- `guided-journey-lab/src/components/app/AppShell.tsx`
- `guided-journey-lab/src/lib/auth/auth-context.tsx`
- `guided-journey-lab/src/lib/api/types.ts`, `client.ts`
- `guided-journey-lab/src/routes/groups.tsx`, `groups.index.tsx`, `groups.$groupId.tsx` (new)
- `guided-journey-lab/src/routes/dashboard.tsx`
- `guided-journey-lab/.env.local` (temporary — points web dev at the local backend until the new endpoints deploy to Render; delete to go back to prod)

## Backend Endpoints Used

- `GET/POST /api/v1/groups`
- `GET /api/v1/groups/{groupId}`
- `POST/DELETE /api/v1/groups/{groupId}/members[/{userId}]`
- `POST /api/v1/groups/{groupId}/courses`
- `GET /api/v1/courses` (catalog for the assign dropdown)

## Architecture Compliance

- Ownership enforced server-side (`loadGroupForManagement`: creator or ADMIN), GETs reuse the same check.
- DTOs only — no entity exposure, no `firebase_uid`.
- Role gate via `@PreAuthorize` unchanged; UI mirrors it.
- Controller stays thin; one-of validation lives in the service.

## States Handled

- [x] Loading / Error / Empty / Success on groups list, group detail, member ops, course assign.

## Tests / Verification

- `GroupControllerTest`: 18/18 pass (4 new: list 200 GROUP_ADMIN, list 403 LEARNER, detail 200 owner, detail 403 non-owner).
- `tsc --noEmit` exits 0.
- Headless e2e against local backend: group admin login → `/groups` with "Group admin portal" sidebar → created group → added member by email (role shown LEARNER) → assigned course. Teacher sidebar shows Teaching Studio + My Cohorts. Group admin visiting `/teach` is bounced to `/groups` (no loop).

## Risks / Notes

- Production (Render) does not have the new GET endpoints until the backend deploys — the groups UI 404s against prod until then. `.env.local` keeps local dev working; delete it after deploy.
- "Group performance summaries" from the role matrix (enrollment/progress tracking per group) not built yet — needs a dedicated aggregate endpoint; counts on the list page are the MVP slice.
- E2E test data (group "Pilot Cohort A") lives in the local DB only.
