# Task Audit - Backend Website Auth

## Date
2026-06-06

## Task Summary
Fixed backend issues that prevented the website authentication flow from working reliably against the Spring Boot API.

## Files Created
- backend/.env.example
- backend/src/test/java/com/edulife/security/SecurityDefaultCorsTest.java
- docs/2026-06-06-backend-website-auth.md

## Files Modified
- backend/mvnw.cmd
- backend/run-local.ps1
- backend/src/main/java/com/edulife/auth/service/AuthSyncService.java
- backend/src/main/java/com/edulife/users/repository/UserRepository.java
- backend/src/main/resources/application.yaml
- backend/src/test/java/com/edulife/auth/AuthSyncControllerTest.java
- backend/src/test/java/com/edulife/progress/ProgressServiceTest.java

## What Was Done
Made `/api/v1/auth/sync` safer for the web app by replacing the first-login user creation path with a database upsert keyed by `firebase_uid`. This prevents duplicate browser auth events or multiple tabs from leaking a unique-key error during first sync.

Allowed local website origins by default in backend CORS configuration so browser preflight requests can reach `/api/v1/auth/sync` during development. Production can still override this with `APP_CORS_ALLOWED_ORIGINS`.

Fixed the Windows Maven wrapper crash caused by indexing `.Target[0]` when the local `.m2` directory is not a symlink. Updated `backend/run-local.ps1` to point developers to `backend/.env.example`.

Added a backend `.env.example` with the required local database, Firebase Admin, and CORS variables for website authentication.

Added auth/CORS tests for role-intent behavior, admin self-assignment prevention, and local website preflight access. Fixed unrelated progress-service Mockito stubbing issues so the full backend test suite passes.

## Architecture Compliance
The implementation stays inside the existing modular monolith. Auth sync logic remains in `auth/service`, persistence access remains in `users/repository`, and security/CORS concerns remain in backend configuration. No microservices, new auth model, or client-trusted role logic were introduced.

## Code Comments Added
Added comments in `AuthSyncService` explaining why first sync uses a database upsert for browser concurrency. Updated CORS configuration comments to explain the local website default and production override. Added `.env.example` comments explaining credential and CORS setup.

## Validation / Testing
Ran `backend/mvnw.cmd -version`; the wrapper now starts correctly.

Ran targeted backend auth/security tests:
`mvn "-Dtest=AuthSyncControllerTest,SecurityDefaultCorsTest,SecurityHardeningTest,FirebaseTokenFilterSecurityTest" test`

Ran the full backend test suite:
`mvn test`

Result: 100 tests passed, 0 failures.

## Risks / Notes
The backend still requires real Firebase Admin credentials via `FIREBASE_ADMIN_CREDENTIALS_PATH` or `FIREBASE_ADMIN_CREDENTIALS_JSON` before it can validate real website Firebase ID tokens.

For deployed environments, set `APP_CORS_ALLOWED_ORIGINS` to the real website origin instead of relying on local development defaults.
