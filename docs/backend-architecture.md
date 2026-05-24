# EduLife Backend Architecture

## Sprint 2 Course Discovery Contract

Date: 2026-05-24

This document defines the backend contract for Sprint 2 course discovery after Sprint 1 authentication work is complete. It is the contract-first reference for:

- Issue #110: prepare Sprint 2 backend contract for course discovery
- Issue #111: define response contract for `GET /api/v1/courses`
- Issue #112: define response contract for `GET /api/v1/courses/{id}`
- Issue #113: identify required tables for courses, sections, and lessons
- Issue #114: confirm endpoints remain behind Firebase token validation
- Issue #115: document seed-data expectations for 3 to 5 initial courses

The contract stays inside the EduLife MVP learner flow:

```text
Discover course -> Enroll -> Learn -> Take exam -> Pass -> Receive certificate
```

Sprint 2 only covers the discovery part of that flow. It does not introduce CMS work, enrollments, lesson completion, exams, or certificates.

---

## Scope and Assumptions

- Sprint 1 auth bridge remains the source of truth for authenticated API access.
- Course discovery endpoints stay inside `/api/v1/**` and require a valid Firebase Bearer token.
- `email_verified = true` is still required before a learner can access protected course discovery routes.
- The initial catalog is seed-data backed. No teacher CMS is required for Sprint 2.
- The backend exposes DTOs only. JPA entities are not serialized directly.

---

## Auth and Security Rules

Course discovery remains protected by the same Firebase validation flow already used for `/api/v1/auth/sync`.

```text
Android app logs in with Firebase
  ->
Android sends Bearer token
  ->
FirebaseTokenFilter verifies token with Firebase Admin SDK
  ->
Backend requires email_verified = true
  ->
Request enters controller
```

### Protected endpoints

- `GET /api/v1/courses`
- `GET /api/v1/courses/{courseId}`

### Security guarantees

- Missing token returns `401`
- Malformed token returns `401`
- Invalid or expired Firebase token returns `401`
- Unverified email returns `403`
- No discovery endpoint is public in Sprint 2

This keeps course browsing aligned with the locked EduLife rule that protected learner flow access requires verified Firebase authentication.

---

## Endpoint Contract: `GET /api/v1/courses`

### Purpose

Return a paginated list of published courses for discovery screens.

### Request

```http
GET /api/v1/courses?category=BEGINNER&page=0&size=20
Authorization: Bearer <firebase-id-token>
```

### Query parameters

| Name | Required | Type | Notes |
|------|----------|------|-------|
| `category` | No | `string` | Current implementation maps this to the `courses.level` column to avoid blocking Sprint 2 on a separate category table |
| `page` | No | `integer` | Zero-based page index |
| `size` | No | `integer` | Defaults to `20`, max `50` |

### Response shape

The API currently returns Spring pagination metadata plus course summary DTOs.

```json
{
  "content": [
    {
      "id": "11111111-1111-1111-1111-111111111111",
      "slug": "math-bac-sm-algebra-foundations",
      "title": "Math Bac SM - Algebra Foundations",
      "shortDescription": "A structured algebra refresher for Moroccan Bac Sciences Math students.",
      "level": "BEGINNER",
      "languageCode": "fr",
      "imageUrl": "https://images.unsplash.com/photo-1635070041078-e363dbe005cb?w=800&q=80",
      "publishedAt": "2026-05-24T09:00:00Z"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "size": 20,
  "number": 0,
  "sort": {
    "sorted": true,
    "unsorted": false,
    "empty": false
  },
  "numberOfElements": 1,
  "first": true,
  "empty": false
}
```

### Course summary fields

| Field | Type | Required | Reason |
|-------|------|----------|--------|
| `id` | `UUID` | Yes | Stable backend identifier for detail navigation |
| `slug` | `string` | Yes | Readable identifier for future sharing and CMS workflows |
| `title` | `string` | Yes | Discovery card title |
| `shortDescription` | `string` | No | Short marketing summary for list screens |
| `level` | `string` | No | Current category bucket used by Android filtering |
| `languageCode` | `string` | Yes | Supports multilingual rendering decisions |
| `imageUrl` | `string` | No | Visual card asset URL |
| `publishedAt` | `timestamp` | No | Stable sort field for newest-first catalog ordering |

### Business rules

- Only courses with `status = PUBLISHED` are returned.
- Results are always sorted by `publishedAt DESC`.
- Draft and archived courses are never visible to learners.
- The list contract intentionally excludes sections and lessons to keep discovery payloads small.

---

