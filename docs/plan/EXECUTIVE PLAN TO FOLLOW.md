EduLife MVP — Realistic Execution Plan
Analysis date: 2026-04-26. Ground truth: Execution Plan v2 + AGENTS.md. Current state: Android skeleton exists, backend = zero.

1. PHASES (HIGH LEVEL)
Phase 0 — Foundation (Sprint 0)
Lock skeleton: Spring Boot project boots, PostgreSQL connects, Flyway runs, Android has Navigation + Retrofit + Firebase SDK wired. No feature code. No UI. Just infrastructure that works.

Phase 1 — Identity Bridge (Sprint 1)
Firebase Auth connects to backend. User can register → verify email → login → backend sync returns internal UUID. Every subsequent request carries a validated token. This is the load-bearing wall — everything else sits on it.

Phase 2 — Content Plane (Sprints 2 + 2A)
Backend exposes course/section/lesson data. Android displays it. CMS allows teacher/admin to create that data. This is the most dangerous phase for scope explosion — Sprint 2A (CMS) is heavier than the plan implies.

Phase 3 — Enrollment (Sprint 3)
Learner enrolls, unenrolls, sees My Courses. Simple CRUD but transactional — enrollment + progress init must be atomic.

Phase 4 — Learning Core (Sprint 4)
Lesson navigation, lesson completion marking, progress calculation. The lesson viewer complexity depends entirely on content type (WebView vs native text).

Phase 5 — Evaluation (Sprint 5)
MCQ exam: questions served without answers, answers submitted server-side, scored server-side, attempt policy enforced (2 attempts → 72h cooldown). Highest security risk in the app.

Phase 6 — Certificate (Sprint 6)
iText PDF generation on exam pass. Verification hash. Download/share in Android.

Phase 7 — UAT + Hardening (Sprint 7)
Full end-to-end flow test. Error/empty states. Play Store requirements (delete account). Fix all contract mismatches found.

2. SPRINT PLAN — REVISED FOR REALISM
Sprint 0 — Foundation
Duration: 3 days (plan says 2 — unrealistic, backend project init + Flyway + Android wiring = 3 days minimum)

Goal: Both sides boot and talk to each other with zero features.

Backend tasks:

Init Spring Boot project, package com.edulife, module folders per AGENTS.md structure
PostgreSQL datasource in application.yml (profile-based: dev/prod)
Flyway dependency + V1__init.sql placeholder migration that runs cleanly
Global ApiError response contract { status, message, timestamp }
/api/v1/ prefix enforced via @RequestMapping
GET /actuator/health returns 200
Android tasks:

Add Firebase Auth SDK to build.gradle + google-services.json
Add Navigation Component, Retrofit 2, OkHttp 3, Gson/Moshi to build.gradle
Wire MainActivity to nav graph (already has activity_main.xml + nav_graph.xml)
Add BaseApiService stub + ApiClient with OkHttp (skeleton exists in core/network/)
Confirm login/register fragments navigate correctly in nav graph
Definition of Done:

Spring Boot starts, hits health endpoint → 200
Flyway migration runs without error
Android builds, onboarding → login → register nav works (no real auth yet)
Sprint 1 — Identity Bridge
Duration: 4 days (plan says 2 — underestimated, token filter + sync endpoint + Android interceptor + 401 retry = 4 days for 1 developer)

Goal: End-to-end: register → verify email → login → backend sync → internal UUID stored in Android.

Backend tasks:

Firebase Admin SDK dependency + FirebaseApp.initializeApp() with service account JSON
FirebaseTokenFilter (OncePerRequestFilter): validate signature, expiry, email_verified claim → resolve to internal UUID via SecurityContext
Flyway V2__users.sql: users table (UUID PK, firebase_uid UNIQUE, email, role, created_at)
POST /api/v1/auth/sync: upsert user, return { userId, role } — firebase_uid never in response
Tests: expired token → 401, email_verified=false → 403, valid token → 200 + UUID
Android tasks:

Wire LoginFragment → FirebaseAuth.signInWithEmailAndPassword() (skeleton exists, not functional)
Wire RegisterFragment → FirebaseAuth.createUserWithEmailAndPassword() + send verification email
Email verification screen: block navigation until currentUser.isEmailVerified()
AuthStateListener in AuthViewModel: no Firebase user → navigate to Login, clear backstack
OkHttp interceptor: getIdToken(false) → Authorization: Bearer {token} on every request
401 retry: getIdToken(true) force-refresh, retry once
Call /auth/sync after login, store userId + role in SharedPreferences
Definition of Done:
Register new account → receive verification email → verify → login → backend sync called → userId UUID logged → Android navigates to (stub) Home screen.

