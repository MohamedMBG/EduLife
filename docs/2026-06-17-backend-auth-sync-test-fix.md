# Fix AuthSyncControllerTest FK Violation on Cleanup

## Goal

Fix backend test suite failure caused by AuthSyncControllerTest cleanup violating
`courses_created_by_user_id_fkey` foreign key constraint.

## Root Cause

`@BeforeEach cleanDatabase()` called `userRepository.deleteAll()`, which attempted
to delete the Flyway V24 seed instructor user (`66666666-6666-6666-6666-666666666666`).
That user is referenced by 5 seed courses via `courses.created_by_user_id` (no CASCADE).
PostgreSQL rejected the DELETE with a foreign key violation.

## What Changed

Single file changed: `AuthSyncControllerTest.java` (test code only).

1. **Replaced `userRepository.deleteAll()` with targeted JDBC cleanup.**
   - Deletes `enrollments`, `teacher_requests`, and `advisor_log` rows for non-seed
     users first (these FKs have no CASCADE).
   - Then deletes only non-seed users using
     `WHERE firebase_uid IS DISTINCT FROM 'seed-instructor-edulife'`.
   - Seed instructor and its course references survive untouched.

2. **Fixed count assertions.**
   - Two tests (`syncReusesExistingUserOnRepeatLogin`,
     `syncAssignsGroupAdminStaffRoleByVerifiedEmail`) used
     `userRepository.findAll().size() == 1` which would now include the seed user.
   - Replaced with `testUserCount()` helper that filters out seed users before counting.

## Files Touched

- `backend/src/test/java/com/edulife/auth/AuthSyncControllerTest.java`

## Backend Impact

None. No production code, no schema changes, no migration changes.

## Architecture Compliance

- Production FK constraints remain intact.
- No `ON DELETE CASCADE` added to `courses.created_by_user_id`.
- Test cleanup respects dependency order.
- Seed data preserved for other test classes sharing the Spring context.

## Tests / Verification

- `./mvnw test -Dtest=AuthSyncControllerTest` — 10/10 passed
- `./mvnw test` — 254/254 passed, 0 failures, 0 errors

## Risks / Notes

- Cleanup assumes `seed-instructor-edulife` is the only Flyway-seeded Firebase UID
  that must survive. If future migrations add more seed users referenced by
  non-CASCADE FKs, the cleanup filter will need updating.
