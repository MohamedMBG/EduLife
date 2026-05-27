# Task Audit - Mobile Backend Alignment

## Date
2026-05-26

## Task Summary
Aligned the Android client more closely with the live backend by removing seeded catalog fallback behavior, routing lesson loading through backend lesson-detail contracts, and moving lesson progress API calls into the feature data layer.

## Files Created
- app/src/main/java/com/baghdad/edulife/features/courses/model/LessonDetail.java
- app/src/main/java/com/baghdad/edulife/features/courses/model/LessonDetailUiState.java
- app/src/main/java/com/baghdad/edulife/features/courses/viewmodel/LessonDetailViewModel.java
- docs/2026-05-26-mobile-backend-alignment.md

## Files Modified
- app/src/main/java/com/baghdad/edulife/core/network/ApiService.java
- app/src/main/java/com/baghdad/edulife/features/courses/data/CourseRepository.java
- app/src/main/java/com/baghdad/edulife/features/courses/viewmodel/CourseCatalogViewModel.java
- app/src/main/java/com/baghdad/edulife/features/courses/ui/CourseDetailFragment.java
- app/src/main/java/com/baghdad/edulife/features/courses/ui/LessonPlayerFragment.java

## What Was Done
Added `GET /api/v1/courses/{courseId}/lessons/{lessonId}` to the Android Retrofit contract and introduced a `LessonDetail` model so the mobile lesson screen now consumes the backend lesson payload directly.

Created `LessonDetailViewModel` and `LessonDetailUiState` so lesson loading and lesson-completion actions follow the app's MVVM pattern instead of calling Retrofit directly from the fragment UI.

Updated `LessonPlayerFragment` to reload canonical backend lesson data from IDs passed by navigation, render the backend section/title/content fields, and respect backend `preview` and `completed` flags for progress UI.

Removed the seeded course fallback path from `CourseRepository` and `CourseCatalogViewModel` so the course catalog now reflects real backend states: loading, error, empty, and success.

Updated `CourseDetailFragment` to pass only stable IDs into lesson navigation, which avoids stale lesson data being shown when backend state changes.

## Architecture Compliance
The changes stay within the Android client layers defined in the project architecture:
- Shared API contract changes remain in `core/network/`
- Course and lesson data access remains in `features/courses/data/`
- Lesson state handling is in `features/courses/viewmodel/`
- Lesson rendering remains in `features/courses/ui/`

This keeps business and networking concerns out of fragment UI code and moves the mobile app closer to the backend-first learner flow required by the EduLife MVP plan.

## Code Comments Added
Added comments in:
- `ApiService.java` to explain why lesson detail should come from the backend instead of navigation placeholders
- `CourseDetailFragment.java` to explain why only IDs are passed into lesson navigation
- `LessonPlayerFragment.java` to explain preview/progress rules and why missing IDs should stop fake UI state
- `CourseCatalogViewModel.java` to explain why backend failures are now surfaced directly

These comments document non-obvious contract and learner-flow decisions.

## Validation / Testing
Ran Android build verification:
- `./gradlew.bat assembleDebug`
- Result: `BUILD SUCCESSFUL`

Manual validation recommended:
- Open Home and verify backend course loading, empty states, and retry behavior
- Open a course detail, then open a lesson and confirm lesson data loads from the backend
- Mark a non-preview lesson complete and verify completion is persisted
- Open a preview lesson and verify the completion CTA stays hidden

## Risks / Notes
The lesson screen still uses simple placeholder playback UI because actual media playback and resource rendering are not implemented yet.

This task improved alignment for course discovery and lesson access, but additional contract alignment may still be needed later for profile editing, certificate download flows, and richer progress visualization if those backend endpoints evolve.
