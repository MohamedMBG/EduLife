# Task Audit - Course Catalog UI

## Date
2026-05-15

## Task Summary
Implemented the first practical Android UI slice that exposes real backend progress: authenticated course catalog discovery, level filtering, and course detail rendering from the live Spring Boot backend.

## Files Created
- app/src/main/java/com/baghdad/edulife/features/courses/data/CourseRepository.java
- app/src/main/java/com/baghdad/edulife/features/courses/model/CourseCatalogUiState.java
- app/src/main/java/com/baghdad/edulife/features/courses/model/CourseDetail.java
- app/src/main/java/com/baghdad/edulife/features/courses/model/CourseDetailUiState.java
- app/src/main/java/com/baghdad/edulife/features/courses/model/CoursePageResponse.java
- app/src/main/java/com/baghdad/edulife/features/courses/model/CourseSection.java
- app/src/main/java/com/baghdad/edulife/features/courses/model/CourseSummary.java
- app/src/main/java/com/baghdad/edulife/features/courses/model/LessonSummary.java
- app/src/main/java/com/baghdad/edulife/features/courses/ui/CourseCatalogAdapter.java
- app/src/main/java/com/baghdad/edulife/features/courses/ui/CourseDetailFragment.java
- app/src/main/java/com/baghdad/edulife/features/courses/viewmodel/CourseCatalogViewModel.java
- app/src/main/java/com/baghdad/edulife/features/courses/viewmodel/CourseDetailViewModel.java
- app/src/main/res/drawable/bg_catalog_badge.xml
- app/src/main/res/drawable/bg_catalog_card.xml
- app/src/main/res/drawable/bg_catalog_filter_button.xml
- app/src/main/res/drawable/bg_catalog_filter_button_active.xml
- app/src/main/res/drawable/bg_catalog_lesson_row.xml
- app/src/main/res/drawable/bg_catalog_logout_button.xml
- app/src/main/res/drawable/bg_catalog_panel.xml
- app/src/main/res/layout/fragment_course_detail.xml
- app/src/main/res/layout/item_course_summary.xml
- docs/2026-05-15-course-catalog-ui.md

## Files Modified
- app/build.gradle.kts
- app/src/main/java/com/baghdad/edulife/MainActivity.java
- app/src/main/java/com/baghdad/edulife/core/network/ApiService.java
- app/src/main/java/com/baghdad/edulife/features/auth/ui/LoginFragment.java
- app/src/main/java/com/baghdad/edulife/features/courses/ui/HomeFragment.java
- app/src/main/res/layout/fragment_home.xml
- app/src/main/res/navigation/nav_graph.xml
- app/src/main/res/values/colors.xml
- app/src/main/res/values/strings.xml

## What Was Done
Added live Retrofit endpoints for `GET /api/v1/courses` and `GET /api/v1/courses/{courseId}` so Android can consume the real Sprint 2 backend contracts.

Created course feature models, repository calls, and dedicated ViewModels to keep API access, UI state, and screen logic separated inside the `features/courses` MVVM structure.

Replaced the old authenticated stub home screen with a course catalog screen that:
- validates the synced Firebase plus backend session
- shows the internal synced role and user ID
- loads published courses from the real backend
- supports `All`, `Beginner`, and `Intermediate` filters using the backend `category` query
- handles loading, empty, error, retry, and logout states

Added a course detail screen that renders:
- title
- descriptions
- level and language metadata
- section ordering
- lesson summaries
- preview versus locked lesson state

Updated app launch routing and login navigation so authenticated users land in the live course catalog instead of the previous placeholder home card.

Added lightweight catalog-specific layouts and drawables to present the new vertical slice cleanly without introducing unrelated feature work such as enrollment or lesson playback.

## Architecture Compliance
The implementation respects the EduLife feature-first Android MVVM structure:
- UI logic is in `features/courses/ui`
- state logic is in `features/courses/viewmodel`
- API and data access is in `features/courses/data`
- backend DTO mapping models are in `features/courses/model`
- shared API contracts remain in `core/network`

The task also respects sprint discipline by exposing only the currently implemented backend modules: auth sync and course discovery. It does not introduce enrollment, progress, exams, certificates, or CMS work ahead of schedule.

## Code Comments Added
Comments were added in the new course screens and ViewModels to explain:
- why the catalog filter maps to the current backend `category` contract
- why paging is intentionally fixed to page 0 for the first real integration slice
- why session identity is shown on the catalog screen
- why preview versus locked lessons are surfaced before enrollment exists

These comments focus on product and architecture intent rather than restating obvious code.

## Validation / Testing
Ran `./gradlew.bat assembleDebug` from the project root.

Build result:
- `BUILD SUCCESSFUL`

This verifies that the new navigation, layouts, Java classes, and resources compile together in the Android app.

## Risks / Notes
The catalog currently loads only page 0. This is intentional for the first real Sprint 2 UI slice, but paging or endless scroll will be needed if seeded data grows.

The backend `category` query currently maps to course `level`. The UI names this as a learner-facing level filter, but the contract should be revisited once a dedicated course category model is introduced.

Course detail currently shows lesson preview and locked states only. It does not open lesson content yet because lesson access and enrollment APIs are not implemented.
