# Task Audit - Firebase Auth OkHttp Interceptor

## Date
2026-04-28

## Task Summary
Implement a centralized OkHttp interceptor that attaches a Firebase ID token as `Authorization: Bearer <token>` on all outbound backend requests. Register the interceptor in the shared ApiClient. Ensure no duplicate token logic exists in repositories.

## Files Created
- `app/src/main/java/com/baghdad/edulife/core/network/FirebaseAuthInterceptor.java`

## Files Modified
- `app/src/main/java/com/baghdad/edulife/core/network/ApiClient.java`

## What Was Done

### FirebaseAuthInterceptor
- Implements `okhttp3.Interceptor`
- Reads `FirebaseAuth.getInstance().getCurrentUser()` on each request
- If no user signed in → proceeds without Authorization header (safe unauthenticated state)
- If user exists → calls `getIdToken(false)` via `Tasks.await()` with 10-second timeout
  - `forceRefresh=false` uses Firebase's cached token, reducing round-trips
  - `Tasks.await()` is safe on OkHttp background threads (not the main thread)
- If token is null or blank → proceeds without header (prevents invalid/partial headers)
- On any exception (timeout, network, etc.) → catches and proceeds without header (fail-open for unauthenticated flow, logged by OkHttp)
- Attaches `Authorization: Bearer <token>` only when token is valid

### ApiClient
- `FirebaseAuthInterceptor` registered as first interceptor in `OkHttpClient.Builder`
- `HttpLoggingInterceptor` registered after (so logged requests include the auth header for debug inspection)
- Singleton `Retrofit` instance reused across the app

### Duplicate Token Logic Audit
- Searched all Java source files for `Authorization`, `Bearer`, `getIdToken`, `addHeader.*token`
- `AuthRepository.prepareBackendSyncToken()` calls `getIdToken(true)` (force-refresh) but does **not** construct an Authorization header — it is a Sprint 1 stub that checks token availability before the future `/auth/sync` call
- The comment inside that method (`// Authorization: Bearer <token>`) is a TODO note, not actual header construction
- No duplicate header construction found

## Architecture Compliance
- Interceptor lives in `core/network/` per AGENTS.md section 7 (shared utilities in `core/`)
- No UI changes, no ViewModel changes, no repository token injection
- Follows security flow from AGENTS.md section 13: Android sends Bearer token with API requests
- Token attachment is centralized in one place — future features get auth automatically via ApiClient

## Code Comments Added
- `FirebaseAuthInterceptor`: comments explain forceRefresh=false rationale, Tasks.await thread safety, and fail-open behavior
- `ApiClient`: existing comment `// attaches Bearer token` preserved

## Validation / Testing
- `./gradlew build` → **BUILD SUCCESSFUL** (92 tasks, lint passed, release APK assembled)
- Lint report: no new issues
- Manual device/emulator test: sign in, then observe `HttpLoggingInterceptor` output — `Authorization: Bearer <token>` header appears on requests when user is authenticated, absent when signed out

## Risks / Notes
- **Token expiry**: Interceptor uses `forceRefresh=false`. Firebase caches tokens for ~1 hour. AGENTS.md section 13 mandates: "Android must refresh expired ID tokens safely and retry once on 401." This retry-on-401 logic is **not yet implemented** — required before Sprint 2 backend calls go live.
- **prepareBackendSyncToken stub**: When Sprint 1 `/auth/sync` call is implemented, the actual HTTP call must go through Retrofit (ApiClient) so the interceptor handles auth. The token fetched in `prepareBackendSyncToken` must not be manually passed as a header — the interceptor will handle it.
- **Logging interceptor in release builds**: `HttpLoggingInterceptor.Level.BODY` logs request/response bodies including tokens in debug. Consider gating to `BuildConfig.DEBUG` to avoid token exposure in production logs.
- **OkHttp singleton race**: `ApiClient.getClient()` is not synchronized. Safe for single-threaded init scenarios but a future improvement if multi-threaded init is possible.
