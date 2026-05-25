# Task Audit - Fix Firebase Admin Bootstrap

## Date
2026-04-26

## Task Summary
Finished the remaining Sprint 1 Firebase Admin bootstrap work so the backend initializes the Admin SDK from environment-provided credentials, fails fast with clear errors, and documents local credential setup without storing secrets in backend resources.

## Files Created
- backend/src/main/java/com/edulife/config/FirebaseAdminProperties.java
- docs/2026-04-26-fix-firebase-admin-bootstrap.md

## Files Modified
- backend/src/main/java/com/edulife/config/FirebaseConfig.java
- backend/src/main/resources/application.yaml
- backend/HELP.md
- backend/src/test/java/com/edulife/BackendApplicationTests.java

## What Was Done
Replaced the classpath-based Firebase service account loading with Spring configuration properties backed by `FIREBASE_ADMIN_CREDENTIALS_PATH` or `FIREBASE_ADMIN_CREDENTIALS_JSON`.

Changed Firebase initialization to a Spring bean that returns the existing `FirebaseApp` when one already exists, so the Admin SDK is initialized exactly once per process.

Added controlled startup failure messages for missing credentials, missing files, and credential parsing failures.

Removed the local service account JSON from `backend/src/main/resources` so backend secrets no longer live beside application code.

Documented local development setup in `backend/HELP.md`.

Added backend tests that verify successful initialization with a valid test credential file and failure when credentials are missing or the path is invalid.

## Architecture Compliance
The changes stay inside the backend configuration layer under `config/`, which matches the EduLife modular monolith structure for infrastructure concerns. No business logic was moved into controllers or unrelated modules.

## Code Comments Added
Added comments in Firebase bootstrap code to explain why the Admin SDK must stay singleton and why raw JSON support exists.

Added a test comment explaining that the embedded credential is structurally valid test data rather than a real secret, so future developers understand why it is safe to keep in test code.

Added an application configuration comment explaining why Firebase credentials must come from the environment instead of the repository.

## Validation / Testing
Validation should include running backend tests and starting Spring Boot with a real Firebase Admin credential supplied through `FIREBASE_ADMIN_CREDENTIALS_PATH` or `FIREBASE_ADMIN_CREDENTIALS_JSON`.

The test suite now covers:
- successful Firebase initialization with valid credentials
- startup failure when credentials are missing
- startup failure when a configured path does not exist

## Risks / Notes
The backend now requires one of the Firebase Admin environment variables on startup in non-test use. Local runs that previously depended on `src/main/resources/edulife-firebase-service-account.json` must switch to environment-based setup.
