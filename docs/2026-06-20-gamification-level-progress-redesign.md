# Achievements / Level & Progress UI Redesign (Android)

## Goal

Make the learner-facing **Level & Progress** (Achievements) screen more beautiful,
gamified, interactive, and motivating while staying production-safe and fully
inside the existing Java + XML + MVVM architecture. No backend changes, no fake
data, no new libraries.

## What Changed

Redesigned `GamificationFragment` from a single static dashboard into a
multi-section, state-aware screen:

1. **Hero header** — kept the level ring + animated XP bar and added a runtime
   **motivational message** chosen from real state (start / "so close" / streak /
   next-level / max).
2. **Interactive level path** — replaced the old vertical text list of levels
   with a horizontal **milestone path** (`LevelMilestoneAdapter`): done levels
   show their number on a brand-surface node, the current level is highlighted
   with a gold ring, future levels show a lock. The path auto-scrolls to the
   learner's current level. A **"How XP & levels work"** info button opens an
   explainer dialog.
3. **Course progress cards** — new "Your Courses" section
   (`CourseProgressAdapter`) listing each enrolled course with completed/total
   lessons, an animated progress bar, a status badge (Not started / In progress /
   Almost done / Lessons complete), and one CTA (Start / Continue / Take exam)
   that opens the course. Cards have press feedback and a ≥48dp touch target.
4. **Streak + weekly activity** — the streak card now also shows **best streak**
   and a **Mon–Sun activity strip**. Active days are derived only from the real
   current-streak window `[lastActivity − (streak−1) … lastActivity]`, so no
   per-day history is fabricated.
5. **States** — added whole-screen **loading** and **error (with Retry)**
   overlays driven by the existing `isLoading` / `errorMessage` LiveData (which
   the screen previously ignored), plus **empty** and **error** states for the
   courses section.
6. **Micro-interactions** — entrance animations (kept), per-card progress-bar
   fill (animated once per course), card press ripple, badge detail dialog
   (kept), and the level info dialog.

### Backend data now surfaced (already in the response, previously dropped)

`GamificationStateResponse.longestStreak` and `lastActivityDate` are now mapped
through `GamificationUiState` so the best-streak line and weekly strip use real
values.

### ViewModel reuse / cleanup

`EnrollmentViewModel` already had an unused rich progress loader
(`loadCourseProgressFor`) alongside the wired percent-only loader. Merged them:
one loader now fills both `myCourseProgress` (full summaries → Achievements
cards) and `progressMap` (percent → Home catalog), so no consumer issues a
duplicate `/progress` request. Dead `loadProgressForEnrollments` removed.

## Files Touched

**New**
- `features/gamification/model/LevelMilestone.java`
- `features/gamification/model/CourseProgressItem.java`
- `features/gamification/ui/LevelMilestoneAdapter.java`
- `features/gamification/ui/CourseProgressAdapter.java`
- `res/layout/item_level_milestone.xml`
- `res/layout/item_course_progress.xml`
- `res/drawable/bg_milestone_current.xml`, `bg_milestone_done.xml`, `bg_milestone_locked.xml`
- `res/drawable/bg_week_day_active.xml`, `bg_week_day_inactive.xml`
- `res/drawable/ic_info_outline.xml`
- `app/src/test/.../gamification/CourseProgressItemTest.java`

**Modified**
- `features/gamification/ui/GamificationFragment.java` (rewritten)
- `features/gamification/model/GamificationUiState.java` (+ longestStreak, lastActivityDate)
- `features/gamification/data/GamificationRepository.java` (map new fields)
- `features/gamification/viewmodel/GamificationViewModel.java` (carry new fields)
- `features/courses/viewmodel/EnrollmentViewModel.java` (merge progress loaders)
- `res/layout/fragment_gamification.xml` (rewritten)
- `res/values/strings.xml` (new strings + two string-arrays)
- `res/navigation/nav_graph.xml` (action gamification → courseDetail)

## Backend Impact

None. No new endpoints, no contract changes. Consumes existing
`GET /gamification/me`, `GET /enrollments/me`, `GET /progress/courses/{id}`,
`GET /analytics/me/summary`. Backend remains the single source of truth for all
progression.

## Android Impact

Screen rewritten; reuses existing repositories, ViewModels, colors, typography,
drawables, and navigation. The `EnrollmentViewModel` loader change is shared with
Home — verified Home still receives `progressMap` percents.

## Web Impact

None.

## Architecture Compliance

- Java + XML + feature-first MVVM preserved; no Compose, no DI framework, no new deps.
- No API calls or business logic in the Fragment beyond view binding/formatting;
  network + state live in repositories/ViewModels.
- No local XP/level/streak/badge computation — all from backend.
- Status thresholds for cards are presentation-only and capped at "Lessons
  complete" (see assumptions).

## Backend / API Assumptions

- `lastActivityDate` is a date or ISO date-time string; parsed defensively
  (`LocalDate.parse`, falling back to the first 10 chars). Unparseable → no days
  marked active (safe).
- Per-course exam-pass / certificate state is **not** available on this screen,
  so cards never claim "Exam passed" / "View certificate"; a 100%-lessons course
  shows "Lessons complete" + a "Take exam" CTA that routes to the course page.
- Weekly strip marks only days provably inside the current streak window; it does
  not attempt to reconstruct older history the backend does not expose.

## Tests / Verification

- `./gradlew :app:assembleDebug` — BUILD SUCCESSFUL (no warnings).
- `./gradlew :app:testDebugUnitTest --tests CourseProgressItemTest` — passing
  (status thresholds, percent clamping, absent-progress handling).

## Manual Testing Checklist

- [ ] Open Achievements from Home → screen loads (loading overlay → content).
- [ ] Level number, title, total XP, and progress bar match `/gamification/me`.
- [ ] XP + progress + stat counters animate on first open.
- [ ] Motivational message reflects state (try near-level-up, active streak).
- [ ] Level path: past levels numbered/unlocked, current highlighted, future
      locked; path scrolls to current level; info button opens explainer.
- [ ] Course cards show real completed/total + %, correct status badge + CTA;
      tapping a card / CTA opens the course.
- [ ] Streak count, best streak, and Mon–Sun strip render; active days fall in
      the streak window.
- [ ] Tap a badge → detail dialog opens.
- [ ] No enrolled courses → "Start your first course…" empty state.
- [ ] Course-progress request fails → courses error row; whole screen with no
      gamification data → error state + Retry reloads.
- [ ] Small phone layout scrolls cleanly; long titles ellipsize.

## Risks / Notes

- Course progress arrives per-course; the card list rebuilds as each lands, but
  bar animations are de-duped per course id to avoid re-animating.
- Weekly strip uses device-local "today" against a UTC-based activity date —
  acceptable for a presentational strip; no logic depends on it.
