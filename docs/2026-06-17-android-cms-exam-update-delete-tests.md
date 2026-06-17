# CMS Exam Update/Delete + Validation Tests

## Goal

Make existing CMS exams editable and deletable by teachers/admins. Add unit test infrastructure for CMS exam validation logic.

## What Changed

### Backend

- **Exam entity**: Added setters for `title`, `passScore`, `timeLimitMinutes` to support updates.
- **ExamQuestionRepository**: Added `deleteAllByExamId(UUID)` for bulk question cleanup.
- **ExamChoiceRepository**: Added `deleteAllByQuestionIdIn(List<UUID>)` with `@Modifying` JPQL for bulk choice cleanup.
- **CmsExamService**: Added `updateExam()` (atomic replace of questions/choices) and `deleteExam()` (cascade delete exam + questions + choices).
- **CmsExamController**: Added `PUT /api/v1/cms/courses/{courseId}/exam` and `DELETE /api/v1/cms/courses/{courseId}/exam`.
- **CmsExamServiceTest**: 13 unit tests covering owner/non-owner/admin/learner access for update and delete, validation errors, replacement verification, and 404-after-delete.

### Android

- **ApiService**: Added `updateCmsCourseExam()` (PUT) and `deleteCmsCourseExam()` (DELETE).
- **CmsExamRepository**: Added `UpdateExamCallback`, `DeleteExamCallback`, `updateCourseExam()`, `deleteCourseExam()` with proper error handling (400/401/403/404).
- **CmsExamBuilderViewModel**: Added edit mode state (`isEditMode`, `deleting`, `examDeleted`), `enterEditMode()` (populates drafts from existing exam), `cancelEditMode()`, `saveChanges()` (PUT), `deleteExam()` (DELETE). Validation delegated to extracted `CmsExamValidator`.
- **CmsExamValidator**: Pure Java validation class extracted from ViewModel. No Android dependencies — fully testable on host JVM.
- **CmsExamBuilderFragment**: Added Edit Exam / Delete Exam buttons on existing exam view. Edit mode shows pre-filled builder form with Save Changes + Cancel. Delete shows AlertDialog confirmation then navigates back on success.
- **Layout XML**: Added `existingExamActions` (Edit/Delete buttons), refactored bottom actions to support Cancel + Save Changes in edit mode.
- **Strings**: Added 8 new string resources for edit/delete UI.
- **CmsExamValidatorTest**: 18 unit tests covering all validation scenarios (title, passScore, timeLimit, questions, choices, correct answer count, boundary values).

## Files Touched

### Backend
- `backend/src/main/java/com/edulife/exams/entity/Exam.java`
- `backend/src/main/java/com/edulife/exams/repository/ExamQuestionRepository.java`
- `backend/src/main/java/com/edulife/exams/repository/ExamChoiceRepository.java`
- `backend/src/main/java/com/edulife/admin/controller/CmsExamController.java`
- `backend/src/main/java/com/edulife/admin/service/CmsExamService.java`
- `backend/src/test/java/com/edulife/admin/CmsExamServiceTest.java` (new)

### Android
- `app/src/main/java/com/baghdad/edulife/core/network/ApiService.java`
- `app/src/main/java/com/baghdad/edulife/features/exams/data/CmsExamRepository.java`
- `app/src/main/java/com/baghdad/edulife/features/exams/viewmodel/CmsExamBuilderViewModel.java`
- `app/src/main/java/com/baghdad/edulife/features/exams/ui/CmsExamBuilderFragment.java`
- `app/src/main/java/com/baghdad/edulife/features/exams/validation/CmsExamValidator.java` (new)
- `app/src/main/res/layout/fragment_cms_exam_builder.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/test/java/com/baghdad/edulife/features/exams/CmsExamValidatorTest.java` (new)

## Backend Impact

Two new endpoints. No schema changes. No Flyway migration needed — entity setters only affect in-memory state before JPA persists.

## Android Impact

Existing exam view now has Edit and Delete actions. Create flow unchanged. Learner exam flow untouched.

## Web Impact

None. Web can consume the new PUT/DELETE endpoints when ready.

## Architecture Compliance

- Business logic in service layer.
- Controller stays thin.
- DTOs used for API I/O.
- Ownership checks enforced server-side.
- Learner DTOs never expose correct answers.
- Validation extracted to pure testable class.

## Tests / Verification

- `./mvnw test -Dtest=com.edulife.admin.CmsExamServiceTest` — 13/13 pass
- `./gradlew testDebugUnitTest` — all pass (including 18 CmsExamValidatorTest)
- `./gradlew assembleDebug` — BUILD SUCCESSFUL

## Risks / Notes

- Update uses "replace" strategy (delete old questions/choices, insert new). If learners are mid-exam during an update, their in-progress answers reference old question/choice IDs. This is an edge case — CMS editors should avoid updating exams while learners are actively taking them. A future mitigation could lock exams during active attempts.
- GROUP_ADMIN ownership check currently only verifies course owner or admin. The `loadCourseForMutation` helper does not yet check group membership for GROUP_ADMIN role — same limitation as the existing create endpoint. A follow-up task could add group-based authorization to the CMS exam service.
- Delete cascades through questions and choices but does not delete exam attempts. Historical attempt data is preserved for analytics.
