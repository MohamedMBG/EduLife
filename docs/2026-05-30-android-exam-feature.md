# Android MCQ Exam Feature

## Goal

Implement the end-to-end MCQ exam flow for the EduLife Android app: load exam questions from the backend, allow learners to answer all questions, submit answers, and display a result screen with pass/fail status and certificate number.

## What Changed

- Added 7 model classes for exam domain (choice, question, exam response, result response, submit request, two UI states)
- Added `ExamRepository` with typed callbacks for load and submit, with specific handling for 403 and 429 status codes
- Added `ExamViewModel` with two separate `LiveData` streams: one for exam loading state, one for submit state
- Added `ExamFragment` with programmatic question rendering (RadioGroup per question), progress tracking, and submit-disabled-until-all-answered logic
- Added `ExamResultFragment` reading nav args only — no ViewModel needed — rendering score circle, pass/fail indicator, certificate card or retry message
- Added `fragment_exam.xml` and `fragment_exam_result.xml` layouts
- Added `bg_score_circle.xml` oval drawable whose tint is set programmatically to green (pass) or red (fail)
- Updated `ApiService.java` with `getExam` and `submitExam` endpoints
- Updated `nav_graph.xml` with `examFragment` (argument: courseId) and `examResultFragment` (arguments: courseId, examId, score, passScore, passed, certificateNumber), plus actions from courseDetailFragment→examFragment and examFragment→examResultFragment
- Updated `fragment_course_detail.xml` with a secondary "Take Final Exam" button below the enroll CTA button
- Updated `CourseDetailFragment.java` to wire the "Take Final Exam" button and navigate with courseId
- Updated `strings.xml` with all exam-related string resources
- Updated `colors.xml` with `exam_pass_green` and `exam_fail_red` aliases

## Files Touched

**Created:**
- `app/src/main/java/com/baghdad/edulife/features/courses/model/ExamChoice.java`
- `app/src/main/java/com/baghdad/edulife/features/courses/model/ExamQuestion.java`
- `app/src/main/java/com/baghdad/edulife/features/courses/model/ExamResponse.java`
- `app/src/main/java/com/baghdad/edulife/features/courses/model/ExamResultResponse.java`
- `app/src/main/java/com/baghdad/edulife/features/courses/model/SubmitExamRequest.java`
- `app/src/main/java/com/baghdad/edulife/features/courses/model/ExamUiState.java`
- `app/src/main/java/com/baghdad/edulife/features/courses/model/ExamSubmitUiState.java`
- `app/src/main/java/com/baghdad/edulife/features/courses/data/ExamRepository.java`
- `app/src/main/java/com/baghdad/edulife/features/courses/viewmodel/ExamViewModel.java`
- `app/src/main/java/com/baghdad/edulife/features/courses/ui/ExamFragment.java`
- `app/src/main/java/com/baghdad/edulife/features/courses/ui/ExamResultFragment.java`
- `app/src/main/res/layout/fragment_exam.xml`
- `app/src/main/res/layout/fragment_exam_result.xml`
- `app/src/main/res/drawable/bg_score_circle.xml`

**Modified:**
- `app/src/main/java/com/baghdad/edulife/core/network/ApiService.java`
- `app/src/main/res/navigation/nav_graph.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values/colors.xml`
- `app/src/main/res/layout/fragment_course_detail.xml`
- `app/src/main/java/com/baghdad/edulife/features/courses/ui/CourseDetailFragment.java`

## Backend Impact

Consumes two existing backend endpoints:
- `GET /api/v1/courses/{courseId}/exam` — returns exam structure with questions and choices (no correct answers)
- `POST /api/v1/courses/{courseId}/exam/submit` — accepts answers array, returns score, passed flag, and certificateNumber

No backend changes required.

## Android Impact

New navigation destinations added to nav graph. The exam flow is reachable from any course detail screen via "Take Final Exam" button. No existing fragment behavior changed — the enroll CTA button is unaffected.

## Web Impact

None. This is an Android-only change.

## Architecture Compliance

- Java only, no Kotlin
- Feature-first MVVM: model / data / viewmodel / ui layers maintained
- Fragment → ViewModel → Repository → ApiService → Backend flow followed exactly
- No business logic in Fragment classes
- No API calls in Fragment classes
- Correct answers are never sent to the client (backend contract enforced)
- Exam scoring happens server-side only
- Certificate generated server-side; Android only displays the returned certificate number
- Ownership / enrollment enforced by backend (403 handled gracefully in repository)
- Cooldown enforcement by backend (429 handled with clear user-facing message)

## Tests / Verification

Manual verification path:
1. Open any course detail screen — "Take Final Exam" button visible below "Enroll Now"
2. Tap "Take Final Exam" — ExamFragment loads, shows spinner, then renders questions
3. All questions must be answered before "Submit Exam" enables (progress counter updates live)
4. Submit navigates to ExamResultFragment with score circle (green = pass, red = fail)
5. Pass: certificate card shown with number; Fail: retry hint shown
6. "Back to Course" button pops back to course detail

Error scenarios tested by backend returning:
- 403 → "You must be enrolled to take this exam."
- 404 → "No exam found for this course."
- 429 on submit → "Too many failed attempts. Please wait 72 hours before trying again."
- Network failure → "Network error: ..."

## Risks / Notes

- `takeExamButton` is always visible to enrolled and non-enrolled users on the course detail screen. The backend enforces enrollment via 403 — the UI shows the error message on ExamFragment if the user is not enrolled.
- `ExamResultFragment` reads all data from nav args only; it requires no ViewModel and performs no network calls.
- The score circle tint is set programmatically via `setBackgroundTintList()` to override the drawable's default brand_primary color.
