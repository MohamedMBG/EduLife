# Decorated Certificate View + Faster Download Notification

## Goal

Make the in-app certificate screen look like an actual decorated certificate (paper, gold frame, seal, ornaments, signature line) and make the PDF download notification appear immediately while it transfers, not only when it completes.

## What Changed

- Redesigned `fragment_certificate_detail.xml` as a framed paper card with:
  - Double gold border on a cream paper background.
  - Gold circular seal with a star vector.
  - "Certificate of Completion" eyebrow, serif typography, cursive student name.
  - Decorative gold ornament strips above and below the title.
  - Signature row: issue date + issuer name above gold underlines with labels.
  - Certificate number rendered as a gold-bordered pill.
  - Verification hash kept outside the frame to preserve the certificate aesthetic.
- Added cert-only color tokens (`cert_paper`, `cert_gold`, `cert_gold_soft`, `cert_gold_pale`, `cert_ink`, `cert_ink_muted`).
- Added drawables: `bg_cert_frame.xml`, `bg_cert_seal.xml`, `bg_cert_number_pill.xml`, `ic_cert_ornament.xml`, `ic_cert_seal_star.xml`.
- Added strings for the new copy + content descriptions.
- `CertificateDetailFragment.downloadPdf`: switched `setNotificationVisibility` from `VISIBILITY_VISIBLE_NOTIFY_COMPLETED` to `VISIBILITY_VISIBLE` so the system notification with progress shows as soon as the download starts, and added `setAllowedOverMetered(true)` / `setAllowedOverRoaming(true)` so the OS does not defer the transfer.

## Files Touched

- `app/src/main/res/layout/fragment_certificate_detail.xml`
- `app/src/main/res/values/colors.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/drawable/bg_cert_frame.xml` (new)
- `app/src/main/res/drawable/bg_cert_seal.xml` (new)
- `app/src/main/res/drawable/bg_cert_number_pill.xml` (new)
- `app/src/main/res/drawable/ic_cert_ornament.xml` (new)
- `app/src/main/res/drawable/ic_cert_seal_star.xml` (new)
- `app/src/main/java/com/baghdad/edulife/features/certificates/ui/CertificateDetailFragment.java`

## Backend Impact

None.

## Android Impact

Visual redesign of the certificate detail screen. Download flow now surfaces a progress notification immediately. Existing behavior (open PDF on completion via broadcast receiver) preserved.

## Web Impact

None.

## Architecture Compliance

- No business logic moved into UI.
- Java + XML only.
- All colors and dimensions reference resource tokens — no hardcoded literals in the layout.
- Download still uses authenticated request with bearer token; certificate ownership enforced server-side.

## Tests / Verification

- `./gradlew :app:compileDebugJavaWithJavac :app:processDebugResources --rerun-tasks` — passes.
- Manual: open a certificate → frame renders with seal + ornaments, tap Download → notification appears immediately and progresses, completion broadcast opens PDF.

## Risks / Notes

- `cursive`/`serif` are system font families; on devices without good cursive coverage the awarded name still renders cleanly thanks to the italic + bold weight.
- `VISIBILITY_VISIBLE` keeps the notification after completion as well; matches user expectation of "appearing notification".