Sprint 2 — Course Discovery
Duration: 4 days

Goal: Authenticated learner sees course list and course detail from live backend.

Backend tasks:

Flyway V3__courses.sql: courses, course_sections, lessons tables
Dev seed: 3–5 courses with sections and lessons (SQL seed file, not CMS yet)
GET /api/v1/courses — list with category filter, offset pagination
GET /api/v1/courses/{id} — detail + sections + lessons
All endpoints behind token filter
Android tasks:

CourseListFragment + CourseCatalogViewModel + CourseRepository + CourseApiService
CourseDetailFragment + CourseDetailViewModel
Category filter chips (hardcoded enum — no dynamic categories yet)
Empty state + error state + loading state on both screens
Navigation: Login success → CourseList → CourseDetail
Definition of Done:
Login → see seeded courses → tap course → see detail with sections. Comes from live backend.

Sprint 2A — Basic CMS
Duration: 7–10 days (this is NOT 2–3 days — be honest)

Goal: Teacher/admin can create and publish courses. Learner can then discover them.

⚠ Risk flag: This sprint is 40–50% of total MVP scope on its own. It introduces 3 new screens on Android (course management list, course editor, section/lesson editor) AND 6+ new backend endpoints AND role-based access AND approval workflow. Consider doing this in 2 sub-sprints if it blocks delivery.

Backend tasks:

Role check middleware: @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
POST /api/v1/cms/courses — create course (teacher-owned)
PUT /api/v1/cms/courses/{id} — update course metadata
POST /api/v1/cms/courses/{id}/sections — add section
PUT /api/v1/cms/courses/{id}/sections/{sectionId} — update/reorder
POST /api/v1/cms/sections/{id}/lessons — add lesson
PUT /api/v1/cms/sections/{id}/lessons/{lessonId} — update/reorder
PUT /api/v1/admin/courses/{id}/approve — admin approval (sets published flag)
Learner GET /api/v1/courses only returns published=true courses
Android tasks:

Role-gated entry point: if role == TEACHER or ADMIN, show management icon in nav
TeacherCoursesFragment — list teacher's own courses
CourseEditorFragment — create/edit course metadata
SectionLessonEditorFragment — add/edit sections and lessons
AdminApprovalFragment (admin only) — list pending courses, approve/reject
All CMS screens live under features/teacher/ and features/admin/ per AGENTS.md
Definition of Done:
Teacher logs in → creates course → adds sections/lessons → submits for approval → admin approves → learner sees it in discovery.

Sprint 3 — Enrollment
Duration: 3 days

Goal: Learner enrolls, unenrolls, sees My Courses.

Backend tasks:

Flyway V4__enrollments.sql
POST /api/v1/enrollments — enroll, idempotent, duplicate-prevented, transactional (enrollment + init lesson_progress rows)
GET /api/v1/enrollments/me — list my enrollments, ownership-scoped
DELETE /api/v1/enrollments/{id} — unenroll, ownership-scoped
Android tasks:

Enroll button on CourseDetailFragment — disabled if already enrolled
Unenroll confirmation dialog + action
MyCoursesFragment + MyCoursesViewModel
Bottom nav: Home (Discovery) ↔ My Courses
Definition of Done:
Enroll → appears in My Courses. Unenroll → disappears. Duplicate enroll attempt → handled gracefully.

Sprint 4 — Lessons + Progress
Duration: 5 days

Goal: Enrolled learner navigates lessons, marks complete, sees progress.

Backend tasks:

Flyway V5__lesson_progress.sql
GET /api/v1/enrollments/{id}/lessons — ordered lesson list with completion status
POST /api/v1/lessons/{id}/complete — mark complete, ownership-checked, idempotent
Progress % calculation returned on enrollment detail
Android tasks:

LessonListFragment — lessons for enrollment, completion indicators
LessonContentFragment — WebView for content_url (decide: WebView or Chrome Custom Tab)
Mark complete button → call API → update UI
Progress bar on MyCoursesFragment cards
Unresolved decision (must decide before Sprint 4): How is lesson content hosted? External URLs only (YouTube, PDF links) or uploaded files? This determines if you need file storage or just URL rendering. WebView = simple but security risk for arbitrary URLs. Chrome Custom Tab = safer.

