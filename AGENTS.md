# AGENTS.md — EduLife Project Instructions

## 1. Project Context

EduLife is a mobile-first educational platform focused on structured learning for Moroccan learners. The platform allows students to create accounts, browse courses, enroll, watch lessons, take final exams, receive certificates, and interact with teachers through course discussions.

The product is designed around multilingual accessibility, especially Darija, French, and English, with future expansion toward AI course recommendations, mentorship, payments, and advanced analytics.

The current goal is to build a clean, realistic MVP, not a fully expanded learning ecosystem.

---

## 2. Core Product Vision

EduLife solves the problem of fragmented learning. Instead of students relying on random YouTube videos, WhatsApp groups, PDFs, and disconnected content, EduLife centralizes the learning journey into one structured platform.

The core learning loop is:

```text
Discover course → Enroll → Learn → Take exam → Pass → Receive certificate
```

Everything in the MVP must support this loop directly.

---

## 3. Current Architecture Decision

### Backend

Use a **Modular Monolith**.

The backend is one deployable application, internally divided into clear domain modules.

Do **not** implement microservices in the MVP.

Microservices are postponed because they would introduce unnecessary complexity in deployment, tracing, service communication, consistency management, and debugging.

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

Do not create heavy Clean Architecture layers unless they are clearly useful.

---

## 4. MVP Scope

### Included in MVP

The MVP includes:

- Authentication
- Role-based access
- Student profile
- Teacher profile
- Course catalog
- Course details
- Course enrollment
- Lesson access
- Video/resource access
- Progress tracking
- Final MCQ exam
- Automatic correction
- Pass/fail result
- Certificate generation after passing
- Course discussion / Q&A threads
- Basic notifications
- Basic admin back-office
- Teacher verification / course approval

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
- Gamification
- Social feed
- Microservices
- Event-driven architecture
- Complex multi-tenant enterprise infrastructure

These features may be prepared conceptually, but must not block the MVP.

---

## 5. User Roles

EduLife has four main operational roles in the MVP.

### Student

Can:

- Register and log in
- Manage own profile
- Browse courses
- View course details
- Enroll in courses
- Access enrolled lessons
- Track own progress
- Take final exams
- View results
- Receive certificates
- Ask questions in course discussions
- Receive notifications

Cannot:

- Create courses
- Access other students’ private data
- Manage teachers
- Access platform business metrics

### Teacher

Can:

- Manage teacher profile
- Create courses
- Add course metadata
- Upload lessons and resources
- Structure lessons inside courses
- Create final exams
- View enrolled students in own courses
- Track student performance in own courses
- Answer student questions

Cannot:

- Access unrelated teachers’ private data
- Manage platform-level settings
- Access unrelated groups unless explicitly authorized

### Group Admin

Can:

- Manage teachers inside own group
- View courses created by group teachers
- Track enrollments inside own group
- View group performance summaries
- View revenue summaries only if enabled later

Cannot:

- Access other groups
- Control the whole platform

### Platform Admin

Can:

- Manage all users
- Manage teachers
- Verify teachers
- Manage groups
- Approve/reject courses
- Monitor enrollments
- Monitor platform activity
- Manage certificates
- Access reports and moderation tools

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
  discussions/
  notifications/
  groups/
  admin/
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
    discussions/
    notifications/
    teacher/
    admin/
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

Expected MVP entities:

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

### Interaction

- DiscussionThread
- DiscussionMessage
- Notification

### Organization

- Group
- GroupMembership
- TeacherVerification

Future-only entities:

- Payment
- Payout
- MentorshipSession
- AIConversation
- Recommendation
- GamificationBadge

Do not implement future-only entities unless explicitly requested.

---

## 9. Course Design Rules

A course must not be a flat object with random videos.

A course should support:

- Metadata
- Sections or chapters
- Lessons
- Downloadable resources
- Final exam
- Certificate eligibility
- Discussion threads

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
- Correct answer
- Student attempt
- Automatic scoring
- Pass/fail threshold
- Result display

Do not implement open text correction, manual review, timed exams, or large question banks in MVP unless explicitly requested.

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
- Verification code/token

---

## 12. Discussion Rules

Use course discussion / Q&A threads in the MVP.

Do not implement full real-time chat.

Discussion requirements:

- Student can ask a question in a course or lesson context
- Teacher can answer
- Messages are attached to course/lesson context
- Thread-based system is enough

---

## 13. Storage Strategy

Do not store videos, PDFs, or heavy files directly in the database.

Use external storage for:

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

## 14. Security Rules

Use JWT authentication and RBAC.

General flow:

```text
User logs in
  ↓
Backend verifies credentials
  ↓
JWT token generated
  ↓
Mobile app stores token securely
  ↓
Token sent with API requests
  ↓
Backend validates token and role permissions
```

Every protected endpoint must check authentication and authorization.

Never trust role or user ID values sent directly from the client without verifying them on the backend.

---

## 15. Backend Resilience Rules

The backend must include:

### Error Isolation

Each module handles its own errors. A failure in exams should not break courses.

### Validation Layer

Validate all incoming requests before business logic.

### Logging

Log important events and errors per module.

### Defensive Coding

