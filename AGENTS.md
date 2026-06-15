# AGENTS.md - EduLife Project Instructions

## 1. Project Context

EduLife is a mobile-first educational platform focused on structured learning for Moroccan learners. The platform helps students create accounts, browse courses, enroll, study lessons, take final exams, and receive certificates through one guided learning flow.

The product is designed around multilingual accessibility, especially Darija, French, and English, with future expansion toward AI recommendations, mentorship, payments, and analytics.

The current goal is a realistic MVP. The current technical state is:

- Android skeleton exists
- Backend does not exist yet

Execution decisions in this file must follow the "EduLife MVP - Realistic Execution Plan" dated `2026-04-26`.

---

## 2. Core Product Vision

EduLife solves the problem of fragmented learning. Instead of students relying on random videos, chat groups, PDFs, and disconnected content, EduLife centralizes the learning journey into one structured platform.

The core learning loop is:

```text
Discover course -> Enroll -> Learn -> Take exam -> Pass -> Receive certificate
```

Everything in the MVP must support this loop directly.

---

## 3. Current Architecture Decision

### Backend

Use a **Modular Monolith**.

The backend is one deployable Spring Boot application divided into clear domain modules.

Do **not** implement microservices in the MVP.

### Mobile

Use **Pragmatic MVVM**.

The Android app is implemented with Java and XML layouts.

Recommended mobile stack:

- Java
- XML layouts
- ViewModel
- LiveData
- Repository pattern
- Retrofit
- OkHttp logging interceptor
- Navigation Component
- Material Design Components
- Firebase Authentication SDK

Do not create heavy Clean Architecture layers unless they are clearly useful.

---

## 4. MVP Scope

### Included in MVP

The realistic MVP includes:

- Firebase-based authentication
- Backend identity sync with internal UUID
- Role-based access
- Student profile basics
- Teacher profile basics
- Course catalog
- Course details
- Course enrollment
- Lesson access
- Video/resource URL access
- Progress tracking
- Final MCQ exam
- Automatic correction
- Pass/fail result
- Certificate generation after passing
- Basic admin approval for courses
- Teacher verification / course approval
- Delete account flow for Play Store compliance

### Deferred Until Core Learner Flow Is Proven

These items are acknowledged product needs but are **not part of the first execution path** and must not block Sprints 0-7:

- Course discussion / Q&A threads
- Basic notifications
- Teacher CMS beyond what is explicitly scheduled

If discussions and notifications must ship in the MVP later, they require an explicit additional sprint after the learner flow is stable.

### Excluded from MVP

Do not implement these in the MVP:

- Advanced AI assistant with memory
- Personalized recommendation engine
- Real-time chat like WhatsApp or Discord
- Live video calls
- Mentor booking system
- Payments
- Revenue split / payouts
- Advanced analytics
- Social feed
- Microservices
- Event-driven architecture
- Complex multi-tenant enterprise infrastructure

These features may be prepared conceptually, but must not block the MVP.

---

## 5. User Roles

EduLife has four main operational roles in the product model.

### Student

Can:

- Register and log in
- Verify email
- Manage own profile
- Browse courses
- View course details
- Enroll in courses
- Access enrolled lessons
- Track own progress
- Take final exams
- View results
- Receive certificates

Cannot:

- Create courses
- Access other students' private data
- Manage teachers
- Access platform business metrics

### Teacher

Can:

- Manage teacher profile
- Create courses when CMS work is scheduled
- Add course metadata
- Structure sections and lessons
- Create final exams when exam authoring is explicitly scheduled

Cannot:

- Access unrelated teachers' private data
- Manage platform-level settings
- Access unrelated groups unless explicitly authorized

### Group Admin

Can:

- Manage teachers inside own group
- View courses created by group teachers
- Track enrollments inside own group
- View group performance summaries

Cannot:

- Access other groups
- Control the whole platform

### Platform Admin

Can:

- Manage all users
- Manage teachers
- Verify teachers
- Manage groups
- Approve or reject courses
- Monitor enrollments
- Manage certificates

Important: `Group` is not a role. It is a business entity. `GroupAdmin` is the user role linked to that group.

---

## 6. Backend Module Structure

Use a modular monolith organized by domain.

Recommended backend structure:

```text
backend/
  auth/
  users/
  profiles/
  roles/
  courses/
  lessons/
  resources/
  enrollments/
  progress/
  exams/
  certificates/
  groups/
  gamification/
  admin/
```

