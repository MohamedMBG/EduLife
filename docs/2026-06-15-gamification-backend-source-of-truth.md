# Gamification — Backend Source of Truth (PR A)

## Goal

Move all gamification logic (XP, levels, streaks, badges, leaderboard) from the
clients to the backend so Android and Web display identical state for the same
learner. PR A delivers the backend foundation: database, module, XP emission
from existing domain services, daily-login hook, and read APIs.

PR B (Android migration) and PR C (Web migration) will follow; clients still
compute locally until those land.

## What Changed

- New gamification module under `com.edulife.gamification` with constants,
  entities, repositories, service, controller, and config.
- Flyway migration `V22__gamification.sql` adds three tables:
  - `user_gamification_state` — aggregate state per learner (xp, level, streak).
  - `gamification_xp_events` — append-only XP ledger with a unique `dedup_key`
    for idempotent emission.
  - `user_badges` — unlocked badge ids per learner.
- XP emission wired into existing domain services:
  - `EnrollmentService.enroll` → `+10 XP` (`enrollment:{id}` dedup).
  - `ProgressService.markLessonComplete` → `+25 XP` (`lesson:{id}:{userId}`),
    plus `+100 XP` once the course reaches 100 % completion
    (`course:{courseId}:{userId}`).
  - `ExamService.submitExam` (on pass) → `+150 XP` (`exam:{attemptId}`).
  - Certificate generation triggered by the same pass → `+200 XP`
    (`certificate:{certificateId}`).
- `DailyLoginXpFilter` (Spring servlet filter, after rate-limit filter) awards
  `+5 XP` once per UTC day per learner (`login:{userId}:{yyyy-mm-dd}` dedup).
- Streak math: consecutive-day activity increments `current_streak`; one-day
  gap resets to 1. Three- and seven-day bonuses (`+30`, `+75`) award once per
  active streak run via persisted flags; flags reset whenever the streak does.
- Badge unlocks evaluated after every emission; twelve badges from
  `BadgeCatalog` are checked against lesson counts, exam pass count,
  certificate count, current streak, and current level.
- Read-only API surface at `/api/v1/gamification`:
  - `GET /me` — current learner's state (xp, level, level name, streak, badges).
  - `GET /leaderboard?limit=N` — global all-time, ordered by `total_xp DESC`
    then earliest `updated_at`.
  - `GET /badges` — definition catalog (id, label, rarity, unlock description).

## Files Touched

### Added

- `backend/src/main/resources/db/migration/V22__gamification.sql`
- `backend/src/main/java/com/edulife/gamification/package-info.java`
- `backend/src/main/java/com/edulife/gamification/model/XpEventType.java`
- `backend/src/main/java/com/edulife/gamification/model/LevelTable.java`
- `backend/src/main/java/com/edulife/gamification/model/BadgeRarity.java`
- `backend/src/main/java/com/edulife/gamification/model/BadgeDefinition.java`
- `backend/src/main/java/com/edulife/gamification/model/BadgeCatalog.java`
- `backend/src/main/java/com/edulife/gamification/entity/UserGamificationState.java`
- `backend/src/main/java/com/edulife/gamification/entity/GamificationXpEvent.java`
- `backend/src/main/java/com/edulife/gamification/entity/UserBadge.java`
- `backend/src/main/java/com/edulife/gamification/repository/UserGamificationStateRepository.java`
- `backend/src/main/java/com/edulife/gamification/repository/GamificationXpEventRepository.java`
- `backend/src/main/java/com/edulife/gamification/repository/UserBadgeRepository.java`
- `backend/src/main/java/com/edulife/gamification/dto/BadgeDto.java`
- `backend/src/main/java/com/edulife/gamification/dto/GamificationStateDto.java`
- `backend/src/main/java/com/edulife/gamification/dto/LeaderboardEntryDto.java`
- `backend/src/main/java/com/edulife/gamification/service/GamificationService.java`
- `backend/src/main/java/com/edulife/gamification/controller/GamificationController.java`
- `backend/src/main/java/com/edulife/gamification/security/DailyLoginXpFilter.java`
- `backend/src/main/java/com/edulife/gamification/config/GamificationConfig.java`
- `backend/src/test/java/com/edulife/gamification/LevelTableTest.java`
- `backend/src/test/java/com/edulife/gamification/GamificationServiceTest.java`
- `backend/src/test/java/com/edulife/gamification/GamificationControllerTest.java`

