# Task Audit - Group Admin Info Retrieval

## Date
2026-06-13

## Task Summary
Fixed the backend group retrieval path so a newly synced `GROUP_ADMIN` account can retrieve usable group data from both website and Android.

## Files Created
- backend/src/test/java/com/edulife/groups/GroupServiceTest.java

## Files Modified
- backend/src/main/java/com/edulife/groups/service/GroupService.java

## What Was Done
Added a lazy bootstrap step to `GroupService.listMyGroups()`: when a verified `GROUP_ADMIN` has no owned groups, the backend creates a default group named `My Institute` for that internal user before returning the group list.

This fixes the shared web/mobile symptom where the group admin portal had no retrievable group record because Flyway seeds cannot reliably create user-owned group data before `/auth/sync` creates the Firebase-linked user row.

Added service-level tests proving:

- a new `GROUP_ADMIN` gets a default group on first group-list retrieval;
- a `TEACHER` does not get an automatic group, preserving the product rule that teachers may create a group, request an institute, or stay independent.

## Architecture Compliance
The change stays in the `groups` backend module. The controller remains thin, the business rule lives in `GroupService`, and existing owner-scoped repository rules remain unchanged.

No new role was introduced. `Group` remains a business entity, and the teacher standalone path remains intact.

## Code Comments Added
Added comments explaining why the default group is created lazily for `GROUP_ADMIN` accounts and why this must not apply to teachers. The comments document the Firebase sync / Flyway ordering issue and the teacher independence rule.

## Validation / Testing
Ran:

```text
./mvnw.cmd "-Dtest=GroupServiceTest,GroupControllerTest" test
```

Result: passed, 26 tests.

## Risks / Notes
Both current clients are configured to the Render backend:

- `guided-journey-lab/.env` uses `https://edulife-2bro.onrender.com`.
- `local.properties` sets Android `edulife.apiBaseUrl=https://edulife-2bro.onrender.com/api/v1/`.

This backend fix must be deployed to that Render backend before website/mobile users see the change there. A stale deployed backend can still fail or return empty group-admin data even though the local code is fixed.
