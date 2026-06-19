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

## Mobile App And Architecture Impact
This task did not add or modify Android code, but it directly affects how the mobile app should be understood architecturally. EduLife uses pragmatic MVVM on Android with a feature-first structure, so the security rules enforced here belong on the backend and must be treated by the app as server-owned truth, not UI-owned policy.

From the mobile side, the important contract is unchanged:
- Android still authenticates with Firebase Authentication.
- the app still sends the Firebase ID token as a Bearer token through the networking layer,
- `/api/v1/auth/sync` still returns the internal `userId` and the resolved backend role,
- the backend remains the only trusted source for role assignment and protected CMS access decisions.

This is important for the app architecture because it keeps responsibility boundaries clean:
- `core/network/` remains responsible for token transport, refresh, retry-on-401 behavior, and API serialization,
- `features/auth/data/` and its repository layer should continue treating auth sync as a synchronization call, not as a place where the client chooses a privileged role,
- `features/auth/viewmodel/` should expose the resolved backend role from the API response and never assume that a locally selected role is authoritative,
- any future teacher or group-admin screens under `features/teacher/` or `features/admin/` must be gated by backend responses and handled as permission-based navigation states, not as client-side trust.

In practical Android terms, this means the current backend fix preserves the existing MVP architecture instead of forcing a mobile redesign. No Retrofit DTO contract was broken, no Navigation graph changes are required, and no XML/UI flow has to be reworked immediately. The app can keep its current auth-sync call pattern, but it should present role-specific UI only after the backend confirms the persisted role.

The CMS read-access tightening also matters for future mobile work. If Android later adds teacher CMS screens for course authoring, section review, or exam management, those screens must expect possible `403` responses for users who are authenticated but outside the ownership scope. In MVVM terms, those cases belong in repository error mapping and ViewModel UI state handling, not in ad hoc Fragment logic. That keeps authorization failures consistent with the existing feature-first architecture and avoids mixing backend permission rules into UI code.

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