Deferred modules after the learner loop is stable:

```text
backend/
  discussions/
  notifications/
```

Each module should generally contain:

```text
controller/
service/
repository/
dto/
entity-or-model/
```

Keep module boundaries clean. Do not let controllers contain business logic.

---

## 7. Android Project Structure

Use feature-first MVVM.

Recommended structure:

```text
app/
  core/
    network/
    storage/
    utils/
    ui/
    navigation/

  features/
    auth/
    onboarding/
    profile/
    courses/
    lessons/
    exams/
    certificates/
    teacher/
    admin/
```

Deferred features after the learner loop is stable:

```text
features/
  discussions/
  notifications/
```

Each feature should follow:

```text
feature-name/
  ui/
  viewmodel/
  data/
  model/
```

Example:

```text
courses/
  ui/
    CourseCatalogFragment.java
    CourseDetailsFragment.java
  viewmodel/
    CourseCatalogViewModel.java
    CourseDetailsViewModel.java
  data/
    CourseRepository.java
    CourseApiService.java
  model/
    Course.java
    CourseUiState.java
```

---

## 8. Core Data Entities

Expected implementation order for MVP entities:

### Identity / Access

- User
- Role
- UserRole
- Profile

### Learning

- Course
- CourseCategory
- CourseSection
- Lesson
- CourseResource
- Enrollment
- Progress

### Evaluation

- Exam
- ExamQuestion
- ExamChoice
- ExamAttempt
- ExamAnswer
- Certificate

### Organization

- Group
- GroupMembership
- TeacherVerification

Deferred entities:

- DiscussionThread
- DiscussionMessage
- Notification
- Payment
- Payout
- MentorshipSession
- AIConversation
- Recommendation

Do not implement deferred entities unless explicitly requested or formally scheduled.

---

## 9. Course Design Rules

A course must not be a flat object with random videos.

A course should support:

- Metadata
- Sections or chapters
- Lessons
- Downloadable resources or external URLs
- Final exam
- Certificate eligibility

Recommended structure:

```text
Course
  Section 1
    Lesson 1
    Lesson 2
  Section 2
    Lesson 3
    Lesson 4
  Final Exam
  Certificate Eligibility
```

---

## 10. Exam Rules

For MVP, exams are MCQ-based.

The system should support:

- Final exam per course
- Questions
- Choices
- Correct answer stored only on the server
- Student attempt
- Automatic scoring on the server
- Pass/fail threshold
- Result display
- Attempt policy of 2 failed attempts followed by a 72-hour cooldown

Do not implement open text correction, manual review, timed exams, or large question banks unless explicitly requested.

Never serialize correct answers to the client.

---

## 11. Certificate Rules

A certificate is generated only after the student passes the final exam.

Do not generate certificates only because a student watched all lessons.

Certificate should contain:

- Student identity
- Course title
- Teacher or verified issuer identity
- Issue date
- Unique certificate identifier
- Verification code or hash

Do not implement certificate generation before exam logic is stable.

---

## 12. Storage Strategy

Do not store videos, PDFs, or heavy files directly in the database.

Use external storage or the file system for:

- Videos
- PDFs
- Course resources
- Certificates

Database stores only:

- File name
- File type
- File URL
- Related entity ID
- Metadata

---

## 13. Security Rules

Use Firebase Authentication on Android and a validated backend token bridge with RBAC.

General flow:

```text
User registers or logs in with Firebase
  ->
Firebase returns ID token
  ->
Android sends Bearer token with API requests
  ->
Backend validates token with Firebase Admin SDK
  ->
Backend checks email_verified and role permissions
  ->
Backend resolves internal user UUID and continues request
```

Mandatory rules:

- Every protected endpoint must validate the Firebase token
- `email_verified` must be enforced before protected learner flow access
- `/api/v1/auth/sync` must upsert and return internal `userId` and `role`
- Never expose `firebase_uid` in API responses
- Never trust role or user ID values sent directly from the client
- Android must refresh expired ID tokens safely and retry once on `401`
- Token refresh race conditions must be guarded with synchronization or equivalent locking

---

## 14. Backend Resilience Rules

The backend must include:

### Error Isolation

Each module handles its own errors. A failure in exams must not break courses.

### Validation Layer

Validate all incoming requests before business logic.

### Logging

