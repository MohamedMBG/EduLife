# CLAUDE.md — EduLife

Operational guide for Claude Code.

Product and architecture rules live in `AGENTS.md` — read it before touching any feature.

---

## Repo Layout

```text
EduLife/
  app/                    Android app (Java + XML, MVVM)
  backend/                Spring Boot modular monolith
  guided-journey-lab/     Web app/dashboard (React 19 + TanStack)
  docs/                   Dated audit files
  diagrams/
  AGENTS.md               Full product + architecture spec
  CLAUDE.md               Claude Code operational rules
```

---

## Project Priority

EduLife is now a **mobile + web MVP** powered by one strong backend.

The backend is the foundation.

Priorities:

1. Stable backend
2. Clean database schema
3. Secure authentication
4. Clear API contracts
5. Android and web consuming the same backend
6. No fake completed features
7. No overengineering

Do not sacrifice backend quality just to make a screen look finished.

---

## Three Separate Deployables

| Component             | Language         | Push target      |
| --------------------- | ---------------- | ---------------- |
| `app/`                | Java + XML       | EduLife `origin` |
| `backend/`            | Spring Boot      | EduLife `origin` |
| `guided-journey-lab/` | React/TypeScript | `web` remote     |

---

## Running Each Component

### Android

Open in Android Studio.

Requires:

```text
app/google-services.json
```

Run on emulator or physical device.

---

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

Required environment variables:

```text
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
FIREBASE_ADMIN_CREDENTIALS_PATH
FIREBASE_ADMIN_CREDENTIALS_JSON
APP_CORS_ALLOWED_ORIGINS
EDULIFE_AVATAR_STORAGE_DIR
EDULIFE_AVATAR_PUBLIC_BASE_URL
```

Database schema is managed by Flyway.

Never use:

```text
ddl-auto: create
ddl-auto: update
```

Never edit already-applied migrations.

Create a new migration instead.

---

### Web

```bash
cd guided-journey-lab
bun install
bun run dev
```

Stack:

* React 19
* TypeScript
* TanStack Start / Router
* shadcn/ui
* Tailwind v4
* Cloudflare Workers via `wrangler.jsonc`

---

## Git Workflow

### Android + Backend

```bash
git push origin <branch>
```

### Web

Always push the web app using subtree:

```bash
git subtree push --prefix=guided-journey-lab web main
```

Remote:

```text
web = https://github.com/MohamedMBG/guided-journey-lab.git
```

If rejected because of non-fast-forward, force push only after user confirmation:

```bash
git push web <commit-hash>:main --force
```

---

## Commit Rules

Use Conventional Commits:

```text
feat(android):
fix(android):
feat(backend):
fix(backend):
feat(web):
fix(web):
docs:
chore:
```

Forbidden:

```text
Co-Authored-By
```

Do not add AI co-author trailers.

---

## Backend Architecture

Backend path:

```text
backend/src/main/java/com/edulife/
```

Current architecture:

```text
Modular Monolith
```

Modules:

```text
auth/
users/
roles/
profiles/
courses/
enrollments/
lessons/
progress/
exams/
certificates/
groups/
admin/
security/
common/
config/
```

Each module should follow:

```text
controller/
service/
repository/
dto/
entity/
exception/
```

Rules:

* Business logic belongs in `service/`
* Controllers must stay thin
* Repositories must not contain business decisions
* DTOs must be used for API input/output
* Never expose JPA entities directly
* Never expose `firebase_uid`
* Never trust `userId` or `role` from the client

---

## Backend Security Rules

Mandatory:

* Firebase token validation on protected routes
* `email_verified` enforced before learner-flow access
* Internal user resolved server-side
* Ownership checks on enrollments, progress, exams, certificates, avatars
* Correct exam answers never sent to client
* Exam scoring happens only on backend
* Certificates generated only on backend
* Avatar upload max size: 5MB
* CORS must be explicit, never wildcard in production

Public or semi-public routes must be intentional and documented.

---

## Backend Quality Rules

Every backend feature must include:

