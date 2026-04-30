# Task Audit - Global API Error Contract

## Date
2026-04-30

## Task Summary
Added the backend-wide API error response contract `{ status, message, timestamp }` before expanding protected endpoints.

## Files Created
- backend/src/main/java/com/edulife/common/error/ApiError.java
- backend/src/main/java/com/edulife/common/error/ApiErrorWriter.java
- backend/src/main/java/com/edulife/common/error/GlobalApiExceptionHandler.java
- docs/2026-04-30-global-api-error-contract.md

## Files Modified
- backend/src/main/java/com/edulife/security/FirebaseTokenFilter.java
- backend/src/main/java/com/edulife/security/SecurityConfig.java
- backend/src/test/java/com/edulife/auth/AuthSyncControllerTest.java
- backend/src/test/java/com/edulife/security/FirebaseTokenFilterSecurityTest.java
- backend/src/test/java/com/edulife/security/TestSecurityController.java

## What Was Done
Implemented `ApiError` as the shared response body for backend errors. Added `GlobalApiExceptionHandler` with controlled handling for bad requests, database conflicts, response-status exceptions, and unexpected exceptions. Added `ApiErrorWriter` so security filter and Spring Security failures can return the same JSON contract even though they run before MVC controller advice. Updated Firebase token rejection paths and authentication/authorization failures to emit the contract.

## Architecture Compliance
The error contract lives in a shared backend `common/error` package because it is cross-cutting infrastructure used by all modules. Security behavior remains in the `security` package, while feature/domain controllers can rely on global advice instead of duplicating error handling.

## Code Comments Added
Added comments explaining why security filters need an explicit writer, why authentication and authorization failures are handled outside MVC, why bad-request handling avoids framework stack traces, why database conflicts are controlled responses, and why unexpected errors log internally while returning a non-sensitive message.

## Validation / Testing
Ran `backend/mvnw.cmd test`. Result: 17 tests passed, 0 failures. Tests now verify the JSON contract for missing token, malformed token, expired token, unverified email, controller bad request, and unexpected controller exceptions.

## Risks / Notes
Validation-specific exception handling can be expanded when request DTO validation is introduced. The current implementation establishes the contract now without adding extra validation dependencies or changing sprint scope.
