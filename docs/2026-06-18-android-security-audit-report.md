# EduLife Android Security Audit Report

## Executive Summary

Overall Android risk level: **High**.

The app has several strong security controls already: Firebase tokens are not stored in `SessionStorage`, local session identifiers use `EncryptedSharedPreferences`, Retrofit requests attach Firebase bearer tokens centrally, OkHttp redacts the `Authorization` header in current code, release minification is enabled, the manifest exposes only the launcher activity, and exam submissions send only `questionId` plus `choiceId`.

The highest-risk issues are practical rather than theoretical:

1. Tracked `.logcat` files contain learner email, Firebase UID, local HTTP auth traffic, and historical bearer tokens.
2. A release build can silently use the default local cleartext API URL because the default base URL is `http://10.0.2.2:8080/api/v1/` and the main network config permits local cleartext hosts.
3. Failed backend `/auth/sync` paths do not clear a previously saved `userId` and role, so stale local role state can influence launch routing.
4. Certificate downloads bypass the OkHttp token-refresh path and write private certificate PDFs to public Downloads.
5. Lesson WebView/resource handling accepts broad external URLs, enables JavaScript, and sends PDF URLs to Google Docs Viewer.

Fix before presentation/demo:

- Remove or redact tracked `.logcat` files and add `*.logcat` to `.gitignore`.
- Make release builds fail unless `API_BASE_URL` is HTTPS.
- Disable release OkHttp logging completely.
- Clear local session state on every backend auth sync failure.
- Move certificate downloads to app-private storage or require explicit export/share.

Fix before production:

- Add a strict release network security config with no cleartext overrides.
- Replace or tightly verify JitPack dependencies.
- Add Android-side tests for stale-session, token refresh, logout, and release config.
- Add WebView URL allowlisting and avoid third-party PDF viewers for protected resources.
- Decide whether backup should be disabled entirely for MVP or changed to a strict include-list.

## Scope

Scope reviewed:

- `app/src/main/AndroidManifest.xml`
- `app/src/debug/AndroidManifest.xml`
- `app/src/main/res/xml/network_security_config.xml`
- `app/src/debug/res/xml/debug_network_security_config.xml`
- `app/src/main/res/xml/backup_rules.xml`
- `app/src/main/res/xml/data_extraction_rules.xml`
- `app/build.gradle.kts`
- `settings.gradle.kts`
- `gradle/libs.versions.toml`
- `app/proguard-rules.pro`
- Android Java source under `app/src/main/java/com/baghdad/edulife`
- Navigation graph under `app/src/main/res/navigation/nav_graph.xml`
- Tracked Android logcat files in repository root

Limitations:

- No code changes were made.
- No destructive tests were run.
- No production APIs were contacted.
- Findings are based on static code review and repository evidence.
- Backend authorization is treated separately. Any control that must be enforced by the backend is called out as backend-side.

## Top Findings

| ID | Severity | Finding | OWASP |
|---|---|---|---|
| F1 | High | Tracked logcat files leak Firebase token material and learner identifiers | M1, M6 |
| F2 | High | Release builds can silently use cleartext local API traffic | M5, M8 |
| F3 | High | Failed backend auth sync does not clear stale local session state | M3, M9 |
| F4 | Medium | Certificate downloads bypass retry-on-401 and write PDFs to public Downloads | M3, M9 |
| F5 | Medium | Lesson WebView/resource handling allows broad external and cleartext content | M4, M5, M6 |
| F6 | Medium | Protected PDF lesson URLs are disclosed to Google Docs Viewer | M6, M9 |
| F7 | Medium | Release OkHttp logging still logs request URLs/status lines | M6, M7 |
| F8 | Medium | Backup is enabled with narrow exclusions only | M6, M9 |
| F9 | Medium | JitPack dependency and missing dependency verification create supply-chain risk | M2 |
| F10 | Low | Avatar upload decodes selected images at full size before bounding dimensions | M4 |
| F11 | Low | Broad R8 keep rules reduce binary hardening | M7 |
| F12 | Needs verification | Email verification appears backend-dependent during login | M3 |

