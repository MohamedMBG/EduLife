# Task Audit — Course List Endpoint (Sprint 2)

## Date
2026-05-01

## What Was Built

`GET /api/v1/courses` — paginated list of published courses, with optional level filter, protected by the Firebase token filter.

---

## Files Created

| File | Purpose |
|------|---------|
| `courses/model/CourseStatus.java` | Enum for course lifecycle states |
| `courses/entity/Course.java` | JPA entity mapped to `courses` table |
| `courses/repository/CourseRepository.java` | Spring Data queries for published courses |
| `courses/dto/CourseListItemResponse.java` | Safe response shape sent to Android |
| `courses/service/CourseService.java` | Business logic, pagination, mapping |
| `courses/controller/CourseController.java` | HTTP endpoint declaration |

---

## Files Modified

None. No existing file was changed.

---

## How It Works — Step by Step

### 1. Request arrives

Android sends:
```
GET /api/v1/courses?page=0&size=20&level=BEGINNER
Authorization: Bearer <Firebase ID token>
```

### 2. Token filter runs first

`FirebaseTokenFilter` (already built in Sprint 1) intercepts the request before it reaches the controller.
It verifies the Firebase token signature, checks `email_verified`, and puts the user identity in `SecurityContext`.
If the token is missing or invalid, the request is rejected with `401` before the controller is ever called.
**No auth code was added to the courses module** — `SecurityConfig` already declares that all `/api/v1/**` requests require authentication.

### 3. Controller receives the call

`CourseController.listCourses()` reads three query parameters:
- `level` (optional) — filters by course difficulty level, e.g. `BEGINNER`
- `page` (default `0`) — zero-based page index
- `size` (default `20`) — number of courses per page

These are passed directly to `CourseService`.

### 4. Service applies business rules

`CourseService.listPublishedCourses()` does three things:

**a) Clamps page size to 50.**
A client asking for `size=1000` would generate a huge DB query and a huge response. The service caps it at 50 regardless of what the caller sends. This is a backend rule, not a client trust — the client cannot bypass it.

**b) Sorts by `publishedAt` descending.**
Newest published courses appear first. This is the natural discovery order — the learner sees what was published most recently, not what was created first.

**c) Filters by status = `PUBLISHED` always.**
`DRAFT` and `ARCHIVED` courses are never returned to learners regardless of any request parameter. The filter is applied in the repository query, not in application code after fetching — this means the DB never returns draft rows to the service layer. There is no "show all if admin" bypass here because the learner catalog does not need it in Sprint 2.

### 5. Repository runs the query

Two Spring Data query methods exist:
- `findAllByStatus(PUBLISHED, pageable)` — used when no level filter
- `findAllByStatusAndLevel(PUBLISHED, level, pageable)` — used when level is provided

Spring Data generates the SQL automatically from the method names. Both methods return `Page<Course>`, which carries the content rows plus total count metadata (needed by Android for pagination UI).

### 6. Entity is mapped to DTO

`Course` entity → `CourseListItemResponse` record.

**Why a separate DTO?** The `Course` entity has a `createdByUserId` column that references the teacher who created the course. That field must never appear in a learner-facing response. The DTO is the firewall: it only contains fields that are safe to expose.

Fields in `CourseListItemResponse`:
- `id` — UUID, used by Android to navigate to course detail
- `slug` — URL-friendly identifier, used by Android for deep links later
- `title` — displayed on the course card
- `shortDescription` — displayed below the title on the card
- `level` — displayed as a chip badge (BEGINNER / INTERMEDIATE / ADVANCED)
- `languageCode` — displayed as a language tag
- `publishedAt` — used for display ordering confirmation and UI date labels

Fields intentionally omitted from the response:
- `description` (full) — too long for a list; returned in detail endpoint only
- `createdByUserId` — internal field, never exposed to learners
- `status` — always `PUBLISHED` in this endpoint; sending it would be redundant
- `createdAt` / `updatedAt` — internal timestamps, no learner UI value

### 7. Spring serializes the Page

