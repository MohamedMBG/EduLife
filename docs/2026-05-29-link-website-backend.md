# Task Audit - Link Website Backend

## Date
2026-05-29

## Task Summary
Connected the React web app to the existing Spring Boot backend by adding Firebase web authentication, a shared authenticated API client, and real backend-backed data flows for login, registration, dashboard, explore, and my-courses routes.

## Files Created
- `guided-journey-lab/.env.example`
- `guided-journey-lab/package-lock.json`
- `guided-journey-lab/src/components/app/AppShell.tsx`
- `guided-journey-lab/src/lib/api/client.ts`
- `guided-journey-lab/src/lib/api/types.ts`
- `guided-journey-lab/src/lib/auth/auth-context.tsx`
- `guided-journey-lab/src/lib/auth/firebase.ts`
- `guided-journey-lab/src/lib/env.ts`
- `docs/2026-05-29-link-website-backend.md`

## Files Modified
- `guided-journey-lab/package.json`
- `guided-journey-lab/src/routeTree.gen.ts`
- `guided-journey-lab/src/routes/__root.tsx`
- `guided-journey-lab/src/routes/login.tsx`
- `guided-journey-lab/src/routes/register.tsx`
- `guided-journey-lab/src/routes/dashboard.tsx`
- `guided-journey-lab/src/routes/explore.tsx`
- `guided-journey-lab/src/routes/courses.tsx`

## What Was Done
Added the Firebase web SDK dependency and installed it so the website can authenticate against the same Firebase project used by the Android app and backend token filter.

Added a shared website environment layer that reads the backend base URL and Firebase web config from `VITE_*` variables. Included `.env.example` so the required runtime values are explicit.

Created a shared authenticated API client in `src/lib/api/` with:
- Bearer token injection from Firebase ID tokens
- One forced token refresh retry on `401`
- Consistent backend error parsing using the existing API error contract
- Typed helpers for `/api/v1/auth/sync`, `/api/v1/profile`, `/api/v1/courses`, `/api/v1/enrollments`, and `/api/v1/progress/courses/{courseId}`

Created a shared auth/session context in `src/lib/auth/` that:
- Initializes Firebase Auth in the browser
- Persists the learner session in browser storage
- Enforces the backend rule that email must be verified before protected learner routes are usable
- Calls `/api/v1/auth/sync` after login so the web app uses the same identity bridge as Android
- Exposes `login`, `register`, `logout`, `getAccessToken`, and `RequireAuth`

Wrapped the app root with `AuthProvider` so authenticated routes can share one session source.

Replaced hardcoded website data with live backend data:
- `login.tsx`: real Firebase sign-in and backend sync
- `register.tsx`: real Firebase registration plus verification-email flow
- `dashboard.tsx`: real profile, enrollments, progress, and course suggestions
- `explore.tsx`: real published course catalog plus enrollment mutation
- `courses.tsx`: real user enrollments plus per-course progress and unenroll action

Created a shared `AppShell` component so authenticated pages reuse the same signed-in layout and logout behavior.

## Architecture Compliance
The work stays inside the web app under the recommended `src/lib/api`, `src/lib/auth`, `src/components`, and `src/routes` areas defined in `CLAUDE.md`. No backend contracts were forked or duplicated. The website now consumes the same `/api/v1/*` contracts and Firebase token bridge as the Android client, which matches the shared API rule and Sprint 1-4 learner flow priorities.

## Code Comments Added
Added comments for:
- Firebase browser persistence and why it is used
- The early sign-out rule for unverified email addresses
- The backend sync requirement after login
- The one-time `401` token refresh retry in the API client

These comments explain the security and session decisions rather than restating obvious code.

## Validation / Testing
Installed the Firebase web SDK with `npm install firebase`.

Built the website successfully with:
- `cmd /c npm run build`

The production build completed for both client and SSR outputs. Existing CSS import-order warnings remained, but they were pre-existing style warnings and did not block the build.

## Risks / Notes
The website code is wired, but runtime login will still fail until real values are provided in a local `.env` file:
- `VITE_API_BASE_URL`
- Firebase web config values from the same Firebase project as the backend

The backend must also allow the website origin in `APP_CORS_ALLOWED_ORIGINS`, or browser requests will be blocked by CORS before reaching the API.

The current website still does not expose lesson-player, exam, or certificate routes. This task focused on the existing visible website routes and the core auth/discovery/enrollment/progress integration requested.
