# Task Audit - Deployed Website Login CORS Diagnosis

## Date
2026-06-19

## Task Summary
Verified why the deployed Vercel website still fails login while the local website works. The root cause is a live Render CORS mismatch: localhost is allowed, but the deployed Vercel origin is not.

## Files Created
- docs/2026-06-19-deployed-website-login-cors-diagnosis.md

## Files Modified
- None

## What Was Done
Checked the live deployed systems directly:

- `https://guided-journey-lab.vercel.app/` loads successfully.
- The deployed frontend bundle contains `https://edulife-2bro.onrender.com`, so Vercel is pointing at the correct backend origin.
- `https://edulife-2bro.onrender.com/actuator/health` returns `UP`, so the backend is alive.

Ran browser-style CORS checks against the live backend:

- `OPTIONS /api/v1/auth/sync` with `Origin: http://localhost:5173` returns `200` and includes `access-control-allow-origin: http://localhost:5173`.
- `OPTIONS /api/v1/auth/sync` with `Origin: https://guided-journey-lab.vercel.app` returns `403 Invalid CORS request` and does not include `Access-Control-Allow-Origin`.

This exactly explains the symptom:

- local website works because localhost is in the live allowlist
- deployed website fails because the Vercel origin is not in the live allowlist
- the website client reports that browser-blocked request as "Cannot reach the server"

## Architecture Compliance
This diagnosis respects the existing EduLife architecture:

- browser access policy remains a backend security/config concern
- website auth flow still correctly uses Firebase first and backend `/api/v1/auth/sync` second
- no business logic or UI routing changes were required to identify the production issue

## Code Comments Added
- None

## Validation / Testing
Validated with live checks against production:

- deployed frontend homepage fetch
- backend health endpoint fetch
- deployed frontend asset inspection for the compiled API base URL
- CORS preflight to `/api/v1/auth/sync` from both localhost and the Vercel origin

## Risks / Notes
- The backend code can already be corrected, but the deployed fix does not matter until Render is redeployed or its env vars are updated.
- If Render has `APP_CORS_ALLOWED_ORIGINS` set explicitly, that env var overrides any code default and must include `https://guided-journey-lab.vercel.app`.
- The fastest production fix is to update Render:

  `APP_CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173,http://127.0.0.1:3000,http://127.0.0.1:5173,https://guided-journey-lab.vercel.app`

  then redeploy the backend.
