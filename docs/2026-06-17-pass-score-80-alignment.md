# Pass Score 80% Alignment

## Goal

Unify all exam pass score defaults to 80% across backend, Android, web, and docs.
Project decision (CLAUDE.md, AGENTS.md) locks pass score at 80%, but V9 migration and seed data used 70%.

## Root Cause

V9__exams.sql (original exam migration) set `DEFAULT 70` and seeded all 5 demo exams with `pass_score = 70`.
Android and web CMS builders were later built with correct 80% default, creating a mismatch.
Backend `CreateExamRequest.java` comment still referenced "70 as default".
Android `ExamResultFragment.java` used 70 as fallback.
Backend test `CmsExamServiceTest.java` created test exam with 70.

## What Changed

### Backend
- **New migration `V26__pass_score_80_alignment.sql`**: changes column default from 70 to 80, updates 5 seed exams from 70 to 80
- **`CreateExamRequest.java`**: comment updated from "Plan specifies 70" to "Project rule: 80"
- **`CmsExamServiceTest.java`**: test exam pass score changed from 70 to 80

### Android
- **`ExamResultFragment.java`**: fallback `args.getInt("passScore", 70)` changed to 80

### Web
- No changes needed. Exam builder already defaults to 80. `analytics.tsx` uses 70 for a competency note threshold (unrelated to exam pass score).

### Docs
- **`docs/plan/2026-05-21-master-plan-phases.md`**: schema example updated from DEFAULT 70 to DEFAULT 80

## Files Touched

- `backend/src/main/resources/db/migration/V26__pass_score_80_alignment.sql` (new)
- `backend/src/main/java/com/edulife/admin/dto/CreateExamRequest.java`
- `backend/src/test/java/com/edulife/admin/CmsExamServiceTest.java`
- `app/src/main/java/com/baghdad/edulife/features/courses/ui/ExamResultFragment.java`
- `docs/plan/2026-05-21-master-plan-phases.md`
- `docs/2026-06-17-pass-score-80-alignment.md` (this file)

## Old Values Found

| Location | Old Value | New Value |
|---|---|---|
| V9__exams.sql column default | 70 | 80 (via V26) |
| V9 seed exams (5 records) | 70 | 80 (via V26) |
| CreateExamRequest.java comment | "70 as default" | "80 is the default" |
| CmsExamServiceTest.java test exam | 70 | 80 |
| ExamResultFragment.java fallback | 70 | 80 |
| Master plan schema example | DEFAULT 70 | DEFAULT 80 |

## Migration Added

`V26__pass_score_80_alignment.sql`:
- `ALTER TABLE exams ALTER COLUMN pass_score SET DEFAULT 80`
- `UPDATE exams SET pass_score = 80 WHERE id IN (5 seed UUIDs) AND pass_score = 70`

Only updates seed/demo records. Teacher-created exams with custom pass scores are not touched.

## Architecture Compliance

- No old migrations edited
- New Flyway migration follows naming convention
- No business logic changed
- Exam scoring logic unchanged
- Certificate generation logic unchanged

## Tests / Verification

- Backend test fixture updated to match 80% rule
- Existing validation (1-100 range) unchanged
- Web/Android CMS builders already defaulted to 80 — confirmed no changes needed

## Risks / Notes

- If database already has teacher-created exams with pass_score = 70 (intentionally set), those are NOT touched by V26
- `analytics.tsx` line 431 uses `overallScore >= 70` for a competency note — this is NOT exam pass score, left unchanged
- Historical audit docs reference the mismatch as a finding — left as-is since they are point-in-time snapshots
