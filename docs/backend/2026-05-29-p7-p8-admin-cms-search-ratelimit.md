# Backend P7+P8 — Admin CMS, Full-Text Search, Rate Limiting

**Date:** 2026-05-29
**Branch:** `feat/backend-p7-p8-admin-cms-search-ratelimit`
**Issues closed:** #272, #273, #274, #275, #278, #280

---

## Overview

This document audits all changes introduced in this branch. Five backend capabilities were added across two phases (P7 Administration and P8 Polish):

1. **Admin user management** (P7 #272)
2. **CMS course CRUD** (P7 #273)
3. **CMS sections + lessons management** (P7 #274)
4. **CMS exam authoring** (P7 #275)
5. **Full-text course search** (P8 #280)
6. **Rate limiting** (P8 #278)

---

## 1. Admin User Management (#272)

### Endpoints

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | `/api/v1/admin/users` | ADMIN | List all users, optional `?role=TEACHER` filter |
| PUT | `/api/v1/admin/users/{id}/role` | ADMIN | Promote or demote a user |

### Files

| File | Change |
|------|--------|
| `admin/controller/AdminUserController.java` | NEW — controller wired to AdminUserService |
| `admin/dto/UserSummaryDto.java` | NEW — `id, email, role, createdAt`; no `firebaseUid` leak |
| `admin/dto/ChangeRoleRequest.java` | NEW — `@NotNull UserRole role` |
| `admin/service/AdminUserService.java` | NEW — list with optional role filter; `changeRole` via dirty-check |
| `users/entity/User.java` | MODIFIED — added `setRole(UserRole)` method |
| `users/repository/UserRepository.java` | MODIFIED — added `findAllByRole(UserRole, Pageable)` |

### Design Decisions

- `firebaseUid` is never returned by admin endpoints — only internal UUIDs cross the API boundary (same contract as all other modules).
- Role change uses JPA dirty-checking: `user.setRole(...)` inside a `@Transactional` method commits on transaction exit without an explicit `save()`.
- Listing uses `findAll(Pageable)` when no role filter is given; `findAllByRole(role, pageable)` when filtered — both stay at the repository level so pagination counts remain correct.

---

## 2. CMS Course CRUD (#273)

### Endpoints

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | `/api/v1/cms/courses` | TEACHER, ADMIN | List own courses (TEACHER) or all (ADMIN) |
| POST | `/api/v1/cms/courses` | TEACHER, ADMIN | Create course in DRAFT status |
| PUT | `/api/v1/cms/courses/{id}` | TEACHER (owner), ADMIN | Update mutable metadata |
| PUT | `/api/v1/cms/courses/{id}/publish` | ADMIN | Transition DRAFT → PUBLISHED |
| PUT | `/api/v1/cms/courses/{id}/archive` | ADMIN | Transition → ARCHIVED |

### Files

| File | Change |
|------|--------|
| `admin/controller/CmsCourseController.java` | NEW |
| `admin/dto/CourseAdminDto.java` | NEW — includes `status`, `createdByUserId` not in learner DTO |
| `admin/dto/CreateCourseRequest.java` | NEW |
| `admin/dto/UpdateCourseRequest.java` | NEW — all fields optional for partial update |
| `admin/service/CmsCourseService.java` | NEW |
| `courses/entity/Course.java` | MODIFIED — added `Course(...)` factory constructor, `updateMetadata()`, `publish()`, `archive()` |

### Design Decisions

- **Slug generation**: Slug is always server-generated (`slugify(title) + "-" + UUID.random().substring(0,8)`) to prevent client-injected values and guarantee global uniqueness.
- **Publish requires ADMIN**: Teachers cannot self-publish. The `@PreAuthorize("hasRole('ADMIN')")` override on `publishCourse` and `archiveCourse` narrows the class-level TEACHER+ADMIN gate.
- **Ownership check**: `loadCourseForMutation()` verifies `createdByUserId == currentUser.getId()` unless the caller is ADMIN. This is enforced at the service layer, not just at the controller.
- **Partial update**: `UpdateCourseRequest` has no `@NotBlank` so clients can send only the fields they want to change. The service applies null fields as no-op by falling back to the current value.

---

## 3. CMS Sections + Lessons Management (#274)

### Endpoints

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | `/api/v1/cms/courses/{courseId}/sections` | TEACHER, ADMIN | List sections |
| POST | `/api/v1/cms/courses/{courseId}/sections` | TEACHER (owner), ADMIN | Add section |
| PUT | `/api/v1/cms/courses/{courseId}/sections/{sectionId}` | TEACHER (owner), ADMIN | Update/reorder section |
| DELETE | `/api/v1/cms/courses/{courseId}/sections/{sectionId}` | TEACHER (owner), ADMIN | Delete section + its lessons |
| GET | `/api/v1/cms/sections/{sectionId}/lessons` | TEACHER, ADMIN | List lessons in section |
| POST | `/api/v1/cms/sections/{sectionId}/lessons` | TEACHER (owner), ADMIN | Add lesson |
| PUT | `/api/v1/cms/sections/{sectionId}/lessons/{lessonId}` | TEACHER (owner), ADMIN | Update lesson |
| DELETE | `/api/v1/cms/sections/{sectionId}/lessons/{lessonId}` | TEACHER (owner), ADMIN | Delete lesson |

### Files

| File | Change |
|------|--------|
| `admin/controller/CmsSectionController.java` | NEW |
| `admin/controller/CmsLessonController.java` | NEW |
| `admin/dto/CreateSectionRequest.java` | NEW |
| `admin/dto/UpdateSectionRequest.java` | NEW |
| `admin/dto/SectionAdminDto.java` | NEW |
| `admin/dto/CreateLessonRequest.java` | NEW |
| `admin/dto/UpdateLessonRequest.java` | NEW |
| `admin/dto/LessonAdminDto.java` | NEW |
| `admin/service/CmsSectionService.java` | NEW |
| `admin/service/CmsLessonService.java` | NEW |
| `courses/entity/CourseSection.java` | MODIFIED — added `CourseSection(...)` constructor, `update()` method |
| `courses/entity/Lesson.java` | MODIFIED — added `Lesson(...)` constructor, `update()` method |

### Design Decisions

- **Cross-section guard**: `updateSection` and `deleteSection` verify `section.getCourseId().equals(courseId)` to prevent a malicious caller from manipulating sections in a different course by using a mismatched path segment.
- **Delete cascade**: `CmsSectionService.deleteSection` relies on the `ON DELETE CASCADE` constraint defined in `V2__courses.sql` — child lessons are automatically removed by Postgres. No explicit lesson deletion loop.
- **Ownership traversal for lessons**: `CmsLessonService.loadSectionForMutation` walks `section → course → createdByUserId` to resolve ownership. This adds one DB read per lesson mutation but keeps the ownership check deterministic and avoids caching stale ownership data.
- **Lesson type validation**: `@Pattern(regexp = "VIDEO|ARTICLE|RESOURCE")` in `CreateLessonRequest` mirrors the `CHECK` constraint in `V2__courses.sql`. Validation fires at the controller before any DB call.

---

## 4. CMS Exam Authoring (#275)

### Endpoints

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | `/api/v1/cms/courses/{courseId}/exam` | TEACHER, ADMIN | Get exam with correct-answer flags |
| POST | `/api/v1/cms/courses/{courseId}/exam` | TEACHER (owner), ADMIN | Create exam + all questions atomically |

### Files

| File | Change |
|------|--------|
| `admin/controller/CmsExamController.java` | NEW |
| `admin/dto/CreateExamRequest.java` | NEW — nested `QuestionRequest` + `ChoiceRequest` |
| `admin/dto/ExamAdminDto.java` | NEW — exposes `isCorrect` unlike learner `ExamDto` |
| `admin/service/CmsExamService.java` | NEW |
| `exams/entity/Exam.java` | MODIFIED — added public constructor |
| `exams/entity/ExamQuestion.java` | MODIFIED — added public constructor |
| `exams/entity/ExamChoice.java` | MODIFIED — added public constructor |

### Design Decisions

- **Atomic creation**: The entire exam — all questions and all choices — is created inside a single `@Transactional` method. A validation failure mid-loop rolls back everything. This prevents a partial exam (with missing questions) from ever becoming visible to learners.
- **Exactly one correct choice**: `CmsExamService.createExam` validates that every question has exactly one choice where `correct=true`. A 400 is returned with the problematic question's text so the caller can fix it immediately.
- **One exam per course**: `examRepository.findByCourseId(courseId).isPresent()` returns 409 on duplicate. The `UNIQUE` constraint on `exam.course_id` would catch it at the DB level anyway, but the explicit check gives a human-readable error.
- **Correct answers in admin DTO**: `ExamAdminDto` deliberately exposes `isCorrect` unlike the learner-facing `ExamDto`. The CMS editor needs this to verify the answer key without accessing the database directly.

---

## 5. Full-Text Course Search (#280)

### Endpoint change

`GET /api/v1/courses?q=machine+learning` — returns paginated results ranked by relevance.

The `?q=` parameter coexists with the existing `?category=` filter. When `q` is present, it takes priority and category is ignored. When `q` is absent, behavior is unchanged.

### Files

| File | Change |
|------|--------|
| `V13__course_fts.sql` | NEW — `search_vector tsvector` column, backfill UPDATE, GIN index, trigger |
| `courses/repository/CourseRepository.java` | MODIFIED — added `searchPublished(@Param("query") String query, Pageable)` native query |
| `courses/service/CourseService.java` | MODIFIED — `getPublishedCourses(category, query, pageable)` routes to FTS when `q` is present |
| `courses/controller/CourseController.java` | MODIFIED — added `@RequestParam(required = false) String q` |

### Migration: V13__course_fts.sql

1. `ALTER TABLE courses ADD COLUMN search_vector tsvector` — adds nullable column.
2. Backfill `UPDATE` — computes `to_tsvector('simple', title || description || short_description)` for existing rows.
3. `CREATE INDEX ... USING GIN` — makes `@@ plainto_tsquery(...)` O(log n).
4. `CREATE OR REPLACE FUNCTION update_course_search_vector()` — recomputes the vector.
5. `CREATE TRIGGER courses_search_vector_update BEFORE INSERT OR UPDATE` — keeps the vector current automatically after CMS edits.

### Design Decisions

- **`simple` dictionary**: Language-agnostic. Does not stem words, but handles multilingual course data (French, Arabic, English) uniformly. Language-specific stemming can be added later by switching to `french` or `english` config per row.
- **`plainto_tsquery`**: Accepts raw user input without special syntax. Safer than `to_tsquery` which requires explicit `&`, `|`, `!` operators.
- **Ranking**: Results are ordered by `ts_rank` descending, then `published_at` descending. Relevance-first matches learner expectation from a search box.
- **Native query**: Spring Data JPA does not support `@@` operator in JPQL, so `nativeQuery = true` is required. The `countQuery` is explicitly provided since Spring Data cannot derive it from the native select query.

---

## 6. Rate Limiting (#278)

### Covered endpoints

| Endpoint | Limit | Key |
|----------|-------|-----|
| `POST /api/v1/auth/sync` | 30 / minute | Principal (Firebase UID) |
| `POST /api/v1/enrollments` | 20 / hour | Principal |
| `POST /api/v1/courses/*/exam/submit` | 5 / hour | Principal |

### Files

| File | Change |
|------|--------|
| `pom.xml` | MODIFIED — added `bucket4j-core:8.10.1` |
| `config/RateLimitFilter.java` | NEW — `OncePerRequestFilter` with per-principal token buckets |
| `security/SecurityConfig.java` | MODIFIED — `RateLimitFilter` bean + `addFilterAfter(rateLimitFilter, FirebaseTokenFilter.class)` |

### Design Decisions

- **In-memory buckets**: `ConcurrentHashMap<String, Bucket>` keyed by `"prefix:principal"`. No external dependency (Redis not required for single-instance MVP). Buckets reset on app restart — acceptable for MVP.
- **No `@Component`**: `RateLimitFilter` is not annotated `@Component` to prevent Spring Boot from auto-registering it as a plain servlet filter in addition to the security chain registration. It is created as a `@Bean` in `SecurityConfig` and added with `addFilterAfter`.
- **After `FirebaseTokenFilter`**: The rate limit filter is placed after `FirebaseTokenFilter` so `SecurityContextHolder` is already populated. This allows using the Firebase UID as the bucket key instead of a raw IP (which would be wrong behind a proxy).
- **Error contract**: Uses `ApiErrorWriter.write(response, TOO_MANY_REQUESTS, ...)` so the response matches the shared `ApiError` contract. The `RATE_LIMITED` code was already defined in `ApiErrorCode` from the #282 security hardening work.

---

## Database migration timeline

| Migration | Content |
|-----------|---------|
| V1–V12 | Previously shipped |
| **V13** | `search_vector tsvector` on `courses` + GIN index + trigger |

---

## Testing checklist (manual)

- [ ] `POST /api/v1/auth/sync` — 31st request within 60 seconds returns 429 with `RATE_LIMITED` code
- [ ] `GET /api/v1/admin/users` — LEARNER token returns 403; ADMIN token returns paginated list
- [ ] `PUT /api/v1/admin/users/{id}/role` — changes role; verified by second GET call
- [ ] `POST /api/v1/cms/courses` — creates DRAFT; slug derived from title; 201 returned
- [ ] `PUT /api/v1/cms/courses/{id}/publish` — LEARNER token → 403; ADMIN → 200 with `status: PUBLISHED`
- [ ] `POST /api/v1/cms/courses/{courseId}/sections` — TEACHER who owns course → 201; TEACHER who doesn't own → 403
- [ ] `POST /api/v1/cms/sections/{sectionId}/lessons` — valid lessonType → 201; invalid lessonType → 400
- [ ] `POST /api/v1/cms/courses/{courseId}/exam` — two correct answers in one question → 400; valid → 201
- [ ] `POST /api/v1/cms/courses/{courseId}/exam` — duplicate → 409
- [ ] `GET /api/v1/courses?q=machine+learning` — returns relevant courses ranked by ts_rank; no DRAFT courses
- [ ] `GET /api/v1/courses?q=` — falls back to regular list (no FTS on blank query)