## Endpoint Contract: `GET /api/v1/courses/{courseId}`

### Purpose

Return one published course with its ordered sections and ordered lessons so Android can render the learning outline before enrollment.

### Request

```http
GET /api/v1/courses/11111111-1111-1111-1111-111111111111
Authorization: Bearer <firebase-id-token>
```

### Path parameters

| Name | Type | Notes |
|------|------|-------|
| `courseId` | `UUID` | Internal course identifier returned by the list endpoint |

### Response shape

```json
{
  "id": "11111111-1111-1111-1111-111111111111",
  "slug": "math-bac-sm-algebra-foundations",
  "title": "Math Bac SM - Algebra Foundations",
  "shortDescription": "A structured algebra refresher for Moroccan Bac Sciences Math students.",
  "description": "Build core confidence in equations, functions, and algebraic methods used across Bac Sciences Math coursework.",
  "level": "BEGINNER",
  "languageCode": "fr",
  "imageUrl": "https://images.unsplash.com/photo-1635070041078-e363dbe005cb?w=800&q=80",
  "publishedAt": "2026-05-24T09:00:00Z",
  "sections": [
    {
      "id": "11111111-aaaa-aaaa-aaaa-111111111111",
      "title": "Algebra Basics",
      "description": "Start with core algebra language and operations.",
      "displayOrder": 1,
      "lessons": [
        {
          "id": "11111111-aaaa-0000-0000-111111111111",
          "title": "Understanding Algebraic Expressions",
          "summary": "Identify variables, constants, and operations in simple expressions.",
          "lessonType": "VIDEO",
          "estimatedDurationMinutes": 12,
          "displayOrder": 1,
          "preview": true
        }
      ]
    }
  ]
}
```

### Course detail fields

| Field | Type | Required | Reason |
|-------|------|----------|--------|
| `id` | `UUID` | Yes | Stable detail identifier |
| `slug` | `string` | Yes | Future SEO/share-friendly identifier |
| `title` | `string` | Yes | Course title |
| `shortDescription` | `string` | No | Header summary |
| `description` | `string` | Yes | Full course overview |
| `level` | `string` | No | Display filter/category label |
| `languageCode` | `string` | Yes | Multilingual context |
| `imageUrl` | `string` | No | Detail hero image |
| `publishedAt` | `timestamp` | No | Display and ordering support |
| `sections` | `array` | Yes | Ordered learning outline |

### Section fields

| Field | Type | Required | Reason |
|-------|------|----------|--------|
| `id` | `UUID` | Yes | Stable section identifier |
| `title` | `string` | Yes | Section heading |
| `description` | `string` | No | Section summary |
| `displayOrder` | `integer` | Yes | Preserve section order |
| `lessons` | `array` | Yes | Ordered lessons within the section |

### Lesson fields

| Field | Type | Required | Reason |
|-------|------|----------|--------|
| `id` | `UUID` | Yes | Stable lesson identifier |
| `title` | `string` | Yes | Lesson title |
| `summary` | `string` | No | Short preview text |
| `lessonType` | `string` | Yes | MVP lesson rendering choice: `VIDEO`, `ARTICLE`, or `RESOURCE` |
| `estimatedDurationMinutes` | `integer` | No | UI duration display |
| `displayOrder` | `integer` | Yes | Preserve lesson order |
| `preview` | `boolean` | Yes | Supports free preview markers before enrollment |

### Business rules

- Only courses with `status = PUBLISHED` are returned.
- Sections are ordered by `displayOrder ASC`.
- Lessons are ordered by `displayOrder ASC`.
- Correct exam answers, progress data, enrollment state, and resource internals are not included in Sprint 2 detail payloads.
- Unknown or unpublished `courseId` returns `404`.

---

## Sprint 2 Required Tables

Sprint 2 requires only the minimum relational structure needed to support seeded discovery with sections and lessons.

### `courses`

| Column | Type | Notes |
|--------|------|-------|
| `id` | `UUID` | Primary key |
| `slug` | `VARCHAR(160)` | Unique course slug |
| `title` | `VARCHAR(255)` | Required |
| `short_description` | `VARCHAR(500)` | Optional summary |
| `description` | `TEXT` | Required full description |
| `language_code` | `VARCHAR(10)` | Required language marker |
| `level` | `VARCHAR(50)` | Temporary category bucket for Sprint 2 filtering |
| `status` | `VARCHAR(20)` | `DRAFT`, `PUBLISHED`, `ARCHIVED` |
| `image_url` | `TEXT` | Optional catalog/detail image |
| `published_at` | `TIMESTAMPTZ` | Used for published sorting |
| `created_by_user_id` | `UUID` | Optional author link for later teacher/admin flows |
| `created_at` | `TIMESTAMPTZ` | Audit timestamp |
| `updated_at` | `TIMESTAMPTZ` | Audit timestamp |

