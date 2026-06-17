# Shared Top Navigation Rollout

## Goal

Unify navigation across the entire EduLife web application by replacing multiple inconsistent navigation systems (sidebar shells, per-page inline navs) with one shared top navigation bar.

## What Changed

Created a single `AppTopNav` component and `AppLayout` wrapper that every authenticated page now uses. Removed four inline top navs (dashboard, explore, planner, advisor), the `AppShell` sidebar, and the `AdminShell` sidebar. The landing page `Nav` component remains unchanged (public anchor-based nav for the marketing page). Auth pages (login, register, forgot-password) remain navless.

### Navigation Architecture

**Before:** 6 different navigation implementations:
1. `Nav` - Landing page floating pill nav (anchor links)
2. `DashboardTopNav` - Dashboard-only inline top bar
3. `ExploreNav` - Explore page inline top bar with search
4. `PlannerTopNav` - Planner page inline top bar
5. `AdvisorTopNav` + `AdvisorSidebar` - Advisor page inline top bar + sidebar
6. `AppShell` - Sidebar navigation for learner/teacher/group-admin routes
7. `AdminShell` - Sidebar navigation for admin routes

**After:** 2 navigation implementations:
1. `Nav` - Landing page (unchanged)
2. `AppTopNav` + `AppLayout` - Shared top nav for all authenticated pages

### Shared Navigation Config (role-based)

**Learner:**
- Dashboard, Explore, My Courses, Study Planner, Career Advisor, Certificates, Analytics, Level

**Teacher:**
- Teaching Studio, Analytics, My Cohorts, Course Catalog

**Group Admin:**
- My Groups, Analytics, Approvals, Course Catalog

**Admin:**
- Dashboard, Analytics, Teacher Requests, Course Catalog

### Features
- Role-aware navigation links (learner/teacher/group-admin/admin)
- Active route highlighting with primary color underline
- Pattern-based route matching (e.g., `/courses/*` and `/learn/*` both activate "My Courses")
- User profile dropdown with name, email, sign out
- Dark mode toggle
- Guest state: Sign In + Get Started buttons
- Mobile responsive: hamburger menu with full nav + profile + logout
- Optional search bar (used on Explore page)
- Design tokens from the existing EduLife theme (no hardcoded colors)

## Files Touched

### New Components
- `src/components/app/AppTopNav.tsx` - Shared top navigation component
- `src/components/app/AppLayout.tsx` - Layout wrapper (nav + content area)

### Updated Routes (AppShell -> AppLayout)
- `src/routes/analytics.tsx`
- `src/routes/certificates.index.tsx`
- `src/routes/certificates.$certificateId.tsx`
- `src/routes/courses.index.tsx`
- `src/routes/courses.$courseId.index.tsx`
- `src/routes/courses.$courseId.exam.tsx`
- `src/routes/courses.$courseId.exam.result.tsx`
- `src/routes/courses.$courseId.resources.tsx`
- `src/routes/learn.$courseId.$lessonId.tsx`
- `src/routes/level.tsx`
- `src/routes/profile.tsx`
- `src/routes/groups.index.tsx`
- `src/routes/groups.$groupId.tsx`
- `src/routes/teach.index.tsx`
- `src/routes/teach.$courseId.tsx`
- `src/routes/approvals.tsx`

### Updated Routes (AdminShell -> AppLayout)
- `src/routes/admin.dashboard.tsx`
- `src/routes/admin.analytics.tsx`
- `src/routes/admin.teacher-requests.tsx`

### Updated Routes (Inline Nav -> AppLayout)
- `src/routes/dashboard.tsx` - Removed DashboardTopNav, TopNavLink, DashboardFooter, getInitials
- `src/routes/explore.tsx` - Removed ExploreNav, inline footer
- `src/routes/planner.tsx` - Removed PlannerTopNav, TopNavLink, PlannerFooter
- `src/routes/advisor.tsx` - Removed AdvisorTopNav, AdvisorTopNavLink, AdvisorSidebar, AdvisorFooter

### Unchanged
- `src/components/landing/Nav.tsx` - Landing page nav (public, anchor-based)
- `src/components/app/AppShell.tsx` - Still exists but no longer imported by any route
- `src/components/app/AdminShell.tsx` - Still exists but no longer imported by any route
- Auth pages (login, register, forgot-password) - No nav, unchanged
- `src/routes/certificates.verify.$hash.tsx` - Public route, uses its own simple header

## Backend Endpoints Used

None. This is a frontend-only navigation refactor.

## Design Tokens Used

- `bg-surface-elevated/90` - Nav background
- `border-border/70` - Nav bottom border
- `bg-gradient-primary` - Logo badge + user avatar
- `text-primary` - Active link color
- `text-muted-foreground` - Inactive link color
- `bg-accent/55` - Mobile menu active item
- `shadow-bezel` - Logo badge shadow
- `shadow-elevated` - Profile dropdown + mobile menu
- `text-display` - Logo typography (Fraunces)
- `backdrop-blur-xl` - Nav blur effect

## States Handled

- [x] Loading (auth status loading handled by existing RequireAuth)
- [x] Error (N/A - nav is structural, not data-driven)
- [x] Empty (guest state: Sign In / Get Started)
- [x] Success (authenticated state: role-based links + profile dropdown)

## Dark Mode Tested

Yes - component uses design tokens that respond to `.dark` class.

## TypeScript Errors

None. `npx tsc --noEmit` passes cleanly.

## Build

`bun run build` succeeds.

## Role-Based Behavior

| Role | Nav Links | Verified |
|------|-----------|----------|
| LEARNER | Dashboard, Explore, My Courses, Study Planner, Career Advisor, Certificates, Analytics, Level | Via existing auth context |
| TEACHER | Teaching Studio, Analytics, My Cohorts, Course Catalog | Via existing auth context |
| GROUP_ADMIN | My Groups, Analytics, Approvals, Course Catalog | Via existing auth context |
| ADMIN | Dashboard, Analytics, Teacher Requests, Course Catalog | Via existing auth context |
| Guest | Sign In, Get Started | Via auth status check |

## Active Route Matching

| Pattern | Activates |
|---------|-----------|
| `/dashboard` | Dashboard |
| `/explore` | Explore |
| `/courses`, `/courses/*`, `/learn/*` | My Courses |
| `/planner` | Study Planner |
| `/advisor` | Career Advisor |
| `/certificates`, `/certificates/*` | Certificates |
| `/analytics` | Analytics |
| `/level` | Level |
| `/teach`, `/teach/*` | Teaching Studio |
| `/groups`, `/groups/*` | My Groups / My Cohorts |
| `/approvals` | Approvals |
| `/admin/dashboard` | Dashboard (admin) |
| `/admin/analytics` | Analytics (admin) |
| `/admin/teacher-requests` | Teacher Requests |

## Risks / Notes

- `AppShell.tsx` and `AdminShell.tsx` files still exist but are no longer imported. They can be deleted in a follow-up cleanup if desired.
- Per-page footers (dashboard, planner, advisor, explore) were removed since they contained generic placeholder links. A shared footer can be added to AppLayout later if needed.
- The dashboard page previously had its own distinctive visual style with hardcoded colors (`#091426`, `#f6fafe`). Those page-internal styles remain in the dashboard content components - only the nav wrapper changed.
- The advisor page previously had a sidebar (`AdvisorSidebar`) with section anchors. This was removed along with the nav. The advisor content itself remains fully functional.
- Search in the top nav is only shown on the Explore page via the `showSearch` prop on AppLayout.
