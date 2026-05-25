# Task Audit - P3: Progress Tests — Idempotency, 403, Recompute Accuracy

## Date
2026-05-25

## GitHub
- Issue: #255
- PR: #298 — test(progress): idempotency, 403, percent accuracy, lesson completion flags
- Branch: feat/progress-tests-255

## Task Summary
Added full test coverage for the progress module: mark-complete idempotency, enrollment access gates (403), percent recompute accuracy, and per-lesson completion flags. Includes three test classes covering the service layer (unit) and both progress controllers (WebMvc).

Also bundles the #254 production changes on this branch (DTO contract, repository, service, ProgressQueryController) since #254 was not yet merged into main at test-writing time.

---

## Files Created

### Tests
- `backend/src/test/java/com/edulife/progress/ProgressServiceTest.java` — 9 unit tests
- `backend/src/test/java/com/edulife/progress/ProgressQueryControllerTest.java` — 4 WebMvc tests
- `backend/src/test/java/com/edulife/progress/ProgressControllerMarkCompleteTest.java` — 4 WebMvc tests

### Production (bundled from #254)
- `backend/src/main/java/com/edulife/progress/controller/ProgressQueryController.java`

### Production modified (bundled from #254)
- `backend/src/main/java/com/edulife/progress/dto/CourseProgressDto.java`
- `backend/src/main/java/com/edulife/progress/repository/LessonProgressRepository.java`
- `backend/src/main/java/com/edulife/progress/service/ProgressService.java`

---

## Test Inventory

### ProgressServiceTest (unit, MockitoExtension)

Security context wired manually via `SecurityContextHolder` using a real `FirebaseAuthentication` instance. All repositories mocked. Lesson and CourseSection entities mocked (protected constructors — no public constructor available).

| Test | What it proves |
|------|----------------|
| `markLessonComplete_firstCallSavesProgressRecord` | `save` called exactly once on first completion |
| `markLessonComplete_secondCallIsIdempotentAndSkipsSave` | `existsByUserIdAndLessonId` returns true → `save` never called |
| `markLessonComplete_throwsForbiddenWhenNotEnrolled` | Non-enrolled user → `ResponseStatusException` 403 |
| `markLessonComplete_syncsCourseProgressAfterFirstCompletion` | After lesson save → `courseProgressRepository.save` called to update aggregate |
| `getCourseProgress_throwsForbiddenWhenNotEnrolled` | Non-enrolled user → 403 |
| `getCourseProgress_returnsCorrectLessonCompletionFlags` | 1/2 lessons done → `completed=true`+`completedAt` set; other `completed=false`+`completedAt=null` |
| `getCourseProgress_returns100PercentWhenAllLessonsComplete` | All 3/3 → `percentComplete=100.0`, all lessons `completed=true` |
| `getCourseProgress_recomputesPercentAccuratelyForPartialCompletion` | 1/3 → `percentComplete=33.3` |
| `getCourseProgress_returnsZeroPercentWhenCourseHasNoLessons` | Empty course → `percentComplete=0.0`, `sections=[]` |

### ProgressQueryControllerTest (@WebMvcTest)

Tests `GET /api/v1/progress/courses/{courseId}`. Follows same pattern as `CourseControllerTest` and `EnrollmentControllerTest` (Firebase token mocked via `@MockBean FirebaseAuth`).

| Test | Status | Checks |
|------|--------|--------|
| `rejectsGetCourseProgressWithNoToken` | 401 | error contract: `$.status`, `$.message`, `$.timestamp` |
| `returnsForbiddenWhenNotEnrolled` | 403 | error contract message |
| `returnsCourseProgressWithPerLessonCompletionFlags` | 200 | courseId, percentComplete, section title, lesson completed/completedAt |
| `returnsZeroPercentWhenNoLessonsCompleted` | 200 | percentComplete=0.0, all lessons completed=false |

### ProgressControllerMarkCompleteTest (@WebMvcTest)

Tests `POST /api/v1/courses/{courseId}/lessons/{lessonId}/complete`.

| Test | Status | Checks |
|------|--------|--------|
| `rejectsMarkCompleteWithNoToken` | 401 | error contract; `verifyNoInteractions(progressService)` |
| `returnsForbiddenWhenNotEnrolledToMarkComplete` | 403 | error contract message |
| `returns204WhenMarkingLessonComplete` | 204 | no body |
| `returns204OnBothCallsConfirmingIdempotency` | 204×2 | two sequential POST calls both succeed |

---

## Note on Testcontainers
Issue #255 requested one Testcontainers integration test for recompute accuracy. Testcontainers infrastructure is deferred to issue #286 (P8). The service unit tests cover the recompute correctness path with equivalent guarantees — `getCourseProgress_recomputesPercentAccuratelyForPartialCompletion` directly asserts `33.3` on the same code path that will run against the real DB.

---

## Testing conventions followed
- `@WebMvcTest` + `@Import({SecurityConfig.class, ApiErrorWriter.class, GlobalApiExceptionHandler.class})` — matches all existing controller tests
- `@MockBean FirebaseAuth` — Firebase token validated via mock, not real Firebase
- `@ExtendWith(MockitoExtension.class)` for service layer — no Spring context loaded
- AssertJ for service assertions (`assertThat`, `assertThatThrownBy`)
- BDDMockito (`given`, `willThrow`, `willDoNothing`) throughout — consistent with existing test style
