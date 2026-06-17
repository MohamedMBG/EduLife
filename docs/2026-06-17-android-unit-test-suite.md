# Android JVM Unit Test Suite

## Goal

Add a serious Android **host-JVM** unit test suite for the most important app logic, without
calling the real backend, Firebase, Cloudinary, or Groq, and without rewriting the Java/XML MVVM
architecture. Priority areas: auth sync fail-closed behavior, SessionStorage, exam builder
validation, advisor response mapping, lesson content-type decisions, and profile/avatar mapping.

## What Changed

The decisive logic for several priority areas lived inside Android-coupled classes
(`AndroidViewModel`s that build Firebase/Retrofit in their constructors, `EncryptedSharedPreferences`,
and a Fragment), which cannot run on the host JVM. Rather than add heavy frameworks
(Robolectric / Mockito-inline / MockWebServer) or rewrite the architecture, the pure decision logic
was extracted into small, framework-free helpers that production code now delegates to. Tests then
assert the **real** production decision paths.

Key behavior change (approved): **Android login is now fail-closed.** Previously
`AuthViewModel.login()` posted an authenticated success regardless of the `/auth/sync` result, so a
Firebase login with a failed/unreachable backend sync still navigated forward (to Home with a null
role). It now posts success **only** when backend sync succeeds with a complete identity.

### Test dependencies added

None. The suite uses the existing **plain JUnit 4.13.2** harness (`testImplementation(libs.junit)`)
already declared in `app/build.gradle.kts`. Gson is available on the unit-test classpath transitively
via `converter-gson`, so the mapping tests need no new dependency. No Mockito, Robolectric,
MockWebServer, or `androidx.arch.core:core-testing` were added (kept minimal per task constraints).

## Files Touched

### Production (minimal, for testability + the approved fail-closed fix)

- `app/.../features/auth/model/AuthSyncDecision.java` — **new** pure helper encoding the fail-closed
  rule (`fromSyncResponse(...)` and `isAuthenticated(...)`).
- `app/.../features/auth/data/AuthRepository.java` — `callBackendSync` now delegates the
  userId/role validation to `AuthSyncDecision.fromSyncResponse(...)`. Behavior-preserving (identical
  messages and outcomes); only the persisted-on-success path is unchanged.
- `app/.../features/auth/viewmodel/AuthViewModel.java` — `login()` now posts success only when
  `AuthSyncDecision.isAuthenticated(syncResult)` is true; otherwise posts an error. **Fail-closed
  behavior change.** The error message passes through the existing sync message strings that
  `LoginFragment.friendlyMessage` already maps to friendly text.
- `app/.../core/storage/SessionStorage.java` — added a **package-private** `SessionStorage(SharedPreferences)`
  constructor as a test seam. The production `SessionStorage(Context)` path is untouched.
- `app/.../features/courses/model/LessonContentTypeResolver.java` — **new** pure helper holding the
  "which content surface to show" decision and the PDF viewer-URL wrapping (`resolveViewerUrl`),
  both moved out of the Fragment.
- `app/.../features/courses/ui/LessonPlayerFragment.java` — `bindLessonContent` now delegates the
  structural decision to `LessonContentTypeResolver.resolve(...)` and maps the result onto views;
  the per-type button label wording is preserved via a small `articleButtonLabel(...)` helper. The
  inline `resolveViewerUrl` was removed in favor of the helper. UI behavior unchanged.

### Tests (new)

- `app/src/test/.../features/auth/AuthSyncDecisionTest.java` — 12 tests.
- `app/src/test/.../core/storage/SessionStorageTest.java` — 9 tests.
- `app/src/test/.../core/storage/FakeSharedPreferences.java` — in-memory `SharedPreferences` test util.
- `app/src/test/.../features/advisor/AdvisorResponseMappingTest.java` — 6 tests.
- `app/src/test/.../features/courses/LessonContentTypeResolverTest.java` — 20 tests.
- `app/src/test/.../features/profile/ProfileResponseMappingTest.java` — 5 tests.

### Tests (pre-existing, untouched — not deleted)

- `ExampleUnitTest` (1), `features/analytics/AnalyticsFormatTest` (3),
  `features/exams/CmsExamValidatorTest` (21 — already covers exam builder validation thoroughly).

## Features Covered

1. **Auth sync fail-closed** — `AuthSyncDecision` (used by both `AuthRepository` and `AuthViewModel`).
2. **SessionStorage** — save / read / clear, role persistence, session-completeness, no partial session.
3. **Exam builder validation** — `CmsExamValidator` (pre-existing suite, retained).
4. **Advisor response mapping** — Gson deserialization of `AdvisorResponse` / `AdvisorRecommendation`.
5. **Lesson content-type decisions** — `LessonContentTypeResolver`.
6. **Profile/avatar mapping** — Gson deserialization of `ProfileResponse` / `AvatarUploadResponse`.

## Important Test Cases

- Firebase login + sync success ⇒ authenticated; + sync failure / network error / null result ⇒
  **not** authenticated (no success state).
- Sync response missing/blank userId or role ⇒ not authenticated, "incomplete data" message.
- Registration sync failure ⇒ never authenticated (cannot reach dashboard/home).
- Session present only when **both** userId and role exist; clear wipes everything incl. pending role;
  no API writes userId/role in isolation (no half-written session).
