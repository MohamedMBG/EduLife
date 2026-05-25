# Enrollment Phase 1 — Completion Summary

**Date:** 2026-05-24  
**Branch:** main  
**Issues closed:** #245, #246, #247, #248

---

## What was done

### Backend

#### Task 1 — DELETE /api/v1/enrollments/{id} (issue #246)

**New endpoint:** `DELETE /api/v1/enrollments/{id}` → `204 No Content`

- **Ownership check:** resolves the Firebase-authenticated caller to their internal `User` record, then verifies `enrollment.userId == user.id`. Returns `403 Forbidden` ("You are not the owner of this enrollment") if mismatch.
- **Not-found guard:** returns `404 Not Found` ("Enrollment not found") if no enrollment with the given UUID exists.
- **Soft delete:** calls `enrollment.cancel()` which sets `status = CANCELLED`. The row is never deleted from the database, preserving audit history.
- **Auth guard:** like all `/api/v1/**` endpoints, requires a valid Firebase Bearer token; returns `401` otherwise.

Files changed:
- `Enrollment.java` — added `cancel()` method
- `EnrollmentService.java` — added `unenroll(UUID enrollmentId)` method
- `EnrollmentController.java` — added `@DeleteMapping("/{id}")` handler

---

#### Task 2 — Fix GET /api/v1/enrollments/me (issue #245)

**Changes:**
1. **Path renamed** from `GET /api/v1/enrollments` to `GET /api/v1/enrollments/me` — clearer REST semantics for a user-scoped resource.
2. **`imageUrl` added** to `EnrolledCourseDto` — the field was already present in the Android model but the backend was not populating it, causing `null` thumbnails in the enrolled list. Now reads `course.getImageUrl()` during the batch course lookup.

Files changed:
- `EnrolledCourseDto.java` — added `String imageUrl` field (position 8, before `enrolledAt`)
- `EnrollmentService.java` — included `course.getImageUrl()` in the DTO constructor call
- `EnrollmentController.java` — changed `@GetMapping` to `@GetMapping("/me")`

**Response shape (after):**
```json
[
  {
    "enrollmentId": "aaaaaaaa-...",
    "courseId": "11111111-...",
    "slug": "math-bac-sm-algebra-foundations",
    "title": "Math Bac SM - Algebra Foundations",
    "shortDescription": "A structured algebra refresher...",
    "level": "BEGINNER",
    "languageCode": "fr",
    "imageUrl": "https://images.unsplash.com/...",
    "enrolledAt": "2026-05-24T10:00:00Z"
  }
]
```

---

#### Task 3 — Enrollment tests (issue #247)

**New file:** `backend/src/test/java/com/edulife/enrollments/EnrollmentControllerTest.java`

11 tests, all green. Pattern: `@WebMvcTest` + `@MockBean EnrollmentService` — controller layer only, no DB.

| Test | Scenario | Expected |
|------|----------|----------|
| `rejectsEnrollRequestWithNoToken` | POST without Bearer token | 401 + error contract |
| `returnsConflictWhenAlreadyEnrolled` | POST duplicate course | 409 + message |
| `returnsNotFoundWhenCourseDoesNotExist` | POST unknown course | 404 + message |
| `createsEnrollmentSuccessfully` | POST valid course | 201 + enrollment body |
| `rejectsUnenrollRequestWithNoToken` | DELETE without token | 401 + error contract |
| `returnsForbiddenWhenUnenrollingAnotherUsersEnrollment` | DELETE other user's enrollment | 403 + message |
| `returnsNotFoundWhenEnrollmentDoesNotExist` | DELETE unknown ID | 404 + message |
| `unenrollsSuccessfullyReturningNoContent` | DELETE own enrollment | 204 |
| `rejectsMyEnrollmentsRequestWithNoToken` | GET /me without token | 401 + error contract |
| `returnsEnrolledCoursesWithImageUrlForAuthenticatedUser` | GET /me authenticated | 200 + imageUrl present |
| `returnsEmptyListWhenUserHasNoEnrollments` | GET /me no enrollments | 200 + empty array |

---

### Android

#### Task 4 — Wire unenroll button + enrolled course list (issue #248)

**`ApiService.java`**
- Added `@DELETE("enrollments/{id}") Call<Void> unenroll(@Path("id") String enrollmentId)`
- Changed `@GET("enrollments")` → `@GET("enrollments/me")` to match renamed backend endpoint

**`CourseRepository.java`**
- Added `UnenrollCallback` interface (`onSuccess()` / `onError(String)`)
- Added `unenroll(String enrollmentId, UnenrollCallback)` — maps 403 → ownership error message, 404 → not-found message, other non-2xx → generic message

**`UnenrollUiState.java`** (new)
- Simple state class with `loading`, `unenrolled`, `errorMessage` fields
- Factory methods: `idle()`, `loading()`, `success()`, `error(String)`

**`EnrollmentViewModel.java`**
- Added `unenrollState: MutableLiveData<UnenrollUiState>`
- Added `unenroll(String enrollmentId)`: sets loading → calls repository → on success posts `success()` then calls `loadMyEnrollments()` to auto-refresh the list

**`item_enrolled_course.xml`** (new)
- Card layout for an enrolled course row: accent bar + level badge + title + description + language
- "Unenroll" button at the bottom-right in `brand_error` red

**`fragment_courses.xml`**
- Header changed: "All Courses" → "My Courses", subtitle → "Courses you're enrolled in"
- Added `coursesEmptyText` TextView for loading / empty / error states

**`CoursesFragment.java`** (rewritten)
- Now backed by `EnrollmentViewModel` instead of hardcoded mock data
- Calls `loadMyEnrollments()` on `onViewCreated` and `onResume` (so list refreshes after enrolling from another tab)
- Filter chips (ALL / BEGINNER / INTERMEDIATE / ADVANCED) filter the enrolled list client-side by `level`
- Each row has an "Unenroll" button → calls `enrollmentViewModel.unenroll(enrollmentId)` → ViewModel auto-refreshes the list after success
- Empty/error/loading states via `coursesEmptyText`
- Uses inner `EnrolledCourseAdapter` (no external class needed)

---

## Architecture decisions

| Decision | Reason |
|----------|--------|
| Soft delete (CANCELLED status) instead of hard delete | Preserves enrollment audit history; CANCELLED enrollments are already excluded from `GET /me` via `findAllByUserIdAndStatus(ACTIVE)` |
| `GET /enrollments/me` instead of `GET /enrollments` | Follows REST convention for user-scoped resources; avoids ambiguity when admin endpoints are added later |
| Auto-refresh in ViewModel after unenroll | ViewModel calls `loadMyEnrollments()` immediately after `unenroll()` succeeds, so CoursesFragment always shows current state without manual refresh |
| Filter chips run client-side | Enrolled course lists are small; no need for a separate server round-trip per filter change |

---

## API contract summary

```
POST   /api/v1/enrollments          → 201 EnrollmentResponse | 409 | 404 | 401
DELETE /api/v1/enrollments/{id}     → 204               | 403 | 404 | 401
GET    /api/v1/enrollments/me       → 200 EnrolledCourseDto[] | 401
```
