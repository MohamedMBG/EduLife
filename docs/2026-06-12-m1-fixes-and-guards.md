# M1 — Fixes and Auth Guards (Android + Web)

## Goal

Execute Milestone 1 from the 2026-06-12 plan: audit fixes A1.1–A1.3 (Android), W1.1–W1.4 (web), W2.1–W2.3 (web auth guards).

## What Changed

### Android

- **A1.1** Removed the non-functional "Continue with Google" placeholder from both login and register screens: click handlers in `LoginFragment`/`RegisterFragment`, the button + "OR" divider blocks in `fragment_login.xml`/`fragment_register.xml`, orphaned strings (`auth_google_unavailable`, `register_or`, `register_google_cta`), and unused drawables (`ic_google_g.xml`, `bg_login_google_button.xml`). `registerRow` in the login layout re-anchored to `loginErrorCard`.
- **A1.2** Removed duplicate `androidx.recyclerview:recyclerview:1.3.2` declaration in `app/build.gradle.kts`.
- **A1.3** `ExamResultFragment` fallback pass score corrected from hardcoded 80 to 70 (matches the seeded exam threshold; real value always arrives from the API via fragment args).

### Web

- **W1.1** Dark-mode storage unified on one key. New shared hook `src/hooks/use-dark-mode.ts` (`edulife-dark`, same key as the `__root.tsx` no-flash script). `Nav.tsx` previously used a different key (`"theme"`), causing landing/app desync — now both `Nav` and `AppShell` use the shared hook.
- **W1.3** Cloudflare worker renamed `tanstack-start-app` → `edulife-web` in `wrangler.jsonc`, with a comment warning that routes/custom domains must be re-pointed on next deploy.
- **W1.4** Landing `Certificate.tsx` claimed "80% pass score" while seeded exams use 70 — copy made threshold-neutral ("Exam / pass required") until decision 0.1 resolves the canonical threshold.
- **W1.2** `routeTree.gen.ts` working-tree modification resolved (line-ending only; no content change to commit).
- **W2.1/W2.2** Deep-link return after login: `RequireAuth` now bounces anonymous users to `/login?redirect=<original path>`; the login route validates the param (same-origin paths only — must start with `/`, not `//`) and navigates back to it after authentication. Note: a true router-level `beforeLoad` guard was deliberately NOT used — Firebase auth state is client-only, and under TanStack Start SSR a `beforeLoad` check would always see "no user" on the server and redirect incorrectly. Component-level `RequireAuth` remains the guard mechanism.
- **W2.3** 401 sweep verdict: `makeRequest` already retries once with a force-refreshed token before surfacing `ApiClientError(401)`; routes render react-query `isError` into visible state cards — no silent spinners found. Automatic sign-out + redirect on persistent 401 deferred (would require coupling the API client to the auth context).

## Files Touched

- app/src/main/java/com/baghdad/edulife/features/auth/ui/LoginFragment.java
- app/src/main/java/com/baghdad/edulife/features/auth/ui/RegisterFragment.java
- app/src/main/java/com/baghdad/edulife/features/courses/ui/ExamResultFragment.java
- app/src/main/res/layout/fragment_login.xml
- app/src/main/res/layout/fragment_register.xml
- app/src/main/res/values/strings.xml
- app/src/main/res/drawable/ic_google_g.xml (deleted)
- app/src/main/res/drawable/bg_login_google_button.xml (deleted)
- app/build.gradle.kts
- guided-journey-lab/src/hooks/use-dark-mode.ts (new)
- guided-journey-lab/src/components/landing/Nav.tsx
- guided-journey-lab/src/components/app/AppShell.tsx
- guided-journey-lab/src/components/landing/Certificate.tsx
- guided-journey-lab/src/routes/login.tsx
- guided-journey-lab/src/lib/auth/auth-context.tsx
- guided-journey-lab/wrangler.jsonc

## Backend Impact

None.

## Android Impact

Login/register screens lose the dead Google button; exam result fallback threshold aligned. No behavior change otherwise.

## Web Impact

Consistent dark mode across landing and app; deep links survive the login bounce; worker rename takes effect on next deploy (re-point routes/domains).

## Architecture Compliance

No new business logic in UI; no backend contracts changed; redirect param validated against open redirects.

## Tests / Verification

- Web: `npx tsc --noEmit` — 9 pre-existing TS18048 errors in `certificates.index.tsx` / `courses.$courseId.index.tsx` (query `.data` possibly undefined), identical before and after these changes; none in touched files.
- Android: `:app:compileDebugJavaWithJavac` run (first attempts hit a stale Gradle daemon pointing at a removed VSCode-extension JRE; resolved by `gradlew --stop`, project JDK is Zulu 21 per gradle.properties).

## Risks / Notes

- Worker rename: next `wrangler deploy` targets a new worker name — old `tanstack-start-app` worker keeps existing routes until re-pointed.
- Decision 0.1 (pass threshold 70 vs 80) still open; copy is now neutral, backend unchanged.
- Pre-existing web TS errors are W3 candidates.
