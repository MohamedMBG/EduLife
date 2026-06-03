# Phase 5 Polish — Certificate Detail Fragment

## Goal
Add CertificateDetailFragment so learners can preview cert metadata + download or share before going to Downloads.

## What Changed

- **CertificateDetail.java** — new model mirroring backend `CertificateDetailDto` (id, courseId, certNumber, studentName, courseTitle, issuerName, issuedAt, verificationHash, pdfUrl)
- **ApiService** — added `GET certificates/{id}`
- **CertificateRepository** — added `CertificateDetailCallback` + `getCertificateById()`
- **CertificateDetailViewModel** — loads `GET /certificates/{id}`, guard against re-fetch
- **CertificateDetailFragment** — shows course title, cert number badge, student/issuer/date/hash metadata, Download PDF (DownloadManager, same logic as list), Share (ACTION_SEND text with cert details + verification hash)
- **fragment_certificate_detail.xml** — header + metadata card + two action buttons
- **CertificateAdapter** — added `OnItemClick` interface; full card click → detail; download button click still downloads directly
- **CertificatesFragment** — passes `openCertDetail` to adapter, navigates to detail fragment with `certId`
- **nav_graph.xml** — `certificateDetailFragment` destination + action from `certificatesFragment`
- **strings.xml** — cert detail labels + share text template

## Files Touched

- `features/certificates/model/CertificateDetail.java` (new)
- `features/certificates/viewmodel/CertificateDetailViewModel.java` (new)
- `features/certificates/ui/CertificateDetailFragment.java` (new)
- `res/layout/fragment_certificate_detail.xml` (new)
- `core/network/ApiService.java`
- `features/certificates/data/CertificateRepository.java`
- `features/certificates/ui/CertificateAdapter.java`
- `features/certificates/ui/CertificatesFragment.java`
- `res/navigation/nav_graph.xml`
- `res/values/strings.xml`

## Backend Impact

None — consumes existing `GET /api/v1/certificates/{id}` endpoint.

## Android Impact

- Cert list: tap card → detail screen (download button still works inline on list)
- Detail screen: all cert metadata visible before downloading
- Share: text share (cert number + course + date + hash) — no FileProvider required

## Architecture Compliance

- Share as text (not file) avoids FileProvider setup which is out of MVP scope
- Download logic copied verbatim from CertificatesFragment to keep the broadcast receiver pattern consistent
- ViewModel guards against re-fetch on rotation

## Tests / Verification

- Cert list → tap card → detail loads → metadata shows
- Download PDF button → DownloadManager → notification → opens in PDF viewer
- Share button → chooser sheet → text with cert details

## Risks / Notes

- PDF inline preview (PdfRenderer) deferred — requires render thread, page management, significant complexity for MVP
- Share text includes verificationHash which can be looked up via `GET /certificates/verify/{hash}` (future web feature)