### Modified

- `backend/src/main/java/com/edulife/enrollments/service/EnrollmentService.java`
  — emit enrollment XP.
- `backend/src/main/java/com/edulife/progress/service/ProgressService.java`
  — emit lesson and course-completion XP.
- `backend/src/main/java/com/edulife/exams/service/ExamService.java`
  — emit exam-pass XP and certificate-earned XP.
- `backend/src/main/java/com/edulife/profiles/repository/ProfileRepository.java`
  — add `findAllByUserIdIn` for leaderboard display names.
- `backend/src/main/java/com/edulife/security/SecurityConfig.java`
  — register `DailyLoginXpFilter` after `RateLimitFilter` (optional bean).
- `backend/src/test/java/com/edulife/progress/ProgressServiceTest.java`
  — mock `GamificationService`.

## Backend Impact

- Three new tables. Migration is additive and cannot break existing flows.
- `DailyLoginXpFilter` adds at most one read + one write per learner per UTC
  day; subsequent same-day requests short-circuit on an in-memory map.
- Emission methods run on `REQUIRES_NEW` propagation so a gamification failure
  cannot roll back the underlying enrollment / lesson / exam / certificate
  transaction. Backend remains source of truth for gamification, but the
  authoritative domain action is never blocked by it.
- No correct-answer data leaves the backend; gamification only consumes
  authoritative pass/fail outcomes that already passed scoring.

## Android Impact

- None in this PR. Android still derives state locally per the shared spec.
- PR B will replace local computation with calls to `/api/v1/gamification/me`
  and refetch on enroll / lesson / exam / certificate / resume.

## Web Impact

- None in this PR. Web still derives state locally.
- PR C will replace local derivation with the same backend calls.

## Architecture Compliance

- Module layout follows the modular-monolith convention
  (`controller/service/repository/dto/entity/exception/model/`).
- Controllers are thin and resolve the user from `FirebaseAuthentication`
  only; no `userId` is accepted from the client.
- All write paths are emission hooks inside domain services; the public API
  exposes reads only.
- Constants are defined exactly once in `XpEventType`, `LevelTable`, and
  `BadgeCatalog`; no duplication elsewhere in the backend, and the future
  client migrations must consume them via these endpoints.

## Tests / Verification

- `LevelTableTest` — binary-search level resolution and threshold edges.
- `GamificationServiceTest` — first lesson awards XP and unlocks
  `first_flame`; duplicate dedup keys skip emission; three consecutive daily
  logins award the streak bonus exactly once; gap resets streak and bonus
  flags; `getState` returns expected level metadata; enrollment awards
  `+10 XP`.
- `GamificationControllerTest` — `/me`, `/leaderboard`, `/badges` require a
  Firebase token; happy paths return expected DTOs.
- Full suite: `./mvnw test` — 174 tests, all green.

## Risks / Notes

- Activity dates use UTC. A learner crossing a timezone boundary near midnight
  may see a streak increment on a day that feels like the same calendar day to
  them locally. Acknowledged for PR A; revisit if learners report drift.
- Per the user-confirmed scope, no historical backfill is performed. Existing
  enrollments / lessons / exams / certificates do not produce XP retroactively;
  earning starts from the deploy of this PR.
- Leaderboard is global all-time only. No per-course or weekly windows in PR A.
- Real-time updates use polling (clients refetch `/me` after enrollment,
  lesson completion, exam pass, certificate issue, and app resume). No
  SSE/WebSocket in this PR.
- The in-memory daily-login cache is per JVM. Cross-instance correctness is
  enforced by the `dedup_key` unique index.
