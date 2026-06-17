# Web CMS Exam Builder

## Goal

Add a teacher-facing exam builder page to the web dashboard so teachers can create a final MCQ exam for their courses via the CMS.

## What Changed

- Added CMS exam TypeScript types (`CmsExamAdmin`, `CreateCmsExamRequest`, and sub-types)
- Added two API client methods: `getCmsExam()` and `createCmsExam()`
- Created exam builder route at `/teach/$courseId/exam`
- Added exam entry point button on the course management page (`/teach/$courseId`)

## Files Touched

- `guided-journey-lab/src/lib/api/types.ts` — added CMS exam types
- `guided-journey-lab/src/lib/api/client.ts` — added `getCmsExam`, `createCmsExam`
- `guided-journey-lab/src/routes/teach.$courseId.exam.tsx` — new exam builder page
- `guided-journey-lab/src/routes/teach.$courseId.tsx` — added exam link button

## Backend Endpoints Used

- `GET /api/v1/cms/courses/{courseId}/exam` — fetch existing exam (with correct-answer flags)
- `POST /api/v1/cms/courses/{courseId}/exam` — create exam atomically

## Roles Allowed

- TEACHER, GROUP_ADMIN, ADMIN (via `RequireTeacher` guard + backend `@PreAuthorize`)

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

Exam-level:
- Title required (max 200 chars)
- Pass score required, 1–100
- Time limit optional, must be >= 1 if provided
- At least one question required

Question-level:
- Question text required
- At least 2 choices per question
- Each choice text required
- Exactly one correct answer per question

## States Handled

- [x] Loading
- [x] Error (generic, 403, 409)
- [x] Empty (no exam → show builder)
- [x] Success (exam created confirmation)

## Dark Mode Tested

N/A — uses design token classes only (bg-card, text-foreground, etc.), auto-compatible.

## TypeScript Errors

None.

## Design Tokens Used

- `bg-card`, `bg-surface`, `bg-surface-elevated`
- `border-border`, `border-input`, `border-primary`
- `text-foreground`, `text-muted-foreground`, `text-primary`, `text-destructive`
- `shadow-soft`, `shadow-elevated`
- `rounded-3xl`, `rounded-2xl`, `rounded-xl`
- `text-display` (Fraunces headings)

## Manual Verification Steps

1. Login as teacher
2. Open `/teach` → click a course → click "Create final exam"
3. Fill title, pass score, time limit
4. Add questions with choices, select correct answers
5. Submit → confirm POST succeeds
6. Refresh → confirm GET shows saved exam read-only
7. Try creating again → confirm 409 handling
8. Login as learner → confirm `/teach/$courseId/exam` redirects away

## Risks / Notes

- Backend only supports create + get (no update/delete exam). Existing exam shown read-only.
- CMS exam types kept separate from learner exam types — no correct-answer leakage.
