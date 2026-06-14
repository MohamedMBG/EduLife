# Gamification Shared Spec — Android + Web Alignment

## Goal

Unify gamification concepts across Android (`app/`) and Web (`guided-journey-lab/`) so a learner with the same activity sees the same XP, level, streak, and badges on both platforms. Document the canonical spec in root `CLAUDE.md` and refactor both clients to match.

## What Changed

### Spec

- Root `CLAUDE.md`: removed `gamification` from MVP "Do not build" list. Added `## Gamification (Shared Spec)` section with canonical XP events, level thresholds, level names, streak rules, badge table, storage rules, and a hard rule that constants must not diverge across platforms.
- `guided-journey-lab/CLAUDE.md`: removed `gamification` from "What NOT To Build" list. Added `## Gamification (Shared Spec)` pointer section deferring to the root spec.

### Canonical constants (now identical on Android + Web)

- XP events: LESSON=25, COURSE=100, EXAM_PASS=150, CERTIFICATE=200, ENROLLMENT=10, DAILY_LOGIN=5, STREAK_3_BONUS=+30, STREAK_7_BONUS=+75.
- Level thresholds: `[0, 250, 600, 1100, 1800, 2700, 3900, 5500, 7500, 10000]`.
- Level names (1→10): Novice, Curious, Explorer, Seeker, Thinker, Achiever, Scholar, Expert, Sage, Master.
- Badges (12): `first_flame`, `bookworm`, `speed_run`, `sharp_mind`, `graduate`, `on_a_roll`, `dedicated`, `star_learner`, `scholar`, `master`, `trophy_hunter`, `inferno`. Same unlock conditions, same rarity tiers on both platforms.

### Android

- `XpEngine`: replaced nature-themed level names + thresholds with shared spec. Replaced 8 nature-themed badges with the 12 spec badges. Added `BADGE_*` id constants matching web `badge.key` values exactly. Added public `reconcileBadges()` that purges legacy badge ids and retroactively unlocks any spec badges already satisfied by current counters.
- `Badge` model: added `BadgeRarity` field (`COMMON | RARE | EPIC | LEGENDARY`).
- `BadgeRarity` (new enum).
- `GamificationPreferences`: added `lessonsTodayCount` (auto-resets per day) for Speed Run badge, `lessonDates` rolling set (90-day cap) for On A Roll badge, and `longestStreak` (auto-maintained when `setStreak` increases) for Dedicated/Star Learner/Inferno badges. Added `purgeLegacyBadges()` to drop the 6 retired badge ids from existing `earned_badges` sets.
- `GamificationViewModel`: calls `engine.reconcileBadges()` once at construction so existing learners migrate on first launch after upgrade.
- `GamificationFragment`: updated local `LEVEL_TITLES` + `LEVEL_XP` arrays used by the level reference table to the shared spec.
- `ExamResultFragment`: when a certificate is issued, now also fires `XpEvent.COURSE_COMPLETE` (between `EXAM_PASS` and `CERTIFICATE_EARNED`). This matches the web `XP_PER_CERTIFICATE_BUNDLE = 200 + 150 + 100 = 450` so a cert issuance awards the same total XP on both platforms.
- `strings.xml`: updated `gamification_levels_subtitle` ("Seedling to Luminary" → "Novice to Master").

### Web

- `level-types.ts`: replaced `LESSON_XP=50, CERT_XP=500` with full event-driven XP constants (`XP_LESSON_COMPLETE`, `XP_COURSE_COMPLETE`, `XP_EXAM_PASS`, `XP_CERTIFICATE`, `XP_ENROLLMENT`, `XP_DAILY_LOGIN`, `XP_STREAK_3_BONUS`, `XP_STREAK_7_BONUS`). Added `XP_PER_CERTIFICATE_BUNDLE = CERTIFICATE + EXAM_PASS + COURSE_COMPLETE = 450` because the web client only sees the certificate signal (cert issuance implies all three server-side events). Kept `LESSON_XP` / `CERT_XP` as aliases for one in-file consumer (`LevelStates`).
- `level-types.ts`: switched all `BADGE_DEFS[].key` values from hyphenated to underscore form (`first-flame` → `first_flame`, etc.) so ids match Android.
- `routes/level.tsx`: rewrote `deriveState` XP formula to event-driven sum (`lessons × 25 + certs × 450 + enrollments × 10 + streak3Runs × 30 + streak7Runs × 75`). Added `countStreakBonuses` helper that scans completion-date runs and counts how many runs crossed each bonus threshold. Updated weekly chart, today XP, and activity feed XP rows to use the new constants. Updated all badge id literals to underscore form.

