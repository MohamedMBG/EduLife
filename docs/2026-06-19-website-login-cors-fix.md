# Task Audit - Website Login CORS Fix

## Date
2026-06-19

## Task Summary
Investigated the web login failure that showed "Cannot reach the server. Check your connection and try again." and fixed the most likely backend/browser integration cause: the deployed website origin was not being allowed by backend CORS, so Firebase sign-in could succeed but browser sync to `/api/v1/auth/sync` failed as a network-style `TypeError`.

## Files Created
- docs/2026-06-19-website-login-cors-fix.md

## Files Modified
- backend/src/main/resources/application.yaml
- backend/.env.example
- guided-journey-lab/src/lib/api/client.ts

## What Was Done
Confirmed the symptom path in the website:

- `guided-journey-lab/src/lib/api/client.ts` converts browser `fetch` `TypeError`s into the exact message the user saw.
- Web login uses Firebase first, then calls `POST /api/v1/auth/sync` via the shared API client.

Validated backend reachability separately from browser access:

- `https://edulife-2bro.onrender.com/actuator/health` returned `UP`, so Render is reachable.
- Browser-style `OPTIONS` preflight requests sent with `Origin: https://guided-journey-lab.vercel.app` to `https://edulife-2bro.onrender.com/api/v1/auth/sync` returned no `Access-Control-Allow-Origin` header.
- That means the browser would block the request before the app can read any backend response, which surfaces as a generic `TypeError`.

Implemented the fix and hardening:

- Updated backend default CORS origins in `application.yaml` to include `https://guided-journey-lab.vercel.app` in addition to localhost development origins.
- Updated `backend/.env.example` to document that `APP_CORS_ALLOWED_ORIGINS` must include the exact deployed website origin, and explained the failure mode.
- Improved the website API client error message so future browser-side blocks explicitly mention the backend origin and likely CORS cause instead of pretending the server is simply down.

## Architecture Compliance
The change stays within the existing EduLife architecture:

- backend cross-origin policy remains centralized in backend configuration, not scattered into controllers
- website transport error handling remains in the shared API client under `guided-journey-lab/src/lib/api/`
- no business logic was moved into UI routes or duplicated across features

This keeps browser/network concerns in the correct cross-cutting layers.

## Code Comments Added
Added comments in:

- `backend/src/main/resources/application.yaml` to explain why the deployed Vercel origin is now part of the explicit default allowlist and why production should still override it
- `backend/.env.example` to explain why omitting the website origin causes Firebase sign-in to succeed but backend sync to fail in-browser

These comments explain the why behind the CORS rule, not just the config values.

## Validation / Testing
Validated with:

- direct health check to `https://edulife-2bro.onrender.com/actuator/health` -> backend returned `UP`
- browser-style preflight (`OPTIONS`) to `https://edulife-2bro.onrender.com/api/v1/auth/sync` with origin `https://guided-journey-lab.vercel.app` -> response lacked `Access-Control-Allow-Origin`, confirming the CORS problem
- `npm run build` inside `guided-journey-lab` -> passed successfully after the web client change

Manual follow-up still required:

- redeploy the backend so the updated default CORS config is live
- if Render already sets `APP_CORS_ALLOWED_ORIGINS`, update that env var directly to include `https://guided-journey-lab.vercel.app`
- retest website login from the deployed browser app after the backend redeploy

## Risks / Notes
- If Render already has `APP_CORS_ALLOWED_ORIGINS` set explicitly, that env var overrides the code default. In that case the Render env must still be corrected manually.
- The website message is now more diagnostic, but the real unblock for deployed login is the backend CORS configuration becoming live on Render.
- The current change does not broaden CORS with wildcards; it adds one explicit deployed origin, which stays aligned with the existing security model.
