# Group Admin Course Approvals

## Goal

Course publishing approval belongs to the group admin when the authoring teacher is a member of one of that group admin's groups. Previously `PUT /api/v1/cms/courses/{id}/publish` was platform-ADMIN only.

## What Changed

### Backend

- `PUT /api/v1/cms/courses/{id}/publish` role gate widened to `GROUP_ADMIN, ADMIN`. The membership rule lives in the service: ADMIN publishes anything; GROUP_ADMIN only courses whose author is a member of a group **created by that group admin** — otherwise 403 "You can only approve courses from teachers in your groups". Teachers still cannot self-publish. Archive stays ADMIN-only.
- `GET /api/v1/cms/courses` scoping per role: TEACHER → own courses (unchanged); ADMIN → all (unchanged); **GROUP_ADMIN → courses authored by teachers in their groups** (their review queue).
- `CourseAdminDto` gains `createdByEmail` so reviewers see which teacher authored a course (batch-resolved, no N+1; additive JSON field — Android parsing unaffected).
- `GroupMemberRepository`: native queries `existsMemberManagedBy(managerId, userId)` and `findMemberUserIdsManagedBy(managerId)`.
- `CourseRepository`: `findAllByCreatedByUserIdIn`.

### Web

- GROUP_ADMIN sidebar gains **Course Approvals** (`/approvals`).
- New `/approvals` page (guard: GROUP_ADMIN/ADMIN via new `RequireCourseApprover`): "Pending review" list (drafts with author email, level, description, **Approve & publish** with confirm) and "Published" list.
- Client: `publishCmsCourse`; `CmsCourse` type gains `createdByEmail`.

## Files Touched

Backend:
- `backend/.../admin/controller/CmsCourseController.java`
- `backend/.../admin/service/CmsCourseService.java`
- `backend/.../admin/dto/CourseAdminDto.java`
- `backend/.../groups/repository/GroupMemberRepository.java`
- `backend/.../courses/repository/CourseRepository.java`
- `backend/src/test/java/com/edulife/admin/CmsCoursePublishTest.java` (new, 5 tests)

Web:
- `guided-journey-lab/src/routes/approvals.tsx` (new)
- `guided-journey-lab/src/components/app/AppShell.tsx`
- `guided-journey-lab/src/lib/auth/auth-context.tsx`
- `guided-journey-lab/src/lib/api/types.ts`, `client.ts`

No schema change, no migration.

## Architecture Compliance

- Authorization in two layers: `@PreAuthorize` role gate at the controller, membership ownership check in the service.
- DTO-only responses; author exposed by internal id + email, never `firebase_uid`.
- Same contract for all clients — Android can adopt the same endpoints later.

## Tests / Verification

- `CmsCoursePublishTest` (new): GROUP_ADMIN publish 200, GROUP_ADMIN non-managed author 403, TEACHER 403, ADMIN 200, archive stays ADMIN-only. Plus `GroupControllerTest`: 23/23 total green.
- Direct API verification against local backend:
  - Teacher **not** in group → group admin's CMS list empty, publish → 403.
  - Teacher added to group → list shows teacher's draft with `createdByEmail`, publish → 200 PUBLISHED.
- Headless UI e2e: teacher created draft in Teaching Studio → group admin saw it under Course Approvals → Approve & publish moved it to Published (queue 0, published 1).
- `tsc --noEmit` exits 0.

## Risks / Notes

- A teacher in no group can only be published by a platform ADMIN — intended fallback.
- A teacher in groups of two different group admins can be approved by either — acceptable for MVP.
- Admin console has no publish UI yet; admins can use the same endpoint (the `/approvals` page also works for ADMIN, showing all courses).
- Prod requires a backend deploy for the new authorization + scoping; web `.env.local` still points dev at the local backend.
- E2E test courses were removed from the local DB after verification.
