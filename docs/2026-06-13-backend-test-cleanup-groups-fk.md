# Task Audit - Backend Test Cleanup Groups FK

## Date
2026-06-13

## Task Summary
Scanned the Samsung Android logcat for the group admin failure and fixed the backend test cleanup blocker that could prevent the Render backend from receiving the latest group admin fixes.

## Files Created
- docs/2026-06-13-backend-test-cleanup-groups-fk.md

## Files Modified
- backend/src/test/java/com/edulife/auth/AuthSyncControllerTest.java

## What Was Done
The logcat showed two concurrent Android calls to `GET https://edulife-2bro.onrender.com/api/v1/groups`, both returning backend `500` responses with the shared `INTERNAL_ERROR` API contract. This means the mobile app is reaching Render successfully and the failure is server-side.

While validating the backend, the focused group/admin tests passed, but the full backend suite failed because `AuthSyncControllerTest.cleanDatabase()` deleted users while existing `groups.created_by` rows still referenced those users. The cleanup now deletes group join requests, group course links, group members, and groups before deleting users.

## Architecture Compliance
The change stays inside backend test code and does not alter production behavior, API contracts, roles, entities, or module boundaries. It respects the relational ownership rule that group rows reference user identities.

## Code Comments Added
Added a short cleanup comment explaining why group-owned rows must be deleted before user identities in the auth sync integration test.

## Validation / Testing
Scanned `samsung-SM-F936B-Android-16_2026-06-13_175908.logcat` and confirmed the failure is `GET /api/v1/groups` returning `500` from Render.

Ran `./mvnw.cmd "-Dtest=AuthSyncServiceTest,GroupServiceTest,GroupControllerTest" test` successfully.

Ran full backend `./mvnw.cmd test` successfully: 125 tests, 0 failures, 0 errors.

## Risks / Notes
The Android logcat does not include the Render server stack trace, only the public error body. The backend now builds cleanly, so the next required step is deploying the backend fixes to Render and retesting the group admin login against `https://edulife-2bro.onrender.com`.
