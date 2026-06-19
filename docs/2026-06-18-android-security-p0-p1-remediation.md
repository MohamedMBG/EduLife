# Task Audit - Android Security P0 P1 Remediation

## Date
2026-06-18

## Task Summary
Implemented the immediate Android security remediation items from the audit report: removed leaked logcat artifacts, blocked repeat logcat commits, made release API configuration require an explicit HTTPS URL, moved local cleartext traffic to debug-only network config, disabled release OkHttp logging, and cleared stale local backend identity on auth sync failure.

## Files Created
- docs/2026-06-18-android-security-p0-p1-remediation.md

## Files Modified
- .gitignore
- app/build.gradle.kts
- app/src/main/res/xml/network_security_config.xml
- app/src/debug/res/xml/debug_network_security_config.xml
- app/src/main/java/com/baghdad/edulife/core/network/ApiClient.java
- app/src/main/java/com/baghdad/edulife/core/storage/SessionStorage.java
- app/src/main/java/com/baghdad/edulife/features/auth/data/AuthRepository.java
- app/src/test/java/com/baghdad/edulife/core/storage/SessionStorageTest.java

## Files Deleted
- login_issues.logcat
- samsung-SM-F936B-Android-16_2026-05-19_120851.logcat

## What Was Done
Removed tracked Android logcat files that contained sensitive learner and authentication evidence, then added `*.logcat` to `.gitignore` so the same artifact type is not committed again.

Updated Gradle API URL handling so debug builds can still use `local.properties` or the emulator default `http://10.0.2.2:8080/api/v1/`, while release-oriented Gradle task requests fail unless `edulife.apiBaseUrl` is passed explicitly with `-Pedulife.apiBaseUrl=` and resolves to an HTTPS URL.

Removed local cleartext domain exceptions from the main network security config. The debug network security config now keeps cleartext disabled by default and allows only `10.0.2.2`, `localhost`, and `127.0.0.1` for local Spring Boot development.

Changed `ApiClient` so `HttpLoggingInterceptor` is attached only in debug builds. Release builds no longer log request URLs, status lines, or timing metadata through OkHttp logging.

Added `SessionStorage.clearAuthenticatedSession()` to remove only the backend-synced `userId` and `role`, preserving the pending registration role needed for first-sync retries. Updated `AuthRepository` so every backend identity sync failure path clears the authenticated local identity before returning an error.

Added a unit test proving stale authenticated identity is removed while pending registration role is preserved.

## Architecture Compliance
The changes stay within the existing Android architecture:

- Network configuration remains in Gradle, Android XML network config, and `core/network`.
- Session persistence stays centralized in `core/storage/SessionStorage`.
- Auth sync behavior remains in `features/auth/data/AuthRepository`.
- Tests were added beside the existing storage tests.

No new architecture layer, backend feature, CMS work, or deferred MVP feature was introduced.

## Code Comments Added
Added comments explaining why release builds reject non-HTTPS API URLs, why release builds do not attach the network logger, why debug cleartext is scoped to local development hosts, and why auth sync failure clears only the backend identity rather than Firebase auth or pending registration role.

## Validation / Testing
Ran `.\gradlew.bat :app:testDebugUnitTest --tests com.baghdad.edulife.core.storage.SessionStorageTest`; it passed.

The broader `.\gradlew.bat :app:testDebugUnitTest` generated passing XML result files for the available unit tests but exceeded the command timeout before returning a shell exit code.

Ran `git diff --check`; it passed.

Ran static checks with `rg` to confirm:

- no `HttpLoggingInterceptor.Level.BASIC` release logging remains in app source,
- the only `cleartextTrafficPermitted="true"` allowlist is under `app/src/debug`,
- `*.logcat` is ignored,
- auth sync failure calls `clearAuthenticatedSession()`.

Completed the final P0/P1 verification pass:

- `git ls-files "*.logcat"` returned no output after the deleted logcat paths were removed from the Git index.
- Created a temporary `codex-ignore-check.logcat`; `git status --short -- codex-ignore-check.logcat` returned no output, then the temporary file was deleted.
- `.\gradlew.bat :app:assembleRelease` failed in 11 seconds with: `Release builds require edulife.apiBaseUrl. Pass -Pedulife.apiBaseUrl=https://your-api.example/api/v1/.`
- `.\gradlew.bat :app:assembleRelease "-Pedulife.apiBaseUrl=http://example.com/api/v1/"` failed in 1 second with: `Release builds must set edulife.apiBaseUrl to an HTTPS endpoint.`
- `.\gradlew.bat :app:assembleRelease "-Pedulife.apiBaseUrl=https://example.com/api/v1/"` passed in 1 minute 51 seconds and produced a release APK.
- `rg -n "HttpLoggingInterceptor|loggingInterceptor|Level\.BASIC|Level\.BODY|BuildConfig\.DEBUG" app/src/main/java/com/baghdad/edulife/core/network/ApiClient.java` showed the interceptor is created and attached only inside `if (BuildConfig.DEBUG)`.
- `rg -n "Level\.BASIC|HttpLoggingInterceptor\.Level\.BASIC" app/src/main app/src/debug` returned no matches.

## Risks / Notes
Deleting the logcat files removes them from the working tree and the next commit, but it does not erase sensitive data from existing Git history. If these files were pushed or shared, credentials/sessions should be rotated where applicable and history rewriting should be considered.

The P2/P3 audit items remain open: certificate downloads still need private storage/FileProvider handling, WebView and PDF resource handling still need allowlists or safer rendering, backups need a product decision, JitPack/dependency verification needs hardening, avatar decoding should be bounded, and ProGuard keep rules should be tightened.
