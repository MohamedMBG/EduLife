# Task Audit - Fix Web Avatar CSP URL

## Date
2026-06-22

## Task Summary
Fixed the web app avatar rendering issue where the browser blocked uploaded profile images because the backend response contained a `http://localhost:8080/...` avatar URL that violated the website Content Security Policy.

## Files Created
- docs/2026-06-22-fix-web-avatar-csp-url.md

## Files Modified
- guided-journey-lab/src/lib/api/client.ts

## What Was Done
Added backend media URL normalization inside the shared web API client. The client now rewrites backend-hosted media URLs such as `avatarUrl`, `imageUrl`, `coverImageUrl`, and `pdfUrl` when they point at localhost-style origins, mapping them onto the configured `VITE_API_BASE_URL` origin before the UI renders them.

This keeps the production CSP strict while preventing the profile page and other media consumers from trying to load blocked `localhost` assets returned by backend fallback configuration. The normalization is applied centrally at the API response boundary so the fix covers all screens using those response fields instead of patching a single component.

## Architecture Compliance
The change stays inside the web app data access layer at `guided-journey-lab/src/lib/api/client.ts`, which is the correct place for transport and response-shaping concerns. No UI business logic was moved into route components, and no backend or Android architecture was changed.

## Code Comments Added
Added a focused comment in `guided-journey-lab/src/lib/api/client.ts` explaining why localhost media URLs are rewritten to the configured API origin: the backend can fall back to localhost public URLs when its media base URL environment variable is unset, and the website must avoid rendering CSP-blocked localhost assets.

## Validation / Testing
Ran `npm run build` inside `guided-journey-lab/`. The production build completed successfully.

## Risks / Notes
This is a safe client-side guardrail, but the backend should still be configured correctly in each environment. For a deployed backend, `EDULIFE_AVATAR_PUBLIC_BASE_URL` (and similar media base URL settings) should point to the real public backend origin so API responses stop emitting `localhost` asset URLs.
