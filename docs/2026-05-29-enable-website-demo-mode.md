# Task Audit - Enable Website Demo Mode

## Date
2026-05-29

## Task Summary
Enabled the `guided-journey-lab` website to run locally in a standalone demo mode without Firebase or the Spring Boot backend.

## Files Created
- `guided-journey-lab/.env`
- `guided-journey-lab/src/lib/api/demo.ts`
- `docs/2026-05-29-enable-website-demo-mode.md`

## Files Modified
- `guided-journey-lab/.env.example`
- `guided-journey-lab/src/lib/env.ts`
- `guided-journey-lab/src/lib/api/client.ts`
- `guided-journey-lab/src/lib/auth/auth-context.tsx`
- `guided-journey-lab/src/routes/login.tsx`
- `guided-journey-lab/src/routes/register.tsx`
- `README.md`

## What Was Done
Added a `VITE_DEMO_MODE` flag so the website can bypass required backend and Firebase environment validation when running as a local demo.

Created a browser-local mock data layer for:
- authentication session state
- learner profile
- course catalog
- enrollments
- lesson progress
- lesson detail
- certificates

Updated the auth provider so demo mode uses local storage instead of Firebase while still keeping protected learner routes behind sign-in.

Updated the API client so all existing route screens reuse their current query code while switching to the demo data provider automatically when demo mode is enabled.

Added a local `.env` file with demo mode enabled by default so the user can run the website immediately.

Documented the local demo run steps in the root `README.md`.

## Architecture Compliance
The change stays inside the website app under `guided-journey-lab` and does not alter the Android architecture or backend module boundaries. The implementation keeps UI routes unchanged and isolates demo behavior inside the web environment and API/auth access layer, which is the smallest architecture-compatible solution for a frontend-only demo.

## Code Comments Added
Comments were added in the demo auth and demo certificate logic to explain:
- why protected routes still require local sign-in in demo mode
- why a synthetic certificate is issued only after all lessons are completed in the demo flow
- why network requests are blocked in demo mode

These comments explain business and technical intent rather than repeating code.

## Validation / Testing
Validated with:
- `npm run build` in `guided-journey-lab` succeeded
- started `npm run dev -- --host 127.0.0.1 --port 3000`
- confirmed the site responds with HTTP `200` on `http://127.0.0.1:3000`

`npm run lint` is currently failing in the existing web project because many files already use formatting that conflicts with the active Prettier/ESLint line-ending rules. This task did not attempt a repository-wide formatting rewrite.

## Risks / Notes
Demo mode uses browser local storage, so demo progress, enrollments, and certificates are not shared across browsers or devices.

The demo certificate flow is intentionally simplified because the real backend exam engine is not part of this standalone run path.

The local dev server was started on port `3000` during validation and may still be running after the task.