## Detailed Findings

### F1 - Tracked logcat files leak Firebase token material and learner identifiers

Severity: **High**

Evidence:

- `samsung-SM-F936B-Android-16_2026-05-19_120851.logcat:101468`
- `samsung-SM-F936B-Android-16_2026-05-19_120851.logcat:107333`
- `samsung-SM-F936B-Android-16_2026-05-19_120851.logcat:98843`
- `login_issues.logcat:54863`
- `login_issues.logcat:57023`
- `.gitignore:47-49`

The repository tracks two `.logcat` files. One contains full `Authorization: Bearer ...` Firebase ID tokens, learner email, Firebase UID, and local auth-sync traffic. The `.gitignore` excludes `*.log` but not `*.logcat`, so the same class of leakage can recur.

Risk:

- Historical ID tokens are probably expired, but the files still expose learner PII and Firebase UID values.
- Fresh logs generated during demo/debug sessions could expose live bearer tokens.
- Committed logs may be copied into reports or shared repositories.

Remediation:

- Remove the tracked `.logcat` files from the repository.
- Add `*.logcat` and common crash-dump extensions to `.gitignore`.
- Rotate or revoke any exposed sessions if logs were shared outside the local machine.
- Keep OkHttp redaction enabled and never collect debug BODY logs into committed artifacts.

### F2 - Release builds can silently use cleartext local API traffic

Severity: **High**

Evidence:

- `app/build.gradle.kts:15-17` builds `API_BASE_URL`, defaulting to `http://10.0.2.2:8080/api/v1/`.
- `app/build.gradle.kts:42` injects that value into `BuildConfig.API_BASE_URL`.
- `app/src/main/res/xml/network_security_config.xml:17` disables cleartext by default.
- `app/src/main/res/xml/network_security_config.xml:25-29` re-allows cleartext for `10.0.2.2`, `localhost`, and `127.0.0.1` in the main config.
- `app/src/debug/res/xml/debug_network_security_config.xml:4` separately allows debug cleartext.
- `app/src/main/java/com/baghdad/edulife/core/network/ApiClient.java:15` uses the injected base URL.

Risk:

If a release is built without an HTTPS override, authenticated API traffic can target local cleartext endpoints. This is especially dangerous because Firebase bearer tokens are attached to protected Retrofit requests.

Remediation:

- For `release`, fail the Gradle build if `edulife.apiBaseUrl` is missing or not HTTPS.
- Move all local cleartext host exceptions out of `main` and into `debug`.
- Keep `main` network security HTTPS-only.
- Add a release unit/config test that asserts `BuildConfig.API_BASE_URL.startsWith("https://")`.

### F3 - Failed backend auth sync does not clear stale local session state

Severity: **High**

Evidence:

- `app/src/main/java/com/baghdad/edulife/features/auth/data/AuthRepository.java:163-230` handles `/auth/sync`.
- `AuthRepository.java:202-208` saves the session on authenticated sync.
- `AuthRepository.java:209-210` returns an error on failed sync without clearing session storage.
- `AuthRepository.java:174-183` handles timeout without clearing session storage.
- `AuthRepository.java:222-227` handles network failure without clearing session storage.
- `app/src/main/java/com/baghdad/edulife/core/storage/SessionStorage.java:154-155` treats non-null `userId` and role as a session.
- `app/src/main/java/com/baghdad/edulife/MainActivity.java:133-151` starts authenticated screens when Firebase has a user and `SessionStorage.hasSession()` is true.

Risk:

The login UI does not navigate forward when sync fails, which is good. The problem is stale state across relaunches. If a previous user/session left a stored `userId` and role, and a later Firebase session exists while backend sync fails, `MainActivity` can route from stale local role state.

