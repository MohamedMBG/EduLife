# Task Audit - Backend Access Control Fixes

## Date
2026-06-19

## Task Summary
Hardened backend access control around auth sync, CMS authoring reads, group membership adds, group course attachment, and sensitive logging.

## Files Created
- `backend/src/main/java/com/edulife/admin/service/CmsCourseAccessGuard.java`
- `backend/src/test/java/com/edulife/admin/CmsLessonServiceTest.java`
- `backend/src/test/java/com/edulife/admin/CmsSectionServiceTest.java`
- `docs/2026-06-19-backend-access-control-fixes.md`

## Files Modified
- `backend/src/main/java/com/edulife/account/service/AccountService.java`
- `backend/src/main/java/com/edulife/admin/service/CmsExamService.java`
- `backend/src/main/java/com/edulife/admin/service/CmsLessonService.java`
- `backend/src/main/java/com/edulife/admin/service/CmsSectionService.java`
- `backend/src/main/java/com/edulife/auth/dto/AuthSyncRequest.java`
- `backend/src/main/java/com/edulife/auth/service/AuthSyncService.java`
- `backend/src/main/java/com/edulife/groups/service/GroupService.java`
- `backend/src/test/java/com/edulife/admin/CmsExamServiceTest.java`
- `backend/src/test/java/com/edulife/auth/AuthSyncControllerTest.java`
- `backend/src/test/java/com/edulife/auth/AuthSyncServiceTest.java`
- `backend/src/test/java/com/edulife/groups/GroupServiceTest.java`

## What Was Done
Closed the self-service role escalation path in `POST /api/v1/auth/sync` by forcing new self-synced accounts to start as `LEARNER` and documenting that `intendedRole` is accepted only for backward compatibility, not trust.

Added `CmsCourseAccessGuard` so CMS read endpoints apply one shared authorization rule before returning authoring data. `CmsExamService`, `CmsSectionService`, and `CmsLessonService` now block non-owner reads that could expose answer keys or unpublished lesson content.

Restricted group management flows in `GroupService` so:
- adding members by email no longer leaks whether an email is registered,
- member adds are limited to internal user IDs or the join-request flow,
- course attachment is limited to courses authored by the group owner or a managed teacher,
- platform admins retain broad management access.

Redacted Firebase UID values from account-deletion logs to avoid writing sensitive identifiers to server logs.

Added and expanded unit and controller tests for the auth sync, CMS read access, and group management cases above.

## Architecture Compliance
The changes stay inside the existing backend modular monolith and keep business rules in service classes. Shared CMS authorization was extracted into `admin/service/` instead of duplicating ownership checks across controllers or unrelated modules. No new architecture layer or cross-module pattern was introduced.

## Code Comments Added
Comments were added where the rules are easy to misunderstand:
- in `AuthSyncService` and `AuthSyncRequest` to explain why client role intent is ignored,
- in CMS services and `CmsCourseAccessGuard` to explain why read access is restricted,
- in `GroupService` to explain the anti-enumeration and course-scope protections,
- in tests to capture the security reason behind each scenario.

## Validation / Testing
Ran:
- `backend\\mvnw.cmd test "-Dtest=AuthSyncControllerTest,AuthSyncServiceTest,CmsExamServiceTest,CmsLessonServiceTest,CmsSectionServiceTest,GroupServiceTest"`

Result:
- `BUILD SUCCESS`
- `Tests run: 52, Failures: 0, Errors: 0, Skipped: 0`

## Risks / Notes
The local `.claude` session files were not included in the source commit because their changes are workstation state, not product behavior.

These changes tighten backend authorization without changing the Android contract. Clients may still send `intendedRole`, but the backend now ignores it for self-service role assignment.
