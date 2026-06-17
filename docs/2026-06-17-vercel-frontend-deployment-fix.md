# Vercel Frontend Deployment Fix

## Goal

Make the EduLife web frontend (`guided-journey-lab/`) deployable on Vercel as a static SPA, without breaking local development or rewriting the frontend.

## Root Cause

The project uses TanStack Start (SSR framework) with `@lovable.dev/vite-tanstack-config`, which automatically adds `@cloudflare/vite-plugin` during production builds. This produces a Cloudflare Workers bundle that Vercel cannot serve.

The app does **not** use any TanStack Start server functions (`createServerFn`), so SSR provides no functional value — the app is purely client-side routing with TanStack Router.

## What Changed

1. **Disabled Cloudflare build plugin** — Added `cloudflare: false` to vite.config.ts. Build now produces a standard client + server bundle without Cloudflare Workers targeting.

2. **Created SPA HTML generator** — `scripts/generate-spa-html.mjs` reads the TanStack Start manifest after build, extracts the client entry point and CSS paths, and generates `dist/client/index.html`. This allows the client bundle to bootstrap without an SSR server.

3. **Added `build:vercel` script** — Chains `vite build` + the HTML generator in one command.

4. **Created `vercel.json`** — Configures Vercel to serve from `dist/client/` with SPA rewrites (all routes → `index.html`) and immutable caching for hashed assets.

5. **Updated `.gitignore`** — Added `.env` to prevent committing environment-specific values.

## Files Touched

- `guided-journey-lab/vite.config.ts` — added `cloudflare: false`
- `guided-journey-lab/package.json` — added `build:vercel` script
- `guided-journey-lab/vercel.json` — new file, Vercel deployment config
- `guided-journey-lab/scripts/generate-spa-html.mjs` — new file, post-build HTML generator
- `guided-journey-lab/.gitignore` — added `.env`

## Vercel Project Settings

| Setting | Value |
|---|---|
| Root Directory | `guided-journey-lab` |
| Framework Preset | Other (or Vite) |
| Install Command | `npm install` |
| Build Command | `npm run build:vercel` |
| Output Directory | `dist/client` |

## Required Environment Variables (Vercel Dashboard)

| Variable | Example |
|---|---|
| `VITE_API_BASE_URL` | `https://edulife-2bro.onrender.com` |
| `VITE_FIREBASE_API_KEY` | `AIzaSy...` |
| `VITE_FIREBASE_AUTH_DOMAIN` | `edulife-d053c.firebaseapp.com` |
| `VITE_FIREBASE_PROJECT_ID` | `edulife-d053c` |
| `VITE_FIREBASE_APP_ID` | `1:22395059629:web:...` |
| `VITE_FIREBASE_MESSAGING_SENDER_ID` | `22395059629` |
| `VITE_FIREBASE_STORAGE_BUCKET` | `edulife-d053c.firebasestorage.app` |
| `VITE_DEMO_MODE` | `false` |

Note: Firebase web config values are public by design (embedded in every client app). They are NOT secrets.

## Backend CORS

The backend reads allowed origins from `APP_CORS_ALLOWED_ORIGINS`. Add the Vercel domain:

```
APP_CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173,https://your-project.vercel.app
```

If a custom domain is used, add it too.

## Local Build Result

```
✓ client built in ~14s
✓ server built in ~4s (unused by Vercel, but harmless)
✓ index.html generated from manifest
```

All routes return 200 when served with SPA fallback.

## Manual Verification Checklist

- [x] `npm run build:vercel` succeeds
- [x] `dist/client/index.html` generated with correct asset paths
- [x] Static server with SPA fallback serves `/` → 200
- [x] Static server with SPA fallback serves `/dashboard` → 200
- [x] Static server with SPA fallback serves `/explore` → 200
- [x] CSS assets load (200)
- [x] JS entry point loads (200)
- [x] `npm run dev` still works (no regression)
- [x] No `localhost` hardcoded in source (API base URL reads from env)
- [x] No case-sensitivity issues in imports (all `@/` paths match filesystem)
- [x] No real secrets committed
- [ ] Firebase auth login flow (needs browser test on deployed site)
- [ ] API calls reach backend (needs deployed backend + CORS config)
- [ ] Cloudinary images display (needs real course data)

## Remaining Risks

1. **First paint is empty** — Since SSR is bypassed, the page shows a blank screen until the JS bundle loads and renders. This is a cosmetic tradeoff for deployment simplicity. If SEO or first-paint performance matters later, re-enable SSR with a Vercel-compatible adapter.

2. **`dist/server/` is built but unused** — The TanStack Start plugin still builds the server bundle. It adds ~4s to build time but is harmless. Could be eliminated by removing TanStack Start entirely (replacing with plain TanStack Router), but that's a larger change.

3. **Backend CORS must be updated** — The deployed backend must allow the Vercel origin. Without this, all API calls will fail with CORS errors.

4. **`package-lock.json` must be in sync** — Vercel uses `npm install`. If dependencies were added via `bun`, run `npm install` locally once to update the lockfile.