Backend RBAC should still reject unauthorized API calls, so this is not a complete server-side authorization bypass. It is a client fail-closed violation and can expose wrong UI flows or confusing privileged navigation.

Remediation:

- Call `sessionStorage.clearSession()` on every `/auth/sync` failure, timeout, missing token, and incomplete response path.
- Consider signing out Firebase on sync failure when the app cannot establish the backend identity.
- Add tests for: previous admin session -> sync failure -> relaunch must show login, not admin dashboard.

### F4 - Certificate downloads bypass retry-on-401 and write PDFs to public Downloads

Severity: **Medium**

Evidence:

- `app/src/main/java/com/baghdad/edulife/features/certificates/ui/CertificatesFragment.java:114-127` manually fetches cached token and adds `Authorization`.
- `CertificatesFragment.java:129` writes to `Environment.DIRECTORY_DOWNLOADS`.
- `app/src/main/java/com/baghdad/edulife/features/certificates/ui/CertificateDetailFragment.java:130-142` manually fetches cached token and adds `Authorization`.
- `CertificateDetailFragment.java:146` writes to `Environment.DIRECTORY_DOWNLOADS`.
- `CertificateDetailFragment.java:160-175` shares certificate metadata.
- `app/src/main/java/com/baghdad/edulife/core/network/FirebaseTokenAuthenticator.java:35-73` implements retry-once only for OkHttp calls.

Risk:

`DownloadManager` does not use the app's OkHttp authenticator, so an expired cached token will not follow the documented forced-refresh retry path. Downloaded PDFs are saved to public Downloads and may include learner name, course title, teacher, certificate number, and verification hash.

Remediation:

- Download with OkHttp/Retrofit so `FirebaseTokenAuthenticator` handles one forced refresh and retry.
- Store PDFs in app-private storage first.
- Use `FileProvider` for explicit open/share grants.
- Only export to public Downloads after user confirmation.

### F5 - Lesson WebView/resource handling allows broad external and cleartext content

Severity: **Medium**

Evidence:

- `app/src/main/java/com/baghdad/edulife/features/courses/ui/LessonPlayerFragment.java:360-370` accepts existing `http://` URLs and sends them via `ACTION_VIEW`.
- `LessonPlayerFragment.java:381-389` enables JavaScript and DOM storage in WebView.
- `LessonPlayerFragment.java:391-396` only blocks non-http/non-https schemes.
- `LessonPlayerFragment.java:459` loads backend-provided lesson URLs into WebView.

Risk:

Course content URLs are backend-provided but can represent teacher/admin-controlled content. Without a scheme and host allowlist, learners can be sent to cleartext or attacker-controlled content. With JavaScript enabled in the in-app viewer, this also increases phishing and content-tampering risk.

Remediation:

- Reject or warn on `http://` lesson/resource URLs.
- Allowlist trusted hosts for in-app WebView rendering.
- Disable JavaScript unless the trusted content type requires it.
- Use a browser/custom tab for arbitrary external links rather than an in-app WebView.
- Add tests for `http://`, `file://`, `intent://`, and untrusted hosts.

### F6 - Protected PDF lesson URLs are disclosed to Google Docs Viewer

Severity: **Medium**

Evidence:

- `app/src/main/java/com/baghdad/edulife/features/courses/model/LessonContentTypeResolver.java:118-125` wraps PDF URLs in `https://docs.google.com/gview?embedded=true&url=...`.
- `app/src/main/java/com/baghdad/edulife/features/courses/ui/LessonPlayerFragment.java:459` loads the resolved URL.
- `app/src/main/java/com/baghdad/edulife/core/network/ApiService.java:135-139` loads protected lesson details.
- `app/src/main/java/com/baghdad/edulife/features/courses/model/LessonDetail.java:15` carries `contentUrl`.

Risk:

If lesson PDF URLs are enrolled-only, signed, or otherwise private, sending them to Google Docs Viewer leaks the URL to a third-party service and may allow fetches outside EduLife's authenticated resource flow.

Remediation:

