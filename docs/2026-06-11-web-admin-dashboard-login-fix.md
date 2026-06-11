# Task Audit - Web Admin Dashboard Login Fix

## Date
2026-06-11

## Task Summary
Fixed the web dashboard path so an authenticated admin account can land on the dashboard instead of crashing when the account has no learner enrollments.

## Files Created
- docs/2026-06-11-web-admin-dashboard-login-fix.md

## Files Modified
- guided-journey-lab/src/routes/dashboard.tsx
- guided-journey-lab/src/lib/api/client.ts
- guided-journey-lab/src/lib/api/types.ts

## What Was Done
The dashboard previously rendered an `Open course` link with `activeCourse.courseId` before checking whether `activeCourse` existed. Admin accounts commonly have no active learner enrollment, so login could succeed but dashboard rendering could fail immediately.

The dashboard now:
- Detects `ADMIN` and `TEACHER` roles from the synced backend session.
- Calls `/api/v1/admin/metrics` only for `ADMIN` sessions.
- Shows platform metrics for admins instead of learner-only metric cards.
- Guards the active course link and shows a catalog link when there is no active enrollment.
- Shows an admin-specific empty state explaining that admin accounts are not required to have learner progress.

The API layer now includes a typed `AdminMetrics` response and a `getAdminMetrics` client method matching the backend admin metrics contract.

## Architecture Compliance
The change stays inside the existing web frontend structure:
- Route rendering logic remains in `guided-journey-lab/src/routes/dashboard.tsx`.
- Shared API access remains in `guided-journey-lab/src/lib/api/client.ts`.
- API response contracts remain in `guided-journey-lab/src/lib/api/types.ts`.

No backend authentication rules, role assignment rules, microservices, payment logic, discussions, notifications, or unrelated Android code were changed.

## Code Comments Added
- Added a dashboard comment explaining why the admin metrics query is gated by the synced backend role before calling the backend.
- Added an API client comment explaining that `/api/v1/admin/metrics` is intentionally admin-only and callers should gate it by role.

## Validation / Testing
- Ran `npm run build` in `guided-journey-lab` after implementation.
- Ran Prettier on the touched web files.
- Ran `npm run build` again after formatting.
- Ran `npx eslint src/routes/dashboard.tsx src/lib/api/client.ts src/lib/api/types.ts`.
- Started the Vite dev server locally and verified `http://127.0.0.1:5173/dashboard` returns HTTP 200.

Both builds completed successfully, ESLint passed for the modified web files, and the dashboard route responds from the dev server. The Codex in-app browser was not available in this session, so visual browser verification could not be completed here. A full `npm run lint` currently fails on repository-wide Prettier CRLF line-ending errors in existing files such as `eslint.config.js` and `src/components/app/AppShell.tsx`; that is unrelated to this dashboard/login fix. The build still reports an existing CSS warning that the Google Fonts `@import` should precede other rules; this warning is also unrelated.

## Risks / Notes
This fixes the dashboard render failure for admin accounts without adding a full admin CMS UI. Admins can now see high-level platform metrics and avoid the no-enrollment crash, but deeper admin workflows such as managing users or approving courses would need dedicated screens later.
