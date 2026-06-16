# Fix Certificate PDF Download Flow

## Goal

Clicking "Download as PDF" on a passed learner's certificate must return a real,
backend-generated PDF containing the learner name, teacher/issuer name, course title,
course level, issue date, certificate ID, and the verification hash/QR — instead of
failing with "Certificate generation failed".

## Root Cause

The download endpoint read the PDF from disk via the stored `pdf_url` path
(`CertificateService.getCertificatePdfForDownload` → `Files.readAllBytes`). When that
file was missing — different `edulife.certificates.storage-dir` across environments,
ephemeral/restarted container storage, an absolute path written on another host, or a
certificate row issued before the PDF was persisted — `Files.readAllBytes` threw an
`IOException`. The service wrapped it in `CertificateGenerationException`, which the
global handler maps to **HTTP 500 with body `{"message":"Certificate generation failed"}`**.
The web client surfaced that backend message verbatim, producing the reported UI text.

## What Changed

- **PDF is now regenerated on demand from the persisted certificate snapshot** at
  download time. The download no longer depends on any file existing on disk, so a
  missing/relocated storage file can no longer fail the download.
- Extracted a dedicated **`CertificatePdfService`** that renders PDF bytes purely from a
  `Certificate` entity (snapshots + verification hash). Used by both issue-time
  pre-rendering and the download endpoint, so on-screen and downloaded credentials match.
- The verification hash is **read from the entity, never recomputed**, so the PDF hash
  always equals the one shown in the UI.
- **Robust snapshot handling**: missing snapshot fields fall back to safe defaults
  (`EduLife Learner` / `EduLife Instructor` / `EduLife Course` / `All Levels`) so null
  text can never crash PDF rendering. Hash and certificate number are taken verbatim.
- **Correct error mapping** in the download path:
  - `404` — certificate does not exist (`CertificateNotFoundException`)
  - `403` — certificate owned by another learner (`CertificateAccessDeniedException`,
    via an id-first lookup + explicit ownership check)
  - `409` — certificate exists but is not downloadable, e.g. missing verification hash
    (new `CertificateNotDownloadableException`)
  - `500` — only for genuine unexpected render failures; the real exception is logged
    server-side while the client receives a clean message.
- **Issuance resilience**: pre-rendering/caching the PDF to disk at exam-pass time is now
  best-effort and logged on failure; a storage hiccup no longer blocks certificate
  creation (which is tied to the authoritative exam-pass result).
- **Web client error messages** mapped by status: "Certificate not found.", "You do not
  have access to this certificate.", "This certificate is not available for download.",
  and "PDF could not be generated. Please try again." instead of the raw backend message.

The download endpoint already returned the correct headers (kept unchanged):
`Content-Type: application/pdf` and
`Content-Disposition: attachment; filename="certificate-{id}.pdf"`.

## Files Touched

- `backend/src/main/java/com/edulife/certificates/service/CertificatePdfService.java` (new)
- `backend/src/main/java/com/edulife/certificates/service/CertificateService.java`
- `backend/src/main/java/com/edulife/certificates/exception/CertificateNotDownloadableException.java` (new)
- `backend/src/main/java/com/edulife/common/error/GlobalApiExceptionHandler.java`
- `backend/src/test/java/com/edulife/certificates/CertificateServiceTest.java`
- `backend/src/test/java/com/edulife/certificates/CertificatePdfServiceTest.java` (new)
- `backend/src/test/java/com/edulife/certificates/CertificateControllerTest.java`
- `guided-journey-lab/src/routes/certificates.$certificateId.tsx`

## Backend Impact

`GET /api/v1/certificates/{id}/download` now renders the PDF on demand from the snapshot
and enforces ownership explicitly (404/403/409/500). Issue flow
(`CertificateService.generateCertificateAfterExamPass`, called from `ExamService`) is
unchanged in contract; PDF caching is now best-effort.

## Android Impact

None required. `CertificateDetailFragment` already downloads via `DownloadManager` against
the correct authenticated endpoint with `application/pdf`; the backend fix makes that
download succeed. (Note: `DownloadManager` cannot easily surface HTTP error bodies — see
Risks.)

## Web Impact

`certificates.$certificateId.tsx` now maps download error status codes to friendly,
user-facing messages. Binary blob handling was already correct.

## Architecture Compliance

- PDF generation stays entirely in the backend; no frontend-only PDF.
- Verification security preserved — hash unchanged after creation, QR encodes the verify
  URL, public verify endpoint untouched.
- Ownership enforced server-side; no `userId`/`role` trusted from the client.
- No `firebase_uid` or internal IDs added to the PDF/API.
- Modular-monolith style followed (service/exception/dto layering); no async queues.

## Tests / Verification

- `mvnw -Dtest='Certificate*Test' test` → **19 tests pass**.
  - `CertificatePdfServiceTest` renders a real PDF, asserts `%PDF` magic + non-empty
    bytes, and extracts text (PDFBox) confirming learner name, teacher name, course
    title, course level, issue date, certificate number, and verification-hash code; plus
    a fallback test for missing snapshot fields.
  - `CertificateServiceTest` covers owner download, 403 for another learner, 404 for
    missing, 409 for a hash-less certificate, and snapshot population on issue.
  - `CertificateControllerTest` covers 200 `application/pdf` + Content-Disposition, 404,
    and 403 on the download route.

Manual: start backend, authenticate as a learner who passed an exam, call
`GET /api/v1/certificates/{id}/download` with their token, confirm a valid PDF opens and
its verification code matches the hash shown on the certificate detail screen.

## Risks / Notes

- Android `DownloadManager` saves whatever the server returns and does not surface backend
  error JSON; with the backend fixed this is moot for the happy path, but a future
  improvement could fetch via OkHttp to show friendly error messages on 403/404/409.
- Pre-existing, unrelated: the web `CertificateDetail` type uses `studentName`/`issuerName`
  while the backend DTO returns `learnerName`/`teacherName`, so those two labels render
  blank on the detail/verify screens. Not part of the download flow; flagged for a
  follow-up field-name alignment.
- On-demand rendering costs a small CPU hit per download (HTML→PDF + QR). Acceptable at
  MVP scale; the cached file can later be served when present if needed.
