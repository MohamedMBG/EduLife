# Android CMS Exam Builder

## Goal

Add an Android screen for TEACHER, GROUP_ADMIN, and ADMIN users to create and view a final MCQ exam for a course, consuming the existing backend CMS exam endpoints.

## What Changed

### New Files

| File | Purpose |
|------|---------|
| `features/exams/model/CmsExamRequest.java` | Request DTO for POST exam |
| `features/exams/model/CmsExamQuestionRequest.java` | Nested question request |
| `features/exams/model/CmsExamChoiceRequest.java` | Nested choice request |
| `features/exams/model/CmsExamResponse.java` | Response DTO from GET/POST exam |
| `features/exams/model/CmsExamQuestion.java` | Nested question response |
| `features/exams/model/CmsExamChoice.java` | Nested choice response (includes `correct` flag for CMS) |
| `features/exams/data/CmsExamRepository.java` | Repository with callback-based error handling |
| `features/exams/viewmodel/CmsExamBuilderViewModel.java` | ViewModel with draft management and validation |
| `features/exams/ui/CmsExamQuestionAdapter.java` | RecyclerView adapter for question cards |
| `features/exams/ui/CmsExamBuilderFragment.java` | Fragment with create/view modes |
| `res/layout/fragment_cms_exam_builder.xml` | Builder screen layout |
| `res/layout/item_cms_exam_question.xml` | Question card layout |
| `res/drawable/bg_cms_exam_save_button.xml` | Filled primary button drawable |

### Modified Files

| File | Change |
|------|--------|
| `core/network/ApiService.java` | Added `getCmsCourseExam()` and `createCmsCourseExam()` |
| `res/navigation/nav_graph.xml` | Added `cmsExamBuilderFragment` destination + action from course detail |
| `res/layout/fragment_cms_course_detail.xml` | Added "Final Exam" button |
| `features/teacher/ui/CmsCourseDetailFragment.java` | Wired exam button navigation |
| `res/values/strings.xml` | Added CMS exam builder string resources |

## Backend Endpoints Used

| Method | Path | Status Codes |
|--------|------|-------------|
| GET | `/api/v1/cms/courses/{courseId}/exam` | 200, 401, 403, 404 |
| POST | `/api/v1/cms/courses/{courseId}/exam` | 201, 400, 401, 403, 409 |

## Roles Allowed

- TEACHER (must own course for POST)
- GROUP_ADMIN (read access; POST returns 403 unless course owner)
- ADMIN (full access)

Backend enforces via `@PreAuthorize("hasAnyRole('TEACHER','GROUP_ADMIN','ADMIN')")` + service-layer ownership check.

## Payload Shape

```json
{
  "title": "Final Exam",
  "passScore": 80,
  "timeLimitMinutes": 30,
  "questions": [
    {
      "questionText": "What is X?",
      "orderIndex": 1,
      "choices": [
        { "choiceText": "Option A", "correct": true },
        { "choiceText": "Option B", "correct": false }
      ]
    }
  ]
}
```

## Validation Rules

- Title: required, max 200 chars
- Pass score: 1-100
- Time limit: >= 1 minute (optional)
- At least 1 question
- Each question: non-empty text
- Each question: >= 2 choices
- Each choice: non-empty text
- Exactly 1 correct choice per question

## Learner Correct-Answer Safety

- CMS exam models (`CmsExamResponse`, `CmsExamChoice`) are in `features/exams/model/`, separate from learner models in `features/courses/model/`
- Learner `ExamChoice.java` has no `correct` field
- CMS exam builder is only reachable from teacher CMS navigation, not learner flows
- Backend CMS endpoints require TEACHER/GROUP_ADMIN/ADMIN role

## Architecture Compliance

- Java + XML layouts
- MVVM: Fragment → ViewModel → Repository → ApiService → Backend
- Follows existing patterns from TeacherRepository, CmsCourseDetailFragment
- No business logic in Fragment
- DTOs for API input/output
- Entities never exposed
- Auth token attached via existing FirebaseAuthInterceptor

## Tests / Verification

- `./gradlew assembleDebug` — BUILD SUCCESSFUL
- Validation logic in ViewModel covers all edge cases

### Manual Verification Steps

1. Login as teacher
2. Open owned course from teacher dashboard
3. Tap "Final Exam" button
4. Confirm builder form appears (GET returns 404)
5. Fill title, pass score, time limit
6. Add questions with choices, select correct answers
7. Save exam — confirm POST succeeds with toast
8. Reopen screen — confirm existing exam displayed with correct answers visible
9. Attempt duplicate creation — confirm 409 handled
10. Login as learner — confirm builder not accessible (no navigation path)
11. Confirm learner exam screens never expose `correct` field

## Risks / Notes

- No update/delete exam endpoint exists on backend; existing exam view is read-only
- GROUP_ADMIN can view but not create exams (403 on POST handled gracefully)
- No unit test infrastructure found in project; validation logic is in ViewModel and can be tested when infra is added
