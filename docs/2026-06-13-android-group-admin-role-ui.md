# Android Role-Based UI + Group Admin Portal (and Web Dashboard Tuning)

## Date
2026-06-13

## Goal
Make sure every role (learner, teacher, group admin, platform admin) lands in the UI
that matches its use cases on **both** Android and web. Web role portals were already
built (see the three 2026-06-13 web docs); the real gaps were on Android. Follow-up
request: tune the web version even better.

## What Changed

### Android — role routing fixes
Two routing defects meant non-learner roles fell into the learner UI:

1. **Relaunch routing** (`MainActivity.configureNavigationStartDestination`): a saved
   session always started on `homeFragment`, so a teacher/admin/group-admin who reopened
   the app landed on the learner home. Now the start destination is chosen by role via a
   new `startDestinationForRole(...)` helper (ADMIN → admin dashboard, TEACHER → teacher
   dashboard, GROUP_ADMIN → group admin dashboard, else learner home).
2. **Login routing** (`LoginFragment`): only ADMIN and TEACHER were routed; GROUP_ADMIN
   fell through to the learner home. Added a GROUP_ADMIN branch.

### Android — new Group Admin portal (the missing role)
GROUP_ADMIN had no Android UI at all. Built a full feature module
`features/groupadmin/` mirroring the web Group Admin portal, consuming the existing
backend endpoints (no backend change required):

- **Dashboard** (`GroupAdminDashboardFragment`): lists the admin's owned cohorts with
  member/course counts, a FAB to create a group, a "Course Approvals" entry point, and
  sign-out. Reloads on resume so counts stay fresh after edits.
- **Group detail** (`GroupDetailFragment`): members (add by email, remove with confirm)
  and attached courses (assign from the published catalog, already-attached filtered out).
- **Course approvals** (`CourseApprovalsFragment`): the CMS review queue split into
  "Pending review" (DRAFT, with **Approve & publish**) and "Published" (read-only). A 403
  surfaces the "only your teachers' courses" rule as a clear message.

Architecture follows the existing Android MVVM pattern: Fragment → ViewModel →
`GroupAdminRepository` → `ApiService`. No API calls in fragments; all Retrofit work lives
in the repository. UI reuses the existing `teacher_*` color tokens and drawables for
visual consistency.

### Web — role UX tuning (follow-up request)
On `/dashboard`, non-learner roles (teacher / group admin / admin) used to mount the full
learner dashboard and fire the learner queries (`profile`, `enrollments`, `courses`,
`progress`) before the redirect effect moved them to their portal — a content flash plus
3–4 wasted backend calls per visit. Now:
- `isLearner` gates `profileQuery`, `enrollmentsQuery`, and `exploreQuery` (`enabled`).
- `adminMetricsQuery` is disabled here entirely (admins have their own AdminShell route).
- Non-learners get a clean `RedirectingScreen` hand-off instead of the learner UI flash.

## Files Touched

### Android — modified
- `app/.../MainActivity.java` (role-based start destination)
- `app/.../features/auth/ui/LoginFragment.java` (GROUP_ADMIN branch)
- `app/.../core/network/ApiService.java` (publish + groups endpoints)
- `app/.../features/teacher/model/CmsCourse.java` (`createdByEmail` field, additive)
- `app/src/main/res/navigation/nav_graph.xml` (3 group-admin destinations + actions)
- `app/src/main/res/values/strings.xml` (group admin + approvals strings)

### Android — created
- `features/groupadmin/model/`: `GroupSummary`, `GroupMember`, `GroupCourse`,
  `GroupDetail`, `CreateGroupRequest`, `AddMemberRequest`, `AttachCourseRequest`,
  `GroupAdminUiState`, `GroupDetailUiState`, `ApprovalsUiState`
- `features/groupadmin/data/GroupAdminRepository.java`
- `features/groupadmin/viewmodel/`: `GroupAdminDashboardViewModel`,
  `GroupDetailViewModel`, `CourseApprovalsViewModel`
- `features/groupadmin/ui/`: `GroupAdminDashboardFragment`, `GroupDetailFragment`,
  `CourseApprovalsFragment`, `GroupSummaryAdapter`, `GroupMemberAdapter`,
  `GroupCourseAdapter`, `ApprovalCourseAdapter`
- `res/layout/`: `fragment_group_admin_dashboard.xml`, `fragment_group_detail.xml`,
  `fragment_course_approvals.xml`, `item_group_summary.xml`, `item_group_member.xml`,
  `item_group_course.xml`, `item_approval_course.xml`

### Web — modified
- `guided-journey-lab/src/routes/dashboard.tsx`

## Backend Endpoints Used (no backend changes)
- `GET /api/v1/groups`, `GET /api/v1/groups/{groupId}`
- `POST /api/v1/groups`, `POST/DELETE /api/v1/groups/{groupId}/members[/{userId}]`
- `POST /api/v1/groups/{groupId}/courses`
- `GET /api/v1/cms/courses` (GROUP_ADMIN review queue, scoped server-side)
- `PUT /api/v1/cms/courses/{id}/publish` (GROUP_ADMIN/ADMIN)
- `GET /api/v1/courses` (catalog for the assign-course picker)

## Architecture Compliance
- Android: Java + XML, feature-first MVVM, manual DI, Retrofit in the data layer only.
  Authorization mirrors the backend (`@PreAuthorize` + service ownership checks); the UI
  only reflects rules, it does not enforce them. No entity exposure — DTO-shaped models.
- Web: consumes the real backend, design tokens only, no hardcoded colors, redirect-only
  change (no business logic duplicated).

## States Handled
- Android: loading / error (with retry) / empty / success on every new screen; mutation
  errors surfaced as toasts; destructive remove gated behind a confirm dialog.
- Web: dashboard redirect hand-off screen; learner states unchanged.

## Tests / Verification
- Android: `./gradlew :app:compileDebugJavaWithJavac` (Zulu 21) → BUILD SUCCESSFUL.
  Resource merge, R file, and navigation resource compile all pass, so the new layouts,
  nav graph, and string references resolve.
- Web: `bun x tsc --noEmit` → exit 0.
- Not exercised end-to-end against a live backend in this task (no group-admin device
  session available); the endpoints and contracts are the same ones the web Group Admin
  portal already verified on 2026-06-13.

## Risks / Notes
- Toolchain note: Gradle must run on a JDK with `jlink` (Zulu 21 / Android Studio JBR);
  the VSCode-bundled JRE lacks it and fails the `androidJdkImage` transform.
- "Group performance summaries" (per-group enrollment/progress aggregates) remain unbuilt
  on both clients — needs a dedicated aggregate endpoint; counts are the MVP slice.
- The `/dashboard` admin metrics block is now dead code for the rendered path (admins are
  redirected to AdminShell); left in place to keep the diff surgical.