Definition of Done:
Enroll → open lesson → view content → mark complete → progress updates on My Courses screen.

Sprint 5 — MCQ Exam
Duration: 5 days

Goal: Learner takes exam, gets pass/fail, attempt policy enforced.

Backend tasks:

Flyway V6__exam.sql: exam_questions, exam_attempts
GET /api/v1/courses/{id}/exam — questions WITHOUT correct_answer field (enforced at serialization level, not just convention)
POST /api/v1/courses/{id}/exam/submit — score server-side, persist exam_attempt, return { score, passed, attemptNumber, nextAttemptAllowed }
Block logic: 2 failed attempts → lock until submitted_at + 72h
Enrollment check: only enrolled users can access exam
email_verified check before exam access
Android tasks:

ExamFragment — display MCQ questions (scrollable list or one-at-a-time — decide before Sprint 5)
Submit button → confirmation dialog (non-reversible action)
ExamResultFragment — score display, pass/fail, retry countdown if locked, proceed to certificate if passed
Definition of Done:
Enroll → complete lessons → take exam → receive score → pass triggers certificate flow → fail shows retry state → 3rd attempt locked for 72h.

Sprint 6 — Certificate
Duration: 4 days

Goal: Passed learner receives server-generated certificate PDF.

Backend tasks:

Flyway V7__certificates.sql
Certificate generation triggered synchronously on exam pass (inside exam/submit transaction)
iText PDF: student name, course title, issue date, UUID, verification hash (SHA-256 of userId+courseId+issuedAt)
PDF stored to file system or external storage, URL stored in certificates table
GET /api/v1/certificates/me — list, ownership-scoped
GET /api/v1/certificates/{id} — ownership-verified PDF retrieval
Android tasks:

CertificatesFragment — list certificates
CertificateDetailFragment — display certificate details + download button
Download PDF: DownloadManager or direct stream to Downloads folder
Definition of Done:
Pass exam → certificate auto-generated → visible in Certificates screen → downloadable PDF.

Sprint 7 — UAT + Polish
Duration: 4 days

Goal: Ship-ready. No known crashes. Play Store compliant.

Tasks:

Full end-to-end flow test: register → verify → login → discover → enroll → lessons → exam → certificate
Error states on ALL screens (network error, 401, 403, 404, 500)
Empty states on ALL screens
DELETE /api/v1/account endpoint + Android "Delete Account" in Settings (Play Store mandatory)
Profile screen: read-only Firebase email + display name
CORS locked to known origins (not wildcard)
Fix all contract mismatches found during UAT
Review security checklist from Execution Plan v2
Definition of Done:
One developer can complete the full flow without hitting an unhandled error state. Delete Account works. All security checklist items verified.

3. PRIORITY ORDER
Build FIRST (in this order):
Backend foundation (Sprint 0) — nothing works without it
Firebase token filter (Sprint 1) — all subsequent endpoints depend on it
/auth/sync endpoint (Sprint 1) — Android depends on UUID from this
Course discovery endpoints (Sprint 2) — seed data, no CMS yet
Enrollment (Sprint 3)
DO NOT TOUCH EARLY:
Sprint 2A (CMS) — tempting to build early but kills momentum. Seed data is enough to prove the learner flow. CMS blocks Sprint 2 completion if you start it simultaneously.
Certificate PDF — do not touch iText until Sprint 5 exam logic is working perfectly
Discussions and notifications (in AGENTS.md as MVP) — these are not in the sprint plan. Flag this: if they must ship in MVP, they need to be added to Sprint 4 or Sprint 7. Do not surprise yourself with this in Sprint 7 UAT.
"Delete Account" — do not defer. Play Store will reject without it. Put it in Sprint 1 stub + complete in Sprint 7.
What breaks the project if done wrong:
Firebase token filter — if you get the email_verified claim check wrong, you'll have a security hole that bypasses email verification. Every other sprint inherits this.
Exam scoring on client — if you ever let the client send a score instead of answers, the entire exam feature is worthless. The filter at DTO serialization level (never serialize correct_answer) must be set up in Sprint 5 and never relaxed.
Enrollment transaction — if enrollment is not atomic (enroll row + progress init), you get orphaned enrollments with no progress state, which breaks Sprint 4.
firebase_uid in API responses — if you leak this once, it becomes a pattern. Enforce from Sprint 1.
4. DEPENDENCIES / BLOCKERS
Technical blockers — must resolve before coding:
Blocker	Blocks	Deadline
Firebase project created, google-services.json downloaded	Sprint 0 Android	Before Sprint 0
Firebase Admin SDK service account JSON generated	Sprint 1 backend	Before Sprint 1
PostgreSQL instance running (local or Docker)	Sprint 0 backend	Before Sprint 0
google-services.json in app/ folder with correct package name com.baghdad.edulife	Sprint 0 Android	Before Sprint 0
Lesson content hosting decision (external URL vs uploaded files)	Sprint 4	Before Sprint 4
Exam screen layout decision (scrolling vs one-at-a-time)	Sprint 5	Before Sprint 5
Decisions already locked (Sprint 0 checklist = complete per Execution Plan v2):
Pass score 80%, attempt policy 2+72h, iText, manual DI, MCQ only — all locked. Do not reopen.

