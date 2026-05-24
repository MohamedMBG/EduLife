# Task Audit - Login Sync Timeout Fix

## Date
2026-05-23

## Task Summary
Fixed the Android login flow so it no longer stays stuck in a loading state after Firebase email/password login when backend identity sync does not complete.

## Files Created
- docs/2026-05-23-login-sync-timeout-fix.md

## Files Modified
- app/src/main/java/com/baghdad/edulife/core/network/ApiClient.java
- app/src/main/java/com/baghdad/edulife/features/auth/data/AuthRepository.java
- app/src/main/java/com/baghdad/edulife/features/auth/ui/LoginFragment.java
- app/src/main/res/layout/fragment_login.xml

## What Was Done
Reviewed `login_issues.logcat` and confirmed the login flow reached Firebase successfully, then issued `POST /api/v1/auth/sync` against `http://22.10.66.162:8080/api/v1/`.

Added explicit network protection around backend sync:

- Added `writeTimeout` and `callTimeout` in the shared OkHttp client so stalled requests fail instead of waiting indefinitely.
- Added a dedicated 12-second timeout guard in `AuthRepository.callBackendSync()` that cancels the Retrofit call, clears session state, signs Firebase out, and returns a readable error.
- Guarded sync callbacks with `AtomicBoolean` so only one terminal auth result is delivered even when cancellation triggers Retrofit failure callbacks afterward.
- Signed out Firebase on sync failure paths so the app does not keep a partial Firebase-only login without a synced internal EduLife session.

Improved login UX:

- Added a button label reference so the login CTA changes from `Log in` to `Signing in...` while the request is in flight.
- Disabled email and password inputs during loading so the UI state matches the pending request.
- Added a specific user-facing timeout message for backend sync failures.

## Architecture Compliance
The fix stays inside the Android auth feature and shared network layer:

- UI behavior remains in `features/auth/ui/`
- Login flow state handling remains in `features/auth/data/`
- Shared HTTP behavior remains in `core/network/`

This respects the EduLife MVVM structure and keeps business rules out of the Fragment.

## Code Comments Added
Added comments in:

- `ApiClient.java` to explain why auth sync now fails fast for unreachable backend IPs.
- `AuthRepository.java` to explain why the in-flight sync call is cancelled and why Firebase is signed out on sync failure.
- `LoginFragment.java` to explain why the button text mirrors the backend sync wait state.

These comments document non-obvious login and session consistency behavior.

## Validation / Testing
Validated with:

- `login_issues.logcat` review to confirm the failure occurs after Firebase login during backend sync.
- `./gradlew.bat assembleDebug` completed successfully on 2026-05-23.

Manual validation still recommended on device:

- Start the backend and verify login succeeds.
- Stop or block the backend and verify login returns the timeout/network error instead of hanging.

## Risks / Notes
The root environmental dependency remains unchanged: the phone must be able to reach `http://22.10.66.162:8080/api/v1/`.

If the backend is down, on a different network, or not listening on that IP from the device, login will now fail clearly instead of hanging, but it still cannot complete successfully until backend connectivity is fixed.
