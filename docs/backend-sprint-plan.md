# EduLife Backend — Full Sprint Plan

**Project:** EduLife  
**Backend:** Spring Boot modular monolith  
**Database:** PostgreSQL + Flyway  
**Auth:** Firebase Admin SDK  
**Last updated:** 2026-06-03  

---

## MVP Learner Flow

```text
Register → Email Verify → Login
  → Browse Courses
  → Enroll
  → Access Lessons + Mark Progress
  → Take MCQ Exam (backend-scored)
  → Pass → Certificate Generated
  → Download Certificate PDF
```

Pass threshold: **80%**  
Attempt policy: **2 failures → 72-hour cooldown**

---

## Sprint History (DONE)

---

### Sprint 0 — Foundation ✅

**Goal:** Backend boots, DB connects, Flyway runs.

**Delivered:**
- Spring Boot project scaffold
- PostgreSQL connection via `application.yml`
- Flyway migrations boot-strapped (V1 schema)
- Global API error contract (`status`, `message`, `timestamp`)
- CORS config (explicit origins, no wildcard)
- Firebase Admin SDK initialized from env var or credentials file

**Flyway migrations:** V1 (`users`, `roles`, `user_roles`), V2 (`profiles`)

---

### Sprint 1 — Identity Bridge ✅

**Goal:** Firebase auth bridged to internal UUID + role.

**Delivered:**
- `FirebaseTokenFilter` validates Bearer token on every protected endpoint
- `email_verified` enforced before learner-flow access
- `POST /api/v1/auth/sync` → upserts internal user, returns `userId` + `role`
- Never exposes `firebase_uid` in responses
- Role enum: `LEARNER`, `TEACHER`, `GROUP_ADMIN`, `ADMIN`
- Role selection at registration (client passes desired role, backend validates)
- Avatar upload endpoint with 5MB limit and filesystem storage
- `DELETE /api/v1/account` soft-delete for Play Store compliance

**Key rule:** `userId` and `role` are never trusted from client — always resolved server-side from token.

---

### Sprint 2 — Course Discovery ✅

**Goal:** Backend serves seeded courses with sections and lessons.

**Delivered:**
- `GET /api/v1/courses` → paginated list, PUBLISHED only
- `GET /api/v1/courses/{id}` → full course detail: sections, lessons, imageUrl, duration
- Lesson entity: `type` (VIDEO, ARTICLE, RESOURCE), `durationMinutes`, `isPreview`
- 5+ seeded courses via Flyway data migrations
- DTOs: `CourseSummaryDto`, `CourseDetailDto`, `CourseSectionDto`, `LessonSummaryDto`, `LessonDetailDto`
- No JPA entities exposed directly

**Flyway migrations:** V3 (`courses`), V4 (`course_sections`), V5 (`lessons`)

---

### Sprint 3 — Enrollment ✅

**Goal:** Enroll, unenroll, list enrollments. Enrollment creates initial progress.

**Delivered:**
- `POST /api/v1/enrollments` → enroll (transactional: enrollment + CourseProgress created atomically)
- `DELETE /api/v1/enrollments/{id}` → soft-delete (CANCELLED status, not hard delete)
- `GET /api/v1/enrollments/me` → user's active enrollments with course image
- Re-enrollment: reactivates CANCELLED record instead of creating duplicate
- Ownership check: user can only cancel their own enrollments
- `409 CONFLICT` if already actively enrolled

**Key rule:** Enrollment and initial CourseProgress creation are one atomic transaction. If either fails, both roll back.

**Flyway migrations:** V6 (`enrollments`)

---

### Sprint 4 — Lessons & Progress ✅

**Goal:** Access lessons, mark complete, track progress percentage.

