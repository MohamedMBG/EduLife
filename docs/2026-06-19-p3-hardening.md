# P3 Security Hardening

## Goal

Close the remaining P3 findings from the security audit: avatar magic-byte
validation, course description length + imageUrl URL validation (stored XSS),
concurrent-exam double XP/certificate, X-Forwarded-For rate-limit spoofing,
non-root Docker user, last-admin guard, and preview-lesson-of-draft access.

## What Changed

1. **Avatar magic-byte validation** — `LocalAvatarStorage` now reads the upload
   bytes and derives the format from the magic bytes (JPEG `FF D8 FF`, PNG
   `89 50 4E 47 0D 0A 1A 0A`, WebP `RIFF…WEBP`) instead of trusting the
   client-supplied `Content-Type`. A renamed script/HTML file is rejected with
   415 and never written.
2. **Course description + imageUrl validation** — `CreateCourseRequest` /
   `UpdateCourseRequest` now cap `description` at 20000 chars and constrain
   `imageUrl` to http(s) URLs (`^(https?://.+)?$`, max 2048). Blocks stored XSS
   via `javascript:`/`data:` URLs rendered into `<img src>` by both clients.
   Authoritative fix at the backend per the shared-API rule.
3. **Concurrent-exam double XP** — new partial unique index
   `idx_exam_attempts_one_pass ON exam_attempts(user_id, exam_id) WHERE passed`
   (V27). `ExamService.submitExam` uses `saveAndFlush` and translates a
   `DataIntegrityViolationException` on a passing attempt into
   `ExamAlreadyPassedException`, so two racing submissions can no longer both
   award XP and issue a certificate. Migration demotes any pre-existing
   duplicate passes to `false` before creating the index.
4. **X-Forwarded-For spoofing** — `RateLimitFilter` no longer keys per-IP
   buckets on the client-controlled leftmost XFF entry. It takes the entry
   `trusted-proxy-count` hops from the right (configurable, default 1) and falls
   back to the socket address. Stops attackers minting unlimited buckets to
   defeat the cert-verify per-IP cap.
5. **Non-root Docker user** — runtime image adds a system user `edulife`
   (uid 10001), chowns `/app` (LocalAvatarStorage writes uploads there), and
   runs `USER edulife`.
6. **Last-admin guard** — `AdminUserService.changeRole` rejects (409) demoting
   the last remaining ADMIN, preventing lockout of all platform management.
7. **Preview-lesson-of-draft access** — `LessonService.getLessonDetail` now
   requires the course to be PUBLISHED before serving a preview lesson to an
   unenrolled user, closing a draft/archived content leak via id guessing.

## Files Touched

- `backend/.../profiles/storage/LocalAvatarStorage.java`
- `backend/.../admin/dto/CreateCourseRequest.java`
- `backend/.../admin/dto/UpdateCourseRequest.java`
- `backend/.../exams/service/ExamService.java`
- `backend/src/main/resources/db/migration/V27__exam_one_pass_per_user.sql` (new)
- `backend/.../config/RateLimitFilter.java`
- `backend/.../security/SecurityConfig.java`
- `backend/src/main/resources/application.yaml`
- `backend/Dockerfile`
- `backend/.../admin/service/AdminUserService.java`
- `backend/.../courses/service/LessonService.java`
- Tests: `LocalAvatarStorageTest`, `ExamServiceCertificateTest`,
  `RateLimitRetryAfterTest`, `AdvisorRateLimitTest`

## Backend Impact

New Flyway migration V27. New config key `edulife.rate-limit.trusted-proxy-count`
(env `EDULIFE_RATE_LIMIT_TRUSTED_PROXIES`, default 1). `LessonService` gains a
`CourseRepository` dependency. No API contract changes; validation tightens
inputs that were previously unconstrained.

## Android Impact

None. Same endpoints/contracts.

## Web Impact

None code-side. The imageUrl XSS surface (`<img src>` in courses.index/explore)
is now neutralised at the backend; no web change required.

## Architecture Compliance

- Validation/security enforced server-side; clients untrusted.
- DTO validation via Bean Validation; thin controllers unchanged.
- Schema change via new Flyway migration (no edits to applied migrations).
- Exam scoring + XP remain authoritative on the backend.

## Tests / Verification

- `mvnw compile` — clean.
- Targeted tests: 17 run, 0 failures
  (`LocalAvatarStorageTest` incl. new spoofed-content-type case,
  `ExamServiceCertificateTest`, `RateLimitRetryAfterTest`, `AdvisorRateLimitTest`).

## Risks / Notes

- V27 dedup demotes historical duplicate passes to `passed = false`; their
  certificates/XP already reference the kept (earliest) attempt, so no learner
  loses a certificate. Verify no unexpected duplicates exist before prod deploy.
- `trusted-proxy-count` must match the real proxy depth (Render = 1; add
  Cloudflare in front = 2) or per-IP cert-verify limiting degrades.
- Avatar validation loads the file fully into memory (≤5MB cap), acceptable for
  avatar sizes.