- Use an in-app PDF renderer or app-authenticated download-to-private-cache flow.
- Do not pass private signed URLs to third-party viewers.
- If third-party viewing remains a deliberate product choice, clearly classify PDF URLs as public and document that privacy tradeoff.

### F7 - Release OkHttp logging still logs request URLs/status lines

Severity: **Medium**

Evidence:

- `app/src/main/java/com/baghdad/edulife/core/network/ApiClient.java:29-33` sets BODY logging in debug and BASIC logging in release.
- `ApiClient.java:30` redacts `Authorization`, which is good.

Risk:

Release BASIC logging can still expose URLs, endpoint paths, status codes, and timing to logcat or crash/log collectors. For learner exams, certificates, admin, and analytics endpoints, even metadata can be sensitive.

Remediation:

- Use `HttpLoggingInterceptor.Level.NONE` in release.
- Prefer attaching the logging interceptor only when `BuildConfig.DEBUG` is true.
- Add a release build test or static check that fails if release logging is not `NONE`.

### F8 - Backup is enabled with narrow exclusions only

Severity: **Medium**

Evidence:

- `app/src/main/AndroidManifest.xml:9` sets `android:allowBackup="true"`.
- `app/src/main/res/xml/backup_rules.xml:14-15` excludes only secure and legacy session prefs.
- `app/src/main/res/xml/data_extraction_rules.xml:11-16` excludes only secure and legacy session prefs.
- `app/src/main/java/com/baghdad/edulife/features/courses/data/PlannerPreferences.java:38` stores study planner data in plain `SharedPreferences`.

Risk:

Session prefs are excluded, which is good. But future or current non-session learner data, planner goals, caches, profile state, or downloaded artifacts can become backup-eligible by default. This is a privacy risk for educational progress and profile data.

Remediation:

- For MVP, consider `android:allowBackup="false"`.
- If backups are required, use a strict include-list rather than narrow exclusions.
- Revisit backup rules whenever new local storage is added.

### F9 - JitPack dependency and missing dependency verification create supply-chain risk

Severity: **Medium**

Evidence:

- `settings.gradle.kts:22` globally enables `https://jitpack.io`.
- `app/build.gradle.kts:113` depends on `com.github.ibrahimsn98:SmoothBottomBar:1.7.9`.
- No Gradle dependency verification metadata was found under `gradle/`.

Risk:

JitPack builds dependencies from GitHub repositories. Without content filtering and dependency verification, the build trusts a broader supply-chain surface than necessary.

Remediation:

- Add a repository content filter for the exact dependency group.
- Enable Gradle dependency verification.
- Prefer a Maven Central dependency or Material component if feasible.

### F10 - Avatar upload decodes selected images at full size before bounding dimensions

Severity: **Low**

Evidence:

- `app/src/main/java/com/baghdad/edulife/features/profile/ui/ProfileFragment.java:108-113` uses the image-only Photo Picker.
- `ProfileFragment.java:164-170` opens the selected URI and calls `BitmapFactory.decodeStream`.
- `ProfileFragment.java:177` scales after full decode.
- `ProfileFragment.java:179` writes compressed output to app cache.

Risk:

The Photo Picker limits the source to images, but a very large image can still trigger high memory usage before scaling. This is mostly availability/reliability risk, not a data exposure issue.

Remediation:

- Decode bounds first with `BitmapFactory.Options.inJustDecodeBounds`.
- Calculate `inSampleSize` before allocating the bitmap.
- Enforce a maximum byte size and pixel count before upload.

### F11 - Broad R8 keep rules reduce binary hardening

Severity: **Low**

Evidence:

- `app/proguard-rules.pro:24` keeps all `retrofit2.**`.
- `app/proguard-rules.pro:47-48` keeps all feature model and DTO classes.
- `app/build.gradle.kts:57-63` enables release minification and resource shrinking, which is good.

Risk:

Release builds are minified, but broad keep rules preserve API and model structure. This does not create a direct vulnerability, but it weakens reverse-engineering resistance.

