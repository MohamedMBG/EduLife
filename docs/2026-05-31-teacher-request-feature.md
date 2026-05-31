# Teacher Request Feature

## Goal
Allow learners to submit a request to become a teacher. Admins approve or reject. Approval atomically promotes the user's role to TEACHER.

## What Changed

### New module: `com.edulife.teacherrequests`
- `model/RequestStatus` — PENDING, APPROVED, REJECTED
- `entity/TeacherRequest` — stores request + review data linked to User entities
- `repository/TeacherRequestRepository` — JPA queries by userId+status, latest by user, page by status
- `dto/TeacherRequestResponse` — response record with user info, status, motivation, adminNote, timestamps
- `dto/SubmitTeacherRequestRequest` — motivation (optional, max 1000 chars)
- `dto/ReviewTeacherRequestRequest` — adminNote (optional, max 500 chars)
- `service/TeacherRequestService` — all business logic
- `controller/TeacherRequestController` — learner endpoints
- `controller/AdminTeacherRequestController` — admin endpoints
- `exception/` — 4 typed exceptions

### Updated
- `common/error/GlobalApiExceptionHandler` — 4 new exception handlers added
- `db/migration/V15__teacher_requests.sql` — new table + 2 indexes

## API Contracts

### Learner (authenticated)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/teacher-requests` | Submit request (LEARNER only) |
| GET | `/api/v1/teacher-requests/me` | Get latest request status (204 if none) |

### Admin (`hasRole('ADMIN')`)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/admin/teacher-requests?status=PENDING` | List requests, paginated |
| PUT | `/api/v1/admin/teacher-requests/{id}/approve` | Approve → role becomes TEACHER |
| PUT | `/api/v1/admin/teacher-requests/{id}/reject` | Reject with optional note |

## Business Rules
- TEACHER and ADMIN accounts cannot submit (→ 409)
- Only one PENDING request allowed at a time (→ 409)
- Approve/reject on non-PENDING request → 400
- Approval atomically sets user.role = TEACHER within the same transaction
- Rejected learner can submit a new request after rejection

## Files Touched
- `backend/src/main/resources/db/migration/V15__teacher_requests.sql` (new)
- `backend/src/main/java/com/edulife/teacherrequests/` (new module, 12 files)
- `backend/src/main/java/com/edulife/common/error/GlobalApiExceptionHandler.java` (updated)

## Backend Impact
New module, no changes to existing modules except global exception handler.

## Android Impact
None yet. Profile screen can add "Request Teacher Access" button consuming `POST /api/v1/teacher-requests`.

## Web Impact
None yet. Profile settings page can surface the request flow.

## Architecture Compliance
- Business logic in service layer only
- Controllers are thin
- DTOs used for all input/output
- Entities not exposed directly
- Flyway migration for schema change
- Typed exceptions mapped in global handler
- Ownership check: learner can only see own request; admin sees all

## Tests / Verification
- Backend must compile cleanly
- Flyway V15 must apply on clean DB
- Submit with TEACHER role → 409
- Submit twice while PENDING → 409
- Approve non-pending → 400
- Approve PENDING → user role changes to TEACHER

## Risks / Notes
- After approval, the Firebase token still carries the old role until the user calls `/auth/sync` again or refreshes their token. Clients should re-sync after role change.