- Advisor: parses message, recommendations (courseId/reason/score/matchedSkills), `source`
  (`groq` and `deterministic-fallback`), empty recommendations, missing `source`/`recommendations`
  (null, no crash).
- Lesson: VIDEO (±body), TEXT (body / url-fallback / fallback), ARTICLE/LINK (url / body / fallback),
  PDF & RESOURCE (action enabled/disabled by url presence), unknown/null type fallback, blank
  whitespace treated as absent, PDF URLs wrapped in Google Docs viewer (by type and by `.pdf`).
- Profile: full profile incl. counts, Cloudinary HTTPS avatar URL preserved exactly, missing
  avatarUrl ⇒ null (UI fallback), numeric fields default to 0; avatar upload URL parsed / null-safe.

## Backend Impact

None. No backend files changed; no calls to the backend, Firebase, Cloudinary, or Groq from tests.

## Android Impact

- New fail-closed login behavior (described above) — the primary intended behavior change.
- Two pure helpers extracted; one Fragment and one repository now delegate to them. SessionStorage
  gained a test-only constructor. No architecture change, still Java + XML MVVM, no new runtime deps.

## Web Impact

None.

## Architecture Compliance

- Java only, XML MVVM preserved; no Kotlin, Hilt/Dagger, or DI framework introduced.
- Business/decision logic kept out of UI: extracted into plain helpers (`AuthSyncDecision`,
  `LessonContentTypeResolver`), mirroring the existing `CmsExamValidator` pattern.
- No JPA/entity exposure concerns (client side). No `firebase_uid` handling changed.
- Security posture improved: client now refuses to treat a user as authenticated until the backend
  confirms identity, aligning the Android client with the documented fail-closed requirement.

## Tests / Verification

Commands run from repo root:

```bash
./gradlew :app:testDebugUnitTest   # BUILD SUCCESSFUL — 77 tests, 0 failures, 0 errors
./gradlew :app:assembleDebug       # BUILD SUCCESSFUL
```

Per-class results (`app/build/test-results/testDebugUnitTest`): AuthSyncDecisionTest 12,
SessionStorageTest 9, AdvisorResponseMappingTest 6, LessonContentTypeResolverTest 20,
ProfileResponseMappingTest 5, CmsExamValidatorTest 21, AnalyticsFormatTest 3, ExampleUnitTest 1.
All green. No network, no emulator, no live backend.

## Risks / Notes

- **Behavior change:** fail-closed login means a user whose Firebase login succeeds but whose backend
  `/auth/sync` fails (server down / unreachable) now sees an error instead of reaching Home. This is
  the intended product rule. The friendly-message mapping in `LoginFragment` already handles the
  sync failure strings, so UX copy is unchanged.
- `LessonPlayerFragment.bindLessonContent` was refactored to delegate; the structural visibility
  outcomes and button-label wording were reproduced exactly, but this is UI code without unit
  coverage — see instrumentation recommendation below.

## Remaining Coverage Gaps

- **Repositories** (`AuthRepository`, `AdvisorRepository`, `ProfileRepository`, `CmsExamRepository`)
  build `ApiClient.getClient()` in their constructors, which constructs `FirebaseAuthInterceptor` /
  `FirebaseTokenAuthenticator` (both call `FirebaseAuth.getInstance()`), so they cannot be
  instantiated on the host JVM. Their HTTP status-code → callback branching is therefore not unit
  tested. (Mapping of the response bodies they return *is* covered via the Gson tests.)
- **ViewModels** (`AuthViewModel`, `AdvisorViewModel`, `CmsExamBuilderViewModel`, `ProfileViewModel`)
  are `AndroidViewModel`s and/or construct repositories in field initializers, so they are not
  host-JVM constructible. Their decision logic is covered indirectly through the extracted helpers
  they delegate to.
- **SessionStorage encryption path** (EncryptedSharedPreferences / Keystore) is exercised only via
  the in-memory fake; the real encrypted read/write is not unit tested.
- **Avatar client validation:** there is no client-side type/size rejection — `ProfileFragment`
  downsizes any picked image to a ≤1024px JPEG (Android `Bitmap`, instrumentation-only) and the
  backend enforces the 5MB limit. So no avatar size/type unit tests were added (none to test).
- **LessonPlayerFragment view wiring** (visibility toggles, click handlers) is not unit tested.

## Suggested Future Instrumentation / UI Tests

- Robolectric or instrumented tests for the repositories using **MockWebServer** + an injectable
  `ApiService` (would require a small DI seam) to cover HTTP status-code branching (401/403/404/409/429/timeout).
- Instrumented `AuthViewModel` test (with a fake `AuthRepository`) asserting `AuthUiState` transitions
  end-to-end, plus a `LoginFragment` navigation test confirming fail-closed (no nav on sync failure).
- Espresso/instrumented `LessonPlayerFragment` test asserting the correct content view becomes
  visible for each lesson type.
- Instrumented `SessionStorage` test against real `EncryptedSharedPreferences` (Keystore-backed).
- Instrumented avatar pipeline test (bitmap scaling + multipart upload via MockWebServer).
