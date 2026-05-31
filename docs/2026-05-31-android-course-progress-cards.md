# Task Audit - Android Course Progress Cards

## Date
2026-05-31

## Task Summary
Added per-course progress summaries to the enrolled course cards in Android. The My Courses screen now calls `GET /api/v1/progress/courses/{courseId}` for each enrolled course and renders `X / Y lessons · Z%` on the card.

## Files Created
- app/src/main/java/com/baghdad/edulife/features/courses/model/CourseProgressSummary.java
- docs/2026-05-31-android-course-progress-cards.md

## Files Modified
- app/src/main/java/com/baghdad/edulife/core/network/ApiService.java
- app/src/main/java/com/baghdad/edulife/features/courses/data/CourseRepository.java
- app/src/main/java/com/baghdad/edulife/features/courses/viewmodel/EnrollmentViewModel.java
- app/src/main/java/com/baghdad/edulife/features/courses/ui/CoursesFragment.java
- app/src/main/res/layout/item_enrolled_course.xml
- app/src/main/res/values/strings.xml

## What Was Done
Added an Android progress model that matches the top-level fields returned by the backend course-progress endpoint: `courseId`, `completedLessons`, `totalLessons`, and `percentComplete`.

Extended Retrofit and `CourseRepository` with a `getCourseProgress(courseId)` call and callback so the feature uses the real backend progress endpoint instead of mocked values.

Updated `EnrollmentViewModel` to fetch progress after enrollments load and expose two separate pieces of UI state:
- a map of successful `courseId -> progress`
- a set of course IDs whose progress calls failed

This lets each enrolled card resolve independently, so one failed progress request does not blank the entire My Courses screen.

Updated `CoursesFragment` and its adapter to observe that progress state and render one of three card states:
- actual progress text when available
- `Loading progress…` while a course progress request is still pending
- `Progress unavailable` when that individual course progress request fails

Added a dedicated progress text row to the enrolled course card layout.

## Architecture Compliance
The change respects the existing feature-first Android structure:
- network endpoint in `core/network`
- backend access logic in `features/courses/data`
- screen state in `features/courses/viewmodel`
- UI rendering in `features/courses/ui`
- presentation model in `features/courses/model`

No unrelated folders or alternate architecture layers were introduced.

## Code Comments Added
Added a ViewModel comment explaining why progress is fetched per course instead of treating the entire list as one atomic request. This documents the UX and resilience decision behind the implementation.

## Validation / Testing
Ran `./gradlew.bat :app:compileDebugJavaWithJavac` successfully.

Manual QA recommended for:
- enrolled course list shows progress after loading
- progress updates after completing a lesson and returning to My Courses
- one course progress call failing does not break other cards
- empty-state and filter behavior still work correctly

## Risks / Notes
This implementation issues one progress request per enrolled course, as explicitly requested. That is fine for current MVP scale, but a batched endpoint would be more efficient if enrollments grow significantly later.

The current adapter uses `notifyDataSetChanged()` on progress updates for simplicity. If the list becomes much larger later, this should move to a diff-based update path.
