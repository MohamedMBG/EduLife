# Certificate verify rate limit + Android session hardening

## Goal

Close two real gaps surfaced by the 2026-06-05 cross-cutting audit:

1. The public `GET /api/v1/certificates/verify/{hash}` endpoint had no rate limit, so
   verification hashes were enumerable by unauthenticated clients.
2. The Android session prefs were stored in plain `SharedPreferences`, allowing the
   persisted EduLife identity (userId + role) to be read by a forensic image of the
   device or restored onto a different device through cloud backup.

A secondary check against the same audit found that the other "CRITICAL" items
(lesson complete endpoint, lesson scope check, progress ownership check,
email_verified gate on `/auth/sync`, ExamStatusDto cooldown fields) were already
implemented, so no work was done there.

## What Changed

### Backend
- `config/RateLimitFilter.java`: added a 30/minute per-IP bucket for
  `GET /api/v1/certificates/verify/{hash}`. The bucket key is sourced from the first
  `X-Forwarded-For` hop and falls back to `request.getRemoteAddr()` when the header is
  absent. The filter previously short-circuited on non-POST requests; that branch was
  reordered so cert verify is evaluated before the POST gate.
- `certificates/CertificateControllerTest.java`: added
  `verifyCertificateRateLimitedAfterBudgetExhausted` which exhausts the 30-request
  budget for a unique forwarded IP and asserts the 31st request returns 429 with the
  shared API error contract.

### Android
- `app/build.gradle.kts`: added `androidx.security:security-crypto:1.1.0-alpha06`.
- `core/storage/SessionStorage.java`: rewrote the prefs initialisation to use
  `EncryptedSharedPreferences` with an AES256-GCM master key bound to the Android
  Keystore. The file moved from `edulife_session` to `edulife_session_secure` so the
  legacy plaintext XML format does not collide with the new encrypted format. On first
  launch the constructor deletes the legacy file; existing users transparently re-run
  `/auth/sync` on next launch. A keystore-corruption fallback recreates the encrypted
  file rather than crashing the process.
- `res/xml/network_security_config.xml`: new file. Default base-config blocks
  cleartext; an explicit domain-config keeps `10.0.2.2`, `localhost`, `127.0.0.1`
  reachable in cleartext so debug builds against the emulator still work. SSL pinning
  is deferred until the production cert chain is finalised.
- `AndroidManifest.xml`: wired `android:networkSecurityConfig="@xml/network_security_config"`.
- `res/xml/backup_rules.xml`: excludes both the new encrypted prefs file and the
  legacy plaintext file from API < 31 Auto Backup.
- `res/xml/data_extraction_rules.xml`: same exclusions for API 31+ cloud-backup and
  device-transfer flows.

## Files Touched

- `backend/src/main/java/com/edulife/config/RateLimitFilter.java`
- `backend/src/test/java/com/edulife/certificates/CertificateControllerTest.java`
- `app/build.gradle.kts`
- `app/src/main/java/com/baghdad/edulife/core/storage/SessionStorage.java`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/xml/network_security_config.xml` (new)
- `app/src/main/res/xml/backup_rules.xml`
- `app/src/main/res/xml/data_extraction_rules.xml`

## Backend Impact

- New rate-limit bucket. In-memory only, single-instance only. The existing
  ConcurrentHashMap-backed Bucket4j store handles it without a schema change.
- Filter ordering unchanged: still runs after `FirebaseTokenFilter` via
  `addFilterAfter`. The cert verify path bypasses authentication via
  `requestMatchers("/api/v1/certificates/verify/**").permitAll()`, which means
  `SecurityContext` is empty when the bucket is resolved; that is why the path uses an
  IP key rather than a principal key.
- No Flyway migration; the audit's proposed `enrollments(user_id, course_id, status)`
  composite index was dropped because the existing
  `UNIQUE (user_id, course_id)` constraint already provides an implicit btree that
  fully serves `existsByUserIdAndCourseIdAndStatus`.

## Android Impact

- Existing installs that already have a `userId` + `role` in the legacy prefs file
  will transparently re-run `/auth/sync` on next launch. No re-login required because
  Firebase auth state is persisted separately by the Firebase SDK.
- minSdk stays at 24; `androidx.security:security-crypto` 1.1.0-alpha06 supports the
  full range.
- Debug build connectivity to `10.0.2.2:8080` preserved via the network security
  config domain exception.
- Existing cloud backups containing the old plaintext prefs will be ignored on
  restore because the exclusion rule covers both filenames.

## Web Impact

None.

## Architecture Compliance

- Backend service-layer rule respected: filter logic stays in the existing
  `config/RateLimitFilter`; no controller or service changes.
- Android networking and storage layers stay in `core/`; no Fragment touches.
- No new dependencies on the backend; one Android dependency added with rationale
  comment.
- No `ddl-auto` use, no Flyway edits, no entity exposure changes.

## Tests / Verification

- New backend test asserts 30 successful verify calls then a 429 on the 31st for a
  given forwarded IP.
- Backend build expected to pass via `./mvnw test`.
- Android build expected to pass via Android Studio sync + `gradlew assembleDebug`.
- Manual verification of session migration: install previous build, log in, upgrade
  to this build, confirm the app re-runs `/auth/sync` without forcing a logout, and
  confirm the new encrypted prefs file appears under
  `/data/data/com.baghdad.edulife/shared_prefs/edulife_session_secure.xml`.

## Risks / Notes

- The per-IP cert verify bucket is in-memory and per-instance. When the backend moves
  behind multiple instances the bucket key collisions will reset between hops; that is
  acceptable for the MVP because the certificate domain hosts a single Spring instance
  today. Migrating to `bucket4j-redis` is the same swap noted at the top of
  `RateLimitFilter`.
- SSL pinning is intentionally not configured yet. Add a `<pin-set>` to
  `network_security_config.xml` once the production backend hostname and cert chain
  are committed; rotation policy must include a backup pin to avoid bricking installed
  apps when the cert is renewed.
- The keystore-corruption fallback in `SessionStorage` silently recreates the prefs
  file. This is the standard pattern from the AndroidX docs and avoids ANRs on launch,
  but it means a clearing event will look identical to a fresh install in telemetry.
- Audit doc location follows CLAUDE.md naming (`docs/YYYY-MM-DD-*.md`). The earlier
  audit's "docs are disorganised" Low item still applies and will be addressed in a
  separate cleanup pass.
