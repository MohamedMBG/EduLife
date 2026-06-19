# Android Course Learning — Progress Card + Lesson Type Icons

## Goal

Improve the enrolled-course learning experience: surface overall progress on the
course detail/learning screen and make each lesson's content type visually clear.

## What Changed

Audit of the existing Android lesson/course flow showed it already implements the
bulk of the requested spec:

- `LessonContentTypeResolver` (unit-tested) maps VIDEO / ARTICLE / TEXT / PDF /
  RESOURCE / FALLBACK and infers from URL/body when type is missing.
- `LessonPlayerFragment` renders each content type, handles loading/error/fallback,
  mark-complete (deduped, backend-confirmed only), next-lesson nav, secure WebView
  + URL policy, authenticated PDF download via FileProvider.
- `CourseDetailFragment` lists sections/lessons with completion, lock, and preview
  states, refreshing completion markers on resume.

Two real gaps were found and fixed:

1. **Dead progress card.** `progressSummaryLayout` / `progressSummaryText` /
   `progressSummaryBar` existed in `fragment_course_detail.xml` and were wired in
   the fragment, but `bindCourseDetail` never populated or showed them.
   `CourseDetailViewModel` discarded `completedLessons` / `totalLessons` /
   `percentComplete` from the progress response, keeping only a flattened
   lessonId→completed map. The aggregate is now exposed and bound: enrolled
   learners see "X of Y lessons completed · Z% complete" with a percent bar that
   refreshes on resume (after a lesson is marked complete).

2. **Ambiguous lesson type icon.** Lesson rows showed only VIDEO vs a generic
   document icon. Added a per-type icon mapping (VIDEO→play, ARTICLE/LINK→book,
   TEXT→notes, PDF/RESOURCE→document, unknown→document fallback).

Follow-ups (former TODOs, now resolved):

3. **Progress card no longer hides on fetch failure.** `CourseDetailViewModel`
   exposes a `progressError` signal; the card now shows a tap-to-retry message on
   failure instead of vanishing.
4. **Final-exam CTA gated on completion.** The CTA is locked ("Complete all lessons
   to unlock the exam") only when progress is *positively known* incomplete; when
   progress is unknown or failed to load the CTA stays enabled so a flaky progress
   endpoint can't trap an eligible learner. Backend remains authoritative.
5. **Type-name duplication removed.** `LessonContentTypeResolver.classifyKind`
   (new `Kind` enum) is the single source of accepted type spellings; the row-icon
   mapping consumes the enum instead of re-listing strings.

## Files Touched

- `app/.../courses/viewmodel/CourseDetailViewModel.java` — expose
  `CourseProgressSummary` via new `getProgressSummary()` LiveData; post it on
  progress load (null on error/not-enrolled).
- `app/.../courses/ui/CourseDetailFragment.java` — observe + `bindProgressSummary`;
  `lessonTypeIcon` helper for row icons.

## Backend Impact

None. No new endpoints, no contract changes. Reuses the existing
`GET /api/v1/progress/courses/{courseId}` (`CourseProgressSummary`).

## Android Impact

Progress card now functional; clearer content-type icons. No nav graph, model
field, or API changes. Existing strings reused (`progress_lessons_summary`,
`progress_percent`).

## Web Impact

None.

## Architecture Compliance

Feature-first MVVM preserved: Fragment → ViewModel → Repository → ApiService.
No API calls in the fragment/adapter, no business logic in XML. Java/XML only.
No Compose, no new DI, no architecture change.

## Tests / Verification

- `./gradlew :app:compileDebugJavaWithJavac :app:testDebugUnitTest` — passed.
- `LessonContentTypeResolverTest` (existing) still green; type-mapping logic
  unchanged.

## Risks / Notes

- Exam gating is intentionally lenient on unknown/failed progress (stays enabled)
  to avoid locking out eligible learners; the backend is the real gate.
- Adding a new lesson content type now only requires updating
  `LessonContentTypeResolver` (`classifyKind` + `resolve`); the fragment maps the
  resulting `Kind`, so type-name strings are no longer duplicated.