Hidden complexity:
Token refresh race condition: If two requests fire simultaneously when token is expired, both trigger getIdToken(true). The OkHttp interceptor must handle this with a lock (synchronized block or mutex) or you'll get double-refresh calls and potential 401 loops. This is non-obvious and will bite you in Sprint 1.

iText license: iText 7 community edition is AGPL. If EduLife is closed source, you cannot use it without a commercial license. Decide this before Sprint 6. Alternative: Apache PDFBox (Apache 2.0 license, no restrictions).

Flyway baseline: If you ever run migrations against a DB that already has tables (e.g., you manually created the schema to test), Flyway will fail. Use baselineOnMigrate=true only in dev. Never in prod.

CMS role detection on Android: After /auth/sync returns role, Android must gate CMS navigation. If role changes server-side (admin promotes user to teacher), Android still shows old role until next login. In MVP this is fine — just document it.

5. RISK ANALYSIS
Risk 1: Sprint 2A (CMS) swallows the project — HIGH
The CMS sprint has more moving parts than any other sprint: 3 Android screens, 8+ backend endpoints, role checks, approval workflow. For 1 developer this is 10–14 days, not 3–4. If you start it before Sprint 2 learner flow is proven, you lose momentum and ship nothing.

Mitigation: Do Sprint 2 (discovery with seed data) fully first. Prove the learner loop works. Only then do Sprint 2A.

Risk 2: Discussions + notifications unplanned — MEDIUM-HIGH
AGENTS.md explicitly lists "course discussion / Q&A threads" and "basic notifications" as MVP scope. The sprint plan has zero sprints for these. If these must ship for MVP, you need 1 extra sprint (roughly 7 days) that is not currently budgeted.

Mitigation: Decide right now: are discussions/notifications truly MVP or post-MVP? If post-MVP, update AGENTS.md. If MVP, add Sprint 4B after lessons.

Risk 3: Android auth UI exists but may have wrong patterns — MEDIUM
LoginFragment, RegisterFragment, AuthViewModel, AuthRepository exist but are not functional. The risk is that the existing code makes assumptions (e.g., direct API login call instead of Firebase SDK call) that require full rewrite rather than light wiring. Looking at the file list, LoginRequest and RegisterRequest models exist — if these were designed for backend auth instead of Firebase Auth, they're wrong.

Mitigation: Audit AuthRepository.java and AuthViewModel.java before Sprint 1. Do not wire on top of wrong assumptions.

Risk 4: iText AGPL license — MEDIUM
If EduLife is proprietary/closed source, AGPL means you must open-source all of it or buy a commercial license.

Mitigation: Verify license compliance before Sprint 6. Switch to PDFBox if needed.

Risk 5: Backend has no deployment plan — MEDIUM
No mention of where Spring Boot runs during development. If you test Android against localhost, it won't work on a real device (device can't reach your laptop's localhost). You need either a local network IP, ngrok, or a cloud instance.

Mitigation: Set up ngrok or a cheap VPS before Sprint 1 Android testing.

Risk 6: Overbuilding the CMS before learner loop is validated — MEDIUM
Building teacher UI before you've confirmed the learner end-to-end flow works is backwards. A teacher creating content that no learner can complete is waste.

Mitigation: Sprint 2 → Sprint 3 → Sprint 4 → Sprint 5 with seed data. Only then Sprint 2A.