Log important events and errors per module.

### Defensive Coding

Use null checks, safe parsing, controlled access, and ownership checks.

### Graceful Failure

Return controlled error responses instead of crashing.

Global API errors should follow this contract:

```json
{
  "status": 400,
  "message": "Validation failed",
  "timestamp": "2026-04-26T10:00:00Z"
}
```

---

## 15. Data Consistency Rules

Use relational database consistency.

Critical operations must use transactions.

Examples:

- Enrollment creation
- Enrollment removal when related progress must be updated
- Exam submission and certificate generation

Enrollment is a high-priority transactional rule:

```text
Create enrollment
Initialize lesson progress
Commit or rollback together
```

If one step fails, rollback all steps.

---

## 16. Delivery Phases And Sprint Plan

Execution must follow this realistic order:

### Sprint 0 - Foundation

Goal:

- Spring Boot starts
- PostgreSQL connects
- Flyway runs
- Android Navigation, Retrofit, OkHttp, and Firebase SDK are wired

### Sprint 1 - Identity Bridge

Goal:

- Register
- Verify email
- Login
- Backend token validation
- `/auth/sync`
- Internal UUID stored on Android

### Sprint 2 - Course Discovery

Goal:

- Backend serves seeded courses, sections, and lessons
- Android shows course list and course detail from live backend

### Sprint 3 - Enrollment

Goal:

- Enroll
- Unenroll
- My Courses

### Sprint 4 - Lessons And Progress

Goal:

- Lesson list
- Lesson content
- Mark complete
- Progress updates

### Sprint 5 - MCQ Exam

Goal:

- Questions served without answers
- Answers submitted to backend
- Score computed on backend
- Attempt policy enforced

### Sprint 6 - Certificate

Goal:

- PDF certificate generated after pass
- Verification hash stored
- Android can list and download certificates

### Sprint 7 - UAT And Hardening

Goal:

- Full end-to-end test
- Error states
- Empty states
- Delete account
- Security checklist review

### Sprint 2A - Basic CMS

This sprint is intentionally **deprioritized**.

Recommended order for a solo developer:

```text
Sprint 0 -> Sprint 1 -> Sprint 2 -> Sprint 3 -> Sprint 4 -> Sprint 5 -> Sprint 6 -> Sprint 7 -> Sprint 2A
```

Do not start CMS early if it risks blocking the learner flow.

---

## 17. Priority Order

Build first, in this exact order:

1. Backend foundation
2. Firebase token validation filter
3. `/api/v1/auth/sync`
4. Course discovery endpoints with seed data
5. Enrollment
6. Lessons and progress
7. Exams
8. Certificates
9. CMS only after the learner flow is proven

Do not touch early:

- CMS before the learner flow works end to end
- Certificate PDF generation before exam logic is stable
- Discussions and notifications unless they are formally added to schedule

---

## 18. Technical Blockers And Locked Decisions

Must be resolved before the matching sprint:

- Firebase project created
- `google-services.json` added to `app/`
- Firebase Email/Password auth enabled
- Firebase Admin service account JSON generated for backend
- PostgreSQL instance available
- Lesson content hosting decision made before Sprint 4
- Exam UI decision made before Sprint 5

Locked product and technical decisions:

- Pass score is `80%`
- Attempt policy is `2 failed attempts + 72 hour cooldown`
- Manual dependency injection is acceptable
- MCQ only for MVP

Do not reopen locked decisions unless explicitly requested.

Certificate engine note:

- If licensing makes iText unsuitable, switch to a license-compatible alternative such as PDFBox instead of blocking Sprint 6

---

## 19. Agent Behavior Rules

When modifying the project, AI agents must:

- Respect the realistic sprint order
- Protect the learner flow first
- Prefer backend-first vertical slices with Android integrated immediately after
- Avoid adding future features unless requested
- Avoid early CMS work unless the user explicitly asks for it
- Avoid discussions and notifications unless formally scheduled
- Keep architecture simple and clean
- Prefer feature-first organization
- Avoid useless abstractions
- Avoid creating microservices
- Avoid payment logic unless requested
- Keep naming consistent
- Keep business logic outside UI/controllers
- Add validation for new inputs
- Add clear error handling
- Maintain role-based access rules
- Explain important architectural changes briefly

When unsure, choose the simpler solution that keeps Sprint 0 through Sprint 7 moving.

---

