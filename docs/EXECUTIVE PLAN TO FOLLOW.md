# EduLife MVP — Execution Plan v2

> Source: Council review (2026-04-26). Supersedes informal planning. Lock this before writing code.

---

## Locked Decisions

| Decision                       | Answer                                                     |
| ------------------------------ | ---------------------------------------------------------- |
| Auth system                    | Firebase Auth only                                         |
| Business data                  | PostgreSQL (Spring Boot owns all logic)                    |
| Firebase scope                 | Identity only — credentials, email verification, ID tokens |
| Firebase data (Firestore/RTDB) | NEVER — rejected permanently                               |
| Custom JWT                     | Deferred post-MVP                                          |
| User roles in MVP              | Learner only                                               |
| Question types in MVP          | MCQ only                                                   |
| Certificate trigger            | Exam pass (score threshold defined before Sprint 4)        |
| Exam attempt policy            | Define before Sprint 4 (single or configurable)            |
| Content seeding                | Manual seed data — no admin/CMS in MVP                     |
| Unenroll                       | Out of MVP unless enrollment limit is required             |
| Admin panel                    | Out of MVP                                                 |

---

## MVP Flow

```
Login → Course Discovery → Enrollment → Lessons → MCQ Exam → Certificate
```

---

## Locked Architecture

```
FIREBASE (auth boundary — nothing else)
├── Credential storage (email + hashed password)
├── Firebase UID (internal only — never in API responses)
├── Email verification
├── Password reset
└── ID token issuance + refresh (1h, auto-refreshed by SDK)

POSTGRESQL (all business state)
├── users             UUID PK, firebase_uid (unique index), email, role, created_at
├── courses           UUID PK, title, description, category, thumbnail_url
├── course_sections   id, course_id FK, title, position
├── lessons           id, section_id FK, title, content_url, position
├── enrollments       UUID PK, user_id FK, course_id FK, enrolled_at, status
├── lesson_progress   user_id, lesson_id, completed_at
├── exam_attempts     UUID PK, user_id, course_id, score, passed, submitted_at
├── exam_questions    MCQ content, correct_answer (server-side only, never sent to client)
└── certificates      UUID PK, user_id, course_id, issued_at, verification_hash

SPRING BOOT (all business logic)
├── Firebase token filter (every protected request)
│   ├── Validates: signature, expiry, email_verified claim
│   └── Resolves: firebase_uid → internal user UUID
├── POST /api/v1/auth/sync
│   ├── Called by Android after every Firebase login
│   ├── Upserts user row in PostgreSQL (idempotent)
│   └── Returns: { userId: UUID, role: "LEARNER" } — never returns firebase_uid
├── Ownership enforcement on all resources
├── Exam scoring (server-side only)
└── Certificate generation (server-generated on exam pass)
```

---

## Authentication Flow

```
1. App opens
   AuthStateListener fires → no Firebase user → Login screen

2. User submits email + password
   Firebase SDK: signInWithEmailAndPassword()
   Check: currentUser.isEmailVerified()
   → Not verified: show email verification screen, block all navigation

3. Email verified → fetch Firebase ID token
   currentUser.getIdToken(false) → idToken string

4. Android calls POST /api/v1/auth/sync
   Header: Authorization: Bearer {idToken}
   Spring Boot filter: verify signature + expiry + email_verified
   Upsert row in users table
   Response: { userId: UUID, role: "LEARNER" }

5. Store internalUserId (in-memory + SharedPreferences)
   Navigate to Home / Course Discovery

6. Every subsequent API request
   OkHttp interceptor attaches fresh Firebase ID token
   Backend filter validates token, resolves to internal UUID
   All DB queries use internal UUID — firebase_uid never leaves the filter

7. Token expired mid-session (401)
   OkHttp interceptor: getIdToken(true) force-refresh
   Retry original request once

8. Sign out
   FirebaseAuth.getInstance().signOut()
   AuthStateListener fires → clear back stack → Login screen
```

---

## Sprint Plan

### Sprint 0 — Foundation (Days 1–2, decisions + skeleton)
No feature code until Sprint 0 is complete.

