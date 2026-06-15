# Android Gamification — Backend Source of Truth (PR B)

## Goal

Replace Android-local XP, level, streak, and badge math with calls to the
backend gamification endpoints shipped in PR A. After PR B the Android client
is a pure presentation layer for `/api/v1/gamification/*` and stores no
progression state of its own.

## What Changed

- Added wire DTOs:
  `BadgeResponse`, `GamificationStateResponse`, `LeaderboardEntryResponse`.
- Added Retrofit endpoints in `ApiService`:
  - `GET gamification/me`
  - `GET gamification/leaderboard?limit=N`
  - `GET gamification/badges`
- Added `GamificationRepository` that calls those endpoints and maps the
  response to the existing `GamificationUiState` so UI code keeps its current
  shape (icons/emojis/colors stay Android-specific while ids, labels, level
  numbers, streaks, and unlock state come from the backend).
- Rewrote `GamificationViewModel`:
  - Holds no engine, no preferences, no local math.
  - `refreshState()` fans out to `GamificationRepository.loadMyState` and
    `AnalyticsRepository.loadStudentSummary` and emits a merged UI state once
    both responses arrive (analytics provides the lesson / enrollment /
    certificate counter cards).
- Deleted the local engine layer:
  - `XpEngine.java`
  - `XpToastHelper.java`
  - `XpEvent.java`
  - `XpAwardResult.java`
  - `GamificationPreferences.java`
- Replaced every XP emission call site so the client no longer mutates local
  state; it only triggers a backend refetch since the backend itself awards
  the XP in the same transaction as the underlying action:
  - `EnrollCourseFragment` — on successful enrol.
  - `LessonPlayerFragment` — on first lesson completion.
  - `ExamResultFragment` — on exam pass (covers exam pass, course completion,
    and certificate XP awarded server-side).
  - `HomeFragment.bindGamificationCard` — fetches the home card subtitle and
    streak chip from the backend instead of reading `SharedPreferences`.
- Added `onResume` refetch hooks:
  - `GamificationFragment.onResume` → `viewModel.refreshState()`.
  - `HomeFragment.onResume` → re-binds the gamification card from the backend.

## Files Touched

### Added

- `app/src/main/java/com/baghdad/edulife/features/gamification/data/GamificationRepository.java`
- `app/src/main/java/com/baghdad/edulife/features/gamification/model/BadgeResponse.java`
- `app/src/main/java/com/baghdad/edulife/features/gamification/model/GamificationStateResponse.java`
- `app/src/main/java/com/baghdad/edulife/features/gamification/model/LeaderboardEntryResponse.java`

### Modified

- `app/src/main/java/com/baghdad/edulife/core/network/ApiService.java`
- `app/src/main/java/com/baghdad/edulife/features/gamification/viewmodel/GamificationViewModel.java`
- `app/src/main/java/com/baghdad/edulife/features/gamification/ui/GamificationFragment.java`
- `app/src/main/java/com/baghdad/edulife/features/courses/ui/HomeFragment.java`
- `app/src/main/java/com/baghdad/edulife/features/courses/ui/EnrollCourseFragment.java`
- `app/src/main/java/com/baghdad/edulife/features/courses/ui/LessonPlayerFragment.java`
- `app/src/main/java/com/baghdad/edulife/features/courses/ui/ExamResultFragment.java`

### Deleted

- `app/src/main/java/com/baghdad/edulife/features/gamification/data/XpEngine.java`
- `app/src/main/java/com/baghdad/edulife/features/gamification/data/GamificationPreferences.java`
- `app/src/main/java/com/baghdad/edulife/features/gamification/ui/XpToastHelper.java`
- `app/src/main/java/com/baghdad/edulife/features/gamification/model/XpEvent.java`
- `app/src/main/java/com/baghdad/edulife/features/gamification/model/XpAwardResult.java`

## Backend Impact

None. PR B only consumes the endpoints already shipped in PR A.

## Android Impact

- Local SharedPreferences-based gamification state is gone. There is no
  on-device cache that could drift from the backend.
- The XP toast that used to fire instantly on action now relies on the
  server-confirmed refresh. The next render of the home card or gamification
  screen reflects the new total. Toast micro-feedback was removed by design;
  bringing it back will require a server-confirmed signal, not a local guess.
- All emission paths run on the existing
  `EnrollmentService` / `ProgressService` / `ExamService` transactions, so
  failures in gamification can no longer corrupt the client's view of XP.

## Web Impact

None. PR C will perform the equivalent migration on the web client.

## Architecture Compliance

- Feature-first MVVM preserved.
- No new Kotlin, no Hilt — manual instantiation of repositories matches the
  existing pattern.
- API calls live in the repository; fragments never call Retrofit directly.
- Wire DTOs are separate from UI models (`BadgeResponse` → `Badge`,
  `GamificationStateResponse` → `GamificationUiState`).
- Per CLAUDE.md gamification rule: clients never recompute progression.

## Tests / Verification

- `./gradlew :app:compileDebugJavaWithJavac` → BUILD SUCCESSFUL.
- Backend test suite from PR A unchanged → 174 tests passing.
- No new unit tests added (the local math they would have covered no longer
  exists on the client; the equivalent backend tests cover the rules).

## Risks / Notes

- Optimistic "+25 XP" toasts during the lesson / enrol / exam flow are gone.
  If product wants visible mid-flow feedback, expose the delta in the
  `/gamification/me` response (e.g. `recentlyEarned`) or fire the toast after
  the refetch resolves.
- `GamificationUiState.lessonsCompleted` / `coursesEnrolled` /
  `certificatesEarned` are now fed by `/analytics/me/summary` rather than the
  gamification endpoint. A failure on that secondary call leaves the counters
  at zero but does not block the level / streak / badges.
- The home card now waits one round-trip on cold start before rendering. If
  this lag is felt, a short cached snapshot can be added later — but never one
  that competes with the backend as a source of truth.
