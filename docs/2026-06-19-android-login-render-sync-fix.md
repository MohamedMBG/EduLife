# Task Audit - Android Login Render Sync Fix

## Date
2026-06-19

## Task Summary
Fixed an Android login failure where the app showed "Cannot reach the server" during backend auth sync even when the Render backend was reachable.

## Files Created
- docs/2026-06-19-android-login-render-sync-fix.md

## Files Modified
- app/src/main/java/com/baghdad/edulife/core/network/ApiService.java
- app/src/main/java/com/baghdad/edulife/core/network/FirebaseAuthInterceptor.java
- app/src/main/java/com/baghdad/edulife/features/auth/data/AuthRepository.java

## What Was Done
The login flow already forced a fresh Firebase ID token before calling `POST /api/v1/auth/sync`, but the shared OkHttp interceptor fetched a second token again before sending the request.

That second fetch could fail independently on mobile networks and surfaced as a Retrofit transport failure, which the login screen translated into the generic "Cannot reach the server" message even when the Render API was online.

Implemented a smaller and more reliable auth-sync path:

- `AuthRepository` now reuses the freshly fetched Firebase ID token for `/api/v1/auth/sync` instead of forcing another token lookup indirectly through the interceptor.
- `ApiService` auth-sync methods now accept an explicit `Authorization` header for that endpoint only.
- `FirebaseAuthInterceptor` now respects an existing `Authorization` header so the manual auth-sync header is not overwritten by another token fetch.

This keeps the rest of the app on the shared authenticated Retrofit pipeline while removing the duplicate token-fetch failure point from login.

## Architecture Compliance
The change stays inside the Android shared networking layer and auth feature:

- shared API contract changes remain in `core/network/`
- token/header handling remains in `core/network/`
- login/sync orchestration remains in `features/auth/data/`

No UI logic, backend business logic, or unrelated features were moved outside their existing EduLife architecture boundaries.

## Code Comments Added
Added comments in:

- `ApiService.java` to explain why `/auth/sync` intentionally accepts an explicit Bearer token
- `FirebaseAuthInterceptor.java` to explain why pre-supplied auth headers must be preserved
- `AuthRepository.java` to explain why the freshly refreshed token is reused for backend sync

These comments document the non-obvious mobile auth reliability rule behind the fix.

## Validation / Testing
Validated with:

- local inspection of generated debug `BuildConfig` confirming the app targets `https://edulife-2bro.onrender.com/api/v1/`
- direct request to `https://edulife-2bro.onrender.com/api/v1/auth/sync` confirming the Render backend is reachable
- `.\gradlew.bat :app:assembleDebug` which completed successfully on 2026-06-19

Manual validation still recommended:

- log in on the emulator/device with the Render backend
- confirm login now reaches the correct dashboard instead of showing the server-unreachable error

## Risks / Notes
This fix removes one Android-side false failure path, but it does not solve genuine backend cold-start latency. If Render takes too long to wake up, login can still fail on timeout and require a retry after the service warms.

If login still fails after this change, the next check should be device logcat for the exact sync exception or a backend-side Firebase Admin token-validation issue.