**Delivered:**
- Lesson access requires active enrollment (403 if not enrolled)
- `PUT /api/v1/progress/lessons/{lessonId}/mark-complete` → records completion with `completedAt` Instant
- `GET /api/v1/progress/courses/{courseId}` → full breakdown: section-level and lesson-level, `percentComplete` as decimal
- `GET /api/v1/progress/courses/{courseId}/summary` (removed in cleanup — use full endpoint)
- Auto-creates `LessonProgress` records on first access
- Idempotent: marking already-complete lesson does not duplicate

**Flyway migrations:** V9 (`course_progress`, `lesson_progress`)

---

### Sprint 5 — MCQ Exam ✅

**Goal:** Serve questions without answers, submit answers, score on backend, enforce attempt policy.

**Delivered:**
- `GET /api/v1/courses/{courseId}/exam` → questions + choices, **no correct answer field**
- `GET /api/v1/courses/{courseId}/exam/status` → pre-exam status check:
  - `{ passed: true }` → return 409 on submit
  - `{ inCooldown: true, cooldownEndsAt: "..." }` → block UI
  - `{ attemptsUsed: 0 }` → allow attempt
- `POST /api/v1/courses/{courseId}/exam/submit` → server-side scoring:
  - Scores against stored correct answers (never sent to client)
  - 80% = pass → auto-generates certificate
  - 2 failures → sets 72-hour cooldown
  - Already-passed → 409 CONFLICT
  - In cooldown → 429 with `cooldownEndsAt` in response
- `ExamAttempt` entity: `userId`, `examId`, `score`, `passed`, `takenAt`
- `ExamResultDto`: `score`, `passed`, `attemptsUsed`, `cooldownEndsAt`, `certificateNumber`

**Security:** Correct answers are never in any DTO or API response.

**Flyway migrations:** V7 (`exams`, `exam_questions`, `exam_choices`), V8 (`exam_attempts`), V17 (exam_attempts index)

---

### Sprint 6 — Certificates ✅

**Goal:** Auto-generate certificate after exam pass, list, download PDF, public verify.

**Delivered:**
- Certificate auto-generated inside exam submit transaction when passing
- `GET /api/v1/certificates/me` → user's certificate list
- `GET /api/v1/certificates/{id}` → certificate detail
- `GET /api/v1/certificates/{id}/download` → PDF binary (OpenHTMLToPDF + ZXing QR code)
- `GET /api/v1/certificates/verify/{hash}` → public endpoint, no auth required
- Certificate number format: `EL-{year}-{12hex}`
- SHA-256 verification hash stored in DB
- PDF content: student name, course title, issuer, issue date, certificate number, QR code
- Filesystem storage (path configurable via `CertificateStorageProperties`)

**Flyway migrations:** V10 (`certificates`), V14 (`certificates_v2` with PDF metadata)

---

### Sprint 7 — UAT & Hardening ✅

**Goal:** Full end-to-end validation, error states, security review.

**Delivered:**
- Full learner flow tested end-to-end
- HTTP codes consistent: 401, 403, 404, 409, 429
- No raw stack traces in API responses
- Security checklist: all protected endpoints require Firebase token, ownership checks on all user-scoped resources, RBAC enforced with `@PreAuthorize`
- Admin metrics: `GET /api/v1/admin/metrics` → user counts, course counts, enrollment counts, certificate counts
- Admin user management: list users, change roles
- Course publish/archive by admin
- Teacher CMS: full CRUD for courses, sections, lessons, exams, questions, choices
- Teacher verification: apply, approve/reject flow
- Groups: create, manage members (GROUP_ADMIN role)
- Rate limiting on sensitive endpoints

**Flyway migrations:** V11–V17

---

## Current State (as of 2026-06-03)

| Module | Status |
|---|---|
| auth / identity | ✅ Complete |
| profiles + avatar | ✅ Complete |
| course discovery | ✅ Complete |
| enrollment (+ re-enrollment) | ✅ Complete |
| lesson access + progress | ✅ Complete |
| MCQ exam + cooldown guard | ✅ Complete |
| certificate PDF + verify | ✅ Complete |
| admin metrics + user mgmt | ✅ Complete |
| teacher CMS (full CRUD) | ✅ Complete |
| teacher verification flow | ✅ Complete |
| groups (GROUP_ADMIN) | ✅ Complete |
| account deletion | ✅ Complete |
| RBAC (4 roles) | ✅ Complete |
| global error contract | ✅ Complete |
| Firebase token filter | ✅ Complete |

