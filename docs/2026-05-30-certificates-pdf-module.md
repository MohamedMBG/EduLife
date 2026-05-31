# Certificates PDF Module

## Goal

Implement a full certificate generation pipeline: on exam pass, generate a verifiable PDF certificate with a QR code, store it to disk, and expose REST endpoints for listing, viewing, downloading, and publicly verifying certificates.

## What Changed

- **pom.xml**: Added Thymeleaf, OpenHTMLToPDF (pdfbox + slf4j), and ZXing (core + javase) dependencies.
- **V14__certificates_v2.sql**: Added 7 new columns to the `certificates` table (`exam_attempt_id`, `student_name`, `course_title`, `issuer_name`, `verification_hash`, `pdf_url`, `created_at`) plus unique constraints and an index on `verification_hash`.
- **Certificate.java**: Rewrote entity to include all new fields with both a backward-compat constructor and a new full constructor.
- **CertificateRepository.java**: Added `findByIdAndUserId`, `findByVerificationHash`, `findByUserIdAndCourseId`, and `existsByUserIdAndExamAttemptId`.
- **CertificateSummaryDto.java**: New DTO for list endpoint.
- **CertificateDetailDto.java**: New DTO for single-cert and generation response.
- **CertificateVerificationDto.java**: New DTO for public verify endpoint.
- **certificates/exception/**: Four new exceptions: `CertificateNotFoundException`, `CertificateAccessDeniedException`, `CertificateAlreadyExistsException`, `CertificateGenerationException`.
- **CertificateStorageProperties.java**: `@ConfigurationProperties(prefix = "edulife.certificates")` with `storageDir` and `publicBaseUrl`.
- **application.yaml**: Added `edulife.certificates` block.
- **CertificateService.java**: Full rewrite — PDF generation via OpenHTMLToPDF, QR code via ZXing, Thymeleaf rendering, SHA-256 verification hash, idempotent generation.
- **CertificateController.java**: Full rewrite — GET /me, GET /{id}, GET /{id}/download, GET /verify/{hash}.
- **SecurityConfig.java**: Added `permitAll` for `/api/v1/certificates/verify/**` before the authenticated catch-all.
- **GlobalApiExceptionHandler.java**: Added handlers for all four certificate exceptions.
- **ExamService.java**: Replaced `CertificateRepository` injection with `CertificateService`; removed `generateCertificateNumber()`; now calls `certificateService.generateCertificateAfterExamPass(userId, courseId, attemptId)`.
- **certificate-academic.html**: Thymeleaf template for PDF rendering (academic style, dual border, QR code).
- **CertificateControllerTest.java**: 7 WebMvcTest tests covering auth, list, detail, not-found, public verify, bad hash, and download.

## Files Touched

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/V14__certificates_v2.sql`
- `backend/src/main/resources/application.yaml`
- `backend/src/main/resources/templates/certificate-academic.html`
- `backend/src/main/java/com/edulife/certificates/entity/Certificate.java`
- `backend/src/main/java/com/edulife/certificates/repository/CertificateRepository.java`
- `backend/src/main/java/com/edulife/certificates/dto/CertificateSummaryDto.java` (new)
- `backend/src/main/java/com/edulife/certificates/dto/CertificateDetailDto.java` (new)
- `backend/src/main/java/com/edulife/certificates/dto/CertificateVerificationDto.java` (new)
- `backend/src/main/java/com/edulife/certificates/exception/CertificateNotFoundException.java` (new)
- `backend/src/main/java/com/edulife/certificates/exception/CertificateAccessDeniedException.java` (new)
- `backend/src/main/java/com/edulife/certificates/exception/CertificateAlreadyExistsException.java` (new)
- `backend/src/main/java/com/edulife/certificates/exception/CertificateGenerationException.java` (new)
- `backend/src/main/java/com/edulife/certificates/config/CertificateStorageProperties.java` (new)
- `backend/src/main/java/com/edulife/certificates/service/CertificateService.java`
- `backend/src/main/java/com/edulife/certificates/controller/CertificateController.java`
- `backend/src/main/java/com/edulife/security/SecurityConfig.java`
- `backend/src/main/java/com/edulife/common/error/GlobalApiExceptionHandler.java`
- `backend/src/main/java/com/edulife/exams/service/ExamService.java`
- `backend/src/test/java/com/edulife/certificates/CertificateControllerTest.java` (new)

## Backend Impact

New endpoints:
- `GET /api/v1/certificates/me` — authenticated; returns learner's certificate list.
- `GET /api/v1/certificates/{id}` — authenticated; returns full certificate detail.
- `GET /api/v1/certificates/{id}/download` — authenticated; streams PDF as `application/pdf`.
- `GET /api/v1/certificates/verify/{verificationHash}` — public (no auth); returns verification status.

PDF generation flow:
1. ExamService saves ExamAttempt, then calls CertificateService.
2. CertificateService resolves student name (Profile), course title (Course), issuer name (creator's Profile).
3. Generates certificate number (`EL-{year}-{12hex}`), SHA-256 verification hash.
4. Generates QR code (ZXing) pointing to the public verify URL.
5. Renders Thymeleaf template to HTML, converts to PDF via OpenHTMLToPDF.
6. Saves PDF to `storageDir/certificate-{uuid}.pdf`.
7. Saves Certificate entity with all fields; returns `CertificateDetailDto`.

## Android Impact

No immediate Android changes. Android can call:
- `GET /api/v1/certificates/me` after passing an exam.
- `GET /api/v1/certificates/{id}/download` to display or share the PDF.

## Web Impact

No immediate web changes. The public verify endpoint (`/api/v1/certificates/verify/{hash}`) can be embedded in a public verification page without authentication.

## Architecture Compliance

- Business logic is in `CertificateService`; controller stays thin.
- DTOs used for all API input/output; JPA entity never exposed directly.
- One-way dependency: ExamService → CertificateService (no circular dependency).
- Security: ownership enforced via `findByIdAndUserId`; verify endpoint is intentionally public and documented.
- `@ConfigurationProperties` used for storage config; no hardcoded paths.
- Flyway migration used for schema changes; existing V10 data untouched (new columns nullable).

## Tests / Verification

7 WebMvcTest cases in `CertificateControllerTest`:
1. GET /me without token → 401
2. GET /me with valid token → 200 list
3. GET /{id} owned cert → 200
4. GET /{id} unowned → 404
5. GET /verify/{hash} without token → 200 (public)
6. GET /verify/bad-hash → 404
7. GET /{id}/download → 200 PDF content type

## Risks / Notes

- PDF file storage is local filesystem; not suitable for multi-instance deployments. Migrate to object storage (S3/R2) before horizontal scaling.
- OpenHTMLToPDF does not support all modern CSS; the template uses a subset known to work with PDFBox rendering.
- The verification hash is deterministic from (certificateNumber + userId + courseId). Changing any of these fields would invalidate existing QR codes — these fields are `updatable = false`.
- Existing certificate rows from V10 will have all new columns as NULL, which is safe. Generation sets all fields.
