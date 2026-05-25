# Task Audit - 401 Retry and Token Refresh Guard

## Date
2026-04-28

## Task Summary
Verified and confirmed the OkHttp 401 retry + Firebase token refresh guard is fully implemented and production-safe in the Android network stack.

## Files Created
- None (implementation already complete)

## Files Modified
- None (implementation already complete)

## What Was Done
Full audit of the network stack against all verification criteria. All checks passed without modification:

1. `FirebaseTokenAuthenticator` implements `okhttp3.Authenticator` — exists under `core/network/`.
2. On 401, calls `user.getIdToken(true)` (forced refresh) inside a `synchronized (TOKEN_REFRESH_LOCK)` block.
3. Retry limit: `responseCount(response) > MAX_AUTH_RETRY_COUNT` where `MAX_AUTH_RETRY_COUNT = 1`. First 401 gets count=1 (≤1 → retry). Second 401 gets count=2 (>1 → return null). Exactly one retry.
4. Static `TOKEN_REFRESH_LOCK` object prevents parallel refresh races across concurrent 401 responses.
5. Returns `null` (no retry) and calls `firebaseAuth.signOut()` if refresh token is null/blank or throws.
6. `ApiClient` registers both `.addInterceptor(new FirebaseAuthInterceptor())` (attaches token eagerly with `getIdToken(false)`) and `.authenticator(new FirebaseTokenAuthenticator())` (handles 401 with forced refresh).
7. No repositories manually attach `Authorization` headers — `AuthRepository.java` has only a comment reference, not actual header code.
8. `./gradlew assembleDebug` compiled cleanly.

## Architecture Compliance
Network layer changes are isolated to `core/network/`. No feature modules, UI, or ViewModels were touched. Follows AGENTS.md §13 security rules and §7 Android structure.

## Code Comments Added
No code was modified. Existing code already has inline comments explaining security intent.

## Validation / Testing
- `./gradlew assembleDebug` — success, no errors
- Manual grep confirmed `Authorization`/`Bearer` only in `core/network/` files and one comment in `AuthRepository.java`
- Retry logic manually traced: count=1 on first 401 (proceeds), count=2 on second 401 (returns null) — confirmed correct

## Risks / Notes
- `TOKEN_REFRESH_LOCK` is a static field on the class, so it guards across all instances. If `ApiClient.getClient()` ever creates multiple `FirebaseTokenAuthenticator` instances (it currently creates one), the lock still works because it is static.
- `Tasks.await()` with 10s timeout blocks the OkHttp thread — acceptable since OkHttp dispatches on a thread pool, not the main thread.
- `firebaseAuth.signOut()` on refresh failure clears local Firebase state but does not navigate the user to the login screen. A future improvement would broadcast a sign-out event (e.g., via an event bus or `LiveData`) so the UI responds. Not required for MVP Sprint 1.
