# Certificate Real Names

## Goal
Certificates must display the real name (entered at registration) for both teacher and student, not their email or username.

## What Changed

### Root cause
During registration, the user's full name is saved to Firebase `displayName` via `updateProfile()`, but the backend auth sync never read it. When the Profile was lazily created later, `displayName` was null, so certificate generation fell back to parsing a readable name from the email address.

### Backend
- **FirebaseAuthentication**: Added `displayName` field. Token filter now extracts `decodedToken.getName()` and carries it through the security context.
- **FirebaseTokenFilter**: Reads `decodedToken.getName()` and passes it to `FirebaseAuthentication`.
- **AuthSyncService**: On every `/auth/sync`, calls `ensureProfileDisplayName()` which creates or updates the Profile with the Firebase `displayName` if Profile.displayName is currently blank. This backfills existing users on their next login.

### Web
- **types.ts**: Fixed `CertificateDetail`, `Certificate`, and `CertificateVerification` interfaces to match backend field names (`learnerName`/`teacherName` instead of `studentName`/`issuerName`; added `courseLevel`, `verificationHash`).
- **certificates.$certificateId.tsx**: Updated references from `studentName` → `learnerName`, `issuerName` → `teacherName`.
- **certificates.verify.$hash.tsx**: Same field name fixes.
- **demo.ts**: Updated demo data and certificate minting to use new field names.

### Android
Already uses `learnerName`/`teacherName` — no changes needed.

## Files Touched
- backend/src/main/java/com/edulife/security/FirebaseAuthentication.java
- backend/src/main/java/com/edulife/security/FirebaseTokenFilter.java
- backend/src/main/java/com/edulife/auth/service/AuthSyncService.java
- backend/src/test/java/com/edulife/auth/AuthSyncServiceTest.java
- backend/src/test/java/com/edulife/account/AccountServiceTest.java
- backend/src/test/java/com/edulife/progress/ProgressServiceTest.java
- backend/src/test/java/com/edulife/analytics/CohortAnalyticsServiceTest.java
- backend/src/test/java/com/edulife/analytics/AnalyticsServiceTest.java
- backend/src/test/java/com/edulife/exams/ExamServiceCertificateTest.java
- backend/src/test/java/com/edulife/groups/GroupServiceTest.java
- guided-journey-lab/src/lib/api/types.ts
- guided-journey-lab/src/lib/api/demo.ts
- guided-journey-lab/src/routes/certificates.$certificateId.tsx
- guided-journey-lab/src/routes/certificates.verify.$hash.tsx

## Backend Impact
- Auth sync now creates/updates Profile with Firebase displayName on every login
- Existing users get backfilled on next login (Profile.displayName was blank → now set from Firebase)
- Certificate generation already reads from Profile.displayName, so certificates now show real names

## Android Impact
None — already uses correct field names.

## Web Impact
Fixed mismatched field names. Certificates now display `learnerName`/`teacherName` from backend.

## Architecture Compliance
- Name comes from trusted Firebase token (server-side), not client input
- Profile is source of truth for display name, populated from Firebase on auth sync
- No schema changes needed

## Tests / Verification
- Backend compiles clean
- Web TypeScript compiles clean
- All test files updated for new FirebaseAuthentication constructor

## Risks / Notes
- Existing users with empty Profile.displayName will be backfilled on their next login
- If a user never set their Firebase displayName (e.g., registered through Android without name field), the fallback chain still works: Profile.displayName → email-derived name
- Already-issued certificates with email-derived snapshot names will NOT be retroactively fixed (snapshots are immutable by design)