## Files Touched

- `CLAUDE.md`
- `guided-journey-lab/CLAUDE.md`
- `app/src/main/java/com/baghdad/edulife/features/gamification/model/Badge.java`
- `app/src/main/java/com/baghdad/edulife/features/gamification/model/BadgeRarity.java` (new)
- `app/src/main/java/com/baghdad/edulife/features/gamification/data/XpEngine.java`
- `app/src/main/java/com/baghdad/edulife/features/gamification/data/GamificationPreferences.java`
- `app/src/main/java/com/baghdad/edulife/features/gamification/ui/GamificationFragment.java`
- `app/src/main/java/com/baghdad/edulife/features/gamification/viewmodel/GamificationViewModel.java`
- `app/src/main/java/com/baghdad/edulife/features/courses/ui/ExamResultFragment.java`
- `app/src/main/res/values/strings.xml`
- `guided-journey-lab/src/components/level/level-types.ts`
- `guided-journey-lab/src/routes/level.tsx`

## Backend Impact

None. Gamification remains client-side on both platforms. Backend has no XP/level/badge tables. Spec documents that if a backend gamification API ships later, both clients must switch to it together.

## Android Impact

- 8 old badges (`first_steps`, `champion`, `on_fire`, `unstoppable`, `certified`, `polymath`, plus retained `bookworm`/`dedicated`) replaced by 12 spec badges. Existing learners' SharedPreferences `earned_badges` set will hold old ids — those become orphaned (not displayed in the 12-badge grid). Counts (lessons/courses/etc.) and total XP are preserved.
- Level number can shift down for existing learners because thresholds widened (e.g. 500 XP was level 5 "Flourishing", now level 3 "Explorer").
- Badge icons reuse existing drawables (`ic_badge_first_steps`, `ic_badge_bookworm`, `ic_badge_champion`, `ic_badge_on_fire`, `ic_badge_certified`, `ic_badge_polymath`, `ic_badge_dedicated`, `ic_badge_unstoppable`) mapped semantically.

## Web Impact

- XP for existing learners increases (1 lesson now 25 instead of 50, but 1 cert now 450 instead of 500, plus enrollments now contribute 10 XP each, plus streak bonuses).
- Level computation unchanged (thresholds identical to old spec).
- Badge ids switched from hyphen to underscore — any persisted client state keyed by old ids would orphan, but web does not persist `earnedBadges`; it derives them on each render, so this is a no-op for users.

## Architecture Compliance

- No new backend endpoints, no new migrations.
- Constants centralized in `XpEngine` (Android) and `level-types.ts` (Web).
- `CLAUDE.md` spec table is the single source of truth; both clients reference it in comments.

## Tests / Verification

- Android: `./gradlew :app:compileDebugJavaWithJavac` → BUILD SUCCESSFUL.
- Web: `bun x tsc --noEmit` → exit 0.
- No unit tests added (existing repo has none for gamification).

## Risks / Notes

- Daily login XP (5) is unreachable on web for now — no login-event signal exists in the API. Documented in spec; will become reachable when backend exposes a login-event endpoint or when both clients use backend gamification state.
- Streak bonus counting on web is derived (scans run lengths) and is awarded once per completed run; on Android it is flag-based (`streak3Awarded`/`streak7Awarded`). Both implementations agree on the rule "one bonus per streak run", but if a learner has multiple historical 3+ day runs on web, all count toward XP, whereas on Android only the current/most-recent run with active flags counts.
- Existing Android learners' legacy badge ids (`first_steps`, `champion`, `on_fire`, `unstoppable`, `certified`, `polymath`) are wiped on first launch after upgrade via `purgeLegacyBadges()`. Spec badges are then retroactively unlocked from current counters via `reconcileBadges()`.
- Backend gamification module is the proper long-term home for this state. Out of scope for this change.
