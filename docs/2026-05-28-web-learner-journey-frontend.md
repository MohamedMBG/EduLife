# Task Audit - Web Learner Journey Frontend

## Date
2026-05-28

## Task Summary
Built the website frontend flow for course enrollment, lesson study, PDF reading, quiz and final exam access, and certificate viewing after a passing result.

## Files Created
- guided-journey-lab/src/lib/learner-flow-data.ts
- guided-journey-lab/src/routes/courses.$courseId.enroll.tsx
- docs/2026-05-28-web-learner-journey-frontend.md

## Files Modified
- guided-journey-lab/src/routes/courses.tsx
- guided-journey-lab/src/routes/courses.$courseId.tsx
- guided-journey-lab/src/routes/courses.$courseId.exam.tsx
- guided-journey-lab/src/routes/certificates.tsx
- guided-journey-lab/src/styles.css

## What Was Done
Added a shared mock frontend contract in `guided-journey-lab/src/lib/learner-flow-data.ts` so the course catalog, detail page, enrollment screen, exam screen, and certificate page all use the same learner journey data while backend integration is still pending.

Rebuilt the web course catalog route so cards now route into concrete learner actions:
- not-started courses open a dedicated enrollment screen
- active courses open the course detail screen
- completed courses open their review flow

Created a dedicated enrollment route at `guided-journey-lab/src/routes/courses.$courseId.enroll.tsx`. This screen explains the post-enrollment sequence: watch video, read PDF, complete quiz, pass the final exam, and unlock the certificate. It also provides a primary action that starts the first lesson immediately.

Reworked `guided-journey-lab/src/routes/courses.$courseId.tsx` so the course detail page now supports the new learner flow state. New courses show an enroll CTA first, while active courses continue into the next lesson. The page also explains the certificate gate and shows the ordered lesson sequence.

Updated `guided-journey-lab/src/routes/courses.$courseId.exam.tsx` to enforce the locked MVP pass score of 80%, keep the frontend quiz flow intact, and redirect a passed learner into the certificate screen with earned-certificate context.

Updated `guided-journey-lab/src/routes/certificates.tsx` so the page can highlight a newly earned certificate when reached from a passing exam result.

Moved the Google Fonts import to the top of `guided-journey-lab/src/styles.css` to remove the CSS build warning.

## Architecture Compliance
The work stays inside the existing website frontend structure under `guided-journey-lab/src/routes` for page-level UI and `guided-journey-lab/src/lib` for shared frontend data. No backend modules or Android feature structure were altered. The implementation is a frontend-only prototype that supports the EduLife learner loop without introducing extra MVP scope like CMS, chat, or payments.

## Code Comments Added
Added targeted comments in the new web route code and shared data module for:
- the temporary shared mock contract used before backend integration
- the certificate transition banner
- the enrollment stepper and course-completion checklist

These comments explain why the frontend flow is structured this way and where the learner journey expectations come from.

## Validation / Testing
Ran `pnpm build` in `guided-journey-lab` successfully after the route changes.

Started the local development server and verified it responds with HTTP 200 at:
- http://127.0.0.1:3001

## Risks / Notes
This is still a frontend prototype backed by local mock data. Enrollment state, lesson completion, exam submission, and certificate issuance are not persisted yet.

The lesson player route already existed and was reused as the video/PDF/quiz surface. Some lesson content remains demo content until it is connected to backend course and lesson contracts.