### `course_sections`

| Column | Type | Notes |
|--------|------|-------|
| `id` | `UUID` | Primary key |
| `course_id` | `UUID` | FK to `courses.id`, delete cascade |
| `title` | `VARCHAR(255)` | Required |
| `description` | `TEXT` | Optional |
| `display_order` | `INTEGER` | Required, must be `> 0` |
| `created_at` | `TIMESTAMPTZ` | Audit timestamp |
| `updated_at` | `TIMESTAMPTZ` | Audit timestamp |

Unique constraint:

- `(course_id, display_order)`

### `lessons`

| Column | Type | Notes |
|--------|------|-------|
| `id` | `UUID` | Primary key |
| `course_section_id` | `UUID` | FK to `course_sections.id`, delete cascade |
| `title` | `VARCHAR(255)` | Required |
| `summary` | `TEXT` | Optional lesson teaser |
| `lesson_type` | `VARCHAR(20)` | `VIDEO`, `ARTICLE`, `RESOURCE` |
| `estimated_duration_minutes` | `INTEGER` | Optional, must be `> 0` if present |
| `display_order` | `INTEGER` | Required, must be `> 0` |
| `is_preview` | `BOOLEAN` | Preview marker for catalog/detail experience |
| `created_at` | `TIMESTAMPTZ` | Audit timestamp |
| `updated_at` | `TIMESTAMPTZ` | Audit timestamp |

Unique constraint:

- `(course_section_id, display_order)`

### Required indexes

- `idx_courses_status`
- `idx_courses_published_at`
- `idx_courses_created_by_user_id`
- `idx_course_sections_course_id`
- `idx_lessons_course_section_id`

These tables are enough for Sprint 2 discovery and do not force early enrollment, progress, exam, or CMS schema decisions.

---

## Seed Data Expectations

Sprint 2 seed data should remain small, stable, and realistic so Android can integrate against real responses instead of mock APIs.

### Initial dataset target

- `5` published courses
- `2` sections per course
- `2` lessons per section
- `20` total lessons
- At least `1` preview lesson per course

### Seeded courses in the current backend

| Slug | Title | Language | Level |
|------|-------|----------|-------|
| `math-bac-sm-algebra-foundations` | Math Bac SM - Algebra Foundations | `fr` | `BEGINNER` |
| `physics-motion-and-forces` | Physics - Motion and Forces | `fr` | `INTERMEDIATE` |
| `english-communication-essentials` | English Communication Essentials | `en` | `BEGINNER` |
| `french-expression-and-writing` | French Expression and Writing | `fr` | `INTERMEDIATE` |
| `digital-skills-study-productivity` | Digital Skills for Study Productivity | `en` | `BEGINNER` |

### Seed rules

- All seed courses must be `PUBLISHED`
- Seed data must support both list and detail endpoint testing
- Titles and descriptions should look production-realistic for UAT
- Seed data should cover multiple languages already relevant to EduLife
- Seed data must not depend on teacher CMS screens

This satisfies the planned 3 to 5 initial course expectation by fixing the backend on 5 discovery-ready courses today.

---

## Error Contract

All course discovery errors must follow the shared API shape:

```json
{
  "status": 400,
  "message": "Validation failed",
  "timestamp": "2026-05-24T10:00:00Z"
}
```

### Common course discovery cases

| Scenario | Status |
|----------|--------|
| Missing token | `401` |
| Malformed token | `401` |
| Invalid or expired token | `401` |
| Unverified email | `403` |
| Course not found | `404` |
| Invalid request params | `400` |

---

## Implementation Notes

- Current filtering uses `category` as the public query param even though the data is stored in `courses.level`.
- This alias is intentional so Android can move forward now without blocking on a separate `course_categories` table.
- The backend should keep business logic in `CourseService`, not in the controller.
- Future modules such as enrollments, progress, exams, and certificates should extend this contract without breaking the Sprint 2 discovery response shapes.

---

## Out of Scope for This Contract

- Course enrollment endpoints
- Lesson progress endpoints
- Final exams
- Certificates
- Teacher course creation CMS
- Discussion threads
- Notifications
- Payments

This document is intentionally narrow so Sprint 2 can start without reopening Sprint 1 identity decisions or pulling future modules into the discovery slice.
