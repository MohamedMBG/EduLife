# Web Login Review Fixes

## Goal

Full review of website login flow and fixes for all issues found: race conditions, error handling, security rule compliance, accessibility, and account enumeration.

## What Changed

### Race condition fix (`login.tsx`)
- Removed redundant `navigate({ to: "/dashboard" })` after `auth.login(...)`. The `useEffect` watching `auth.status === "authenticated"` already handles redirect once the `onIdTokenChanged` listener hydrates the session. The old code raced the listener and could briefly land on `/dashboard` before session was ready.

### Unverified email path now throws
- `auth-context.tsx` `login()` previously detected `!emailVerified`, called `commitAnonymous(message)`, and returned successfully — leaving the login page to think login worked. Now throws a new `UnverifiedEmailError` so the login page surfaces it inline via `submitError`.

### Firebase error mapping
- `getReadableAuthError` is now exported.
- Added mappings: `auth/invalid-email`, `auth/user-disabled`, `auth/network-request-failed`, `auth/operation-not-allowed`, `auth/missing-password`.
- `login()` and `register()` wrap all Firebase calls in `try/catch` and throw `new Error(getReadableAuthError(err))` so users no longer see raw `"Firebase: Error (auth/invalid-credential)."` strings.
- `forgot-password.tsx` now routes errors through `getReadableAuthError`.

### Email trim
- `login()` and `register()` in `auth-context.tsx` trim email before passing to Firebase. Removes false `auth/invalid-email` failures from trailing whitespace.

### Token persistence — CLAUDE.md compliance
- `firebase.ts` switched from `browserLocalPersistence` to `browserSessionPersistence`. CLAUDE.md explicitly says: *"Never store the Firebase token in `localStorage` — use memory or `sessionStorage`."* Session persistence keeps tokens in `sessionStorage` so refresh works but closing the tab clears auth.

### Account enumeration on forgot-password
- `forgot-password.tsx` now swallows `auth/user-not-found` and `auth/invalid-credential` and shows the success state regardless. Prevents attackers from probing which emails are registered.

### Intended role hardening
- `auth-context.tsx` reads `INTENDED_ROLE_KEY` from `localStorage` and only accepts values in `REGISTERABLE_ROLES = {LEARNER, TEACHER, GROUP_ADMIN}`. A tampered storage value of `ADMIN` is now ignored. Register path also gates writes through the same whitelist (defense in depth — backend remains the source of truth).

### Stale error cleanup
- `AuthContextValue` now exposes `clearError()`. `login.tsx` calls it on email/password input change, plus clears local `submitError`. Stops yesterday's error from haunting today's form.

### Accessibility
- Added `aria-pressed={showPassword}` / `aria-pressed={showConfirm}` to all password-visibility toggles (login + register).
- Error containers (login, register, forgot-password) now have `role="alert"` and `aria-live="polite"` so screen readers announce them.

### Visual
- Sign In / Send reset link buttons gained `disabled:opacity-40 disabled:pointer-events-none` so the disabled state is visible (registration already had it).

## Files Touched

- `guided-journey-lab/src/routes/login.tsx`
- `guided-journey-lab/src/routes/register.tsx`
- `guided-journey-lab/src/routes/forgot-password.tsx`
- `guided-journey-lab/src/lib/auth/auth-context.tsx`
- `guided-journey-lab/src/lib/auth/firebase.ts`

## Backend Endpoints Used

- `POST /api/v1/auth/sync` — unchanged. Still receives optional `intendedRole` body for first-time hydrate after registration; whitelist now enforced client-side.

## Design Tokens Used

No new tokens. Existing tokens preserved (`bg-foreground`, `text-background`, `border-destructive`, `bg-destructive/5`, `text-destructive`, `bg-primary/8`, etc.).

## States Handled

- [x] Loading (`submitting`)
- [x] Error (`submitError`, `auth.error`, both clear on edit)
- [x] Empty (N/A for login)
- [x] Success (redirect via `auth.status === "authenticated"` useEffect)

## Dark Mode Tested

Yes — all changes are token-only. No mode-specific values introduced.

## TypeScript Errors

None in touched files. Verified via `bun run tsc --noEmit | grep -E "(login|auth-context|firebase|forgot-password|register)"` — clean.

Pre-existing errors in `demo.ts`, `certificates.tsx`, `courses.$courseId.tsx`, `learn.$courseId.$lessonId.tsx` are unrelated to this task.

## Follow-up Fix — Rate-Limit Cascade

After deploy the user hit a flood of `429` from `POST /api/v1/auth/sync`. Root cause:

- The listener used `onIdTokenChanged`, which Firebase fires on every hourly token refresh AND on every page reload.
- Dev HMR + React StrictMode double-mount AuthProvider, doubling sync calls per reload.
- Backend caps `/auth/sync` at **30/min per principal** (`RateLimitFilter.java:50`).
- Bucket drains → 429 → listener catches → `signOut()` → fresh sign-in re-triggers sync → still 429. Cascade.

Fix in `auth-context.tsx`:
- Switched listener to `onAuthStateChanged` (only fires on sign-in / sign-out, not on token refresh).
- Added `syncedUidRef` to dedupe `hydrateSession` calls per uid — remounts and StrictMode no longer re-hit `/auth/sync`.
- `logout()` clears the ref so next sign-in re-syncs.
- Token refresh path still works via existing `client.ts:120-124` 401 → `getIdToken(true)` retry.

`login.tsx` now also redirects when `status === "loading"` after a successful sign-in so `RequireAuth`'s spinner takes over during hydrate instead of leaving the user staring at an idle login form.

## Risks / Notes

- **Session persistence regression risk:** Users who close their browser tab will be signed out. This is the trade-off for honoring the CLAUDE.md storage rule. Mobile users on Safari iOS will also re-auth more frequently. If UX feedback pushes back, revisit by either (a) relaxing the CLAUDE.md rule, or (b) implementing a custom refresh-token cookie via the backend.
- **Forgot-password enumeration:** Client-side fix only. Firebase still emits `auth/user-not-found` and the underlying email-enumeration protection setting in the Firebase console should be enabled to fully close the gap.
- **Backend role whitelist:** Client-side filter is defense-in-depth. `AuthSyncService` must continue rejecting `ADMIN` in `intendedRole`.
- **No CAPTCHA / abuse limit:** Firebase enforces `auth/too-many-requests` server-side. No client-side throttling added — out of scope.
