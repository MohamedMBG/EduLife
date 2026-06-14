# Android Gamification System

## Goal

Add a client-side gamification system (XP, levels, streaks, badges) to the EduLife Android app so learners are rewarded for meaningful learning actions. The feature is local-first (SharedPreferences) and requires no backend changes. Per explicit instruction, this overrides the AGENTS.md MVP scope constraint and is implemented as a self-contained `features/gamification` module.

## What Changed

- New `features/gamification` module wired end-to-end (data, engine, viewmodel, ui).
- Bottom navigation expanded from 4 to 5 tabs with the new "Achievements" tab between Planner and Profile.
- Home screen surfaces a gamification quick card showing current level, total XP, and active streak.
- XP awards fire from three call sites: lesson completion, exam pass + certificate earned, and new course enrollment. Each award shows a brief toast with the XP delta, a level-up toast on level boundary crossings, and a badge toast for newly unlocked badges.

### XP table

| Action               | XP   |
|----------------------|------|
| Enrollment           | +10  |
| Lesson complete      | +25  |
| Course complete      | +100 |
| Exam pass            | +150 |
| Certificate earned   | +200 |
| Daily login          | +5   |
| 3-day streak bonus   | +30  |
| 7-day streak bonus   | +75  |

### Level path

Seedling → Sprout → Sapling → Blooming → Flourishing → Thriving → Scholar → Sage → Master → Luminary, at XP thresholds `{0, 50, 150, 350, 600, 1000, 1500, 2200, 3200, 5000}`.

### Badges

`first_steps`, `bookworm`, `champion`, `on_fire`, `unstoppable`, `certified`, `polymath`, `dedicated` — earned automatically by `XpEngine.evaluateBadges()` after each XP event.

### Streak rules

A "streak day" is any learning action (lesson complete, course complete, exam pass, certificate earned, enrollment). The streak increments only when the previous activity was on the calendar day before. Same-day re-actions are a no-op; gaps reset the streak to 1 and clear the 3/7-day bonus flags so they can be re-earned on the next run.

## Files Touched

### New files

- `app/src/main/java/com/baghdad/edulife/features/gamification/ui/GamificationFragment.java`
- `app/src/main/java/com/baghdad/edulife/features/gamification/ui/XpToastHelper.java`
- `app/src/main/res/drawable/bg_home_quick_card_gamification.xml`
- `docs/2026-06-13-android-gamification-system.md`

### Pre-existing scaffold (kept as-is)

The branch already contained the data layer, engine, models, viewmodel, badge adapter, fragment/item layouts, all badge icons, header/progress/streak drawables, and `ic_nav_gamification`. The work in this task was to add the missing UI fragment, toast helper, app-wide wiring, and integration points.

### Modified

- `app/src/main/res/values/strings.xml` — added ~30 gamification strings; also added 6 pre-existing missing `career_advisor_*` strings the resource compiler complained about (unrelated to gamification but blocked the build).
- `app/src/main/res/values/colors.xml` — added `gamification_xp_gold`, `gamification_streak_orange`, `gamification_badge_locked`, `gamification_level_ring`, `gamification_header_start`/`end`, `gamification_toast_bg`.
- `app/src/main/res/menu/bottom_nav_menu.xml` — inserted Achievements tab.
- `app/src/main/res/navigation/nav_graph.xml` — added `gamificationFragment` destination and `action_homeFragment_to_gamificationFragment`.
- `app/src/main/res/layout/fragment_home.xml` — added gamification quick card below the planner card.
- `app/src/main/java/com/baghdad/edulife/MainActivity.java` — extended `tabDestinations` array to 5 entries and updated the destination-changed listener so the new tab maps to active index 3 and Profile shifts to 4.
- `app/src/main/java/com/baghdad/edulife/features/courses/ui/HomeFragment.java` — bound the gamification card; reads level/XP/streak directly from `GamificationPreferences`/`XpEngine`.
- `app/src/main/java/com/baghdad/edulife/features/courses/ui/LessonPlayerFragment.java` — awards `LESSON_COMPLETE` XP exactly once per lesson via the `completed` observer.
- `app/src/main/java/com/baghdad/edulife/features/courses/ui/ExamResultFragment.java` — awards `EXAM_PASS` and (when a certificate number is present) `CERTIFICATE_EARNED`. Guarded by `savedInstanceState == null` so rotation does not re-award.
- `app/src/main/java/com/baghdad/edulife/features/courses/ui/EnrollCourseFragment.java` — awards `ENROLLMENT` only on the fresh-enroll branch (not on a 409 already-enrolled response).

## Backend Impact

None. Strictly client-side. No new endpoints, DTOs, migrations, or auth changes.

## Android Impact

- Bottom nav now has 5 tabs. Devices with very small screens may compress the labels — the existing `SmoothBottomBar` handles this gracefully.
- `GamificationPreferences` writes to a new SharedPreferences file (`edulife_gamification`). It does not touch existing prefs.
- XP awards are fire-and-forget; they never block the user flow, never surface errors, and never gate UI navigation.

## Web Impact

None.

## Architecture Compliance

- Feature-first MVVM: the module follows the existing `data/`, `model/`, `ui/`, `viewmodel/` split.
- Java + XML only; no Kotlin, no Hilt.
- No business logic in UI classes — `XpEngine` is the single source of truth for XP/level/badge/streak rules.
- Reuses brand color tokens (`brand_primary`, `brand_text_*`, etc.) for theme consistency.
- The MVP scope override is explicit per the task brief; the audit doc is the record.

## Tests / Verification

- `./gradlew :app:compileDebugJavaWithJavac` passes.
- `./gradlew :app:assembleDebug` passes (full APK built).
- Manual verification still pending on device for: bottom-nav tab switching, XP toast on lesson complete, badge unlock visuals, streak day rollover, max-level state (XP ≥ 5000), Home card values after a session.

## Risks / Notes

- Local-only storage means XP/badges reset on reinstall or device switch. Acknowledged in the task brief; a future sprint can sync to backend.
- Two XP awards (`EXAM_PASS` + `CERTIFICATE_EARNED`) fire back-to-back on a successful first-attempt exam pass with certificate. They show as two stacked toasts. Acceptable for MVP; a future iteration could batch them into a single combined notification.
- `XpToastHelper.award()` constructs `XpEngine` + `GamificationPreferences` on each call. Cheap (SharedPreferences read) but if XP events become high-frequency we should switch to a process-wide singleton.
- The 5-tab bottom bar reduces per-tab width. If usability suffers, fallback is to drop the Achievements tab and keep only the Home quick-card entry point (the nav graph already supports both).
