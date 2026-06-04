# Task Audit - Android Teacher Request Flow

## Date
2026-05-31

## Task Summary
Added the learner-to-teacher upgrade request flow on Android Profile. Learner accounts can now view their latest teacher-request status and submit a new motivation from the Profile screen using the existing backend `/api/v1/teacher-requests` endpoints.

## Files Created
- app/src/main/java/com/baghdad/edulife/features/profile/model/SubmitTeacherRequestBody.java
- app/src/main/java/com/baghdad/edulife/features/profile/model/TeacherRequestResponse.java
- app/src/main/java/com/baghdad/edulife/features/profile/data/TeacherRequestRepository.java
- app/src/main/java/com/baghdad/edulife/features/profile/viewmodel/TeacherRequestViewModel.java
- docs/2026-05-31-android-teacher-request-flow.md

## Files Modified
- app/src/main/java/com/baghdad/edulife/core/network/ApiService.java
- app/src/main/java/com/baghdad/edulife/features/courses/ui/ProfileFragment.java
- app/src/main/res/layout/fragment_profile.xml
- app/src/main/res/values/strings.xml

## What Was Done
Added Retrofit support for:
- `GET /api/v1/teacher-requests/me`
- `POST /api/v1/teacher-requests`

Created a dedicated teacher-request repository and ViewModel so request status loading and submission stay separate from the existing profile-load logic.

Updated the Profile screen with a new teacher-access row that:
- is shown only for learner/student accounts
- displays the latest request status
- opens a motivation dialog when the account can submit a request

Submission now validates that motivation is not blank before hitting the network. On success, the latest request state is updated immediately so the UI reflects the backend response without waiting for a full screen refresh.

If the backend reports that a pending request already exists, the ViewModel refreshes the canonical latest request from the server instead of leaving the UI in a guessed state.

## Architecture Compliance
The implementation follows the current Android feature structure:
- request DTOs in `features/profile/model`
- backend access in `features/profile/data`
- screen state in `features/profile/viewmodel`
- UI wiring in the existing `ProfileFragment`

No unrelated architecture was introduced, and network calls remain outside the fragment.

## Code Comments Added
Added comments in the ViewModel and fragment around:
- refreshing backend state after an already-pending response
- hiding the feature for non-learner roles
- why the dialog validates motivation locally before submission

These comments document the business behavior rather than repeating obvious code.

## Validation / Testing
Ran `./gradlew.bat :app:compileDebugJavaWithJavac` successfully.

Manual QA recommended for:
- learner account with no request sees the submit path
- learner account with pending request sees status and cannot submit another one accidentally
- rejected request can be resubmitted
- elevated roles do not see the request row
- backend 409 path refreshes the latest request correctly

## Risks / Notes
The current UI uses a compact profile row plus dialog rather than a dedicated full-screen flow. That is intentional for MVP scope and keeps the profile screen lightweight.

The fragment currently formats fallback timestamps with a hardcoded `"recently"` string path if the backend date is missing or malformed. This is safe, but if more request history UI is added later it should be moved to a dedicated display string.
