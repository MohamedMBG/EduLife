# Web Profile + Password Reset (Sprint W2)

## Goal

Add learner profile editing (display name, bio, avatar upload) and a
Firebase-backed password-reset flow to `guided-journey-lab`. Wire the
"Forgot password?" link on the login page.

## What Changed

- **API types**: added `UpdateProfileRequest` and `AvatarUploadResponse`
  to `src/lib/api/types.ts` to match the backend `ProfileController`
  contract.
- **API client**: added `updateProfile` (`PUT /api/v1/profile`) and
  `uploadAvatar` (`POST /api/v1/profile/avatar`, multipart). Demo mode
  rejects both with a 501 — no fake profile mutations.
- **Profile page** (`src/routes/profile.tsx`):
  - Fetches profile via existing `getProfile`.
  - Form pre-fills display name + bio from the response.
  - Avatar upload enforces 5MB and image-only client-side before sending.
  - Save and upload feedback show inline (success / error states).
  - Invalidates `["profile"]` after either mutation succeeds.
- **Forgot password page** (`src/routes/forgot-password.tsx`):
  - Uses the existing `getFirebaseAuth` / `getFirebaseAuthModule` helpers
    so the Firebase SDK stays code-split.
  - Calls `sendPasswordResetEmail`. On success shows "Check your inbox"
    without revealing whether the address exists.
  - Demo mode blocks submission with an inline error.
- **Login link**: replaced the `href="#"` placeholder with a typed
  `<Link to="/forgot-password">`.
- **AppShell**: sidebar user block now navigates to `/profile`. The logout
  button still sits outside the link area.

## Files Touched

- `guided-journey-lab/src/lib/api/types.ts`
- `guided-journey-lab/src/lib/api/client.ts`
- `guided-journey-lab/src/routes/profile.tsx` (new)
- `guided-journey-lab/src/routes/forgot-password.tsx` (new)
- `guided-journey-lab/src/routes/login.tsx`
- `guided-journey-lab/src/components/app/AppShell.tsx`
- `guided-journey-lab/src/routeTree.gen.ts` (auto-regenerated)

## Backend Endpoints Used

- `GET /api/v1/profile`
- `PUT /api/v1/profile`
- `POST /api/v1/profile/avatar`

## Design Tokens Used

- `bg-surface-elevated`, `border-border`, `shadow-soft`, `shadow-elevated`
- `bg-gradient-primary`, `text-primary-foreground`
- `bg-primary/8`, `border-primary/20` (success cards)
- `bg-destructive/8`, `border-destructive/20`, `text-destructive`
- `bg-hero-gradient`, `bg-gradient-aurora`, `animate-glow`
  (forgot-password background)
- No hardcoded colors.

## States Handled

- [x] Loading — profile query renders `StateCard`; submit/upload buttons
  show "Saving..." / "Uploading...".
- [x] Error — query, update, upload, and Firebase reset surface inline
  messages.
- [x] Empty — profile route guards against `data === undefined`;
  forgot-password renders the form when no email has been sent.
- [x] Success — "Profile updated.", "Avatar updated.", and the
  "Check your inbox" confirmation card.

## Dark Mode Tested

N/A — relies on token-based styling that already responds to `.dark`. No
hardcoded colors added. Manual dark-mode smoke test deferred to Sprint W3.

## TypeScript Errors

None introduced. Pre-existing strict-null errors elsewhere unchanged.

## Risks / Notes

- Avatar upload enforces 5MB and `image/*` client-side, but the backend
  is still the authority. Server-side rejection messages bubble up via
  `ApiClientError`.
- `sendPasswordResetEmail` returns success without distinguishing
  unknown emails — intentional to avoid account enumeration.
- The profile route appears under the "Home" sidebar entry (no dedicated
  nav item) because adding nav slots requires changing the `AppShell`
  `active` type. Profile navigation happens through the avatar / name in
  the sidebar footer.
- `demoMode` rejects all profile mutations. Demo profile remains read-only.
