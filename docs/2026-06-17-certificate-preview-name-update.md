# Task Audit - Certificate Preview Name Update

## Date
2026-06-17

## Task Summary
Updated the public homepage certificate preview name from `Yassine El-Amrani` to `BAGHDAD Mohamed`.

## Files Created
- docs/2026-06-17-certificate-preview-name-update.md

## Files Modified
- guided-journey-lab/src/components/landing/PublicCertificatesSection.tsx

## What Was Done
- Replaced the learner name shown in the public landing page certificate mockup.
- Kept the change scoped to the visitor homepage preview only.

## Architecture Compliance
- The change stays inside the web landing UI layer under `guided-journey-lab/src/components/landing/`.
- No backend logic, API contracts, authentication flows, or dashboard routes were modified.

## Code Comments Added
- No new comments were needed because this task only changed display text.

## Validation / Testing
- Verified by source search that the old name had a single occurrence and it was replaced.
- No build or test command was necessary for this text-only change.

## Risks / Notes
- This updates only the marketing certificate preview on the homepage, not any real generated certificate data.
