# Task Audit - Fix Profile Avatar Api Service

## Date
2026-05-28

## Task Summary
Fixed an Android compile error caused by `ProfileRepository` calling an `uploadAvatar` API method that was not declared in the shared Retrofit `ApiService` interface.

## Files Created
- docs/2026-05-28-fix-profile-avatar-api-service.md

## Files Modified
- app/src/main/java/com/baghdad/edulife/core/network/ApiService.java

## What Was Done
Added the missing avatar upload Retrofit contract to `ApiService`, including the `AvatarUploadResponse` import, multipart Retrofit annotations, the multipart `Part` import, and the `uploadAvatar` method declaration targeting `POST /profile/avatar`.

## Architecture Compliance
This change stays inside the Android shared networking layer under `core/network`, which is the correct location for Retrofit endpoint contracts used by feature repositories such as `features/profile/data/ProfileRepository`.

## Code Comments Added
Added a short API contract comment above the new `uploadAvatar` method to explain why the backend owns avatar validation and canonical storage behavior.

## Validation / Testing
Validated by tracing the compile error from `ProfileRepository` to `ApiService` and confirming that `uploadAvatar` was missing before adding the Retrofit declaration.
Android build/test execution was not run in this task.

## Risks / Notes
This resolves the reported missing symbol error in the repository layer. Runtime success still depends on the backend exposing a compatible `POST /profile/avatar` multipart endpoint and response body.
