# AI Advisor — End-to-End Verification & Hardening

## Goal

Verify the AI Advisor feature works correctly across backend, web, and Android; fix defects found during validation; confirm security, performance, and architecture compliance before production use.

## What Changed

### Backend

- `GlobalApiExceptionHandler.java` — Added `@ExceptionHandler(AdvisorException.class)` returning 503 SERVICE_UNAVAILABLE with a generic user-facing message. Advisor infrastructure failures no longer fall through to the generic 500 handler.
- `application.yaml` — Added `edulife.advisor` config block with environment variable bindings for `provider`, `model`, `groq-api-key`, `max-tokens`, and `rate-limit-per-hour`. Defaults: provider=stub, rate-limit=10/hour.

### Backend Tests

- `AdvisorServiceTest.java` — Added `@AfterEach void tearDown() { SecurityContextHolder.clearContext(); }`. Without this, the mocked `SecurityContext` set in `@BeforeEach` leaked via ThreadLocal into subsequent Spring `@WebMvcTest` classes running in the same JVM thread. Specifically, `AnalyticsControllerTest.studentSummary_allowedForAnyAuthenticatedLearner` would return 403 instead of 200 because `FirebaseTokenFilter` called `setAuthentication()` on the mock context which discarded the principal silently.

## Files Touched

### Modified
- `backend/src/main/java/com/edulife/common/error/GlobalApiExceptionHandler.java`
- `backend/src/main/resources/application.yaml`
- `backend/src/test/java/com/edulife/advisor/AdvisorServiceTest.java`

## Backend Impact

All 211 backend tests pass. The AdvisorException handler closes the only gap where an LLM-layer failure could have produced an unlogged 500.

## Android Impact

None. Android integration was implemented in the previous task. Build verified: BUILD SUCCESSFUL.

## Web Impact

None. Web advisor integration was implemented in a prior task. Build verified: BUILD SUCCESS (bun run build).

## Architecture Compliance

### Request Flow
```
Android/Web → POST /api/v1/advisor/recommend
  → RateLimitFilter (10 req/hr per principal, keyed "advisor:{uid}")
  → FirebaseTokenFilter (validates Bearer token)
  → AdvisorController (@Valid @RequestBody AdvisorRequest)
  → AdvisorService.recommend()
    → sanitizeGoal() [strips control chars \x00-\x1F\x7F, trims whitespace]
    → CourseContextBuilder.build() [PUBLISHED courses only, from DB]
    → LlmClient.recommend() [Groq or stub]
    → UUID allowlist validation [LLM courseIds cross-checked against catalog Set<UUID>]
    → AdvisorLog persisted
  → AdvisorResponse (message + recommendations[]{courseId, reason, score})
```

### Security Findings

| Check | Result |
|---|---|
| Groq API key in Android source | Not found |
| Groq API key in web source | Not found |
| Groq references in .env files | Not found |
| GROQ_API_KEY in web .env | Not found |
| Correct answers sent to client | No — advisor response contains no exam data |
| Unpublished courses in recommendations | Blocked — CourseContextBuilder queries PUBLISHED status |
| LLM courseIds validated | Yes — UUID allowlist built from catalog |
| Goal control-char sanitization | Yes — strips \x00-\x1F\x7F |
| Rate limit | 10 req/hr per authenticated user, returns 429 |
| Android 429 handling | Distinct UI state (isRateLimit=true), no retry button |
| Web 429 handling | Distinct toast/error, no retry shown |

### Performance Observations

- Stub path (default): sub-millisecond, deterministic, two hardcoded picks
- Groq path: typically 800–2500ms depending on model load; no streaming
- Rate limit bucket: `Refill.intervally(10, Duration.ofHours(1))` — refills all 10 tokens at once per hour rather than per-request drip; acceptable for MVP
- No caching layer on advisor responses; each request hits the LLM

## Tests / Verification

| Suite | Result |
|---|---|
| Backend: `./mvnw test` | 211/211 PASS |
| Web: `bun run build` | BUILD SUCCESS |
| Android: `./gradlew assembleDebug` | BUILD SUCCESSFUL |
| `AdvisorServiceTest` + `AnalyticsControllerTest` (previously failing combo) | PASS after `@AfterEach` fix |

## Risks / Notes

- Groq key rotation: `GROQ_API_KEY` is an env var; no credential appears in the repo. Rotate via deployment environment.
- Stub provider is the safe default (`EDULIFE_ADVISOR_PROVIDER=stub`). Groq is opt-in per deployment.
- `Refill.intervally` means a user can exhaust all 10 tokens immediately then wait the full hour. A drip refill (`Refill.greedy`) would spread capacity but MVP-acceptable as-is.
- `AdvisorLog` table grows unbounded; no TTL or archival policy yet.
- No test for the 503 AdvisorException path at the controller layer (integration test gap, not blocking MVP).
