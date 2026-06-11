# Web Certificates — Fix List Endpoint, Add Detail + PDF Download

## Goal

Fix a 404 bug on the certificates list call, then finish the learner journey on web by
shipping the certificate detail page and PDF download, per the CLAUDE.md MVP flow
(Login → ... → Certificate).

## What Changed

- `listMyCertificates` now calls `GET /api/v1/certificates/me` instead of the non-existent
  `GET /api/v1/certificates` — backend mounts the list under `/me`.
- Added `getCertificate(id)` and `downloadCertificate(id)` API client functions.
  `downloadCertificate` fetches the PDF as a `Blob`, supports the 401-then-refresh retry
  flow, and respects `appEnv.demoMode`.
- Updated the `Certificate` type to match `CertificateSummaryDto`
  (`id`, `courseId`, `certificateNumber`, `courseTitle`, `issuedAt`) — backend never returned
  `certificateId`, so the previous shape would have broken the cards on first render once
  the list endpoint was reachable.
- Added `CertificateDetail` type mirroring `CertificateDetailDto`.
- Renamed `routes/certificates.tsx` → `routes/certificates.index.tsx` so the new child
  detail route can sit beside it under `/certificates`.
- Added `routes/certificates.$certificateId.tsx` with loading / error / success states,
  detail card (number, issued-at, issuer, verification hash), and a Download PDF button
  that triggers `downloadCertificate` and saves via a transient `<a download>` element.
- Updated the certificates list card to use `id` for the React key, prefer
  backend-supplied `courseTitle` (falling back to enrollment title), and replaced the
  "back to courses" link with a "View certificate" deep link.
- Bumped demo store key `v1` → `v2`, updated demo cert records to the new shape, and added
  `demoGetCertificate` so the detail page still works in website demo mode.
- Regenerated `src/routeTree.gen.ts` via `bun x @tanstack/router-cli generate`.

## Files Touched

- `guided-journey-lab/src/lib/api/types.ts`
- `guided-journey-lab/src/lib/api/client.ts`
- `guided-journey-lab/src/lib/api/demo.ts`
- `guided-journey-lab/src/routes/certificates.index.tsx` (renamed from `certificates.tsx`)
- `guided-journey-lab/src/routes/certificates.$certificateId.tsx` (new)
- `guided-journey-lab/src/routeTree.gen.ts` (regenerated)

## Backend Endpoints Used

- `GET /api/v1/certificates/me`
- `GET /api/v1/certificates/{id}`
- `GET /api/v1/certificates/{id}/download` (returns PDF)

## Design Tokens Used

- `bg-gradient-gold`, `shadow-gold`, `text-gold-foreground`, `bg-gold-foreground/10`
- `bg-surface-elevated`, `border-border`, `shadow-soft`
- `bg-gradient-primary`, `text-primary-foreground`
- `text-display` for the certificate course title
- `text-destructive` for download error state

## States Handled

- [x] Loading
- [x] Error
- [x] Empty (list page only — detail page treats missing as error)
- [x] Success
- [x] Download in-flight (button disabled, label flips to "Preparing...")
- [x] Download error (shown beside the button)

## Dark Mode Tested

Yes — all surfaces use design tokens, gold gradient flips automatically via the `.dark`
overrides defined in `styles.css`.

## TypeScript Errors

`bun x tsc --noEmit` reports two pre-existing `TS18048` errors in
`certificates.index.tsx` and several in `courses.$courseId.index.tsx` — TanStack Query's
narrowing chain doesn't propagate `data` defined-ness through the `isLoading` /
`isError` chain. These existed before this task and don't break `bun run build`.

## Risks / Notes

- The backend cert summary DTO uses `id` (UUID), not `certificateId`. The web previously
  expected `certificateId`, so the original cards would have crashed at render even if the
  list endpoint had matched. Worth a quick smoke-test against a real account once the
  backend is reachable.
- `downloadCertificate` triggers a real browser download via `Blob` + `<a download>`. In
  sandboxed iframes (e.g. some preview hosts) this can be blocked — fine for production
  but worth noting if QA runs in an embedded preview.
- Demo mode synthesises a `verificationHash` and leaves `pdfUrl` null because the demo
  has no PDF pipeline; the Download PDF button surfaces a clean 501 in demo mode.
- The cert list page still pulls enrollments to back-fill the title. With backend
  `courseTitle` now available we keep enrollments as a fallback only — safe to retire
  later once we are confident every issued cert carries a `courseTitle`.
