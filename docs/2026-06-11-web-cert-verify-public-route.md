# Certificate Public Verification Route

## Goal
Add `/certificates/verify/$hash` as a public (no-auth) landing page for QR-code
verification links embedded in printed/PDF certificates.

## What Changed
New route `certificates.verify.$hash.tsx` (nested under the passthrough `/certificates`
layout, so no auth wrapper is inherited). Calls `GET /api/v1/certificates/verify/{hash}`
with no bearer token. Renders four states: loading skeleton, 404 (hash not found),
valid: false (invalid/tampered), valid: true (gold-card verified view). Full page is
standalone — no AppShell, has its own minimal header with EduLife branding and a Home
link.

Added `CertificateVerification` type to `types.ts`, `verifyCertificate(hash)` to
`client.ts`, and `demoVerifyCertificate(hash)` stub to `demo.ts`.

## Files Touched
- `guided-journey-lab/src/lib/api/types.ts`
- `guided-journey-lab/src/lib/api/client.ts`
- `guided-journey-lab/src/lib/api/demo.ts`
- `guided-journey-lab/src/routes/certificates.verify.$hash.tsx` (new)

## Backend Endpoints Used
- `GET /api/v1/certificates/verify/{verificationHash}` — public, no auth, rate-limited

## Design Tokens Used
- `bg-gradient-gold`, `shadow-gold`, `text-gold-foreground` — verified state card
- `bg-destructive/5`, `border-destructive/30` — invalid / 404 state
- `bg-muted/60` — loading skeleton
- `bg-teal`, `shadow-soft` — branding icon

## States Handled
- [x] Loading
- [x] Error — 404 (cert not found)
- [x] Error — unexpected (network, server)
- [x] Success — valid: true
- [x] Success — valid: false (cert exists but invalid)

## Dark Mode Tested
Yes — all tokens resolve correctly under `.dark`.

## TypeScript Errors
None.

## Risks / Notes
Route is nested under `/certificates` layout which is purely a passthrough `<Outlet />` —
no auth enforcement. Backend rate-limits this endpoint at 60 req/min per IP.