Remediation:

- Add `@SerializedName` to DTO fields and keep only needed members.
- Narrow Retrofit/Gson keep rules to documented minimums.
- Verify release serialization after tightening rules.

### F12 - Email verification appears backend-dependent during login

Severity: **Needs verification**

Evidence:

- `app/src/main/java/com/baghdad/edulife/features/auth/data/AuthRepository.java:116-119` defines `isCurrentUserEmailVerified()`.
- `AuthRepository.java:99-113` returns login success after Firebase sign-in without checking `isEmailVerified()`.
- `app/src/main/java/com/baghdad/edulife/features/auth/viewmodel/AuthViewModel.java:54-66` then calls backend sync before UI success.

Risk:

This is acceptable only if backend `/auth/sync` rejects unverified Firebase users every time. Android does not enforce it locally before sync.

Remediation:

- Verify backend `/api/v1/auth/sync` rejects unverified `email_verified=false`.
- Optionally add an Android pre-check to block sync/navigation for unverified users, while keeping backend as source of truth.

## OWASP Mobile Top 10 2024 Table

| OWASP category | Status | Evidence from code | Risk explanation | Severity | Fix recommendation |
|---|---|---|---|---|---|
| M1: Improper Credential Usage | Fail | `samsung-SM-F936B-Android-16_2026-05-19_120851.logcat:101468`, `SessionStorage.java:20-24`, `ApiClient.java:30` | Session storage avoids token persistence, but tracked logs include bearer token material and learner identifiers. | High | Remove/redact logs, ignore `*.logcat`, keep redaction, avoid committing debug logs. |
| M2: Inadequate Supply Chain Security | Partial | `settings.gradle.kts:22`, `app/build.gradle.kts:113`, `app/build.gradle.kts:124` | JitPack is globally trusted and no dependency verification metadata was found. `security-crypto` is alpha/deprecated lifecycle risk. | Medium | Content-filter JitPack, enable dependency verification, replace or justify risky dependencies. |
| M3: Insecure Authentication/Authorization | Partial | `AuthRepository.java:209-227`, `MainActivity.java:133-151`, `LoginFragment.java:111-125`, `ApiService.java:246-342` | Backend sync is required before UI success, but failed sync does not clear stale local role. Role authorization must remain backend-enforced. | High | Clear session on sync failure and add role/navigation tests. Verify backend RBAC and email verification. |
| M4: Insufficient Input/Output Validation | Partial | `LessonPlayerFragment.java:360-370`, `LessonPlayerFragment.java:391-396`, `ProfileFragment.java:164-177`, `RegisterFragment.java:121-167` | Auth forms validate basics; lesson URLs and image decode need stricter validation. | Medium | Add URL allowlists, reject cleartext, bounds-decode images, cap advisor input. |
| M5: Insecure Communication | Fail | `app/build.gradle.kts:16`, `network_security_config.xml:25-29`, `ApiClient.java:15`, `CertificatesFragment.java:118-127` | Release can default to local HTTP and main network config allows local cleartext hosts. | High | Enforce HTTPS in release and move cleartext exceptions to debug only. |
| M6: Inadequate Privacy Controls | Partial | `.logcat` token/email lines, `CertificateDetailFragment.java:160-175`, `CertificatesFragment.java:129`, `AndroidManifest.xml:9` | Logs and public certificate downloads expose learner identity data. Screenshots on sensitive screens need verification. | High | Remove logs, use private downloads, review screenshot policy, minimize analytics/advisor logs. |
| M7: Insufficient Binary Protections | Partial | `app/build.gradle.kts:57-63`, `proguard-rules.pro:24`, `proguard-rules.pro:47-48`, `ApiClient.java:31-33` | Release minification is enabled, but broad keep rules and release BASIC logging reduce hardening. | Low/Medium | Disable release logging and narrow keep rules. |
| M8: Security Misconfiguration | Partial | `AndroidManifest.xml:18-20`, `network_security_config.xml:25-29`, `debug_network_security_config.xml:4`, `LessonPlayerFragment.java:381` | Manifest exposure is minimal, but main network config includes dev cleartext and WebView JS is broad. | Medium | Strict main network config, debug-only overrides, safer WebView defaults. |
| M9: Insecure Data Storage | Partial | `SessionStorage.java:65-77`, `PlannerPreferences.java:38`, `CertificatesFragment.java:129`, `CertificateDetailFragment.java:146` | Session storage is encrypted; planner data is plain local prefs and certificates are public downloads. | Medium | Keep sensitive storage encrypted/private and use explicit share/export for certificates. |
| M10: Insufficient Cryptography | Pass/Needs verification | `SessionStorage.java:67-77`, `LessonContentTypeResolver.java:118-125`, no weak crypto hits found | No custom weak crypto was found. Certificate verification hash handling is backend-owned. | Low | Avoid custom crypto; verify certificate hashes server-side; plan migration from deprecated crypto APIs. |

