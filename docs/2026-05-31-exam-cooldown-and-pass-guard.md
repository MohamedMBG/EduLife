# Exam Cooldown and Already-Passed Guard

## Goal

Enforce two submission rules:
1. A learner who has already passed an exam cannot retake it.
2. After 2 failed attempts the learner is locked out for 72 hours (cooldown resets from the most recent failure).

## What Changed

### `ExamAttemptRepository`
Added two derived-query methods:
- `countByUserIdAndExamIdAndPassedFalse` — counts failures for a user/exam pair
- `findTopByUserIdAndExamIdAndPassedFalseOrderByTakenAtDesc` — fetches the most recent failure (used to compute cooldown expiry)

### `ExamService.submitExam()`
Pre-submission guard block added (runs before scoring):
1. `existsByUserIdAndExamIdAndPassedTrue` → 409 `ExamAlreadyPassedException`
2. Count failures; if ≥ 2, read latest failure timestamp + 72 h; if still in window → 429 `ExamCooldownException`

Post-save: `ExamResultDto` now includes `attemptsUsed` (count of failures after save) and `cooldownEndsAt` (non-null only when this submission triggered the 2-failure threshold).

### New exception classes
- `com.edulife.exams.exception.ExamAlreadyPassedException` → mapped to HTTP 409
- `com.edulife.exams.exception.ExamCooldownException` (carries `cooldownEndsAt` Instant) → mapped to HTTP 429

### `GlobalApiExceptionHandler`
Two new handlers wired:
- `ExamAlreadyPassedException` → `build(CONFLICT, message)`
- `ExamCooldownException` → `ResponseEntity<ExamCooldownError>` with 429 + `cooldownEndsAt` field

### `ExamCooldownError`
New record in `com.edulife.common.error` — extends the standard error shape with a `cooldownEndsAt` field so clients can display a countdown.

### `ExamResultDto`
Added `attemptsUsed` (int) and `cooldownEndsAt` (Instant, nullable).

### `V17__exam_attempt_passed_index.sql`
```sql
CREATE INDEX idx_exam_attempts_user_passed ON exam_attempts (user_id, exam_id, passed);
```
Supports both new repository queries without a full table scan.

## Files Touched

- `backend/src/main/java/com/edulife/exams/repository/ExamAttemptRepository.java`
- `backend/src/main/java/com/edulife/exams/service/ExamService.java`
- `backend/src/main/java/com/edulife/exams/dto/ExamResultDto.java`
- `backend/src/main/java/com/edulife/exams/exception/ExamAlreadyPassedException.java` (new)
- `backend/src/main/java/com/edulife/exams/exception/ExamCooldownException.java` (new)
- `backend/src/main/java/com/edulife/common/error/ExamCooldownError.java` (new)
- `backend/src/main/java/com/edulife/common/error/GlobalApiExceptionHandler.java`
- `backend/src/main/resources/db/migration/V17__exam_attempt_passed_index.sql` (new)

## Backend Impact

`POST /api/v1/courses/{courseId}/exam/submit` now returns:
- **409 Conflict** — already passed; body: standard `ApiError`
- **429 Too Many Requests** — in cooldown; body: `ExamCooldownError` with `cooldownEndsAt`
- **200 OK** — scored normally; body: `ExamResultDto` extended with `attemptsUsed` + `cooldownEndsAt`

## Android Impact

None required immediately. Android `ExamResultDto`-equivalent models should be updated to parse the two new fields when the exam screen is built.

## Web Impact

None required immediately. Same as Android — new fields are additive and nullable.

## Architecture Compliance

- Business rules enforced in service layer only
- Repository has no business logic — only JPA derived queries
- New exceptions follow existing `certificates/exception` and `teacherrequests/exception` patterns
- Error response shape consistent with `ApiError` contract; cooldown body is a superset (additive)
- Flyway migration adds index only — no schema destructive change
- No exam answers sent to client at any point

## Risks / Notes

- Cooldown window (72 h) is hardcoded. Can be extracted to config property if needed in future.
- The `attemptsUsed` field in `ExamResultDto` reflects post-save count (failures only). Passed attempts are always 0 or 1 (max, since you can't retake after passing).
- If the user had ≥ 2 failures and the cooldown expired, a new failure restarts the 72-hour window from the new attempt timestamp.
