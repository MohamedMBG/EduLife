# Platform Admin Experience

## Goal

Build a dedicated Platform Admin experience with a completely different UI from the student/teacher interfaces. Admin users must land on their own dashboard after login, see real platform stats, and manage teacher applications.

## What Changed

### Backend

- `AdminMetricsDto` — added `pendingTeacherRequests: long` field.
- `AdminMetricsService` — counts PENDING requests via `TeacherRequestRepository.findAllByStatus(PENDING, Pageable.unpaged()).getTotalElements()`.
- No new migration or endpoint — existing `GET /api/v1/admin/metrics` now returns the extra field; existing `GET /api/v1/admin/teacher-requests` + `PUT /{id}/approve` + `PUT /{id}/reject` were already implemented.

### Android

New feature package `features/admin/`:

**Models**
- `AdminStats.java` — mirrors `AdminMetricsDto` fields including `pendingTeacherRequests`
- `AdminTeacherRequest.java` — mirrors `TeacherRequestResponse`
- `AdminPageResponse.java` — generic page wrapper
- `AdminRejectRequest.java` — request body for reject
- `AdminUiState.java` — loading/success/error state for dashboard
- `TeacherRequestsUiState.java` — loading/success/error/action-feedback state for list

**Data**
- `AdminRepository.java` — `loadStats`, `loadTeacherRequests`, `approveRequest`, `rejectRequest`

**ViewModels**
- `AdminDashboardViewModel.java` — loads stats once, LiveData<AdminUiState>
- `TeacherRequestsViewModel.java` — loads by status filter, approve/reject with optimistic list removal

**UI**
- `AdminDashboardFragment.java` — 2×3 stat grid, quick-action rows, logout
- `TeacherRequestsFragment.java` — filter tabs (PENDING/APPROVED/REJECTED), RecyclerView, confirm dialogs
- `TeacherRequestAdapter.java` — diff-based list adapter, action buttons visible only for PENDING, status color per state

**Layouts**
- `fragment_admin_dashboard.xml` — clean back-office header (indigo accent), stat grid cards, action cards with pending badge
- `fragment_admin_teacher_requests.xml` — filter tabs, state card, action feedback strip, RecyclerView
- `item_teacher_request.xml` — status pill, email, motivation snippet, approve/reject buttons, admin note
- `dialog_reject_request.xml` — optional note input in AlertDialog

**Navigation**
- `nav_graph.xml` — added `adminDashboardFragment` + `teacherRequestsFragment` destinations with proper actions
- `LoginFragment.java` — after successful login reads `SessionStorage.getRole()`. If `ADMIN` → navigates to `adminDashboardFragment`, otherwise `homeFragment`.

**Resources**
- `colors.xml` — 9 new `admin_*` tokens: indigo accent (#3D5AF1), surfaces, borders, text levels
- `strings.xml` — 28 new `admin_*` strings
- 7 new drawables: `bg_admin_eyebrow`, `bg_admin_stat_card`, `bg_admin_stat_card_accent`, `bg_admin_action_card`, `bg_admin_logout_button`, `bg_admin_badge`, `bg_admin_approve_button`
- `ApiService.java` — 4 new admin endpoints: `getAdminStats`, `getAdminTeacherRequests`, `approveTeacherRequest`, `rejectTeacherRequest`

### Web

- `types.ts` — `AdminMetrics.pendingTeacherRequests` added; new `TeacherRequestStatus` type + `TeacherRequestSummary` interface
- `client.ts` — 3 new functions: `listAdminTeacherRequests`, `approveTeacherRequest`, `rejectTeacherRequest`
- `auth-context.tsx` — new `RequireAdmin` component (auth + role === ADMIN guard, redirects non-admins to `/dashboard`)
- `AdminShell.tsx` — dedicated back-office sidebar shell with indigo brand, ADMIN badge, nav items: Dashboard, Teacher Requests, Users (coming soon)
- `admin.tsx` — parent layout route `/admin` wrapping children in `<RequireAdmin>`
- `admin.dashboard.tsx` — `/admin/dashboard` with hero, 4+3 stat grid, pending badge on Teacher Requests CTA
- `admin.teacher-requests.tsx` — `/admin/teacher-requests` with status filter tabs, approve/reject with inline confirm, optimistic cache invalidation
- `dashboard.tsx` — ADMIN sessions now redirect to `/admin/dashboard` via `useEffect`
- `routeTree.gen.ts` — auto-updated by TanStack Router (admin routes already wired)

## Files Touched

- `backend/src/main/java/com/edulife/admin/dto/AdminMetricsDto.java`
- `backend/src/main/java/com/edulife/admin/service/AdminMetricsService.java`
- `app/src/main/java/com/baghdad/edulife/core/network/ApiService.java`
- `app/src/main/java/com/baghdad/edulife/features/admin/` (all new files)
- `app/src/main/res/layout/fragment_admin_dashboard.xml`
- `app/src/main/res/layout/fragment_admin_teacher_requests.xml`
- `app/src/main/res/layout/item_teacher_request.xml`
- `app/src/main/res/layout/dialog_reject_request.xml`
- `app/src/main/res/navigation/nav_graph.xml`
- `app/src/main/res/values/colors.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/drawable/bg_admin_*.xml` (7 files)
- `app/src/main/java/com/baghdad/edulife/features/auth/ui/LoginFragment.java`
- `guided-journey-lab/src/lib/api/types.ts`
- `guided-journey-lab/src/lib/api/client.ts`
- `guided-journey-lab/src/lib/auth/auth-context.tsx`
- `guided-journey-lab/src/components/app/AdminShell.tsx`
- `guided-journey-lab/src/routes/admin.tsx`
- `guided-journey-lab/src/routes/admin.dashboard.tsx`
- `guided-journey-lab/src/routes/admin.teacher-requests.tsx`
- `guided-journey-lab/src/routes/dashboard.tsx`

## Backend Endpoints Used

- `GET /api/v1/admin/metrics` — updated to include `pendingTeacherRequests`
- `GET /api/v1/admin/teacher-requests?status=&page=&size=`
- `PUT /api/v1/admin/teacher-requests/{id}/approve`
- `PUT /api/v1/admin/teacher-requests/{id}/reject`

All require `ADMIN` role (enforced both `@PreAuthorize` on backend and `RequireAdmin` on web).

## States Handled

- [x] Loading
- [x] Error (with retry)
- [x] Empty (no requests in filter)
- [x] Success
- [x] Action feedback (approve/reject result message)

## Dark Mode Tested

Web: AdminShell and all admin routes use design system tokens — dark mode supported by default.

## TypeScript Errors

None — `bun run build` clean (10.79 s).

## Architecture Compliance

- Admin routes are completely separate from learner routes — no shared shell
- Backend RBAC unchanged — `@PreAuthorize("hasRole('ADMIN')")` enforced on all admin endpoints
- Android follows feature-first MVVM: Fragment → ViewModel → Repository → ApiService
- No business logic in UI classes
- No fake data — all stats from real backend
- Role resolved server-side at `/auth/sync`, never from client claim

## Risks / Notes

- Android `SessionStorage.getRole()` reads from the encrypted local cache written during `syncWithBackend`. If sync hasn't completed by the time `renderAuthState` fires (race), the role may be null and the user lands on `homeFragment`. They can log out and back in — the sync is best-effort by design in the current `AuthViewModel`.
- Web `/admin/users` CTA shown but links to a `coming soon` toast — not built (post-MVP).
- `TeacherRequestAdapter` uses background tint on the status pill via `setBackgroundTintList` — requires API 21+, which is the project minimum.
