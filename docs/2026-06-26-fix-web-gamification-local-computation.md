# Fix Web Gamification Local Computation

## Goal

Replace local XP/level/streak/badge derivation in `level.tsx` with authoritative
data from `GET /api/v1/gamification/me`. Fixes the shared-spec violation where the
web computed progression independently of the backend, causing divergence from Android.

## What Changed

- `src/routes/level.tsx`: Removed `deriveLevel`, `computeStreak`, `computeLongestStreak`,
  `countStreakBonuses`, `maxRolling7DayCount`, and `deriveState`. Added `buildLevelState`
  that maps backend `GamificationState` to `LevelState`. Added `gamificationQuery`
  (`useQuery` on `getGamificationState`). Dropped profile query (no longer needed).
  XP total, level, xpInto/xpRequired, streak, longestStreak, and earnedBadges now
  come from backend. Weekly chart and recent activity feed remain local (display only).

- `src/lib/api/client.ts`: Added `getGamificationState` function calling
  `GET /api/v1/gamification/me`.

- `src/lib/api/types.ts`: Added `GamificationBadge` and `GamificationState` interfaces
  matching `BadgeDto` and `GamificationStateDto` from the backend.

## Files Touched

- `guided-journey-lab/src/routes/level.tsx`
- `guided-journey-lab/src/lib/api/client.ts`
- `guided-journey-lab/src/lib/api/types.ts`

## Backend Endpoints Used

- `GET /api/v1/gamification/me` — authoritative XP, level, streak, badges

## Design Tokens Used

None (no UI changes).

## States Handled

- [x] Loading
- [x] Error
- [x] Empty
- [x] Success

## Dark Mode Tested

N/A — no UI changes.

## TypeScript Errors

None (`tsc --noEmit` passes clean).

## Risks / Notes

- Weekly chart (`weeklyXp`) and `xpToday`/`xpWeek` remain locally computed from
  lesson completion timestamps. These are display-only chart values, not authoritative
  progression state. The authoritative total is `gamState.totalXp`.
- Demo mode throws `ApiClientError(501)` for gamification — the level page is not
  accessible in demo mode (correct, same as exams).
- At max level (level 10), backend returns `xpForNextLevel = 0`. The builder guards
  against 0-denominator with `xpRequired = 1` and forces `xpPct = 100`.