Use null checks, safe parsing, and controlled access.

### Graceful Failure

Return controlled error responses instead of crashing.

Example:

```text
Exam unavailable → return clear error message
Courses remain accessible
```

---

## 16. Data Consistency Rules

Use relational database consistency.

Critical operations must use transactions.

Example: course enrollment should be transactional.

```text
Create enrollment
Update course statistics
Initialize student progress
```

If one step fails, rollback all steps.

---

## 17. Development Principles

Follow these principles:

1. Business before buzzwords
2. Modular monolith before microservices
3. Pragmatic MVVM before overengineering
4. Clear domain boundaries
5. Relational data where relationships matter
6. MVP simplicity with growth awareness
7. Future-ready design without premature complexity

---

## 18. Agent Behavior Rules

When modifying the project, AI agents must:

- Respect the MVP scope
- Avoid adding future features unless requested
- Keep architecture simple and clean
- Prefer feature-first organization
- Avoid useless abstractions
- Avoid creating microservices
- Avoid real-time chat unless requested
- Avoid payment logic unless requested
- Keep naming consistent
- Keep business logic outside UI/controllers
- Add validation for new inputs
- Add clear error handling
- Maintain role-based access rules
- Explain important architectural changes briefly

When unsure, choose the simpler MVP-compatible solution.

---

## 19. What Not To Do

Do not:

- Turn the backend into microservices
- Add Kafka/event-driven architecture in MVP
- Add complex Clean Architecture layers on Android without need
- Store video files in the database
- Mix admin, teacher, and student permissions
- Let students access other students’ private data
- Let teachers access unrelated courses
- Treat group as a user role
- Build full real-time chat in MVP
- Build payment/revenue/payout flows in MVP
- Overbuild AI features before the learning core works

---

## 20. Recommended Delivery Phases

### Phase 1 — Foundation

- Authentication
- Roles/permissions
- Profiles
- Course catalog
- Enrollments

### Phase 2 — Learning Core

- Lessons
- Resources
- Progress tracking

### Phase 3 — Evaluation

- Exams
- Attempts
- Scoring
- Certificates

### Phase 4 — Interaction

- Discussion threads
- Notifications

### Phase 5 — Organization/Admin

- Teacher verification
- Groups
- Group admin features
- Admin back-office

### Phase 6 — Post-MVP

- AI recommendation
- Mentorship
- Payments
- Analytics
- Richer community features

---

## 21. Final Architecture Statement

EduLife uses a mobile-first architecture with Pragmatic MVVM on Android and a Modular Monolith on the backend. This is intentional because the product is still in MVP phase and its domains are strongly connected: users, courses, lessons, enrollments, exams, certificates, discussions, and groups.

This architecture gives the project strong maintainability, simpler data consistency, faster development, and a clean path for future service extraction only if scale later justifies it.

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
// Only enrolled students can access lessons to prevent users from bypassing course enrollment.
```

Bad comment example:

```java
// This is a variable.
```

Do not over-comment obvious code. The goal is clarity, not noise.

---

## 23. Architecture Respect Rule

AI agents must respect the current EduLife architecture before writing any code.

Before implementing a task, the agent must identify where the change belongs:

- Android UI logic → `features/<feature>/ui/`
- Android state logic → `features/<feature>/viewmodel/`
- Android API/data access → `features/<feature>/data/`
- Android shared utilities → `core/`
- Backend endpoint → correct domain module controller
- Backend business logic → correct domain module service
- Backend persistence → correct domain module repository
- Backend DTOs → correct module `dto/`
- Backend entities/models → correct module entity/model folder

The agent must not create random folders, duplicate existing patterns, or introduce a new architecture style without explicit instruction.

If a task conflicts with the current architecture, the agent must explain the conflict and propose the smallest architecture-compatible solution.

---

## 24. Mandatory Task Audit File

After every completed task, the AI agent must create a documentation file inside `/docs`.

The file name must follow this format:

```text
/docs/YYYY-MM-DD-task-name.md
```

Rules:

- Use the current date.
- Use a short kebab-case task name.
- Example: `/docs/2026-04-25-add-login-fragment.md`
- If `/docs` does not exist, create it.
- This file is mandatory for every coding task, refactor, bug fix, setup task, or architecture change.

The audit file must include:

```markdown
# Task Audit — <Task Name>

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

The audit must be specific. Do not write generic text like “updated files”. Mention exactly what changed.

---

## 25. Final Response Required After Each Task

After completing a task, the AI agent must provide a clear final summary to the user.

The final response must include:

1. What was done
2. Which files were created or modified
3. Where the `/docs/YYYY-MM-DD-task-name.md` audit file was created
4. Any validation/testing performed
5. Any remaining risks or next steps

The explanation must be understandable for a human developer reviewing the work.

Do not only say “done”. Always provide a full task audit explanation.

---

## 26. Strict Implementation Discipline

For every task, the AI agent must follow this workflow:

```text
1. Understand the requested task
2. Locate the correct module/feature based on the architecture
3. Implement the smallest clean solution
4. Add useful comments in the code
5. Validate the change when possible
6. Create the dated audit file in /docs
7. Report clearly what was done
```

The agent must not skip steps 4, 6, or 7.

