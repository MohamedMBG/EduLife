# Android Study Analytics Redesign

## Goal

Redesign the learner Study Analytics screen into a modern, premium, scannable
dashboard: overall-progress hero, weekly bar chart, course performance cards,
exam stats, learning streak, and insight lines — kept in the existing Java/XML
MVVM architecture (no Compose, no Kotlin, no chart library).

## What Changed

- New aggregate model `StudyAnalytics` plus section models: `WeeklyStudyActivity`,
  `DayStudyActivity`, `CourseProgressAnalytics`, `ExamPerformanceSummary`,
  `LearningStreakSummary`, `AnalyticsInsight`, and `StudyAnalyticsUiState`.
- `StudyAnalyticsViewModel` exposes one `LiveData<StudyAnalyticsUiState>` with
  loading / error / success.
- `AnalyticsRepository.loadStudyAnalytics()` fetches the **real** student summary
  endpoint and derives overall progress + exam pass count from it, then enriches
  weekly activity, per-course progress, exam scores, streak, and insights with
  realistic client-side mock data. Every mocked section carries a `TODO(backend)`.
- New dependency-free custom view `WeeklyBarChartView` draws the Mon–Sun bar
  chart (rounded bars, value labels, today highlighted, zero-safe).
- `CourseProgressAdapter` (ListAdapter + DiffUtil) renders course progress cards.
- `StudentAnalyticsFragment` rewritten to bind the new screen; class name and
  nav id unchanged so existing navigation (Profile → My Learning Stats) keeps
  working with no graph edits.
- Removed orphaned `StudentAnalyticsViewModel`, `StudentAnalyticsUiState`,
  `StudentTrendUiState` (no longer referenced after the redesign).
- New drawables, dimens, and strings for the screen.

## Files Touched

Java (new):
- features/analytics/model/StudyAnalytics.java
- features/analytics/model/StudyAnalyticsUiState.java
- features/analytics/model/WeeklyStudyActivity.java
- features/analytics/model/DayStudyActivity.java
- features/analytics/model/CourseProgressAnalytics.java
- features/analytics/model/ExamPerformanceSummary.java
- features/analytics/model/LearningStreakSummary.java
- features/analytics/model/AnalyticsInsight.java
- features/analytics/viewmodel/StudyAnalyticsViewModel.java
- features/analytics/ui/CourseProgressAdapter.java
- features/analytics/ui/widget/WeeklyBarChartView.java

Java (modified):
- features/analytics/data/AnalyticsRepository.java (added loadStudyAnalytics + mock builders)
- features/analytics/ui/StudentAnalyticsFragment.java (full rewrite)

Java (deleted):
- features/analytics/viewmodel/StudentAnalyticsViewModel.java
- features/analytics/model/StudentAnalyticsUiState.java
- features/analytics/model/StudentTrendUiState.java

Resources:
- res/layout/fragment_student_analytics.xml (redesigned)
- res/layout/item_course_progress_analytics.xml (new)
- res/layout/item_study_insight.xml (new)
- res/drawable/bg_study_card.xml, bg_study_card_tinted.xml, bg_study_hero.xml,
  bg_study_insight_dot.xml, bg_study_streak_icon.xml (new)
- res/drawable/ic_trophy.xml, ic_lightbulb.xml (new)
- res/values/dimens.xml (study_* dimens)
- res/values/strings.xml (study_* + analytics_* format strings)

## Backend Impact

None. No new endpoints, no schema, no migration. The screen consumes the
existing `GET /api/v1/analytics/me/summary` only; all other sections are
client-side mock pending real endpoints.

## Android Impact

New learner Study Analytics screen with loading / error / empty / success
states. Entry point unchanged (Profile → My Learning Stats → `studentAnalyticsFragment`).
Empty (zeroed) learner renders gracefully — no crash on empty data.

## Web Impact

None.

## Architecture Compliance

- Feature-first MVVM preserved: Fragment → ViewModel → Repository → ApiService.
- No API calls or business logic in the Fragment; mock data lives in the
  Repository, not the UI.
- Strings in strings.xml; dimens in dimens.xml; spacing on the 4/8/12/16/24 scale.
- Material components reused (`CircularProgressIndicator`); custom bar chart adds
  no dependency.
- Icons have content descriptions; decorative icons use `@null`/`contentDescription="@null"`.

## Tests / Verification

- `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL (exit 0).
- Java compiles; resources link; no unresolved references after deleting the
  three orphaned types.

## Risks / Notes

- Weekly activity, course breakdown, exam average/best score, and streak values
  are **mock** today (clearly TODO-marked in `AnalyticsRepository`). Real wiring:
  - weekly: a lessons/study-minutes-per-day endpoint;
  - courses: learner enrollments + progress;
  - exam scores: expose averageScore/bestScore on the analytics summary;
  - streak: mirror `/api/v1/gamification/me` (gamification is the source of truth).
- Exams-passed count and certificate/lesson/active-course counts are **real**
  (from the summary endpoint).
- Overall-progress % is a coarse honest derivation (certs ÷ active courses) until
  a real learning-path completion metric exists.
