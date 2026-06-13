# Web Login Fix — stale .env.local backend override

## Goal
Website login was not working. Find and fix the root cause.

## What Changed
Deleted `guided-journey-lab/.env.local`, which contained a single line:

```
VITE_API_BASE_URL=http://localhost:8081
```

Vite loads `.env.local` with **higher precedence than `.env`**, so this override forced the
whole web app to call the backend at `http://localhost:8081`. Login itself runs in two
stages: (1) Firebase email/password sign-in, then (2) `POST /api/v1/auth/sync` to the
EduLife backend to resolve the internal user id + role. Stage 1 still succeeded (Firebase
config comes from `.env`), but stage 2 hit `localhost:8081` where no backend was running,
so `syncAuth` failed and the auth context fell back to anonymous — login "did nothing".

`.env.local` was added on 2026-06-13 only so the new groups/approvals endpoints could be
tested against a local backend before the Render deploy; its own audit doc said to delete
it to go back to prod. Removing it restores the real backend URL from `.env`:

```
VITE_API_BASE_URL=https://edulife-2bro.onrender.com
```

## Files Touched
- `guided-journey-lab/.env.local` (deleted — was gitignored / local-only, not tracked)

## Backend Endpoints Used
- `POST /api/v1/auth/sync` — verified reachable on prod (returns `HTTP 401` without a token,
  i.e. server up and the endpoint exists).
- `GET /api/v1/courses` — also `HTTP 401` (reachable, auth-gated).

## States Handled
- [x] Login success path now reaches the live backend.
- [x] Error path unchanged (sync failures still surface a readable message).

## Dark Mode Tested
N/A — config-only change.

## TypeScript Errors
None — no source changed.

## Risks / Notes
- The dev server caches env at startup. After this change, **restart** `bun run dev` (a
  running server still holds the old `localhost:8081`).
- To test the group-admin GET endpoints against a *local* backend again, recreate
  `.env.local` AND start the backend on that port — do not leave the override in place
  without a backend running, or login breaks again.
- CORS: local dev origin calling the Render backend already worked before `.env.local`
  was introduced, so `APP_CORS_ALLOWED_ORIGINS` already allows it.
- Prod (Render) may still 404 the newest groups/approvals GETs until the backend redeploys;
  that does not affect login or the learner flow.
