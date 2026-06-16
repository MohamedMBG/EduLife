# Task Audit - Dynamic Certificates

## Date
2026-06-16

## Task Summary
Fixed certificate generation and display so issued certificates use dynamic learner, teacher, course, level, issue date, certificate number, and verification hash data from backend relationships.

## Files Created
- backend/src/main/resources/db/migration/V24__certificate_dynamic_snapshots.sql
- backend/src/test/java/com/edulife/certificates/CertificateServiceTest.java
- backend/src/test/java/com/edulife/exams/ExamServiceCertificateTest.java
- docs/2026-06-16-dynamic-certificates.md

## Files Modified
- backend/src/main/java/com/edulife/certificates/entity/Certificate.java
- backend/src/main/java/com/edulife/certificates/service/CertificateService.java
- backend/src/main/java/com/edulife/certificates/dto/CertificateSummaryDto.java
- backend/src/main/java/com/edulife/certificates/dto/CertificateDetailDto.java
- backend/src/main/java/com/edulife/certificates/dto/CertificateVerificationDto.java
- backend/src/main/java/com/edulife/certificates/exception/CertificateGenerationException.java
- backend/src/main/java/com/edulife/exams/service/ExamService.java
- backend/src/main/resources/templates/certificate-academic.html
- backend/src/test/java/com/edulife/certificates/CertificateControllerTest.java
- app/src/main/java/com/baghdad/edulife/features/certificates/model/CertificateSummary.java
- app/src/main/java/com/baghdad/edulife/features/certificates/model/CertificateDetail.java
- app/src/main/java/com/baghdad/edulife/features/certificates/ui/CertificateAdapter.java
- app/src/main/java/com/baghdad/edulife/features/certificates/ui/CertificateDetailFragment.java
- app/src/main/res/layout/item_certificate.xml
- app/src/main/res/layout/fragment_certificate_detail.xml
- app/src/main/res/values/strings.xml

## What Was Done
Added certificate snapshot fields for learner name, teacher name, course title, and course level, and updated certificate generation to populate them from Profile and Course database relationships. Existing legacy certificate fields remain readable as fallbacks for old rows.

Updated certificate generation to fail with a controlled generation error when a course is missing an instructor, title, level, or resolvable learner/teacher identity instead of issuing certificates with generic placeholders.

Added a Flyway migration that creates snapshot columns, backfills existing rows from users/profiles/courses, and attaches existing seeded MVP catalog courses to a seeded instructor through `courses.created_by_user_id`.

Updated certificate API DTOs so list, detail, and public verification responses expose learner name, teacher name, course title, course level, issue date, certificate number, and verification hash.

Updated the PDF template and Android certificate list/detail screens to render the same dynamic certificate data and removed hardcoded certificate placeholder fallbacks from certificate-specific code.

## Architecture Compliance
The backend change stays inside the existing modular monolith certificate, exam, course, profile, and migration boundaries. Certificate business logic remains in `CertificateService`; exam submission remains in `ExamService`; controllers only expose service results.

The Android change stays inside the existing feature-first certificates module and uses the current Retrofit model/repository/viewmodel/UI pattern without adding new architecture layers.

## Code Comments Added
Added comments explaining why certificate snapshot fields exist, why certificates are issued only from the final-exam pass branch, and why the Android detail sentence mirrors the backend snapshot used by the PDF.

## Validation / Testing
Ran focused backend tests:

```text
./mvnw.cmd "-Dtest=CertificateServiceTest,ExamServiceCertificateTest,CertificateControllerTest" test
```

Result: 12 tests passed.

Ran Android build:

```text
./gradlew.bat :app:assembleDebug
```

Result: build passed.

Attempted full backend test suite:

```text
./mvnw.cmd clean test
```

Result: blocked by local Flyway validation because the local PostgreSQL database has an applied version 23 migration that is not present in the current source tree. No database repair was run.

## Risks / Notes
Existing environments with a historical migration version 23 in Flyway history may need their missing migration source restored or Flyway history repaired before running the full Spring Boot context tests.

Courses without `created_by_user_id`, missing course level, or missing learner/teacher profile display names will now fail certificate generation instead of issuing incomplete certificates.