## 20. What Not To Do

Do not:

- Turn the backend into microservices
- Add Kafka or event-driven architecture in MVP
- Add complex Clean Architecture layers on Android without need
- Store video files in the database
- Mix admin, teacher, and student permissions
- Let students access other students' private data
- Let teachers access unrelated courses
- Treat group as a user role
- Build full real-time chat in MVP
- Build payment/revenue/payout flows in MVP
- Overbuild AI features before the learning core works
- Mock large parts of the backend once seed-data-backed APIs exist
- Build teacher CMS before validating the learner flow end to end
- Score exams on the client

---

## 21. Execution Strategy

Use these delivery rules:

### Backend First By One Sprint

Backend foundation and identity work start first. Android may wire infrastructure in parallel, but feature work should target the real backend as soon as it exists.

### Vertical Slices

Each sprint should deliver backend endpoint, Android screen, navigation, and error or empty states together before moving on.

### Seed Data Over Mock APIs

Use Flyway seed data for discovery instead of maintaining long-lived mocked APIs.

### Contract First

Define DTOs and response contracts before writing controllers and Android callers.

### UAT Before Marking Complete

Each sprint must be validated against its Definition of Done before the next sprint begins.

---

## 22. Mandatory Code Commenting Rules

AI agents must always include helpful comments in the code they create or modify.

Comments are required for:

- Non-obvious business rules
- Security checks
- Role/permission checks
- Validation logic
- Error handling
- Database transactions
- API calls
- ViewModel state changes
- Repository methods
- Navigation logic
- Any workaround or technical decision that may confuse a future developer

Comments must explain **why the code exists**, not repeat what the code already says.

Good comment example:

```java
// Only enrolled students can access lessons so users cannot bypass enrollment with a direct URL.
```

Bad comment example:

```java
// This is a variable.
```

Do not over-comment obvious code. The goal is clarity, not noise.

---

## 23. Architecture Respect Rule

AI agents must respect the current EduLife architecture before writing any code.

Before implementing a task, identify where the change belongs:

- Android UI logic -> `features/<feature>/ui/`
- Android state logic -> `features/<feature>/viewmodel/`
- Android API/data access -> `features/<feature>/data/`
- Android shared utilities -> `core/`
- Backend endpoint -> correct domain module controller
- Backend business logic -> correct domain module service
- Backend persistence -> correct domain module repository
- Backend DTOs -> correct module `dto/`
- Backend entities/models -> correct module entity/model folder

Do not create random folders, duplicate patterns, or introduce a new architecture style without explicit instruction.

If a task conflicts with the current architecture, explain the conflict and propose the smallest architecture-compatible solution.

---

## 24. Mandatory Task Audit File

After every completed task, the AI agent must create a documentation file inside `/docs`.

The file name must follow this format:

```text
/docs/YYYY-MM-DD-task-name.md
```

Rules:

- Use the current date
- Use a short kebab-case task name
- If `/docs` does not exist, create it
- This file is mandatory for every coding task, refactor, bug fix, setup task, documentation architecture change, or execution policy update

The audit file must include:

```markdown
# Task Audit - <Task Name>

## Date
YYYY-MM-DD

## Task Summary
Short explanation of the task.

## Files Created
- path/to/file

## Files Modified
- path/to/file

## What Was Done
Detailed explanation of the implementation.

## Architecture Compliance
Explain how the task respects the current EduLife architecture.

## Code Comments Added
Explain where comments were added and why.

## Validation / Testing
Explain what was tested or what should be tested manually.

## Risks / Notes
Mention possible risks, limitations, or follow-up work.
```

The audit must be specific.

---

## 25. Final Response Required After Each Task

After completing a task, the AI agent must provide a clear final summary to the user.

The final response must include:

1. What was done
2. Which files were created or modified
3. Where the `/docs/YYYY-MM-DD-task-name.md` audit file was created
4. Any validation or testing performed
5. Any remaining risks or next steps

Do not only say "done". Always provide a full task audit explanation.

---

## 26. Strict Implementation Discipline

For every task, the AI agent must follow this workflow:

```text
1. Understand the requested task
2. Locate the correct module or feature based on the architecture
3. Implement the smallest clean solution
4. Add useful comments in the code
5. Validate the change when possible
6. Create the dated audit file in /docs
7. Report clearly what was done
```

The agent must not skip steps 4, 6, or 7.
