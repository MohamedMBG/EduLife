# Phase 3 — Progress UI

## Goal
Wire lesson completion state into the UI: per-lesson checkmarks in CourseDetailFragment and progress bars on enrolled course cards in HomeFragment.

## What Changed

- **New model** `CourseProgressResponse.java` — mirrors backend `CourseProgressDto` (courseId, completedLessons, totalLessons, percentComplete, sections → lessons with completed flag)
- **ApiService** — added `GET progress/courses/{courseId}` endpoint
- **CourseRepository** — added `CourseProgressCallback` + `getCourseProgress()` method
- **CourseDetailViewModel** — added `progressLiveData` + `loadProgress(courseId)` (guarded against re-fetch)
- **CourseDetailFragment** — observes `progressLiveData`; builds `completedLessonIds` Set; lesson rows show "✓ Completed" badge when completed; progress summary bar + "X of Y lessons completed" shown above sections when enrolled
- **fragment_course_detail.xml** — added `progressSummaryLayout` with ProgressBar + TextView between description and sectionContainer
- **EnrollmentViewModel** — after `loadMyEnrollments()` succeeds, fires `getCourseProgress()` for each enrolled course; exposes `progressMap: LiveData<Map<String, Integer>>` (courseId → percent)
- **CourseCatalogAdapter** — added `progressMap` field + `updateProgressMap()`; `bind()` shows/hides `courseProgressLayout` with ProgressBar + % text
- **item_course_summary.xml** — added `courseProgressLayout` (ProgressBar + percent TextView) at bottom of card overlay, hidden by default
- **HomeFragment** — observes `enrollmentViewModel.getProgressMap()`, calls `courseCatalogAdapter.updateProgressMap()`
- **strings.xml** — added `progress_lessons_summary`, `progress_percent`, `progress_lesson_completed`

## Files Touched

- `app/src/main/java/com/baghdad/edulife/features/courses/model/CourseProgressResponse.java` (new)
- `app/src/main/java/com/baghdad/edulife/core/network/ApiService.java`
- `app/src/main/java/com/baghdad/edulife/features/courses/data/CourseRepository.java`
- `app/src/main/java/com/baghdad/edulife/features/courses/viewmodel/CourseDetailViewModel.java`
- `app/src/main/java/com/baghdad/edulife/features/courses/ui/CourseDetailFragment.java`
- `app/src/main/java/com/baghdad/edulife/features/courses/viewmodel/EnrollmentViewModel.java`
- `app/src/main/java/com/baghdad/edulife/features/courses/ui/CourseCatalogAdapter.java`
- `app/src/main/java/com/baghdad/edulife/features/courses/ui/HomeFragment.java`
- `app/src/main/res/layout/fragment_course_detail.xml`
- `app/src/main/res/layout/item_course_summary.xml`
- `app/src/main/res/values/strings.xml`

## Backend Impact

None. Consumes existing `GET /api/v1/progress/courses/{courseId}` endpoint.

## Android Impact

- CourseDetailFragment: enrolled learners see "✓ Completed" badge on finished lessons + progress bar above sections
- HomeFragment: enrolled course cards show a thin progress bar + % label in the card overlay

## Web Impact

None.

## Architecture Compliance

- No business logic in Fragment — progress state computed in ViewModel/Repository
- Silent failure on progress fetch — progress UI is additive, not blocking
- `loadProgress()` guarded against re-fetch if value already loaded

## Tests / Verification

- Enroll in course → open detail → progress bar visible (0% initially if no lessons done)
- Complete a lesson → return to detail → checkmark visible on that lesson
- HomeFragment cards for enrolled courses show progress bar with correct %

## Risks / Notes

- Progress for HomeFragment uses N+1 calls (one per enrolled course). Acceptable for MVP — learners typically have 1–5 enrollments. A batch endpoint can replace this post-MVP.
- `progressMap` in `EnrollmentViewModel` updates reactively as each call returns, so the home screen updates card-by-card as responses arrive.