**Decisions to lock (before any code):**
- [ ] Confirm learner-only role for MVP
- [ ] Confirm MCQ-only exam type
- [ ] Define certificate trigger (pass score threshold)
- [ ] Define exam attempt policy (single or N attempts)
- [ ] Confirm unenroll is out of MVP

**Backend:**
- [ ] Spring Boot project, module package structure
- [ ] Flyway configured
- [ ] PostgreSQL connection
- [ ] Global error response contract `{ status, message, timestamp }`
- [ ] `/api/v1/` prefix enforced
- [ ] Health endpoint

**Android:**
- [ ] Project setup: Navigation Component, Retrofit, OkHttp, ViewBinding
- [ ] Firebase Auth SDK added
- [ ] Base network layer structure

---

### Sprint 1 — Identity Bridge (Days 2–3)
Goal: Android can log in with Firebase and reach an authenticated backend session.

**Backend:**
- [ ] Firebase Admin SDK dependency
- [ ] Token verification filter (signature + expiry + email_verified)
- [ ] Flyway migration: `users` table
- [ ] `POST /api/v1/auth/sync` endpoint (upsert, return UUID + role)
- [ ] Tests: expired token → 401, unverified email → 403, valid token → 200

**Android:**
- [ ] Login screen (email + password)
- [ ] Register screen (email + password)
- [ ] Email verification screen (block navigation until verified)
- [ ] `AuthStateListener` in main ViewModel (sign-out → Login)
- [ ] OkHttp interceptor: attach Firebase ID token to all requests
- [ ] `/auth/sync` call after login — navigation blocked until success
- [ ] 401 retry: force-refresh Firebase ID token, retry once

**Gate:** End-to-end test — register → verify email → login → backend sync → internal UUID returned.

---

### Sprint 2 — Course Discovery (Days 3–5)
Goal: Authenticated user sees course list and course detail.

**Backend:**
- [ ] Flyway migrations: `courses`, `course_sections`, `lessons`
- [ ] Seed data: 5 courses with sections and lessons
- [ ] `GET /api/v1/courses` (list, category filter, offset pagination)
- [ ] `GET /api/v1/courses/{id}` (detail + sections)
- [ ] All endpoints protected by token filter

**Android:**
- [ ] Course list screen (RecyclerView, ViewModel, Repository)
- [ ] Course detail screen (title, description, sections)
- [ ] Category filter UI
- [ ] Empty state + error state on both screens

**Gate:** Login → see course list → open course detail → data comes from live backend.

---

### Sprint 3 — Enrollment (Days 5–6)
Goal: Learner can enroll in a course and see enrolled courses.

**Backend:**
- [ ] Flyway migration: `enrollments`
- [ ] `POST /api/v1/enrollments` (enroll, ownership-scoped, duplicate-prevented)
- [ ] `GET /api/v1/enrollments/me` (list my enrolled courses)
- [ ] Ownership check: user can only see own enrollments

**Android:**
- [ ] Enroll button on course detail (disabled if already enrolled)
- [ ] My Courses screen (enrolled course list)
- [ ] Navigation: Home ↔ My Courses ↔ Course Detail

**Gate:** End-to-end — login → discover → enroll → see in My Courses.

---

### Sprint 4 — Lessons + Progress (1.5 weeks)
Goal: Enrolled learner navigates lessons and tracks completion.

**Backend:**
- [ ] Flyway migration: `lesson_progress`
- [ ] `GET /api/v1/enrollments/{id}/lessons` (ordered lesson list)
- [ ] `POST /api/v1/lessons/{id}/complete` (mark lesson complete, ownership-checked)
- [ ] Progress calculation per enrollment

**Android:**
- [ ] Lesson list screen (sequential, locked until previous complete or open — decide policy)
- [ ] Lesson content viewer (WebView or text, depending on content_url type)
- [ ] Progress indicator on My Courses screen

---

### Sprint 5 — MCQ Exam (1.5 weeks)
Goal: Learner completes MCQ exam, receives pass/fail result.

**Decisions required before Sprint 5 starts:**
- Pass score threshold (e.g. 70%)
- Attempt policy (single or configurable count)

