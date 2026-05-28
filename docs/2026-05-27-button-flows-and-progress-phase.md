# Task Audit - Button Flows And Progress Phase

## Date
2026-05-27

## Task Summary
Fixed broken or misleading Android button flows across auth, lesson, course detail, and profile screens, then advanced the learner flow by wiring exam availability to real backend course progress.

## Files Created
- app/src/main/java/com/baghdad/edulife/features/courses/model/CourseProgress.java
- app/src/main/java/com/baghdad/edulife/features/courses/model/CourseProgressUiState.java
- app/src/main/java/com/baghdad/edulife/features/courses/viewmodel/CourseProgressViewModel.java
- app/src/main/java/com/baghdad/edulife/features/profile/model/UpdateProfileRequest.java
- docs/2026-05-27-button-flows-and-progress-phase.md

## Files Modified
- app/src/main/java/com/baghdad/edulife/core/network/ApiService.java
- app/src/main/java/com/baghdad/edulife/features/auth/data/AuthRepository.java
- app/src/main/java/com/baghdad/edulife/features/auth/ui/LoginFragment.java
- app/src/main/java/com/baghdad/edulife/features/auth/ui/RegisterFragment.java
- app/src/main/java/com/baghdad/edulife/features/auth/viewmodel/AuthViewModel.java
- app/src/main/java/com/baghdad/edulife/features/courses/data/CourseRepository.java
- app/src/main/java/com/baghdad/edulife/features/courses/ui/CourseDetailFragment.java
- app/src/main/java/com/baghdad/edulife/features/courses/ui/LessonPlayerFragment.java
- app/src/main/java/com/baghdad/edulife/features/courses/ui/ProfileFragment.java
- app/src/main/java/com/baghdad/edulife/features/profile/data/ProfileRepository.java
- app/src/main/java/com/baghdad/edulife/features/profile/viewmodel/ProfileViewModel.java
- app/src/main/res/layout/fragment_lesson_player.xml
- app/src/main/res/layout/fragment_login.xml
- app/src/main/res/layout/fragment_profile.xml
- app/src/main/res/layout/fragment_register.xml
- app/src/main/res/values/strings.xml

## What Was Done
Replaced dead auth actions with real MVP behavior:
- hid unsupported Google sign-in and sign-up buttons because OAuth is outside the current MVP scope
- wired Forgot Password to Firebase password reset
- moved key auth validation and status labels into string resources

Fixed lesson and course button behavior:
- wired lesson resource open behavior to `contentUrl`
- added previous and next lesson navigation based on the backend course outline
- refreshed the lesson action label based on actual lesson content availability
- added backend progress loading to the course detail screen
- locked the Start Exam button until all lessons are complete, using `/api/v1/courses/{courseId}/progress`

Fixed profile action rows:
- hid Notifications because the EduLife MVP plan explicitly defers notifications
- wired Edit Profile to backend `PUT /api/v1/profile`
- wired Language, Privacy Policy, and About EduLife rows to actual informational dialogs
- cleaned up visible button and row text into string resources

## Architecture Compliance
The task follows the existing EduLife Android structure:
- API contract updates stay in `core/network/`
- course progress and lesson/certificate data access stay in feature data layers
- state handling stays in ViewModels
- screen-specific navigation and dialogs stay in UI fragments

The changes also respect the MVP plan by removing unsupported OAuth and notification actions from the visible flow instead of leaving misleading placeholders.

## Code Comments Added
Added comments for:
- why unsupported OAuth buttons are hidden in the MVP auth flow
- why the notifications row is hidden
- why exam availability must use backend progress instead of optimistic UI state
- why profile updates must go through the backend

These comments explain product and security reasons, not just code mechanics.

## Validation / Testing
Ran:
- `./gradlew.bat assembleDebug`

Result:
- `BUILD SUCCESSFUL`

Manual validation recommended:
- login with email/password
- request a password reset from the login screen
- open a course detail screen while enrolled and not fully complete
- complete the remaining lessons and confirm the exam button unlocks
- open lesson resource links and previous/next lesson navigation
- edit the profile name and bio from the profile screen

## Risks / Notes
Google OAuth is intentionally hidden, not implemented, because the project instructions define Firebase email/password as the active MVP auth path.

Lesson previous and next navigation is derived from the course outline payload, so if backend lesson ordering changes unexpectedly the navigation order will follow that backend contract.

Certificate download or PDF viewing is still not implemented because the current mobile/backend contract in this repo does not yet expose a certificate file endpoint.
