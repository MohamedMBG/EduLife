# EduLife Exam and Certificate Workflows

## Workflow: Exam Availability and Status

Role:
Learner

Platform:
Android, Web, Backend, Database

Status:
Partially working

Entry point:

- Android: `CourseDetailFragment`, `ExamFragment`
- Web: `/courses/$courseId`, `/courses/$courseId/exam`
- Backend: `GET /api/v1/courses/{courseId}/exam/status`, `GET /api/v1/courses/{courseId}/exam`

End result:

- Learner sees whether an exam is available, already passed, or locked by cooldown.

Step-by-step:

1. Client requests exam status.
2. Backend verifies enrollment and computes:
   - whether the learner already passed
   - failed attempt count
   - cooldown state after two failures
3. If not blocked, client requests exam questions.
4. Client renders the final exam UI or a pass/cooldown state card.

Backend code:

- file path: `backend/src/main/java/com/edulife/exams/controller/ExamController.java`
- class/method: `getExam`, `getExamStatus`

- file path: `backend/src/main/java/com/edulife/exams/service/ExamService.java`
- class/method: `getExamForCourse`, `getExamStatus`

Android code:

- file path: `app/src/main/java/com/baghdad/edulife/features/courses/ui/ExamFragment.java`
- file path: `app/src/main/java/com/baghdad/edulife/features/courses/ui/CourseDetailFragment.java`

Web code:

- file path: `guided-journey-lab/src/routes/courses.$courseId.tsx`
- file path: `guided-journey-lab/src/routes/courses.$courseId.exam.tsx`

Database:

- tables: `exams`, `exam_questions`, `exam_choices`, `exam_attempts`, `enrollments`
- migration files: `V9__exams.sql`, `V17__exam_attempt_passed_index.sql`

API contract:

- `GET /api/v1/courses/{courseId}/exam/status`
  - response DTO: `ExamStatusDto`
  - fields: `examId`, `passed`, `failedAttempts`, `maxAttemptsBeforeCooldown`, `inCooldown`, `cooldownEndsAt`
- `GET /api/v1/courses/{courseId}/exam`
  - response DTO: `ExamDto`
  - fields: `examId`, `courseId`, `title`, `passScore`, `timeLimitMinutes`, `questions`

Security:

- authentication: required
- authorization: enrollment required
- ownership checks: attempts and pass state are scoped to current user

Problems found:

- Web only exposes the exam CTA after 100% progress.
- Android shows the exam CTA whenever the learner is enrolled.
- Backend `getExam` itself does not enforce progress completion or cooldown; only status/submit govern those cases.

Missing pieces:

- unified server-side exam-readiness rule

Recommended next fix:

- decide whether exam eligibility should require course completion, then enforce it in the backend and surface it via a dedicated field

## Workflow: Exam Submission, Backend Scoring, Result, and Cooldown

Role:
Learner

Platform:
Android, Web, Backend, Database, Tests

Status:
Fully working with product-spec mismatch

Entry point:

- Android: `ExamFragment` submit, `ExamResultFragment`
- Web: `/courses/$courseId/exam` and `/courses/$courseId/exam/result`
- Backend: `POST /api/v1/courses/{courseId}/exam/submit`

End result:

- Learner submits answers, backend scores the exam, enforces pass/fail and cooldown, and returns result metadata.

Step-by-step:

1. Client submits `answers[]`.
2. Backend verifies enrollment, active exam, answer ownership, and question-choice integrity.
3. Backend calculates score server-side and never exposes correct answers.
4. Failed attempt counter increments.
5. After two failed attempts, backend returns/records a 72-hour cooldown.
6. If the learner passes, backend prevents future resubmissions and starts certificate issuance.

Backend code:

- file path: `backend/src/main/java/com/edulife/exams/service/ExamService.java`
- class/method: `submitExam`
- important snippet:

```java
boolean passed = score >= exam.getPassScore();
if (passed) {
    certificateService.generateForPassedExam(...);
}
```

Android code:

- file path: `app/src/main/java/com/baghdad/edulife/features/courses/ui/ExamFragment.java`
- class/method: answer collection and submit
- file path: `app/src/main/java/com/baghdad/edulife/features/courses/ui/ExamResultFragment.java`

Web code:

- file path: `guided-journey-lab/src/routes/courses.$courseId.exam.tsx`
- file path: `guided-journey-lab/src/routes/courses.$courseId.exam.result.tsx`

Database:

- tables: `exam_attempts`
- migration files: `V9__exams.sql`, `V17__exam_attempt_passed_index.sql`

API contract:

- endpoint: `POST /api/v1/courses/{courseId}/exam/submit`
- request DTO:
  - `SubmitExamRequest`
  - `answers[] { questionId, choiceId }`