Risk 7: Single-activity navigation complexity — LOW-MEDIUM
Navigation Component with a single MainActivity is correct per AGENTS.md. But deep linking into Certificate from Exam result, handling back stack on sign-out (clearing entire back stack), and conditional navigation based on role will require careful NavOptions setup. Getting this wrong causes phantom back navigation or "re-login after sign-out shows home screen briefly" bugs.

6. EXECUTION STRATEGY
Backend first or mobile first?
Backend first, by 1 sprint. Start backend Sprint 0+1 while Android Sprint 0 runs in parallel. Never let Android wait for backend — the moment Sprint 1 backend is done, Android Sprint 1 can start against a real API. But do not mock the backend API. You have only one developer — mocking creates double maintenance and false confidence.

Should you mock APIs?
No. Use seed data instead. Seed data in Flyway (SQL insert files) gives you a real backend response without mocking. The only acceptable mock is a local Retrofit MockInterceptor for Sprint 0 Android navigation testing — and only until Sprint 1 backend is live. Remove it the moment the real backend is up.

Vertical slices vs layers?
Vertical slices. Each sprint delivers one working feature end-to-end (backend endpoint + Android screen + navigation + error states). Do not build all backend layers first then all Android screens. That approach guarantees contract mismatches discovered late.

Concretely: Sprint 2 means GET /courses endpoint works AND CourseListFragment displays data from it AND error/empty states work. Fully done before Sprint 3 starts.

How to iterate:
Start each sprint with the backend contract first (define the DTO/response shape before writing controllers)
Then implement backend
Then implement Android against the real backend
Write the Definition of Done test manually before marking sprint done
7. FIRST 10 CONCRETE TASKS
In this exact order:

Create Firebase project — Firebase Console, add Android app with package com.baghdad.edulife, download google-services.json, place in app/. Enable Email/Password auth provider.

Audit existing Android auth code — Read AuthRepository.java, AuthViewModel.java, LoginFragment.java. Identify what must be deleted vs reused. If AuthRepository calls a custom backend login, delete it — Sprint 1 auth is Firebase SDK only.

Init Spring Boot project — spring-boot-starter-web, spring-boot-starter-data-jpa, flyway-core, postgresql, spring-boot-starter-security. Package com.edulife. Module folder structure per AGENTS.md section 6. Commit.

Wire PostgreSQL + Flyway — application-dev.yml with local DB config. V1__init.sql with empty migration (just a comment). Verify mvn spring-boot:run starts and Flyway runs. Commit.

Add global error handler — @RestControllerAdvice returning { status, message, timestamp } for all exceptions. GET /actuator/health → 200. Commit.

Add Firebase Auth SDK to Android — build.gradle dependency, sync. Add FirebaseApp.initializeApp() in Application class (create EduLifeApp.java if not exists, register in manifest). Confirm build succeeds.

Wire Navigation Component in MainActivity — Confirm nav_graph.xml has correct destinations for Onboarding → Login → Register → (stub) Home. Test navigation manually on device/emulator.

Implement Firebase token filter (backend) — FirebaseTokenFilter extends OncePerRequestFilter. Validate token, check email_verified, store firebaseUid in SecurityContext. Write unit tests: expired → 401, unverified → 403, valid → passes. This is the most critical code in the project — get it right before anything else.

Flyway V2 + /auth/sync — users table migration. POST /api/v1/auth/sync upserts user, returns { userId, role }. Test manually with a real Firebase token from the Firebase console (REST API to get a test ID token).

Wire Firebase login in Android — LoginFragment.java: call FirebaseAuth.signInWithEmailAndPassword(), check isEmailVerified(), call /auth/sync, store UUID + role in SharedPreferences, navigate to Home stub. This is the first real end-to-end moment. It proves the stack works.

CRITICAL NOTES FOR THE DEVELOPER
AGENTS.md vs Execution Plan v2 conflict to resolve today:
AGENTS.md includes discussions and notifications as MVP. Execution Plan v2 sprint plan has no sprint for them. You must decide: are they MVP or not? If yes, add Sprint 4B. If no, update AGENTS.md. Leaving it unresolved means Sprint 7 UAT will surface a missing feature with no sprint budget to build it.

Sprint reorder recommendation:
The Execution Plan v2 puts Sprint 2A between Sprint 2 and Sprint 3. This is a mistake for a solo developer. Recommended reorder: Sprint 0 → 1 → 2 → 3 → 4 → 5 → 6 with seed data the whole time, then Sprint 2A after Sprint 6 (or parallel with Sprint 6 if you have energy left). The learner flow must be proven before the teacher flow is built.