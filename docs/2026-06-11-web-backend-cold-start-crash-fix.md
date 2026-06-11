# Fix: Web App Crashes on Backend Cold Start (Render Free Tier)

## Goal

Login showed "This page didn't load / Something went wrong on our end" whenever the
Render backend was sleeping. Needed the app to degrade gracefully instead of crashing.

## Root Cause

1. `fetch` has no built-in timeout. Render free tier cold start takes 50–90 s.
2. After ~60 s the browser aborts the hung `fetch` with `TypeError: Failed to fetch`.
3. `syncAuth` inside `hydrateSession` was not caught — the `TypeError` propagated to
   the `onAuthStateChanged` callback in `AuthProvider`, which was caught by the call
   site (`void hydrateSession(...).catch(...)`) and called `auth.signOut()` + 
   `commitAnonymous`. That looked correct, but the error re-thrown from there reached
   TanStack Router's root `errorComponent` instead of the login page with a message.

## What Changed

### `guided-journey-lab/src/lib/api/client.ts`
- Added `AbortSignal.timeout(15_000)` to every `fetch` call.
- Wrapped `fetch` in try/catch to convert `TimeoutError` → `ApiClientError(503, "server taking too long")` and `TypeError` → `ApiClientError(503, "cannot reach server")`.
- Users now get a readable error in 15 s instead of a blank crash after 60+ s.

### `guided-journey-lab/src/lib/auth/auth-context.tsx`
- Wrapped `syncAuth(...)` call in `hydrateSession` with its own try/catch.
- On failure: `commitAnonymous(getReadableAuthError(syncError))` — shows the error message on the login page, does NOT crash the root error boundary.
- `syncedUidRef` reset on failure so the next "Try again" click retries properly.

## Files Touched

- `guided-journey-lab/src/lib/api/client.ts`
- `guided-journey-lab/src/lib/auth/auth-context.tsx`

## Backend Endpoints Used

- `POST /api/v1/auth/sync` (the one that was timing out)

## States Handled

- [x] Loading
- [x] Error (network timeout → readable message on login page, not crash)
- [x] Empty
- [x] Success

## Dark Mode Tested

N/A — logic change only.

## TypeScript Errors

None — `bun run build` clean.

## Risks / Notes

- 15 s timeout is tight for Render free tier cold start (~50–90 s). User will see
  "server taking too long" and need to click again once the backend is warm. This is
  acceptable and far better than a crash page with no actionable message.
- Long-term: upgrade Render to paid tier, add a keep-alive ping, or add a retry button
  on the login error that automatically retries the sync.
- Deployed to Cloudflare via `git subtree push --prefix=guided-journey-lab web main`.
