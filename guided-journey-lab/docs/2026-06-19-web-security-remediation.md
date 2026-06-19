# Web Security Audit Remediation

## Goal

Apply the fixes from the 2026-06-19 OWASP web security audit of the EduLife web app
(`guided-journey-lab/`): security headers + CSP, sanitize the AI advisor HTML sink,
enforce HTTPS on the API base URL, default demo mode off, patch dev-tooling
dependencies, and clear local/private state on logout.

## What Changed

- **P2-1 — Security headers + CSP (`vercel.json`).** Added a global `/(.*)` header block:
  `Content-Security-Policy`, `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`,
  `Referrer-Policy: strict-origin-when-cross-origin`, `Permissions-Policy`,
  `Strict-Transport-Security`, and `Cross-Origin-Opener-Policy: same-origin-allow-popups`.
  The CSP keeps `script-src 'self' 'unsafe-inline'` because TanStack Start prerenders
  build-dynamic inline scripts (the `$tsr` stream barrier + hydration bootstrap) and a static
  host (Vercel) cannot inject a per-request nonce; everything else is locked down
  (`default-src 'self'`, `object-src 'none'`, `base-uri 'self'`, `frame-ancestors 'none'`,
  `form-action 'self'`, `upgrade-insecure-requests`, scoped `connect-src`/`img-src`/`font-src`).

- **P2-2 — Sanitized the AI advisor HTML sink (`src/routes/advisor.tsx`).** `highlightKeyTerms`
  now HTML-escapes the (untrusted, Groq-generated) advisor text **before** injecting the
  `<strong>` highlight markup, so a prompt-injected/echoed `<img onerror=…>`-style payload
  renders as inert text instead of executing through `dangerouslySetInnerHTML`.

- **P2-3 / P1-1 — HTTPS enforcement + demo default (`src/lib/env.ts`, `.env.example`).**
  Added `getInsecureApiBaseUrlError()`, wired into `getEnvConfigurationError()`: outside demo
  mode, a non-HTTPS `VITE_API_BASE_URL` (except `localhost`/`127.0.0.1`) is now a hard
  configuration error, so production cannot silently ship cleartext token traffic.
  `.env.example` now defaults `VITE_DEMO_MODE=false` with a comment that demo mode disables real
  auth/backend.

- **P3-1 — Dev-tooling dependencies (`package-lock.json`).** `npm audit fix` reduced the
  advisory count from 16 (6 high) to 1 low. The remaining `esbuild` dev-server advisory
  (Windows, low) requires a breaking major bump and is left for a deliberate upgrade.

- **P3-4 — Logout clears local private data + query cache (`src/lib/auth/auth-context.tsx`).**
  `logout()` now removes every `edulife_`-prefixed `localStorage` key (planner, lesson notes,
  advisor briefs, pending registration role) and calls `queryClient.clear()`. The `edulife-dark`
  theme preference is preserved; the demo store is left to `demoLogout`.

- **P3-3 — `public/robots.txt`.** Disallows the authenticated route trees
  (`/dashboard`, `/teach`, `/groups`, `/admin`, `/profile`, …) while allowing the landing,
  auth, and public certificate-verification pages.

## Files Touched

- `guided-journey-lab/vercel.json`
- `guided-journey-lab/src/routes/advisor.tsx`
- `guided-journey-lab/src/lib/env.ts`
- `guided-journey-lab/.env.example`
- `guided-journey-lab/src/lib/auth/auth-context.tsx`
- `guided-journey-lab/public/robots.txt` (new)
- `guided-journey-lab/package-lock.json` (`npm audit fix`)

## Backend Endpoints Used

None changed. The HTTPS enforcement affects how the existing `VITE_API_BASE_URL` is validated
before any `api/v1/*` call is made.

## Design Tokens Used

None (security/config changes only; no UI tokens added).

## States Handled

- [x] Loading — unchanged
- [x] Error — insecure/missing env now surfaces a clear configuration error
- [x] Empty — unchanged
- [x] Success — unchanged

## Dark Mode Tested

N/A (no UI changes; the `edulife-dark` preference is explicitly preserved through logout).

## TypeScript Errors

None — `npx tsc --noEmit` passes; `npm run build` passes; ESLint clean on changed files
(2 pre-existing `react-refresh` warnings remain).

## Risks / Notes

- **Deployment verification required:** confirm the Vercel production env has
  `VITE_DEMO_MODE` unset/`false` and `VITE_API_BASE_URL` set to the `https://…onrender.com`
  origin. The code now fails closed on a cleartext non-local API URL, but the env value itself
  lives in the Vercel dashboard.
- **CSP `script-src 'unsafe-inline'`** is a deliberate tradeoff for a static-hosted SSR-hydrated
  SPA. To reach a strict (nonce-based) CSP, the app would need to serve the shell from an SSR
  function that injects a per-request nonce. The advisor XSS sink (the actual risk) is fixed
  independently in P2-2.
- **`bun.lock` not updated** — `npm audit fix` only touched `package-lock.json`, which is what
  Vercel uses (`installCommand: "npm install"`). Run `bun install` locally to resync `bun.lock`.
- **1 low dev-only advisory remains** (`esbuild` dev server, Windows) — needs a breaking bump;
  tracked for a deliberate upgrade.
