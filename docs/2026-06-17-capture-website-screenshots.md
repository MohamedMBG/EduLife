# Task Audit - Capture Website Screenshots

## Date
2026-06-17

## Task Summary
Captured screenshots of all the EduLife website pages (including public landing/auth screens and the various student, teacher, group admin, and platform admin portal views) and saved them in the `/docs` directory.

## Files Created
- `guided-journey-lab/take-screenshots.js` (automation script)
- `docs/2026-06-17-capture-website-screenshots.md` (this audit file)
- All the generated screenshot files:
  - `docs/landing-desktop.png`
  - `docs/landing-mobile.png`
  - `docs/auth-login-desktop.png`
  - `docs/auth-register-role-desktop.png`
  - `docs/auth-register-role-mobile.png`
  - `docs/auth-register-create-account-desktop.png`
  - `docs/auth-forgot-password-desktop.png`
  - `docs/public-certificate-verify-desktop.png`
  - `docs/learner-dashboard-desktop.png`
  - `docs/learner-dashboard-mobile.png`
  - `docs/course-catalog-desktop.png`
  - `docs/course-details-desktop.png`
  - `docs/lesson-study-desktop.png`
  - `docs/mcq-exam-desktop.png`
  - `docs/study-planner-desktop.png`
  - `docs/study-planner-mobile.png`
  - `docs/gamification-level-desktop.png`
  - `docs/gamification-level-mobile.png`
  - `docs/career-advisor-desktop.png`
  - `docs/career-advisor-mobile.png`
  - `docs/certificates-desktop.png`
  - `docs/certificate-detail-desktop.png`
  - `docs/profile-desktop.png`
  - `docs/teacher-dashboard-desktop.png`
  - `docs/course-cms-desktop.png`
  - `docs/group-admin-dashboard-desktop.png`
  - `docs/admin-dashboard-desktop.png`
  - `docs/admin-analytics-desktop.png`

## Files Modified
- `guided-journey-lab/src/lib/api/demo.ts` (updated `demoLogin` to assign roles dynamically based on standard test email addresses)

## What Was Done
1. **Dynamic Roles in Demo Mode**: Updated `demoLogin` in `guided-journey-lab/src/lib/api/demo.ts` to assign appropriate user roles dynamically when logging in with standard test emails (`admin@edulife.test` for ADMIN, `teacher@edulife.test` for TEACHER, and `groupadmin@edulife.test` for GROUP_ADMIN).
2. **Automated Capturing**: Created an ESM Node script `guided-journey-lab/take-screenshots.js` that:
   - Starts the dev server on port `8091` with standalone `VITE_DEMO_MODE=true` environment setting.
   - Waits until the port is open.
   - Spawns Playwright Chromium to navigate, interact, and take high-quality viewport screenshots of all public pages.
   - Signs in under the `STUDENT` demo account to capture the complete learner workflow, including dashboard, catalog, details, lesson study, planner, gamification level, career advisor, profile, and certificates.
   - Signs in under the `TEACHER` account to capture the teacher dashboard and course CMS editor.
   - Signs in under the `GROUP_ADMIN` account to capture the cohort and teacher management dashboard.
   - Signs in under the `ADMIN` account to capture the platform administrator analytics and user role managers.
   - Signs out cleanly and cleans up the background dev server processes automatically.
3. **Execution**: Ran the automation script to compile and generate all requested assets inside the `/docs` directory.

## Architecture Compliance
- Changes inside the web app were restricted to the offline/standalone `demo.ts` helper mock file, ensuring zero modifications to the production API clients or data synchronizations.
- The automation script is isolated from the main source code bundle.

## Code Comments Added
- Added standard conditional comments to the `demoLogin` method explaining the role mapping logic used for administrative, teacher, and group admin portal mock logins in testing.

## Validation / Testing
- Ran the screenshot automation script via Node and validated that the dev server starts correctly under `VITE_DEMO_MODE=true`, logs in as each of the four roles successfully, captures the screenshots, and shuts down cleanly.
- Visual inspection of the screenshots in `/docs` verified that they capture the complete layouts, dashboard metrics, advisor chats, and side-bars without rendering spinners.

## Risks / Notes
- Standalone demo mode relies on local storage; some screens (e.g. daily planner goals) use pre-populated or mock data, which is normal for a front-end demo presentation.
