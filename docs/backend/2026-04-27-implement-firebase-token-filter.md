# Task Audit - Implement Firebase Token Filter

## Date
2026-04-27

## Task Summary
Completed the Firebase token filter flow for protected backend requests by wiring Firebase Admin token verification through Spring Security, enforcing `email_verified`, skipping explicit public routes, and adding tests that prove the required authentication behavior.

## Files Created
- backend/src/test/java/com/edulife/security/FirebaseTokenFilterSecurityTest.java
- backend/src/test/java/com/edulife/security/TestSecurityController.java
- docs/2026-04-27-implement-firebase-token-filter.md

## Files Modified
- backend/src/main/java/com/edulife/config/FirebaseConfig.java
- backend/src/main/java/com/edulife/security/FirebaseAuthentication.java
- backend/src/main/java/com/edulife/security/FirebaseTokenFilter.java
- backend/src/main/java/com/edulife/security/SecurityConfig.java

## What Was Done
- Reviewed the current branch and confirmed it already had a basic `FirebaseTokenFilter`, but it still relied on `FirebaseAuth.getInstance()` directly, had no proof via tests, and applied header parsing even on public routes.
- Added a `FirebaseAuth` bean in `FirebaseConfig` so token verification uses the shared `FirebaseApp` through dependency injection instead of static lookup.
- Updated `FirebaseTokenFilter` to:
  - extend `OncePerRequestFilter` with injected `FirebaseAuth`
  - parse `Authorization: Bearer <token>`
  - reject malformed headers with `401`
  - verify Firebase token signature and expiry through the Admin SDK
  - reject unverified emails with `403`
  - populate the Spring Security context with Firebase UID and email
  - skip explicit public routes so public endpoints remain accessible without authentication noise
- Updated `SecurityConfig` to:
  - build the filter from the injected `FirebaseAuth`
  - keep public routes open
  - require authentication everywhere else
  - return `401` for missing authentication
  - use stateless session management for token-based API requests
- Updated `FirebaseAuthentication` so it does not invent a learner role during authentication. Role resolution must come from trusted backend data later, not from a default assumption.
- Added MVC security tests covering:
  - valid Firebase token on protected endpoint
  - missing token on protected endpoint
  - malformed header on protected endpoint
  - invalid or expired token
  - unverified email token
  - public endpoint without auth
  - public endpoint with malformed auth header still accessible
- Verified startup with a temporary local Firebase credential fixture passed through environment variables only, without adding secrets to the repository.

## Architecture Compliance
This work stays inside the backend security layer of the modular monolith and keeps business logic out of controllers. Firebase token validation lives in the security filter where it belongs, shared Firebase bootstrap remains in `config`, and authentication context data is carried through Spring Security instead of request payloads.

## Code Comments Added
- Added a comment in `FirebaseConfig` explaining why `FirebaseAuth` must use the shared `FirebaseApp`.
- Added a comment in `FirebaseAuthentication` explaining why roles are intentionally not assigned there.
- Added a comment in `FirebaseTokenFilter` explaining why public endpoints must bypass the Firebase filter.
- Added a comment in the test controller explaining that protected endpoints must read identity from the authenticated security context.

## Validation / Testing
- Ran `mvn test` successfully.
- Confirmed the new security test suite passes all scenarios for the Firebase token filter.
- Verified application startup with `mvn spring-boot:run` using temporary environment-provided Firebase credentials.
- Confirmed startup logs included:
  - `Firebase Admin SDK initialized successfully`
  - `Tomcat started on port 8080 (http)`
  - `Started BackendApplication`

## Risks / Notes
- `FirebaseTokenFilter` currently authenticates identity only. It does not yet resolve EduLife internal roles from the database, which is correct for this stage because request payload roles must not be trusted.
- The test suite uses `@MockBean`, which is currently deprecated in the Spring Boot version in this project. It still works, but the project should eventually migrate to the replacement API when that migration is scheduled.
- The request matcher used for public path skipping compiles with deprecation warnings in the current Spring Security version. It is functionally correct now, but should be modernized later if the team standardizes on the newer matcher API.
