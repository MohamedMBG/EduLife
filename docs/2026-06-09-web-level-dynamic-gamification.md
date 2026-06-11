# Web · Dynamic Gamification on /level

## Goal
Replace the static gamification UI on `src/routes/level.tsx` with values derived from real backend data. The page previously rendered hard-coded XP, level, streak, weekly chart, leaderboard, badges, and quests.

## What Changed
- Refactored `LevelPage` to a `RequireAuth`-wrapped route that uses the shared `AppShell` (same pattern as `dashboard.tsx`).
- Replaced the bespoke sidebar/topbar with `AppShell` so the user name/email/initials come from the live `useAuth()` session.
- Pulls real data via TanStack Query:
  - `getProfile` (profile counters)
  - `listMyEnrollments` (enrolled courses)
  - `listMyCertificates` (issued certificates with `issuedAt`)
  - `getCourseProgress` per enrollment via `useQueries` (per-lesson `completedAt`)
- Added a derivation layer (`deriveState`) that computes:
  - `totalXp` = `completedLessons * 50 + certificates * 500`
  - `level` (1–10) from a fixed cumulative XP threshold table, with `xpInto` / `xpRequired` per level
  - Current `streak` (allows today to be empty before midnight) and `longestStreak`
  - Mon–Sun XP histogram driven by `completedAt` and `issuedAt` dates
  - `streakDays` calendar with `today` highlighting based on real local date
  - `earnedBadges` (12 badges) from real milestones — lessons completed, single-day count, rolling 7-day count, longest streak, certificates earned, level reached
  - `recentActivity` feed merging recent completed lessons and certificate events, sorted desc
- Replaced the fake `leaderboard` panel (which is not derivable from any backend endpoint) with a Recent Activity panel that lists the user's most recent XP events with relative timestamps.
- Quests now reflect real progress:
  - Daily Warrior — lessons completed today (0–3)
  - Knowledge Seeker — any certificate earned
  - Streak Keeper — current streak vs 7
- Skill Tree current/locked/mastered nodes follow the derived `level`.
- Added loading, error, and empty states. Empty state CTA links to `/explore`.

## Files Touched
- `guided-journey-lab/src/routes/level.tsx`
- `docs/2026-06-09-web-level-dynamic-gamification.md`

## Backend Endpoints Used
- `GET /api/v1/profile`
- `GET /api/v1/enrollments/me`
- `GET /api/v1/certificates/me`
- `GET /api/v1/progress/courses/{courseId}` (one call per enrollment via `useQueries`)

No new endpoints required. XP/level/streak/badges are derived client-side from existing data; they are not persisted on the backend.

## Design Tokens Used
No new tokens. Reuses existing tokens: `bg-surface-elevated`, `border-border`, `bg-gradient-primary`, `text-primary`, `text-amber-500`, `text-teal`, `shadow-glow`, `text-display`.

## States Handled
- [x] Loading — skeleton stack while queries pending
- [x] Error — destructive-tinted banner at top with first error message
- [x] Empty — when user has no completed lessons or certificates, shows CTA to `/explore`
- [x] Success — full dynamic dashboard

## Dark Mode Tested
N/A — kept all existing dark-mode token usage from the previous static UI; no new color literals introduced.

## TypeScript Errors
`bun x tsc --noEmit` reports no new errors on `level.tsx`. Pre-existing errors in `certificates.index.tsx` and `courses.$courseId.index.tsx` are unchanged and unrelated to this task.

## Risks / Notes
- XP economy (50/lesson, 500/cert) and level thresholds are client-side constants. If the product later needs a server-owned XP system (anti-cheat, cross-device parity), this must be moved into the backend and exposed via a new endpoint.
- Streak is computed in the browser's local timezone; users crossing midnight in another timezone may see a small drift. Acceptable for the MVP.
- Loads one `GET /api/v1/progress/courses/{courseId}` request per enrollment. For learners with many enrollments this could be chatty; consider a future bulk endpoint if it becomes a problem.
- Leaderboard was removed entirely instead of mocked, per CLAUDE.md "no fake data" rule. Re-add only after a real leaderboard endpoint exists.
- "Sharp Mind" badge is awarded on any certificate (proxy for passing an exam) because we do not expose per-exam scores in `/profile`. Refine when exam history is surfaced.
