# Web Exam Flow (Sprint W1)

## Goal

Add full learner exam flow to `guided-journey-lab` web app. Two new routes:
`/courses/$courseId/exam` and `/courses/$courseId/exam/result`. Backend endpoints
already exist — no business logic on the client.

## What Changed

- **API types**: added `Exam`, `ExamQuestion`, `ExamChoice`, `ExamStatus`,
  `ExamResult`, `ExamAnswer`, `ExamSubmitRequest` to `src/lib/api/types.ts`.
- **API client**: added `getExam`, `getExamStatus`, `submitExam` in
  `src/lib/api/client.ts`. Demo mode rejects exam calls with a 501 error
  (no mocked exam data).
- **Exam page** (`src/routes/courses.$courseId.exam.tsx`):
  - Pre-exam `GET /api/v1/courses/{id}/exam/status` check.
  - Branches: already passed → cert CTA; in cooldown → locked card with
    `cooldownEndsAt`; allowed → fetch and render MCQ.
  - Tracks selections in local state, submit disabled until all answered.
  - `POST .../submit` then navigates to result page via search params.
  - Catches 409 / 429 from submit → refetches status, surfaces error.
  - Never renders or stores correct answers — backend scores.
- **Result page** (`src/routes/courses.$courseId.exam.result.tsx`):
  - Reads result data from typed search params via `validateSearch`.
  - Pass: score, pass threshold, certificate number, link to `/certificates`.
  - Fail: score, attempts used, cooldown timer (if locked), retry CTA when
    not locked.
- **Course detail CTA**: when enrolled and progress = 100%, course hero now
  shows a "Take final exam" button linking to the exam route.

## Files Touched

- `guided-journey-lab/src/lib/api/types.ts`
- `guided-journey-lab/src/lib/api/client.ts`
- `guided-journey-lab/src/routes/courses.$courseId.exam.tsx` (new)
- `guided-journey-lab/src/routes/courses.$courseId.exam.result.tsx` (new)
- `guided-journey-lab/src/routes/courses.$courseId.tsx` (added exam CTA)
- `guided-journey-lab/src/routeTree.gen.ts` (auto-regenerated)

## Backend Endpoints Used

- `GET /api/v1/courses/{courseId}/exam`
- `GET /api/v1/courses/{courseId}/exam/status`
- `POST /api/v1/courses/{courseId}/exam/submit`

## Design Tokens Used

- `bg-gradient-to-br from-primary to-primary-glow` (exam header)
- `bg-gradient-gold`, `shadow-gold`, `text-gold-foreground` (pass card)
- `bg-destructive/8`, `border-destructive/30`, `text-destructive`
  (fail / cooldown cards)
- `bg-surface-elevated`, `border-border`, `shadow-soft` (default cards)
- No hardcoded colors.

## States Handled

- [x] Loading — status + exam queries show `StateCard`.
- [x] Error — status / exam / submit failures surface human messages.
- [x] Empty — exam route shows "No questions yet" when published exam has
  zero questions; result route shows "No result to display" without
  search params.
- [x] Success — exam render, submit → navigate to result; pass and fail
  variants on result page.

## Dark Mode Tested

N/A in this pass — relies on token-based styling that already responds to
`.dark`. No hardcoded colors added. Manual dark-mode smoke test deferred
to Sprint W3 (dark-mode toggle).

## TypeScript Errors

None introduced by this task. Pre-existing strict-null errors in
`courses.$courseId.tsx`, `learn.$courseId.$lessonId.tsx`,
`certificates.tsx`, and `demo.ts` are unchanged.

## Risks / Notes

- Submit retry: TanStack Query mutation does not auto-retry. Selected
  answers are kept in component state, so the learner can press submit
  again after a network blip without re-answering.
- Result page derives state from search params. Refreshing the page after
  navigation keeps the result because params are URL-encoded; closing the
  tab loses it (expected — `/certificates` and exam status are the source
  of truth for any later view).
- Exam CTA on the course detail page only appears when progress reaches
  100% per the backend `CourseProgress.percentComplete`. The backend will
  also enforce its own eligibility — UI is a hint, not a gate.
- Demo mode (`VITE_DEMO_MODE=true`) throws on all three exam calls. Demo
  exam content is out of scope; do not mock it.