* request DTO
* response DTO
* validation
* service-layer logic
* clear error handling
* security/ownership check
* Flyway migration if schema changes
* audit doc in `/docs`

Do not create quick endpoints that bypass the architecture.

---

## Android Architecture

Path:

```text
app/src/main/java/com/baghdad/edulife/
```

Architecture:

```text
Feature-first MVVM
```

Structure:

```text
core/
  network/
  session/
  storage/

features/
  auth/
  courses/
  onboarding/
  profile/
```

Rules:

* Java only
* XML layouts only
* No Kotlin
* No Hilt/Dagger
* Manual dependency injection is acceptable
* Retrofit + OkHttp only
* Token refresh must be synchronized
* Never put API calls inside Fragments
* Never put business logic inside UI classes

Flow:

```text
Fragment
→ ViewModel
→ Repository
→ ApiService
→ Backend
```

---

## Web Architecture

Path:

```text
guided-journey-lab/
```

Routes:

```text
src/routes/index.tsx
src/routes/login.tsx
src/routes/register.tsx
src/routes/dashboard.tsx
src/routes/courses.tsx
src/routes/explore.tsx
src/routes/level.tsx
```

Recommended structure:

```text
src/
  routes/
  components/
  features/
  lib/
    api/
    auth/
  types/
```

Rules:

* Web must consume backend API
* No duplicated business logic
* No direct database access
* No fake local data after backend endpoint exists
* No hardcoded API responses
* No auth bypass
* No UI-only implementation pretending to be complete

A web feature is not complete unless it works with the real backend or is clearly marked as temporary.

---

## Shared API Rule

Android and web must consume the same backend contracts.

Do not create different backend behavior for each client.

If an endpoint is needed by both clients, design it once properly.

---

## Key Business Rules

From `AGENTS.md`:

* Pass threshold: **80%**
* Exam attempts: **2 failures → 72-hour cooldown**
* Enrollment is transactional
* Enrollment must create initial progress
* Certificates only after passing exam
* Completing lessons alone does not generate certificates
* Correct answers are never serialized to client
* Avatar upload max size: **5MB**
* Avatar file stored on filesystem
* Avatar URL stored in database

---

## MVP Boundaries

Do not build:

* microservices
* payments
* revenue sharing
* real-time chat
* AI recommendations
* gamification
* discussions
* notifications
* CMS
* complex admin dashboards
* advanced analytics

Do not add post-MVP features unless the user explicitly asks.

Current MVP focus:

```text
Login
→ Course Discovery
→ Enrollment
→ Lessons
→ Progress
→ MCQ Exam
→ Certificate
```

---

## Flyway Rules

Allowed:

```text
V14__add_example_table.sql
V15__alter_profile_avatar.sql
```

Forbidden:

```text
Editing V1 after it has already run
Editing V2 after it has already run
Using ddl-auto to mutate schema
Dropping schema casually
```

Always preserve existing data unless the user explicitly requests a reset.

---

## Error Handling

Use consistent backend error responses:

```json
{
  "status": 400,
  "message": "Clear error message",
  "timestamp": "2026-05-29T00:00:00Z"
}
```

No raw stack traces in API responses.

No silent frontend failures.

Every client feature needs:

* loading state
* error state
* empty state
* success state

---

## Docs Audit

After every coding task, create:

```text
docs/YYYY-MM-DD-task-name.md
```

Include:

```text
# Task Title

## Goal

## What Changed

## Files Touched

## Backend Impact

## Android Impact

## Web Impact

## Architecture Compliance

## Tests / Verification

## Risks / Notes
```

Do not skip docs.

---

## Definition of Done

A task is done only when:

* backend compiles
* Android compiles if touched
* web compiles if touched
* Flyway migrations run cleanly
* real API works
* no fake data remains unless explicitly documented
* security rules are respected
* ownership rules are respected
* audit doc is created
* MVP scope is respected

---

## Final Rule

EduLife must be built like a real product, not a fragile demo.

Move fast, but protect the backend.

The backend is the product foundation.
