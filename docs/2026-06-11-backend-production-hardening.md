# Backend Production Hardening — Sprint 9

## Goal
Harden the backend for production: rate-limit responses with Retry-After, restrict actuator,
validate storage directories at startup, complete env-var documentation, and pin the security
audit properties (email_verified, ownership isolation, correct answers not serialised) with tests.

## What Changed

### Rate-limit Retry-After header (`RateLimitFilter.java`)
Replaced `bucket.tryConsume(1)` with `bucket.tryConsumeAndReturnRemaining(1)`.  When the
bucket is exhausted, the filter now sets a `Retry-After` header (seconds, ceiling of
nanoseconds-to-refill ÷ 1e9) before writing the 429 response.  Clients can now retry at the
right time without guessing or hammering the endpoint.

### Actuator exposure (`application.yaml`)
Added `management.endpoints.web.exposure.include=health` and
`management.endpoint.health.show-details=never`.  This makes the restriction explicit and
ensures that env, beans, metrics, and all other sensitive endpoints are never exposed via
the web regardless of Spring Boot version defaults.

### Storage readiness check at startup (`StorageReadinessCheck.java`)
New `@Component` with `@PostConstruct`.  On startup it:
1. Calls `Files.createDirectories` for both the avatar and certificate storage dirs.
2. Asserts both dirs are writable.
3. Throws `IllegalStateException` with a clear message if either check fails — the app
   will refuse to start rather than failing silently on the first upload.

### Env-var documentation (`.env.example`)
Added all missing variables: `EDULIFE_AVATAR_STORAGE_DIR`, `EDULIFE_AVATAR_PUBLIC_BASE_URL`,
`EDULIFE_AVATAR_MAX_FILE_BYTES`, `EDULIFE_CERTIFICATES_STORAGE_DIR`,
`EDULIFE_CERTIFICATES_PUBLIC_BASE_URL`.  Annotated each as Required / Optional with prod
example values and a note about the startup writability check.

### CORS comment clarified (`application.yaml`)
Added note that an empty `APP_CORS_ALLOWED_ORIGINS` closes CORS entirely, and that prod
must inject the deployed website URL.

## Files Touched
- `backend/src/main/java/com/edulife/config/RateLimitFilter.java`
- `backend/src/main/java/com/edulife/config/StorageReadinessCheck.java` (new)
- `backend/src/main/resources/application.yaml`
- `backend/.env.example`
- `backend/src/test/java/com/edulife/security/RateLimitRetryAfterTest.java` (new)
- `backend/src/test/java/com/edulife/exams/ExamAnswerSecurityTest.java` (new)

## Security Audit — Findings

| Property | Verdict | Evidence |
|---|---|---|
| `email_verified` enforced | ✅ Already correct | `FirebaseTokenFilter` L82 — 403 if false; test `rejectsUnverifiedEmailOnProtectedEndpoint` |
| Correct answers never serialised | ✅ Already correct | `ExamDto.ChoiceDto` has only `choiceId` + `choiceText`; new `ExamAnswerSecurityTest` pins it |
| Cross-user ownership — exam | ✅ Already correct | `ExamService.resolveCurrentUser()` uses Firebase UID; enrollment check is by authenticated user |
| Cross-user ownership — profile | ✅ Already correct | `ProfileService.resolveCurrentUser()` same pattern; no user ID accepted from client |
| Cross-user ownership — certificates | ✅ Already correct | `CertificateService.getCertificateById()` checks `userId` against `cert.getUserId()` |
| CORS no wildcard | ✅ Already correct | `CorsConfig.setAllowedOrigins()` — explicit list only; empty list closes CORS |
| Actuator restricted | ✅ Now explicit | `management.endpoints.web.exposure.include=health` in yaml |
| Rate limit Retry-After | ✅ Fixed this sprint | See above |
| Storage dirs writable | ✅ Fixed this sprint | `StorageReadinessCheck` validates on startup |

## Backend Impact
- 429 responses now include `Retry-After` header — clients can retry at the correct time.
- Startup fails hard if storage dirs are not writable — prevents silent failures in prod.
- Only `/actuator/health` is reachable over HTTP — internal details not exposed.

## Android Impact
None — Android already handles 429 via `RetryAfter` or exponential backoff in OkHttp.

## Web Impact
None — web already handles 429 in the API client error handler.

## Architecture Compliance
No new modules, no schema changes, no new endpoints.  All changes are in the security and
config layers, consistent with the modular monolith architecture.

## Tests / Verification
- 104 tests — 0 failures, 0 errors (including 3 new + 1 new).
- `RateLimitRetryAfterTest` — 3 tests: cert-verify and exam-submit buckets exhausted → Retry-After present and > 0; within-budget request → no header.
- `ExamAnswerSecurityTest` — 1 test: exam GET response JSON has no `isCorrect`/`correct` fields at choice level.

## Risks / Notes
- `StorageReadinessCheck` runs in the full Spring context.  `@WebMvcTest` slices do not
  load `@Component` beans so existing tests are unaffected.
- `Refill.intervally` restores all tokens at the end of the window (burst-at-end model).
  `getNanosToWaitForRefill()` returns the time until the next full refill, so `Retry-After`
  reflects the worst-case wait — always safe from the server's perspective.
