# Task Audit - Certificate PDF Layout And Dynamic Data Fix

## Date
2026-06-16

## Task Summary
Fixed backend-generated certificate PDFs so they render as centered A4 landscape certificates, keep the QR verification area inside the page, and use resolved learner, teacher, course, level, issue date, certificate ID, and verification hash data.

## Files Created
- backend/src/main/java/com/edulife/certificates/service/CertificatePdfPayload.java
- docs/2026-06-16-certificate-pdf-layout-and-dynamic-data-fix.md

## Files Modified
- backend/src/main/java/com/edulife/certificates/service/CertificateService.java
- backend/src/main/java/com/edulife/certificates/service/CertificatePdfService.java
- backend/src/main/resources/templates/certificate-academic.html
- backend/src/test/java/com/edulife/certificates/CertificateServiceTest.java
- backend/src/test/java/com/edulife/certificates/CertificatePdfServiceTest.java

## What Was Done
Root cause was twofold:

- The HTML template used a fixed 793px certificate canvas without an explicit landscape PDF page. OpenHTMLToPDF rendered it in a way that could spill or clip the right-side QR and verification area.
- Historical or incomplete certificate rows could reach the PDF renderer with missing snapshot fields, causing renderer fallbacks such as `EduLife Learner` instead of resolving real backend data.

Implementation changes:

- Added `CertificatePdfPayload` as an internal immutable render model so the service can resolve dynamic data before PDF rendering.
- Updated `CertificateService` to resolve missing data in this order:
  - learner name: certificate snapshot, then profile display name, then readable email local-part, then `Learner <short-user-id>`.
  - teacher name: certificate snapshot, then course owner profile display name, then readable email local-part, then `EduLife Instructor`.
  - course title and level: certificate snapshot, then course row, then controlled fallback text.
- Kept verification hashes unchanged and passed the stored hash into the PDF payload.
- Updated certificate summaries, detail responses, public verification responses, and PDF downloads to use the same resolved data path.
- Reworked `certificate-academic.html` with `@page size: A4 landscape`, millimeter-based margins, table-safe footer columns, wrapped long names/course titles, full verification hash text, and a constrained QR area.

## Architecture Compliance
The fix stays inside the backend `certificates` module and uses existing repositories for cross-module reads through established Spring Data boundaries. Controllers still delegate to services, PDF rendering remains in `CertificatePdfService`, and ownership checks remain in `CertificateService`.

No frontend certificate generation was added. Public verification remains public for verification data only, protected PDF downloads still check certificate ownership, and no Firebase UID is exposed.

## Code Comments Added
- Added comments in `CertificatePdfPayload` explaining why resolved render data is separated from certificate entities.
- Added comments in `CertificatePdfService` explaining that the verification hash is read, never recomputed, and that the QR code contains only the public verification URL.
- Added comments in `CertificateService` explaining historical snapshot fallback resolution and why client-provided identity values are not trusted.
- Added a template comment explaining why table layout is used instead of flexbox for OpenHTMLToPDF.

## Validation / Testing
Focused tests passed:

```text
.\mvnw.cmd -q "-Dtest=CertificateServiceTest,CertificatePdfServiceTest,CertificateControllerTest" "-DforkCount=0" test
Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
```

Test coverage added or updated:

- PDF is non-empty and starts with `%PDF`.
- PDF text contains real learner name, teacher name, course title, course level, issue date, certificate ID, short verification code, and full verification hash.
- PDF text does not contain `EduLife Learner` when real learner data exists.
- PDF page is one-page A4 landscape, validating the QR safe-zone page bounds.
- Long learner name and long course title PDF text extraction still contains the expected dynamic data and full hash.
- Historical missing snapshot download resolves learner data from email and teacher data from course owner profile without changing the verification hash.
- Existing controller tests still cover PDF content type, missing certificate 404, and forbidden download for another learner.

Manual/visual validation:

- Generated sample PDFs:
  - backend/target/certificate-visual-samples/normal-certificate.pdf
  - backend/target/certificate-visual-samples/long-learner-certificate.pdf
  - backend/target/certificate-visual-samples/long-course-certificate.pdf
- Rendered PNG previews with PDFBox:
  - backend/target/certificate-visual-samples/rendered/normal-certificate.png
  - backend/target/certificate-visual-samples/rendered/long-learner-certificate.png
  - backend/target/certificate-visual-samples/rendered/long-course-certificate.png
- Visual inspection confirmed no right-side clipping, no bottom clipping, no overlapping metadata, and a fully visible QR / scan-to-verify area.

Full backend suite note:

```text
.\mvnw.cmd -q test
```

The full suite currently fails in `AuthSyncControllerTest` cleanup because seeded user `66666666-6666-6666-6666-666666666666` is referenced by `courses.created_by_user_id`, causing a foreign key violation when the test calls `userRepository.deleteAll()`. This is unrelated to the certificate PDF/data-binding change.

## Risks / Notes
- `pdftoppm` was not available in this environment, so visual rendering used PDFBox instead of Poppler.
- The full verification hash is intentionally small in the footer to fit inside the certificate safe zone while still being extractable and readable when zoomed.
- Historical certificates with missing snapshots are resolved from current profile/user/course data at response/render time, but the original verification hash is not changed.
