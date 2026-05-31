# Role Selection at Registration + GROUP_ADMIN Role

## Goal
Allow users to self-select their role (Student / Teacher / Institute Admin) at registration. Add GROUP_ADMIN as a new first-class role. Expose business metrics endpoint for platform ADMIN.

## What Changed

### Backend

**UserRole enum** (`users/model/UserRole.java`)
- Added `GROUP_ADMIN` between `TEACHER` and `ADMIN`

**V16 migration** (`V16__add_role_constraint.sql`)
- Adds CHECK constraint on `users.role` including all 4 values
- V1 had no constraint; this enforces DB-level integrity

**Auth sync** (`auth/`)
- New `AuthSyncRequest` DTO with optional `intendedRole` field
- `AuthController.sync()` accepts optional `@RequestBody`
- `AuthSyncService.syncCurrentUser()` applies `intendedRole` only on NEW user creation
- `ADMIN` cannot be self-assigned via registration (silently falls back to LEARNER)
- Existing users: `intendedRole` is ignored — role never changes on re-sync

**@PreAuthorize updates**
- `GroupController` — `TEACHER | GROUP_ADMIN | ADMIN`
- `CmsCourseController` — `TEACHER | GROUP_ADMIN | ADMIN`
- `CmsLessonController` — `TEACHER | GROUP_ADMIN | ADMIN`
- `CmsSectionController` — `TEACHER | GROUP_ADMIN | ADMIN`
- `CmsExamController` — `TEACHER | GROUP_ADMIN | ADMIN`
- `AdminUserController` — unchanged (ADMIN only)
- publish/archive — unchanged (ADMIN only)

**GroupService**
- Renamed `isAdmin` → `isPlatformAdmin` for clarity
- GROUP_ADMIN manages own groups via existing `isCreator` check (no logic change needed)

**Repository additions**
- `UserRepository.countByRole(UserRole)`
- `CourseRepository.countByStatus(CourseStatus)`
- `EnrollmentRepository.countByStatus(EnrollmentStatus)`

**Admin metrics** (new)
- `GET /api/v1/admin/metrics` — ADMIN only
- Returns: learners, teachers, group admins, courses by status, active enrollments, certificates

### Web

**`lib/api/types.ts`**
- Added `UserRole` union type: `"LEARNER" | "TEACHER" | "GROUP_ADMIN" | "ADMIN"`
- `AuthSyncResponse.role` now typed as `UserRole`

**`lib/api/client.ts`**
- `syncAuth()` accepts optional `intendedRole` param, sends it as JSON body

**`lib/auth/auth-context.tsx`**
- `RegisterInput` now includes optional `intendedRole`
- On register: stores non-LEARNER role in `localStorage` under `edulife_intended_role`
- On `hydrateSession` (first sign-in after verification): reads localStorage, passes to `syncAuth`, then clears it
- Subsequent logins: localStorage key is gone, sync called without role

**`routes/register.tsx`**
- Two-step form: Step 1 = role selection cards, Step 2 = credentials
- Role options: Student (LEARNER), Teacher (TEACHER), Institute Admin (GROUP_ADMIN)
- Animated step transition (AnimatePresence)
- Back button to change role from credentials step

## Files Touched
- `backend/.../users/model/UserRole.java`
- `backend/.../auth/dto/AuthSyncRequest.java` (new)
- `backend/.../auth/controller/AuthController.java`
- `backend/.../auth/service/AuthSyncService.java`
- `backend/.../groups/controller/GroupController.java`
- `backend/.../groups/service/GroupService.java`
- `backend/.../admin/controller/CmsCourseController.java`
- `backend/.../admin/controller/CmsLessonController.java`
- `backend/.../admin/controller/CmsSectionController.java`
- `backend/.../admin/controller/CmsExamController.java`
- `backend/.../admin/controller/AdminMetricsController.java` (new)
- `backend/.../admin/service/AdminMetricsService.java` (new)
- `backend/.../admin/dto/AdminMetricsDto.java` (new)
- `backend/.../users/repository/UserRepository.java`
- `backend/.../courses/repository/CourseRepository.java`
- `backend/.../enrollments/repository/EnrollmentRepository.java`
- `backend/db/migration/V16__add_role_constraint.sql` (new)
- `guided-journey-lab/src/lib/api/types.ts`
- `guided-journey-lab/src/lib/api/client.ts`
- `guided-journey-lab/src/lib/auth/auth-context.tsx`
- `guided-journey-lab/src/routes/register.tsx`

## Role Matrix

| Action | LEARNER | TEACHER | GROUP_ADMIN | ADMIN |
|--------|---------|---------|-------------|-------|
| Take courses | ✓ | ✓ | ✓ | ✓ |
| Create/edit courses (CMS) | ✗ | ✓ | ✓ | ✓ |
| Publish/archive courses | ✗ | ✗ | ✗ | ✓ |
| Manage own groups | ✗ | ✓ | ✓ | ✓ |
| Manage all groups | ✗ | ✗ | ✗ | ✓ |
| Change user roles | ✗ | ✗ | ✗ | ✓ |
| View platform metrics | ✗ | ✗ | ✗ | ✓ |

## Architecture Compliance
- intendedRole never trusted from client for existing users
- ADMIN cannot be self-assigned
- Role set only at DB level (server-side) on first sync
- localStorage key cleared after one use

## Risks / Notes
- Android registration screen not yet updated — Android users sync with no body and get LEARNER by default until Android side is updated
- GROUP_ADMIN can create courses directly; if this should be restricted later, add service-layer check
- Teacher-request flow (V15) still valid for existing LEARNERs who want to upgrade without re-registering
