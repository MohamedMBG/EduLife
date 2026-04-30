# Task Audit - Move Auth Sync Production

## Date
2026-04-30

## Task Summary
Moved `POST /api/v1/auth/sync` from test sources into the production Spring Boot backend so Android login can call the real endpoint.

## Files Created
- backend/src/main/java/com/edulife/auth/controller/AuthController.java
- backend/src/main/java/com/edulife/auth/dto/AuthSyncResponse.java
- backend/src/main/java/com/edulife/auth/service/AuthSyncService.java
- docs/2026-04-30-move-auth-sync-production.md

## Files Modified
- backend/src/test/java/com/edulife/auth/controller/AuthController.java
- backend/src/test/java/com/edulife/auth/dto/AuthSyncResponse.java
- backend/src/test/java/com/edulife/auth/service/AuthSyncService.java

## What Was Done
Created the production `auth` backend module with controller, service, and DTO packages. `AuthController` exposes `POST /api/v1/auth/sync`. `AuthSyncService` reads the verified Firebase identity from `SecurityContext`, validates required identity fields, upserts the internal EduLife user by Firebase UID, and returns only the internal UUID and role. Removed the accidental test-source controller, service, and DTO so tests now exercise the production implementation.

## Architecture Compliance
The endpoint belongs in the backend `auth` module and keeps business logic in the service layer instead of the controller. It follows the modular monolith structure and preserves the Sprint 1 identity bridge before moving to Sprint 2 course discovery.

## Code Comments Added
Added comments around the security-sensitive parts of auth sync: the controller never trusts request body identity, the service only accepts `FirebaseAuthentication`, missing Firebase identity values are treated as broken security context, repeated sync is idempotent, and `firebaseUid` is never exposed in API responses.

## Validation / Testing
Ran `backend/mvnw.cmd test`. Result: 15 tests passed, 0 failures. The auth sync controller tests now compile against and execute the production auth module rather than duplicate test-source classes.

## Risks / Notes
This still needs a real manual end-to-end test with Firebase Admin credentials and Android login to confirm the running backend, Android interceptor, and `/auth/sync` call work together outside MockMvc.
