# Task Audit - Group Join Requests

## Date
2026-06-13

## Task Summary
Added backend support for teachers who are not attached to an institute group to request group membership, while preserving the rule that standalone teachers' course uploads are reviewed by the platform admin.

## Files Created
- backend/src/main/resources/db/migration/V21__group_join_requests.sql
- backend/src/main/java/com/edulife/groups/model/GroupJoinRequestStatus.java
- backend/src/main/java/com/edulife/groups/entity/GroupJoinRequest.java
- backend/src/main/java/com/edulife/groups/repository/GroupJoinRequestRepository.java
- backend/src/main/java/com/edulife/groups/dto/CreateGroupJoinRequest.java
- backend/src/main/java/com/edulife/groups/dto/ReviewGroupJoinRequest.java
- backend/src/main/java/com/edulife/groups/dto/GroupJoinRequestDto.java

## Files Modified
- backend/src/main/java/com/edulife/groups/controller/GroupController.java
- backend/src/main/java/com/edulife/groups/service/GroupService.java
- backend/src/main/java/com/edulife/admin/controller/CmsCourseController.java
- backend/src/main/java/com/edulife/admin/service/CmsCourseService.java
- backend/src/test/java/com/edulife/groups/GroupControllerTest.java

## What Was Done
Created a `group_join_requests` table with pending/approved/rejected status and a partial unique index to prevent duplicate pending requests for the same teacher and group.

Added backend endpoints for:

- `POST /api/v1/groups/{groupId}/join-requests` so a teacher can ask to join an institute group.
- `GET /api/v1/groups/join-requests/mine` so a teacher can see their own requests.
- `GET /api/v1/groups/{groupId}/join-requests` so a group owner or platform admin can review requests.
- `PUT /api/v1/groups/{groupId}/join-requests/{requestId}/approve` to approve a request and create group membership in one transaction.
- `PUT /api/v1/groups/{groupId}/join-requests/{requestId}/reject` to reject a request with an optional note.

Updated CMS comments to make the approval rule explicit: group admins approve courses only for teachers inside their groups, while standalone teachers remain in the platform admin review queue.

## Architecture Compliance
The change stays inside the existing modular monolith. Group membership request logic belongs to the `groups` backend module, while course publishing scope remains in the existing `admin` CMS service. No new role was added; `Group` remains a business entity and `GROUP_ADMIN` remains the reviewer role for owned groups.

The implementation keeps business logic in `GroupService`, persistence in repositories/entities, HTTP mapping in `GroupController`, and request/response contracts in `groups/dto`.

## Code Comments Added
Added comments explaining why teacher group joining is reviewer-controlled, why approval and membership creation are transactional, and why standalone teachers stay in the platform admin review queue. These comments document security and ownership rules rather than restating obvious code.

## Validation / Testing
Ran `./mvnw.cmd -Dtest=GroupControllerTest test` from `backend/`; it passed with 24 tests.

Also ran `./mvnw.cmd test`; compilation succeeded, but the full suite failed in existing `AuthSyncControllerTest.cleanDatabase` cleanup because seeded `groups.created_by` rows still reference users when the test tries to delete all users. That failure is unrelated to the group join request implementation.

## Risks / Notes
This task adds backend support only. Android/web screens still need to expose group discovery, teacher join request submission, and group admin review actions.

The service currently lets any teacher request any known group by ID. A future UI/API may need searchable public institute groups or invitation codes if groups should not be openly discoverable.
