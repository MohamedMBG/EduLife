# Task Audit - Global Stitch Design System Rollout

## Date
2026-06-16

## Task Summary
Applied the Stitch-inspired EduLife premium minimalist visual system to the web app foundation, shared UI primitives, app shells, and broad route-local CTA/status styling without changing backend logic, API contracts, or data hooks.

## Screen Inventory Found
- Landing: `/`
- Auth: `/login`, `/register`, `/forgot-password`
- Learner dashboard: `/dashboard`
- Catalog and learning: `/explore`, `/courses`, `/courses/$courseId`, `/courses/$courseId/resources`, `/learn/$courseId/$lessonId`
- Exam flow: `/courses/$courseId/exam`, `/courses/$courseId/exam/result`
- Certificates: `/certificates`, `/certificates/$certificateId`, `/certificates/verify/$hash`
- Career Advisor: `/advisor`
- Study tools: `/analytics`, `/planner`, `/level`
- Teacher tools: `/teach`, `/teach/$courseId`
- Admin console: `/admin/dashboard`, `/admin/analytics`, `/admin/teacher-requests`
- Group admin: `/groups`, `/groups/$groupId`
- Account: `/profile`

## Files Created
- guided-journey-lab/src/components/app/design-system.tsx
- docs/2026-06-16-global-stitch-design-system-rollout.md

## Files Modified
- guided-journey-lab/eslint.config.js
- guided-journey-lab/src/styles.css
- guided-journey-lab/src/components/app/AppShell.tsx
- guided-journey-lab/src/components/app/AdminShell.tsx
- guided-journey-lab/src/components/app/design-system.tsx
- guided-journey-lab/src/components/ui/button.tsx
- guided-journey-lab/src/components/ui/card.tsx
- guided-journey-lab/src/components/ui/badge.tsx
- guided-journey-lab/src/components/ui/input.tsx
- guided-journey-lab/src/components/ui/input-otp.tsx
- guided-journey-lab/src/components/ui/textarea.tsx
- guided-journey-lab/src/components/ui/table.tsx
- guided-journey-lab/src/components/ui/dialog.tsx
- guided-journey-lab/src/components/ui/sheet.tsx
- guided-journey-lab/src/components/ui/skeleton.tsx
- guided-journey-lab/src/components/landing/Footer.tsx
- guided-journey-lab/src/components/landing/Hero.tsx
- guided-journey-lab/src/components/landing/Journey.tsx
- guided-journey-lab/src/components/landing/Nav.tsx
- guided-journey-lab/src/components/lesson/LessonContentRenderer.tsx
- guided-journey-lab/src/routes/admin.analytics.tsx
- guided-journey-lab/src/routes/admin.dashboard.tsx
- guided-journey-lab/src/routes/admin.teacher-requests.tsx
- guided-journey-lab/src/routes/advisor.tsx
- guided-journey-lab/src/routes/analytics.tsx
- guided-journey-lab/src/routes/certificates.$certificateId.tsx
- guided-journey-lab/src/routes/courses.$courseId.exam.result.tsx
- guided-journey-lab/src/routes/courses.$courseId.exam.tsx
- guided-journey-lab/src/routes/courses.$courseId.resources.tsx
- guided-journey-lab/src/routes/courses.index.tsx
- guided-journey-lab/src/routes/dashboard.tsx
- guided-journey-lab/src/routes/explore.tsx
- guided-journey-lab/src/routes/forgot-password.tsx
- guided-journey-lab/src/routes/groups.index.tsx
- guided-journey-lab/src/routes/learn.$courseId.$lessonId.tsx
- guided-journey-lab/src/routes/login.tsx
- guided-journey-lab/src/routes/planner.tsx
- guided-journey-lab/src/routes/profile.tsx
- guided-journey-lab/src/routes/register.tsx
- guided-journey-lab/src/routes/teach.$courseId.tsx
- guided-journey-lab/src/routes/teach.index.tsx

## What Was Done
- Replaced the global web theme with Stitch-aligned tokens: `#f8f9fa` background, white/paper surfaces, `#003631` primary, `#134e48` primary container, mint accents, thin outline colors, and `#ba1a1a` destructive color.
- Set Montserrat as the display font and Inter as the body font while preserving the existing Tailwind v4 setup.
- Refined shared primitives for consistent premium UI: buttons, cards, badges, inputs, textareas, tables, dialogs, sheets, skeletons, and OTP caret color.
- Added reusable app-level primitives: `PageHeader`, `SectionHeader`, `GlassPanel`, `MetricCard`, `StatusPill`, `EmptyState`, `LoadingState`, `ErrorState`, and `ActionFooter`.
- Updated `AppShell` and `AdminShell` so learner, teacher, group admin, and platform admin routes share the off-white page frame, green active states, max-width rhythm, and softer shell borders.
- Replaced route-local solid black CTAs with EduLife green CTAs across learner, auth, certificate, lesson, study, teacher, group, and admin screens.
- Removed admin-specific blue one-off `oklch(...250)` styling and aligned admin dashboard/analytics/request status UI to the same green/mint system.
- Kept all existing React Query calls, auth guards, navigation routes, mutation handlers, and backend contracts intact.

## Architecture Compliance
- Changes stayed inside the web app under `guided-journey-lab`.
- No backend files, API contracts, Android files, or Firebase/backend auth logic were changed.
- Shared styling lives in global tokens and reusable components instead of duplicating one-off classes.
- Screen-specific route files still own their workflow behavior, while common visual behavior is centralized in `src/styles.css`, `src/components/ui/*`, and `src/components/app/*`.

## Code Comments Added
- Added a global token comment in `src/styles.css` explaining why the Stitch tokens are centralized for all role surfaces.
- Added a comment in `src/components/app/design-system.tsx` explaining why loading, empty, and error states share one frame.
- Preserved existing comments around role-specific navigation, RBAC expectations, learner redirects, auth state handling, and data-query guards.

## Validation / Testing
- Ran `npm run build`: passed for client and SSR production builds.
- Ran `npm run lint`: failed because the project has existing Prettier formatting errors across untouched landing, level, lesson, and course files. The failure is formatting-only; build completed successfully.
- Started the local Vite server at `http://127.0.0.1:8080/`.
- Checked HTTP headers with `curl.exe -I --max-time 10`:
  - `/` returned `HTTP/1.1 200`
  - `/login` returned `HTTP/1.1 200`
  - `/explore` returned `HTTP/1.1 200`
- Attempted in-app browser verification, but the browser surface was unavailable in this session (`iab` unavailable).

## Risks / Notes
- The requested `screen.png`, `code.html`, and `DESIGN.md` were not present in the provided attachment directory; only the text brief was available, so the rollout used the brief's tokens and the existing Stitch-inspired Advisor direction.
- `src/routes/advisor.tsx` was already modified before this task began. It remains part of the working tree; this rollout did not intentionally replace its workflow behavior.
- `npm run lint` is still blocked by broader repository formatting debt. Running Prettier across the whole web source tree would likely clear those formatting errors, but that would be a separate broad formatting task.
- Manual browser screenshots could not be captured because the in-app browser connector was unavailable.
