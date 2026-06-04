# Task Audit - Exam Status Guard

## Date
2026-05-31

## Task Summary
Added an Android-side preflight guard for the final exam screen so `ExamFragment` checks `GET /api/v1/courses/{courseId}/exam/status` before rendering questions. The screen now blocks learners who already passed and shows a readable cooldown message before the exam loads.

## Files Created
- app/src/main/java/com/baghdad/edulife/features/courses/model/ExamStatusResponse.java
- docs/2026-05-31-exam-status-guard.md

## Files Modified
- app/src/main/java/com/baghdad/edulife/core/network/ApiService.java
- app/src/main/java/com/baghdad/edulife/features/courses/data/ExamRepository.java
- app/src/main/java/com/baghdad/edulife/features/courses/model/ExamUiState.java
- app/src/main/java/com/baghdad/edulife/features/courses/viewmodel/ExamViewModel.java
- app/src/main/java/com/baghdad/edulife/features/courses/ui/ExamFragment.java

## What Was Done
Added a Retrofit contract and Android response model for the backend exam status endpoint.

Updated `ExamRepository` with a `getExamStatus` call that maps enrollment, missing-exam, server, and network failures into UI-safe messages.

Changed `ExamViewModel.loadExam()` to call exam status first. If the backend reports `passed`, the ViewModel publishes a blocked state instead of loading questions. If the backend reports `inCooldown`, the ViewModel publishes the cooldown end time and stops the question fetch. Only eligible learners proceed to `GET /exam`.

Extended `ExamUiState` so `ExamFragment` can render preflight blocked states without mixing that logic into submit-only state.

Updated `ExamFragment` to:
- hide the exam content when the learner already passed
- show a formatted cooldown message instead of a raw ISO timestamp
- avoid re-fetching on simple fragment recreation when the current state is already resolved
- clear prior radio selections before binding a newly loaded exam

## Architecture Compliance
The change stays inside the existing Android feature-first MVVM structure:
- API wiring remains in `core/network`
- exam API access remains in `features/courses/data`
- exam UI state remains in `features/courses/model`
- orchestration remains in `features/courses/viewmodel`
- rendering remains in `features/courses/ui`

No new architecture style or unnecessary abstraction was introduced.

## Code Comments Added
Added comments in:
- `ApiService` to explain why the status endpoint exists before loading questions
- `ExamViewModel` to document that backend status is the source of truth for pass and cooldown lockouts
- `ExamFragment` to explain why already-passed learners are blocked from the question screen and why cooldown formatting falls back safely

These comments cover non-obvious business rules and UI behavior tied to backend exam policy.

## Validation / Testing
Validated with Android compilation:
- `./gradlew.bat :app:compileDebugJavaWithJavac`

Manual testing still recommended:
- enrolled learner who has never taken the exam should see questions
- learner who already passed should see the blocked state immediately
- learner in cooldown should see the blocked state with the formatted retry time
- learner who is not enrolled should see the existing enrollment error

## Risks / Notes
`ExamResultFragment` still has its own cooldown date formatter, so the formatting logic is currently duplicated between the result and exam screens. That is acceptable for the small change, but a later cleanup could centralize exam timestamp formatting if more exam states reuse it.

The cooldown message currently shows the local formatted unlock date/time rather than a live ticking countdown. If the product later requires a true timer, that should be added explicitly in the fragment UI.
