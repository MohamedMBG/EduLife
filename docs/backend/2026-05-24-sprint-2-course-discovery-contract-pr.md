# Task Audit - Sprint 2 Course Discovery Contract PR

## Date
2026-05-24

## Task Summary
Prepared a Sprint 2 backend contract package for course discovery so the repository can close the planning issues for authenticated course list and course detail contracts, required catalog tables, Firebase protection confirmation, and initial seed-data expectations.

## Files Created
- docs/2026-05-24-sprint-2-course-discovery-contract-pr.md

## Files Modified
- docs/backend-architecture.md
- backend/src/main/java/com/edulife/courses/controller/CourseController.java
- backend/src/main/java/com/edulife/courses/service/CourseService.java
- backend/src/main/java/com/edulife/security/SecurityConfig.java
- backend/src/main/java/com/edulife/security/FirebaseTokenFilter.java
- backend/src/test/java/com/edulife/courses/CourseControllerTest.java

## What Was Done
Rewrote `docs/backend-architecture.md` into a contract-focused Sprint 2 reference that maps directly to issues `#110` through `#115`.

The document now explicitly defines:

- the authenticated contract for `GET /api/v1/courses`
- the authenticated contract for `GET /api/v1/courses/{courseId}`
- the minimal required Sprint 2 tables for `courses`, `course_sections`, and `lessons`
- the rule that discovery endpoints remain behind Firebase token validation and verified email checks
- the current seed-data target and the five seeded discovery-ready courses already in the backend
- the shared API error contract relevant to course discovery

Added helpful why-comments in the backend controller, service, and security classes so the protected course discovery behavior and DTO/service boundaries are easier to maintain.

Updated `CourseControllerTest` fixture construction to match the current DTO constructors that include `imageUrl`.

## Architecture Compliance
The task stays inside the existing EduLife architecture:

- backend contract documentation remains in `docs/`
- HTTP endpoint behavior stays in the `courses` and `security` backend modules
- business logic remains in `CourseService`
- controller code stays thin and delegates to the service layer
- no new folders, alternate architecture patterns, or out-of-scope MVP features were introduced

The work respects the current sprint order because it only hardens Sprint 2 course discovery and does not pull CMS, enrollments, exams, or certificates forward.

## Code Comments Added
Added comments in:

- `CourseController` to explain why the controller remains thin and why UUID detail lookup is used
- `CourseService` to explain the read-only transactional boundary, published-only detail reads, and DTO mapping responsibility
- `SecurityConfig` to explain the shared Firebase filter bean and the stateless Bearer-token security setup
- `FirebaseTokenFilter` to explain why missing headers fall through to Spring Security, why verified email is enforced, and why only trusted Firebase claims enter the security context

These comments explain non-obvious contract and security decisions rather than repeating the code.

## Validation / Testing
Ran targeted backend web/security tests successfully:

```text
./mvnw "-Dtest=CourseControllerTest,FirebaseTokenFilterSecurityTest" test
```

This passed with `14` tests green.

Also ran the full backend suite:

```text
./mvnw test
```

That run failed before completion because Flyway validation detected checksum drift in historical migrations `V2__courses.sql` and `V3__seed_courses.sql`.

## Risks / Notes
- The repository currently has historical migration checksum drift, so full-context backend tests that boot Flyway are blocked until the migration history is repaired or aligned.
- `docs/backend-architecture.md` was already in the working tree before this task; this PR keeps that path but replaces the content with a contract-focused version.
- There are unrelated local changes in `.claude/settings.local.json`, `.idea/misc.xml`, and `rapport PFA`; they should not be included in this PR.