Spring Boot serializes `Page<CourseListItemResponse>` to JSON automatically. The response shape is:
```json
{
  "content": [ { "id": "...", "title": "...", ... } ],
  "totalElements": 5,
  "totalPages": 1,
  "number": 0,
  "size": 20,
  "first": true,
  "last": true
}
```
Android reads `content` to render the list, `totalPages` to know when to stop paginating.

---

## Why Each Decision Was Made

### Why not filter by category instead of level?
The `courses` table (built in `V2__courses.sql`) has no `category` column — it has `level` (BEGINNER, INTERMEDIATE, ADVANCED) and `language_code`. The execution plan says category filter chips should be "hardcoded enum" in Sprint 2. `level` is that enum. A proper `categories` table with many-to-many joins would require Sprint 2A CMS work we are explicitly deferring.

### Why is size clamped at 50 in the service, not as a validation annotation?
A `@Max(50)` annotation on the controller param would return a `400 Bad Request` if the client sends `size=51`. Clamping silently is better UX for a mobile client — no crash, just a corrected page. The client is not cheating; it may simply not know the server limit.

### Why `@Transactional(readOnly = true)` on the service method?
Read-only transactions let the database (and Hibernate) optimise the query: no dirty-check, no flush, potentially read from a replica in future. No data is written in this call so there is no reason to open a write transaction.

### Why are `CourseRepository` methods derived from method names, not `@Query`?
The queries are simple two-condition filters. Spring Data method name derivation handles this correctly and keeps the code readable without raw JPQL strings. A `@Query` annotation would only be needed if the query requires a join, subquery, or something the naming convention cannot express.

### Why no `CourseNotFoundException`?
A paginated list endpoint returns an empty `content: []` when no courses match — this is correct HTTP behaviour. `404` on a list endpoint would be wrong (it means the resource path doesn't exist, not that the filter returned zero results).

### Why did we not add auth logic to CourseController?
`SecurityConfig` already declares `.requestMatchers("/api/v1/**").authenticated()`. Every route under `/api/v1/` is protected globally. Adding `@PreAuthorize` or manual auth checks in the controller would be redundant and would create a second, weaker enforcement layer.

---

## Architecture Compliance

- Follows `com.edulife.<module>.<layer>` package structure used by `users` and `auth` modules.
- No cross-module imports (courses module does not import from `auth` or `users`).
- No CMS, enrollment, or exam tables touched.
- No mock interceptors or test-only endpoints introduced.
- Learner-facing endpoint enforces published-only at the repository query level, not in memory after fetch.

---

## Validation

- `./mvnw.cmd compile` — clean, zero warnings, zero errors.
- `./mvnw.cmd test` — 17 tests, 0 failures, 0 errors. All pre-existing tests continue to pass. No new tests were broken.

---

## Manual Testing Required

Before marking Sprint 2 backend done, run these steps against the live backend:

1. Start backend: `./mvnw.cmd spring-boot:run`
2. Run Flyway migrations — confirm `V2__courses.sql` and `V3__seed_courses.sql` applied successfully
3. Get a real Firebase ID token from the Firebase console (Authentication → Users → copy token via REST API)
4. Call `GET /api/v1/courses` with `Authorization: Bearer <token>` — expect 5 courses in `content`
5. Call `GET /api/v1/courses?level=BEGINNER` — expect only courses with `level = BEGINNER`
6. Call `GET /api/v1/courses?size=2` — expect 2 courses in `content`, `totalPages > 1`
7. Call `GET /api/v1/courses` with no token — expect `401 {"status":401,"message":"Authentication required"}`

---

## What Comes Next

Sprint 2 backend still needs:

- `GET /api/v1/courses/{id}` — course detail with full description, sections list, and lessons per section

After that, Sprint 2 Android:
- `CourseApiService`, `CourseRepository` (Android), `CourseCatalogViewModel`, `CourseListFragment`
- `CourseDetailFragment` + `CourseDetailViewModel`
- Navigation wire: Login success → CourseList → CourseDetail
- Loading / empty / error states on both screens
