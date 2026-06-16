# Android Study Analytics — wire to live backend

## Goal

Replace the mock sections in the learner Study Analytics screen (weekly bars,
per-course progress, exam avg/best, streak) with real backend data so the
screen reflects the authenticated learner's actual activity.

## What Changed

- **Backend (analytics module)**
  - `ExamAttemptRepository` gains two scoped aggregate queries —
    `averageScoreByUserId` and `maxScoreByUserId` — both `coalesce`-guarded so
    a learner with no attempts gets `0`.
  - `StudentAnalyticsSummaryDto` extends with `averageExamScore` and
    `bestExamScore` (0-100 ints).
  - `AnalyticsService.getMyStudentSummary` reads the new aggregates and rounds
    average to an int percentage.
  - `AnalyticsServiceTest` and `AnalyticsControllerTest` updated for the new
    fields; added a no-attempts case to verify the null-coalesce path.

- **Android (analytics feature)**
  - `StudentAnalyticsSummary` model extended with the two new score fields.
  - `AnalyticsRepository.loadStudyAnalytics` rewritten:
    - Fires `/analytics/me/summary`, `/enrollments/me`, `/gamification/me` in
      parallel.
    - For each enrolled course, fans out `/progress/courses/{id}` and waits
      for all responses before delivering.
    - Weekly bar chart is built by bucketing every lesson's `completedAt`
      into the last seven days (Mon→Sun) using `java.time` (already enabled
      via core library desugaring).
    - Per-course progress, overall completion %, and last-activity label come
      from real progress data; the last-activity label uses a small relative
      formatter ("Today", "Yesterday", "N days ago", ...).
    - Streak (`currentStreak` / `longestStreak`) reads from the gamification
      endpoint directly — clients never compute streaks (per gamification
      spec).
    - Exam block uses `s.averageExamScore`, `s.examsPassed`, `s.bestExamScore`
      from the summary.
    - Hard failure only on `/summary` (the hero block); enrollment or
      gamification failures degrade to empty/zero rather than blocking the
      screen.
  - Model doc comments updated to remove the "mock" wording and point to the
    real source endpoints.

## Files Touched

```
backend/src/main/java/com/edulife/analytics/dto/StudentAnalyticsSummaryDto.java
backend/src/main/java/com/edulife/analytics/service/AnalyticsService.java
backend/src/main/java/com/edulife/exams/repository/ExamAttemptRepository.java
backend/src/test/java/com/edulife/analytics/AnalyticsServiceTest.java
backend/src/test/java/com/edulife/analytics/AnalyticsControllerTest.java

app/src/main/java/com/baghdad/edulife/features/analytics/data/AnalyticsRepository.java
app/src/main/java/com/baghdad/edulife/features/analytics/model/StudentAnalyticsSummary.java
app/src/main/java/com/baghdad/edulife/features/analytics/model/StudyAnalytics.java
app/src/main/java/com/baghdad/edulife/features/analytics/model/WeeklyStudyActivity.java
app/src/main/java/com/baghdad/edulife/features/analytics/model/CourseProgressAnalytics.java
app/src/main/java/com/baghdad/edulife/features/analytics/model/LearningStreakSummary.java
app/src/main/java/com/baghdad/edulife/features/analytics/model/ExamPerformanceSummary.java
```

## Backend Impact

- Extends an existing read-only DTO with two integer fields; no schema change,
  no Flyway migration. Aggregates run over `exam_attempts` rows scoped to the
  resolved user id; ownership scoping is unchanged.
- Public surface stays the same: the existing `GET /api/v1/analytics/me/summary`
  contract is additive (clients ignore unknown JSON fields, and the Android
  Gson default tolerates missing ones on older builds).

## Android Impact

- Same screen, real data. No new screen or navigation entry. The view-model and
  fragment binding are unchanged.
- New fan-out hits 1 + 1 + 1 + N requests where N is the number of active
  enrollments. Calls run in parallel; the four pre-existing summary/enrollment/
  gamification calls also run in parallel.

## Web Impact

None — web is untouched. The same `/analytics/me/summary` extension is
available to web if it chooses to render scores too.

## Architecture Compliance

- Backend: thin controller, business logic in `AnalyticsService`, repository
  aggregates only. No client-supplied user id used anywhere.
- Android: ApiService method already existed for each call; the repository
  orchestrates them and the fragment stays presentation-only.
- Gamification spec: client never recomputes streaks — values read from
  `/gamification/me` straight onto the streak card.
- No fake data remains in the Study Analytics path.

## Tests / Verification

- `mvnw test` — 177 tests pass, including 5 in `AnalyticsServiceTest` (1 new)
  and 9 in `AnalyticsControllerTest`.
- `./gradlew :app:compileDebugJavaWithJavac --rerun-tasks` — clean.
- Manual UI verification still required against a populated learner account
  (see Risks).

## Risks / Notes

- Weekly bars depend on lessons being marked complete with an accurate
  `completedAt`. If multiple time zones are in play the bucketing uses the
  device default zone — acceptable for an MVP.
- Average/best scores aggregate over *all* attempts. If a teacher publishes a
  badly-scored exam later, that average will drop; the screen reflects the
  truth — no smoothing.
- The "Current learning path" caption is still a placeholder string until a
  learning-path concept exists on the backend.