## Prioritized Fix Backlog

### P0 Critical - fix immediately

- Remove/redact `login_issues.logcat` and `samsung-SM-F936B-Android-16_2026-05-19_120851.logcat`.
- Add `*.logcat` to `.gitignore`.
- Rotate/revoke any credentials or sessions if these logs were shared.

### P1 High - fix before demo/presentation

- Make release builds require an HTTPS API URL.
- Move local cleartext network config into debug only.
- Disable release OkHttp logging.
- Clear `SessionStorage` on any backend auth sync failure, timeout, missing token, or incomplete response.

### P2 Medium - fix before production

- Rework certificate downloads through OkHttp/private storage/FileProvider.
- Add WebView and external resource URL allowlists.
- Stop using Google Docs Viewer for protected PDFs.
- Disable backup or switch to strict include-list backup rules.
- Add dependency verification and JitPack content filters.
- Verify backend `/auth/sync` rejects unverified emails.

### P3 Low - hardening/improvement

- Bounds-decode avatar images.
- Narrow R8 keep rules.
- Add screenshot protection for sensitive screens if product requires it.
- Add max length and client-side guardrails for advisor prompts, while keeping backend enforcement.

## Exact Files To Inspect/Fix

- `.gitignore`
- `login_issues.logcat`
- `samsung-SM-F936B-Android-16_2026-05-19_120851.logcat`
- `app/build.gradle.kts`
- `app/src/main/res/xml/network_security_config.xml`
- `app/src/debug/res/xml/debug_network_security_config.xml`
- `app/src/main/java/com/baghdad/edulife/core/network/ApiClient.java`
- `app/src/main/java/com/baghdad/edulife/features/auth/data/AuthRepository.java`
- `app/src/main/java/com/baghdad/edulife/MainActivity.java`
- `app/src/main/java/com/baghdad/edulife/features/certificates/ui/CertificatesFragment.java`
- `app/src/main/java/com/baghdad/edulife/features/certificates/ui/CertificateDetailFragment.java`
- `app/src/main/java/com/baghdad/edulife/features/courses/ui/LessonPlayerFragment.java`
- `app/src/main/java/com/baghdad/edulife/features/courses/model/LessonContentTypeResolver.java`
- `app/src/main/java/com/baghdad/edulife/features/profile/ui/ProfileFragment.java`
- `app/src/main/res/xml/backup_rules.xml`
- `app/src/main/res/xml/data_extraction_rules.xml`
- `settings.gradle.kts`
- `app/proguard-rules.pro`

## Safe Remediation Plan

1. Hygiene first: remove leaked logs from the working tree and future commits. This is safe and does not touch runtime behavior.
2. Build safety second: enforce HTTPS for release and disable release logging. This prevents accidental insecure artifacts.
3. Auth fail-closed third: clear local session data on failed backend sync. Keep backend RBAC as the real authorization boundary.
4. File/privacy fourth: move certificates to private storage and gate public export behind explicit user action.
5. Content rendering fifth: add URL allowlists and replace third-party PDF viewing for protected content.
6. Supply chain and hardening last: add dependency verification, tighten ProGuard rules, and review backup policy.

