# Task Audit - Android Exam Status Precheck

## Date
2026-05-31

## Task Summary
Implemented the Android-side exam status pre-check so the app calls `GET /api/v1/courses/{courseId}/exam/status` before loading exam questions. Users who already passed or are still in cooldown now see a locked state immediately instead of reaching a submit-time dead end.

## Files Created
- app/src/main/java/com/baghdad/edulife/features/courses/model/ExamStatusResponse.java
- app/src/main/java/com/baghdad/edulife/features/courses/model/ExamStatusUiState.java
- docs/2026-05-31-android-exam-status-precheck.md

## Files Modified
- app/src/main/java/com/baghdad/edulife/core/network/ApiService.java
- app/src/main/java/com/baghdad/edulife/features/courses/data/ExamRepository.java
- app/src/main/java/com/baghdad/edulife/features/courses/ui/ExamFragment.java
- app/src/main/java/com/baghdad/edulife/features/courses/viewmodel/ExamViewModel.java
- app/src/main/res/values/strings.xml

## What Was Done
Added an Android model for the backend `ExamStatusDto` contract and a dedicated `ExamStatusUiState` so the exam status gate is tracked separately from question loading and answer submission.

Extended Retrofit and the repository with a `getExamStatus(courseId)` call. The repository now maps common failure cases such as not enrolled and exam missing into controlled UI messages.

Updated `ExamViewModel` to expose `examStatusState` and load exam availability before question loading. This keeps the pre-check isolated from submit-time state and avoids mixing separate business rules into one LiveData stream.

Changed `ExamFragment` so it observes the status state first, shows a loading gate while availability is checked, blocks the full exam UI for already-passed and cooldown users, and only calls `loadExam(courseId)` when the learner is eligible to continue.

Improved cooldown messaging in `ExamFragment` by formatting ISO timestamps into local date/time text instead of showing raw backend values.

## Architecture Compliance
The change stays inside the existing Android feature-first MVVM structure:
- network contract in `core/network`
- data access in `features/courses/data`
- screen state in `features/courses/viewmodel` and `features/courses/model`
- UI rendering in `features/courses/ui`

No new architecture style or cross-feature coupling was introduced.

## Code Comments Added
Added comments in `ExamViewModel` and `ExamFragment` to explain why exam status is tracked separately from question loading and why the status check must happen before rendering answer inputs.

These comments cover non-obvious business rules from the MVP exam policy and avoid future regressions back to submit-time-only validation.

## Validation / Testing
Ran `./gradlew.bat :app:compileDebugJavaWithJavac` successfully.

Manual QA still recommended for:
- passed learner opens exam and sees locked state immediately
- learner in cooldown opens exam and sees the formatted cooldown end time
- eligible learner opens exam and still reaches the normal question UI
- not-enrolled learner still gets the correct blocked message

## Risks / Notes
The current locked UI uses the existing `examStatusText` area instead of a richer dedicated card. This is sufficient for the bug fix, but the future `M2` polish can still replace it with a more explicit status screen if needed.

Submit-time guards remain in place intentionally. The pre-check improves UX, but the backend must still enforce pass and cooldown rules as the source of truth.
