# Task Audit - Public Homepage Midnight Minimalist Redesign

## Date
2026-06-17

## Task Summary
Redesigned the public visitor homepage in the EduLife web app to match the provided Midnight Minimalist reference while keeping login, registration, backend logic, API contracts, and authenticated dashboards unchanged.

## Files Created
- guided-journey-lab/src/components/landing/PublicLandingPage.tsx
- guided-journey-lab/src/components/landing/PublicNavbar.tsx
- guided-journey-lab/src/components/landing/PublicHeroSection.tsx
- guided-journey-lab/src/components/landing/PublicConflictSection.tsx
- guided-journey-lab/src/components/landing/PublicMethodologySection.tsx
- guided-journey-lab/src/components/landing/PublicCertificatesSection.tsx
- guided-journey-lab/src/components/landing/PublicWaitlistCTA.tsx
- guided-journey-lab/src/components/landing/PublicMobileLearningSection.tsx
- guided-journey-lab/src/components/landing/PublicFooter.tsx
- docs/2026-06-17-public-homepage-midnight-minimalist-redesign.md

## Files Modified
- guided-journey-lab/src/routes/index.tsx
- guided-journey-lab/src/routeTree.gen.ts

## What Was Done
- Replaced the `/` file-route composition with a dedicated `PublicLandingPage` entry point for the visitor homepage.
- Built a new public navbar with section anchors, a mobile menu, and auth-aware CTAs that point guests to `/register` and signed-in users to `/dashboard`.
- Implemented the requested sections:
  - cinematic hero with metrics and a premium phone mockup
  - conflict and solution comparison
  - four-step methodology cards
  - dark credentialing section with certificate preview
  - centered admissions CTA with waitlist form fallback
  - mobile learning section
  - premium footer with contact and verify-certificate access
- Updated homepage metadata so the landing page title and description align with the redesign.
- Kept the waitlist form intentionally local and non-persistent because no real waitlist endpoint exists in the current web/backend contract.
- Used the provided `screen.png`, `code.html`, and `DESIGN.md` as the visual and system references.
- `routeTree.gen.ts` was regenerated during the web build process.

## Architecture Compliance
- The change stays inside the web UI layer under `guided-journey-lab/src/components/landing/` and the public route file `guided-journey-lab/src/routes/index.tsx`.
- No backend files, API clients, auth flows, or authenticated dashboard navigation were changed.
- The redesign is isolated to the visitor homepage, which matches the request to avoid changing logged-in app navigation and backend behavior.

## Code Comments Added
- Added a comment in `PublicCertificatesSection.tsx` to explain why the homepage certificate preview is local UI and intentionally decoupled from real PDF generation.
- Added a comment in `PublicWaitlistCTA.tsx` to explain why the admissions form remains presentational until a real waitlist endpoint exists.

## Validation / Testing
- Ran `npm run build` in `guided-journey-lab` successfully.
- Ran `npx tsc --noEmit` in `guided-journey-lab` successfully.
- Ran `npx prettier --write` on the new landing files and `src/routes/index.tsx`.
- Ran `npx eslint` on the modified landing files and `src/routes/index.tsx` successfully.
- Ran `npm run lint` repo-wide, but it still fails because of a large pre-existing Prettier backlog in unrelated files outside this homepage task.
- Started the local dev server successfully on `http://127.0.0.1:4173/`.
- Attempted an in-app browser verification pass, but this Codex session did not expose an active `iab` browser instance, so no final visual browser inspection could be completed through the Browser plugin.

## Risks / Notes
- `npm run lint` is not green at repository scope because of unrelated formatting issues already present across many files.
- `guided-journey-lab/src/routeTree.gen.ts` changed as part of route generation during build; this was not hand-edited.
- The waitlist CTA currently shows a toast instead of submitting data because no backend endpoint exists yet.
- A browser-based visual pass still needs to be performed manually once an in-app browser handle is available or from a normal local browser session.
