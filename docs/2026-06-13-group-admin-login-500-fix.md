# Task Audit - Group Admin Login 500 Fix

## Date
2026-06-13

## Task Summary
Fixed a likely backend 500 during group-admin login when the verified email already exists in `users` with an old Firebase UID.

## Files Created
- backend/src/test/java/com/edulife/auth/AuthSyncServiceTest.java

## Files Modified
- backend/src/main/java/com/edulife/auth/service/AuthSyncService.java
- backend/src/main/java/com/edulife/users/entity/User.java

## What Was Done
Updated `/api/v1/auth/sync` behavior so it no longer only upserts by `firebase_uid`.

If the Firebase UID from the token is not found, the backend now also checks for an existing user row with the same verified email. When found, it re-links that backend user row to the current Firebase UID and continues staff-role reconciliation.

This handles the common development/demo failure where `groupadmin@edulife.test` already exists in PostgreSQL, but the Firebase account was deleted/recreated and now has a different UID. Before this fix, the backend attempted a new insert, hit the unique `email` constraint, and returned 500.

## Architecture Compliance
The change stays in the `auth` and `users` modules. Firebase token validation remains the trusted identity source, and role assignment still comes from server-side staff configuration or backend state, not client input.

No mobile or web special casing was added; both clients continue using the same `/auth/sync` and group endpoints.

## Code Comments Added
Added comments explaining why a verified email row can be re-linked to a new Firebase UID and why this prevents a unique-key 500 while keeping role assignment server-controlled.

## Validation / Testing
Ran:

```text
./mvnw.cmd "-Dtest=AuthSyncServiceTest,GroupServiceTest,GroupControllerTest" test
```

Result: passed, 27 tests.

## Risks / Notes
This fix must be deployed to the backend that the clients are using. The current website and Android config point to `https://edulife-2bro.onrender.com`, so local code changes will not affect the app until that backend is redeployed.

If a 500 still appears after deploy, check whether it is coming from `POST /api/v1/auth/sync` or `GET /api/v1/groups`; those are different failure points.
