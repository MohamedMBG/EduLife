# Android Teacher CMS Module

## Goal

Implement the `features/teacher/` module so authenticated TEACHER-role users are routed to their own CMS dashboard instead of falling through to the learner home screen.

## What Changed

### New files

**Models** (`features/teacher/model/`):
- `CmsCourse.java` — matches `CourseAdminDto` shape
- `CmsSection.java` — matches `SectionAdminDto` shape
- `CmsLesson.java` — matches `LessonAdminDto` shape
- `CreateCourseRequest.java` — POST body
- `CreateSectionRequest.java` — POST body
- `CreateLessonRequest.java` — POST body
- `TeacherDashboardUiState.java` — loading / success / error states
- `CmsCourseDetailUiState.java` — loading / success / error / actionMessage states

**Data** (`features/teacher/data/`):
- `TeacherRepository.java` — wraps all CMS Retrofit calls with typed callback interfaces

**ViewModels** (`features/teacher/viewmodel/`):
- `TeacherDashboardViewModel.java` — loadCourses, createCourse
- `CmsCourseDetailViewModel.java` — loadSections, createSection, deleteSection, createLesson, deleteLesson

**UI** (`features/teacher/ui/`):
- `TeacherCourseAdapter.java` — ListAdapter for CmsCourse items with status-colour logic
- `CmsSectionAdapter.java` — ListAdapter for CmsSection items with long-press/icon delete
- `TeacherDashboardFragment.java` — welcome header, RecyclerView, FAB, AlertDialog create-course flow
- `CmsCourseDetailFragment.java` — toolbar with back, sections list, add-section dialog, delete confirm

**Layouts** (`res/layout/`):
- `fragment_teacher_dashboard.xml`
- `fragment_cms_course_detail.xml`
- `item_cms_course.xml`
- `item_cms_section.xml`

**Drawables** (`res/drawable/`):
- `bg_teacher_course_card.xml`
- `bg_teacher_status_draft.xml`
- `bg_teacher_status_published.xml`
- `bg_teacher_status_archived.xml`
- `bg_teacher_fab.xml`
- `bg_teacher_add_section_button.xml`

### Modified files

- `core/network/ApiService.java` — added 8 CMS endpoint methods + 6 import lines
- `features/auth/ui/LoginFragment.java` — added TEACHER branch in `renderAuthState()`
- `res/navigation/nav_graph.xml` — added `teacherDashboardFragment` + `cmsCourseDetailFragment` + `action_loginFragment_to_teacherDashboardFragment`
- `res/values/colors.xml` — added 9 `teacher_*` color aliases
- `res/values/strings.xml` — added 17 `teacher_*` string resources

## Files Touched

See "What Changed" above — 21 new files, 5 modified files.

## Backend Impact

None. All CMS endpoints already exist in the backend. The Android module is a pure consumer.

## Android Impact

- TEACHER-role users now route to `TeacherDashboardFragment` after login
- LEARNER and all other roles are unaffected
- No changes to existing fragments or ViewModels

## Web Impact

None.

## Architecture Compliance

- Feature-first MVVM under `features/teacher/`
- Java only, no Kotlin
- No Hilt — `ApiClient.getClient().create(ApiService.class)` in repository constructor
- Repository callbacks are single-method interfaces (not multi-arg lambdas)
- No API calls inside Fragments
- No business logic in UI classes
- Navigation uses Bundle args (no SafeArgs) consistent with the rest of the project

## Tests / Verification

- Verify login as a TEACHER-role account → lands on `TeacherDashboardFragment`
- Verify FAB opens create-course dialog, fills title/lang → calls `POST /api/v1/cms/courses`
- Verify course item tap navigates to `CmsCourseDetailFragment`
- Verify "Add Section" opens dialog, fills title → calls `POST /api/v1/cms/courses/{courseId}/sections`
- Verify delete icon (or long press) on a section shows confirm dialog → calls `DELETE /api/v1/cms/courses/{courseId}/sections/{id}`

## Risks / Notes

- `SessionStorage` does not persist `displayName`; the welcome header currently shows "Teacher" as a placeholder. Wiring the actual name requires either a profile fetch on load or extending `SessionStorage.saveSession()` with a `displayName` param — deferred as a follow-up.
- `CmsCourseDetailUiState.success(null, sections)` passes `null` for the course object because sections are loaded independently. The detail screen relies on nav args for the title, not this field. If a future screen needs the full `CmsCourse` object, a combined load method should be added to the repository.