- response DTO:
  - `ExamResultDto`
  - `score`, `passScore`, `passed`, `certificateNumber`, `attemptsUsed`, `cooldownEndsAt`
- errors:
  - `409` already passed
  - `429` cooldown active
  - validation failures for invalid question/choice relationships

Security:

- authentication: required
- authorization: owner + active enrollment
- ownership checks:
  - answer choices are checked against exam questions
  - correct answers are never serialized to clients

Problems found:

- Pass threshold mismatch remains unresolved:
  - product docs/AGENTS say `80%`
  - schema seed/defaults still use `70%`

Recommended next fix:

- choose one pass score source of truth and update migrations, docs, and UI copy together

## Workflow: Certificate Generation, Listing, Detail, and PDF Download

Role:
Learner

Platform:
Android, Web, Backend, Database, File storage, Tests

Status:
Fully working

Entry point:

- Backend generation on exam pass
- Android: `CertificatesFragment`, `CertificateDetailFragment`
- Web: `/certificates`, `/certificates/$certificateId`

End result:

- Passing learners receive a certificate record, can view details, and download a generated PDF.

Step-by-step:

1. `ExamService` calls `CertificateService` after a passing attempt.
2. Backend creates a unique certificate with historical snapshot data.
3. Certificate list/detail endpoints expose learner-owned records.
4. Download endpoint generates or streams the certificate PDF using the stored snapshots and verification hash.

Backend code:

- file path: `backend/src/main/java/com/edulife/certificates/service/CertificateService.java`
- class/method: `generateForPassedExam`, `findMyCertificates`, `getMyCertificate`
- file path: `backend/src/main/java/com/edulife/certificates/service/CertificatePdfService.java`
- class/method: PDF generation

Android code:

- file path: `app/src/main/java/com/baghdad/edulife/features/certificates/ui/CertificatesFragment.java`
- file path: `app/src/main/java/com/baghdad/edulife/features/certificates/ui/CertificateDetailFragment.java`

Web code:

- file path: `guided-journey-lab/src/routes/certificates.index.tsx`
- file path: `guided-journey-lab/src/routes/certificates.$certificateId.tsx`

Database:

- tables: `certificates`
- migration files:
  - `V10__certificates.sql`
  - `V14__certificates_v2.sql`
  - `V24__certificate_dynamic_snapshots.sql`

API contract:

- `GET /api/v1/certificates/me`
  - response DTO: list of `CertificateSummaryDto`
- `GET /api/v1/certificates/{id}`
  - response DTO: `CertificateDetailDto`
- `GET /api/v1/certificates/{id}/download`
  - response: PDF bytes

Security:

- authentication: required
- authorization: owner only
- ownership checks:
  - certificate lookup is scoped to current user
  - certificate snapshots preserve historical truth even if profile/course data later changes

Problems found:

- Android has no public verification flow even though the backend and web do.

Recommended next fix:

- expose public verify on Android only if it is part of the mobile product; otherwise keep web as the public verification surface

## Workflow: Public Certificate Verification

Role:
Public visitor

Platform:
Web, Backend, Database, Tests

Status:
Partially working

Entry point:

- Web: `/certificates/verify/$hash`
- Backend: `GET /api/v1/certificates/verify/{verificationHash}`

End result:

- Anyone with the verification hash can validate certificate authenticity.

Step-by-step:

1. Visitor opens a verification URL.
2. Web requests the public backend verify endpoint without authentication.
3. Backend resolves the certificate by `verification_hash`.
4. Web displays verified learner name, course title, issuer, issue date, and certificate number.

Backend code:

- file path: `backend/src/main/java/com/edulife/certificates/controller/CertificateController.java`
- class/method: `verify`

Web code:

- file path: `guided-journey-lab/src/routes/certificates.verify.$hash.tsx`

Database:

- tables: `certificates`
- migration files: `V14__certificates_v2.sql`, `V24__certificate_dynamic_snapshots.sql`

API contract:

- endpoint: `GET /api/v1/certificates/verify/{verificationHash}`
- response DTO:
  - `CertificateVerificationDto`
  - `studentName`, `courseTitle`, `issuerName`, `issuedAt`, `certificateNumber`, `valid`
- errors:
  - `404` invalid hash

Security:

- authentication: none
- authorization: public endpoint explicitly allowed in `SecurityConfig`
- ownership checks: hash acts as the verification token

Problems found:

- no Android equivalent

Missing pieces:

- no backend rate-limit specifically on verification, beyond global hardening

Recommended next fix:

- leave verification on web/public by default; only add another client if there is a real product need