**Backend:**
- [ ] Flyway migrations: `exam_questions`, `exam_attempts`
- [ ] `GET /api/v1/courses/{id}/exam` (return questions WITHOUT correct_answer field)
- [ ] `POST /api/v1/courses/{id}/exam/submit` (receive answers, score server-side, persist attempt)
- [ ] Return: score, passed boolean, attempt number
- [ ] Block re-attempt if policy = single attempt

**Android:**
- [ ] Exam screen (MCQ question display, one question at a time or scrolling — decide)
- [ ] Submit flow
- [ ] Result screen (score, pass/fail, retry or continue to certificate)

---

### Sprint 6 — Certificate (1 week)
Goal: Passed learner receives server-generated certificate.

**Backend:**
- [ ] Flyway migration: `certificates`
- [ ] Certificate generation triggered on exam pass (synchronous in MVP)
- [ ] `GET /api/v1/certificates/me` (list my certificates)
- [ ] `GET /api/v1/certificates/{id}` (ownership-verified retrieval)
- [ ] Verification hash generated server-side
- [ ] PDF generation library chosen (iText or JasperReports — lock before Sprint 6)

**Android:**
- [ ] Certificate screen (view certificate details in-app)
- [ ] Download/share certificate PDF

---

### Sprint 7 — End-to-End UAT + Polish
- [ ] Full flow test: register → verify → login → discover → enroll → complete lessons → pass exam → receive certificate
- [ ] Error states on all screens
- [ ] Empty states on all screens
- [ ] Google Play policy: "Delete Account" in settings (required — not optional)
- [ ] Profile screen: read-only display of Firebase email/display name
- [ ] Fix all contract mismatches found in UAT

---

## Security Checklist (enforce per sprint, not at end)

| Item | When |
|---|---|
| Firebase ID token verified server-side on every request | Sprint 1 — never remove |
| `email_verified` checked before enrollment/exam access | Sprint 1 |
| `firebase_uid` never in any API response | Sprint 1 |
| All sensitive resource IDs are UUIDs (not sequential) | Sprint 0 schema |
| Ownership check on every resource (enrollments, progress, attempts, certificates) | Per sprint |
| CORS locked to known origins (never wildcard) | Sprint 0 |
| Bean Validation on all request DTOs | Sprint 1+ |
| Exam answers scored server-side only — never trust client score | Sprint 5 |
| Certificate retrieval ownership-verified | Sprint 6 |
| "Delete Account" feature present before Play Store submission | Sprint 7 |

---

## What NOT to Build in MVP

- Admin panel / CMS
- Content upload by users
- Course ratings and reviews
- Complex search (category filter only)
- Mentorship, AI, payments, gamification, real-time chat
- Offline caching (post-MVP)
- Kafka, RabbitMQ, any async queue
- Multiple Activities (single-activity, Navigation Component)
- Custom HTTP client (Retrofit + OkHttp only)
- Microservices (modular monolith only)

---

## What Firebase Owns vs PostgreSQL

| Data | Owner |
|---|---|
| User credentials (password hash) | Firebase — never leaves |
| Firebase UID | Firebase internal — stored in `users` table as index only |
| Email verification state | Firebase (snapshot cached in `users.email_verified`) |
| Password reset | Firebase |
| ID token issuance | Firebase |
| User profile (display name, photo) | Firebase Auth profile (read-only in MVP) |
| Courses, sections, lessons | PostgreSQL |
| Enrollments | PostgreSQL |
| Lesson progress | PostgreSQL |
| Exam questions + answers | PostgreSQL (correct_answer server-side only) |
| Exam attempts + scores | PostgreSQL |
| Certificates | PostgreSQL |
| User roles | PostgreSQL |

---

## Blocking Questions (answer before Sprint 1 code starts)

1. Pass score threshold for exam? (e.g. 70%)
2. Single exam attempt or multiple?
3. Are lessons sequentially locked or open?
4. Certificate: generated synchronously on pass, or async job?
5. PDF library choice: iText or JasperReports?
6. DI on Android: Hilt or manual constructor injection? (check AGENTS.md)

---

*Generated from council review 2026-04-26. Cross-reference AGENTS.md for stack overrides.*