**All MVP backend endpoints are built and functional.**

---

## Remaining Sprint Plan

---

### Sprint 8 — Web Integration ⬜

**Goal:** guided-journey-lab fully consumes all backend endpoints.

**Tasks:**

#### Auth
- [ ] POST `/auth/sync` called after Firebase login/register
- [ ] Firebase token attached to all API requests (interceptor/middleware)
- [ ] 401 triggers re-authentication, not blank screen

#### Courses
- [ ] GET `/courses` → course catalog page with real data
- [ ] GET `/courses/{id}` → course detail page with sections/lessons

#### Enrollment
- [ ] POST `/enrollments` → enroll button wired
- [ ] DELETE `/enrollments/{id}` → unenroll wired
- [ ] GET `/enrollments/me` → dashboard shows real enrolled courses

#### Progress
- [ ] PUT `/progress/lessons/{id}/mark-complete` → lesson completion wired
- [ ] GET `/progress/courses/{id}` → progress bar shows real data

#### Exam
- [ ] GET `/courses/{id}/exam/status` → gate shown before exam starts
- [ ] GET `/courses/{id}/exam` → questions rendered from real backend
- [ ] POST `/courses/{id}/exam/submit` → submit wired, shows real score
- [ ] Handle 409 (already passed), 429 (cooldown with timer)

#### Certificate
- [ ] GET `/certificates/me` → certificate list on dashboard
- [ ] GET `/certificates/{id}/download` → PDF download button wired
- [ ] GET `/certificates/verify/{hash}` → public verify page

#### Profile
- [ ] GET/PUT `/profiles/me` → profile page
- [ ] POST `/profiles/me/avatar` → avatar upload (5MB limit enforced in UI)

**Definition of done:** No route uses mocked/hardcoded data where a backend endpoint exists.

---

### Sprint 9 — Production Hardening ⬜

**Goal:** Backend ready for real users. No open security holes.

**Tasks:**

#### CORS
- [ ] Verify `APP_CORS_ALLOWED_ORIGINS` is set to production domain(s) only
- [ ] Confirm no wildcard `*` in production CORS config

#### Rate Limiting
- [ ] Verify auth/sync and exam/submit have rate limiting
- [ ] Confirm 429 responses include `Retry-After` header

#### Token & Security
- [ ] Verify `email_verified` enforcement is tested with unverified accounts
- [ ] Verify 403 on cross-user resource access (enrollment, progress, certificate)
- [ ] Confirm correct exam answers never appear in any API response (audit with Postman)

#### Environment
- [ ] All required env vars documented and set in deployment target:
  ```
  SPRING_DATASOURCE_URL
  SPRING_DATASOURCE_USERNAME
  SPRING_DATASOURCE_PASSWORD
  FIREBASE_ADMIN_CREDENTIALS_PATH or FIREBASE_ADMIN_CREDENTIALS_JSON
  APP_CORS_ALLOWED_ORIGINS
  EDULIFE_AVATAR_STORAGE_DIR
  EDULIFE_AVATAR_PUBLIC_BASE_URL
  ```
- [ ] Certificate storage dir writable in production environment
- [ ] Avatar storage dir writable in production environment

#### Database
- [ ] All Flyway migrations run cleanly on a fresh DB
- [ ] No pending uncommitted schema changes
- [ ] `ddl-auto` is `validate` or `none` in production — never `create` or `update`

#### Logging
- [ ] No stack traces leak into API error responses
- [ ] Firebase UID not logged to application logs

#### Health
- [ ] Confirm Spring Boot Actuator health endpoint available for uptime monitoring
- [ ] Confirm backend returns proper error on DB connection failure (not 500 with stack trace)

