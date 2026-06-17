# Fix Vercel Blank Page — Enable TanStack Start SPA Mode

## Goal
Deployed web app at https://guided-journey-lab.vercel.app/ served a blank page
(HTTP 200, no visible content, no console errors). Make the app actually render
on Vercel's static hosting.

## Root Cause
TanStack Start is **SSR-first**: its client entry calls `hydrateRoot`, which
expects server-rendered markup in `<body>` to hydrate. The previous Vercel
pipeline used a custom `scripts/generate-spa-html.mjs` that wrote an
`index.html` with an **empty `<body>`** containing only the module script tag.
`hydrateRoot` against an empty body silently bails — React mounts nothing →
blank page, no error.

Asset/CSS/HTML all returned 200; the failure was purely the hydration model
mismatch.

## What Changed
- Enabled TanStack Start **SPA mode** (`spa: { enabled: true }`) in
  `vite.config.ts`. The build now prerenders a proper hydratable shell to
  `dist/client/_shell.html` (contains `$_TSR.router` bootstrap + stream barrier
  + module import). Hydration succeeds, then the client router renders routes.
- `vercel.json`: rewrite destination changed `/index.html` → `/_shell.html`;
  build command changed `npm run build:vercel` → `npm run build`.
- Removed `scripts/generate-spa-html.mjs` (the broken empty-body generator) and
  the `build:vercel` npm script.

## Files Touched
- guided-journey-lab/vite.config.ts
- guided-journey-lab/vercel.json
- guided-journey-lab/package.json
- guided-journey-lab/scripts/generate-spa-html.mjs (deleted)

## Backend Endpoints Used
None (deployment/config only).

## Design Tokens Used
None.

## States Handled
- [ ] Loading
- [ ] Error
- [ ] Empty
- [ ] Success

N/A — config/deploy fix, no UI added.

## Dark Mode Tested
N/A.

## TypeScript Errors
None.

## Verification
- `vite build` → prerender step logs `Prerendered 1 pages: - /`, writes
  `dist/client/_shell.html` (hydratable, ~3.5 KB shell).
- Local static server simulating Vercel rewrites (existing files served
  directly, all other paths → `_shell.html`):
  - `GET /` → 200, shell contains `$_TSR.router` hydration bootstrap.
  - `GET /login` → 200 (deep route falls back to shell; client router renders).
  - `GET /assets/index-*.js` → 200 `application/javascript` (real asset, not
    rewritten).

## Risks / Notes
- SPA mode prerenders only the root shell, not per-route content — fine, the app
  is client-rendered via react-query.
- Vercel applies `rewrites` only to paths with no matching static file, so
  `/assets/*` are not swallowed by the `/(.*) → /_shell.html` rewrite.
- Env vars are baked at build time (Vite). Ensure `VITE_*` are set in Vercel
  Production **before** the build runs, then redeploy.
- After it renders: backend CORS (`APP_CORS_ALLOWED_ORIGINS` on Render) must
  include the Vercel domain, and Firebase Auth → Authorized domains must include
  `guided-journey-lab.vercel.app`.
