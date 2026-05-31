# Task Audit - Android Course Detail Checkmarks

## Date
2026-05-31

## Task Summary
Added lesson completion checkmarks to the enrolled course detail screen. Android now uses the existing backend course-progress endpoint to mark completed lessons inside the course outline and refreshes those indicators when the learner returns from the lesson player.

## Files Created
- docs/2026-05-31-android-course-detail-checkmarks.md

## Files Modified
- app/src/main/java/com/baghdad/edulife/features/courses/model/CourseProgressSummary.java
- app/src/main/java/com/baghdad/edulife/features/courses/viewmodel/CourseDetailViewModel.java
- app/src/main/java/com/baghdad/edulife/features/courses/ui/CourseDetailFragment.java
- app/src/main/res/values/strings.xml

## What Was Done
Expanded the Android course-progress model so it can read the nested `sections` and `lessons` data already returned by the backend progress endpoint.

Extended `CourseDetailViewModel` with a lesson-completion state derived from course progress. The ViewModel now flattens the nested response into a `lessonId -> completed` map so the fragment does not need to parse backend DTO shape directly.

Updated `CourseDetailFragment` to:
- observe the lesson completion map
- refresh completion state on resume for enrolled users
- rebuild lesson rows with a completed indicator when a lesson is marked done

The lesson row now shows:
- `✓ Completed` for completed enrolled lessons
- `Open lesson` for incomplete enrolled lessons
- the existing preview or locked labels for non-enrolled users

## Architecture Compliance
The API response handling remains in the shared courses model, the state logic stays in `features/courses/viewmodel`, and the UI rendering remains in `features/courses/ui`.

No network code was pushed into the fragment and no unrelated architecture changes were introduced.

## Code Comments Added
Added comments in the ViewModel explaining why lesson completion is flattened into a simple map for UI consumption.

Added a fragment comment explaining why completion state is refreshed on resume after returning from the lesson player, so future changes do not remove that progress-sync behavior accidentally.

Added a UI comment explaining why completed lessons need a visual marker even though enrolled lessons remain accessible.

## Validation / Testing
Ran `./gradlew.bat :app:compileDebugJavaWithJavac` successfully.

Manual QA recommended for:
- opening an enrolled course with existing completed lessons and checking the markers
- completing a lesson in the lesson player and returning to course detail
- confirming non-enrolled users still see preview/locked behavior only
- confirming exam CTA and enroll CTA behavior remain unchanged

## Risks / Notes
The current implementation rebuilds the course detail section list when lesson completion state changes. This is acceptable for current MVP screen size, but a more incremental update path would be better if course outlines become much larger later.

If the progress endpoint fails, the course detail still loads normally and simply omits completion markers. This is intentional so progress UX cannot block lesson access.