---

### Sprint 10 — Launch Checklist ⬜

**Goal:** Platform is live. Real users can complete the full learner flow.

**Tasks:**

#### Backend Deploy
- [ ] Backend deployed to production server
- [ ] HTTPS enforced (no plain HTTP in production)
- [ ] Domain/subdomain pointed to backend
- [ ] Flyway migrations ran on production DB without errors

#### Web Deploy
- [ ] guided-journey-lab deployed to Cloudflare Workers via `wrangler.jsonc`
- [ ] `git subtree push --prefix=guided-journey-lab web main`
- [ ] Production environment variables set in Cloudflare dashboard
- [ ] VITE_API_BASE_URL points to production backend URL

#### Android
- [ ] API base URL updated to production backend
- [ ] Release build signed and tested on physical device
- [ ] Full learner flow tested on release build (not debug)

#### Smoke Test (full flow end-to-end)
- [ ] Register new account
- [ ] Verify email
- [ ] Login on Android + Web
- [ ] Browse course catalog
- [ ] Enroll in course
- [ ] Complete lessons, verify progress updates
- [ ] Take exam:
  - [ ] Pass path: score ≥ 80% → certificate generated
  - [ ] Fail path: score < 80% → attempt count increments
  - [ ] Cooldown path: 2nd fail → 72h cooldown shown
- [ ] Download certificate PDF
- [ ] Verify certificate via public URL
- [ ] Delete account flow tested

---

## Deferred (Post-MVP)

Do not build until the learner flow is stable and real users are using it:

| Feature | Reason deferred |
|---|---|
| Course discussions / Q&A | Requires moderation, non-trivial |
| Push notifications | Requires FCM integration, separate sprint |
| Payments / revenue | Not in MVP scope |
| AI recommendations | Needs learner data first |
| Real-time chat | Complex infra, not core to learning |
| Live video | Complex infra, separate product |
| Mentor booking | Separate workflow |
| Advanced analytics | Needs data volume first |
| Gamification | Nice to have, not core |
| Microservices | Not needed at MVP scale |

---

## Flyway Migration Index

| Version | Table(s) | Status |
|---|---|---|
| V1 | users, roles, user_roles | ✅ Applied |
| V2 | profiles | ✅ Applied |
| V3 | courses | ✅ Applied |
| V4 | course_sections | ✅ Applied |
| V5 | lessons | ✅ Applied |
| V6 | enrollments | ✅ Applied |
| V7 | exams, exam_questions, exam_choices | ✅ Applied |
| V8 | exam_attempts | ✅ Applied |
| V9 | course_progress, lesson_progress | ✅ Applied |
| V10 | certificates | ✅ Applied |
| V11 | groups, group_members | ✅ Applied |
| V12 | course_resources | ✅ Applied |
| V13 | group_courses | ✅ Applied |
| V14 | certificates_v2 (PDF metadata) | ✅ Applied |
| V15 | teacher_requests | ✅ Applied |
| V16 | users role constraint CHECK | ✅ Applied |
| V17 | exam_attempts index on (user_id, passed) | ✅ Applied |

**Next migration:** V18 (use for any new schema change needed in Sprint 8+)

Rules:
- Never edit an already-applied migration
- Always create a new `V{n}__description.sql`
- Never use `ddl-auto: create` or `ddl-auto: update`
- Always preserve existing data unless user explicitly requests reset

---

## Key Business Rules (Locked)

These are fixed decisions. Do not reopen without explicit user instruction.

| Rule | Value |
|---|---|
| Pass threshold | 80% |
| Max attempts before cooldown | 2 failures |
| Cooldown duration | 72 hours |
| Exam scoring location | Backend only |
| Correct answers serialized to client | Never |
| Certificate trigger | Exam pass only (not lesson completion) |
| Avatar max size | 5MB |
| Certificate number format | `EL-{year}-{12hex}` |
| Verification hash | SHA-256 |