No remediation step requires attacking real systems, contacting production APIs, or destructive testing.

## Android-Specific Test Checklist

- Failed backend sync after Firebase login:
  - Given an existing stored `userId/role`, force `/auth/sync` to fail.
  - Assert `SessionStorage.hasSession()` becomes false.
  - Relaunch app and assert login/onboarding screen, not role dashboard.
- Email not verified:
  - Use a Firebase user with `email_verified=false`.
  - Assert backend sync rejects access.
  - Optionally assert Android blocks before sync if local pre-check is added.
- Expired token and retry once:
  - Mock first API response as `401`.
  - Assert `FirebaseTokenAuthenticator` calls forced refresh once.
  - Assert second `401` forces session-expired handling.
- Logout clears session and back stack:
  - Sign in, navigate to a protected screen, log out.
  - Assert Firebase sign-out and `SessionStorage.clearSession()`.
  - Press back and assert protected screen is not reachable.
- Role navigation blocked:
  - Tamper local role to `ADMIN` while backend token is learner.
  - Assert admin API calls return `403`.
  - Add client-side guard if desired, but backend must remain source of truth.
- Release build has no cleartext/debug logs:
  - Build release without `edulife.apiBaseUrl`; assert build fails.
  - Build release with HTTPS URL; assert network config has no cleartext domain override.
  - Assert logging interceptor is absent or `NONE`.
- No sensitive data in logs:
  - Run login/sync in debug and release.
  - Assert no bearer tokens, emails, UIDs, profile payloads, exam answers, or certificate IDs appear in collected logs.
- No public external storage for private files:
  - Download certificate.
  - Assert file is app-private unless user explicitly exports it.
- WebView/file URL safety:
  - Test `http://`, `https://trusted`, `file://`, `content://`, `intent://`, `javascript:`, and untrusted host URLs.
  - Assert only approved HTTPS URLs are rendered in-app.

## What Is Safe Already

- `SessionStorage` uses `EncryptedSharedPreferences` with AES-backed Android Keystore material.
- Firebase ID tokens, refresh tokens, and passwords are not stored in local session prefs.
- Retrofit calls use `FirebaseAuthInterceptor`.
- OkHttp `Authorization` header redaction exists in current code.
- `FirebaseTokenAuthenticator` bounds token refresh to one retry and serializes refreshes with a lock.
- Release minification and resource shrinking are enabled.
- Manifest permissions are minimal: only `INTERNET`.
- No exported deep links were found.
- Exam answer models do not include correct-answer fields.
- Account deletion flow exists and signs out after deletion.

## Backend-Side Assumptions

These cannot be proven from Android alone:

- Every protected backend endpoint validates the Firebase ID token.
- Backend `/auth/sync` rejects unverified email users.
- Backend never exposes `firebase_uid` in API responses.
- Backend RBAC prevents learners from accessing other learners' progress, attempts, certificates, analytics, and private data.
- Teacher, group admin, and platform admin APIs are scoped server-side.
- Exam scoring, cooldown, pass/fail, and certificate issuance are server-side only.
- Certificate verification/download endpoints do not leak private learner data.

## Final Jury-Ready Summary

EduLife Android already follows several important mobile security practices: Firebase is used for identity, backend sync is required before normal login success, tokens are attached centrally to Retrofit requests, session identifiers are stored with encrypted preferences, release builds are minified, and exam answers are not trusted from the client. The audit identified practical hardening work before demo and production: remove leaked logcat files, enforce HTTPS-only release builds, clear stale session data on failed backend sync, keep certificate PDFs private by default, and restrict lesson WebView/resource URLs. These changes keep the learner flow intact while aligning the app with OWASP Mobile Top 10 2024 expectations.

