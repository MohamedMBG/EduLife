# EduLife Android Security — Post-Remediation Verification

## Date
2026-06-19

## Scope
Read-only verification pass over the P0/P1 and P2 Android security remediation work
documented in:

- `docs/2026-06-18-android-security-audit-report.md`
- `docs/2026-06-18-android-security-p0-p1-remediation.md`
- `docs/2026-06-18-android-security-p2-remediation.md`

No source code was modified during this pass. The goal was to confirm whether the
findings reported in the original OWASP Mobile Top 10 audit are now Fixed, Partially
Fixed, Still Open, or Needs Backend Verification, with file/line evidence and rerun
commands.

---

## Executive Summary

| Result | Findings |
|---|---|
| Fixed | F1, F2, F3, F4, F5, F6, F7, F8, F10 |
| Partially Fixed | F9, F11 |
| Needs Backend Verification | F12 |
| Still Open | (none) |

All P0/P1 items are Fixed. All P2 items that can be verified on the Android side are
Fixed. The remaining gaps are scoped, documented as deferred in the P2 remediation
audit, and not exploitable in the demo build:

- F9: JitPack is now content-filtered, but Gradle dependency verification
  metadata is still deferred.
- F11: Retrofit catch-all keep rule replaced with targeted rules; DTO/model keep
  rules remain broad until `@SerializedName` migration lands.
- F12: backend-side `email_verified` enforcement on `/auth/sync` cannot be proven
  from Android alone.

No new findings were discovered during this verification pass.

---

## Verification Commands Run

All commands executed at the repo root on Windows PowerShell. Static checks use the
Grep tool (ripgrep).

### Gradle

| Command | Expected | Actual |
|---|---|---|
| `.\gradlew.bat :app:testDebugUnitTest` | BUILD SUCCESSFUL | **BUILD SUCCESSFUL in 19s** |
| `.\gradlew.bat :app:assembleDebug` | BUILD SUCCESSFUL | **BUILD SUCCESSFUL in 4s** |
| `.\gradlew.bat :app:assembleRelease -Pedulife.apiBaseUrl=https://example.com/api/v1/` | BUILD SUCCESSFUL | **BUILD SUCCESSFUL in 9s** |
| `.\gradlew.bat :app:assembleRelease` (no URL) | FAIL with explicit message | **BUILD FAILED in 2s** — `Release builds require edulife.apiBaseUrl. Pass -Pedulife.apiBaseUrl=https://your-api.example/api/v1/.` |
| `.\gradlew.bat :app:assembleRelease -Pedulife.apiBaseUrl=http://example.com/api/v1/` | FAIL on HTTPS gate | **BUILD FAILED in 2s** — `Release builds must set edulife.apiBaseUrl to an HTTPS endpoint.` |

### Git / static checks

| Check | Expected | Result |
|---|---|---|
| `git ls-files "*.logcat"` | empty | **empty** |
| `.gitignore` contains `*.logcat` | yes | `.gitignore:49` — `*.logcat` |
| `Grep DownloadManager\|Environment.DIRECTORY_DOWNLOADS` (under `app/src`) | no live production references | only doc comments in `CertificatesFragment.java:108`, `CertificateDownloader.java:25`, `ApiService.java:206`. No live code references. |
| `Grep docs\.google\.com/gview` (under `app/src`) | no live production references | only doc comment in `LessonContentTypeResolver.java:112` and the assertion in `LessonContentTypeResolverTest.java:153`. No live code references. |
| `Grep cleartextTrafficPermitted="true"` (under `app/`) | only under `app/src/debug` | **only** `app/src/debug/res/xml/debug_network_security_config.xml:10`. |
| `Grep HttpLoggingInterceptor\.Level\.(BASIC\|BODY)` (under `app/src`) | only inside `if (BuildConfig.DEBUG)` in `ApiClient` | only `ApiClient.java:64` (`Level.BODY`) — surrounded by `if (BuildConfig.DEBUG)` block at `ApiClient.java:59-66`. No `Level.BASIC` anywhere. |
| `Grep allowBackup="true"` (under `app/`) | not in manifest | not in manifest; only a doc comment in `backup_rules.xml:5` warning future readers. Manifest has `android:allowBackup="false"` at `AndroidManifest.xml:9`. |

---

## P0 / P1 Verification

### P0 — Tracked `.logcat` files removed and future `.logcat` blocked

**Status: Fixed.**

Evidence:

- `git ls-files "*.logcat"` returns no output. The two files the original audit
  flagged (`login_issues.logcat`, `samsung-SM-F936B-Android-16_2026-05-19_120851.logcat`)
  are no longer tracked. `git status` (start-of-session snapshot) confirms them as
  `D` (deletions staged) at the repo root.
- `.gitignore:49` contains `*.logcat`, so any future logcat capture lands in an
  ignored class.

Remaining risk: the original audit notes that historical commits still contain the
old logcat content, and that history rewriting / credential rotation was the only
way to fully eliminate the exposure. That residual risk is operational, not in the
current working tree.

### P1 — Release builds reject missing / HTTP API URL, accept HTTPS

**Status: Fixed.**

Evidence:

- `app/build.gradle.kts:17-30` detects release-class task names and refuses to
  configure without an explicit `-Pedulife.apiBaseUrl=…`.
- `app/build.gradle.kts:39-41` rejects non-HTTPS values with an explicit message.
- Reproduced live: `.\gradlew.bat :app:assembleRelease` failed in 2s with
  `Release builds require edulife.apiBaseUrl. Pass -Pedulife.apiBaseUrl=https://your-api.example/api/v1/.`
- Reproduced live: `.\gradlew.bat :app:assembleRelease -Pedulife.apiBaseUrl=http://example.com/api/v1/`
  failed in 2s with `Release builds must set edulife.apiBaseUrl to an HTTPS endpoint.`
- Reproduced live: `.\gradlew.bat :app:assembleRelease -Pedulife.apiBaseUrl=https://example.com/api/v1/`
  succeeded in 9s.
- `app/src/main/res/xml/network_security_config.xml:14-21` has **no** cleartext
  domain exception at all in the main config. The base-config is HTTPS-only.
- All local cleartext exceptions live exclusively in
  `app/src/debug/res/xml/debug_network_security_config.xml:10-14` (10.0.2.2,
  localhost, 127.0.0.1).

### P1 — Release OkHttp logging disabled

**Status: Fixed.**

Evidence:

- `ApiClient.java:59-66` builds the logging interceptor **only inside
  `if (BuildConfig.DEBUG)`**. Debug uses `HttpLoggingInterceptor.Level.BODY` with
  `Authorization` redacted (`redactHeader("Authorization")`).
- Release path adds no logging interceptor at all. `Grep HttpLoggingInterceptor.Level.BASIC`
  is empty across `app/src/main` and `app/src/debug`.

### P1 — Backend sync failure clears stale `userId/role`

**Status: Fixed.**

Evidence:

- `AuthRepository.failBackendSync(...)` at `AuthRepository.java:228-236` calls
  `sessionStorage.clearAuthenticatedSession()` on every failure path before
  delivering the failure result.
- Every sync failure path routes through `failBackendSync`:
  missing user (`AuthRepository.java:136`),
  missing token (`AuthRepository.java:146`),
  Firebase token error (`AuthRepository.java:155`),
  timeout (`AuthRepository.java:178`),
  HTTP-error response (`AuthRepository.java:209`),
  network onFailure (`AuthRepository.java:223`).
- `SessionStorage.clearAuthenticatedSession()` at `SessionStorage.java:179-184`
  removes `KEY_USER_ID` and `KEY_ROLE` via `commit()` while preserving
  `KEY_PENDING_REGISTRATION_ROLE` (so a newly-verified user retrying their first
  sync does not lose their selected role).
- Unit test `app/src/test/java/com/baghdad/edulife/core/storage/SessionStorageTest.java`
  exercises this contract — proved by the `:app:testDebugUnitTest` pass above.

---

## P2 Verification

### F4 — Certificate downloads no longer use DownloadManager / public Downloads

**Status: Fixed.**

Evidence:

- No `android.app.DownloadManager` references in `app/src/main`. Static check above.
- No `Environment.DIRECTORY_DOWNLOADS` references in `app/src/main`. Static check
  above.
- `CertificatesFragment.downloadCertificate(...)` at
  `CertificatesFragment.java:101-141` calls `CertificateDownloader.download(...)`
  and opens the result via `CertificatePdfIntents.viewIntent(...)`. No manual
  `Authorization` header construction.
- `CertificateDetailFragment.downloadPdf(...)` at
  `CertificateDetailFragment.java:117-152` and `shareCert(...)` at
  `CertificateDetailFragment.java:154-197` use the same pipeline.
- `CertificateDownloader.writeToPrivateStorage(...)` at
  `CertificateDownloader.java:80-96` writes to `context.getFilesDir()/certificates/`,
  i.e., app-private storage.
- `CertificateDownloader.sanitizeFileName(...)` at
  `CertificateDownloader.java:103-111` collapses non-`[a-z0-9._-]` to `_` and
  rejects `.`/`..`, blocking path traversal even if a future backend format change
  yields hostile certificate numbers.
- New Retrofit method `ApiService.downloadCertificatePdf(certificateId)` (`ApiService.java`)
  routes through the same authenticated OkHttp pipeline as every other call, so
  `FirebaseAuthInterceptor` attaches the bearer and `FirebaseTokenAuthenticator`
  applies a one-shot 401 refresh.

### F4 — Certificate open/share uses FileProvider

**Status: Fixed.**

Evidence:

- `AndroidManifest.xml:34-42` declares a `<provider>` with
  `android:authorities="${applicationId}.fileprovider"`,
  `android:exported="false"`, `android:grantUriPermissions="true"`, and the
  `FILE_PROVIDER_PATHS` meta-data pointing at `@xml/file_paths`.
- `app/src/main/res/xml/file_paths.xml` whitelists exactly two private subtrees:
  `<files-path name="certificates" path="certificates/"/>` and
  `<cache-path name="lesson_downloads" path="lesson_downloads/"/>`. No other
  internal directory is exposed.
- `CertificatesFragment.java:118` and `CertificateDetailFragment.java:129` open
  the file via `CertificatePdfIntents.viewIntent(...)`; share uses
  `CertificatePdfIntents.shareIntent(...)` at `CertificateDetailFragment.java:167`.

### F5 — Lesson WebView URL policy

**Status: Fixed.**

Evidence:

- `UrlSecurityPolicy.classify(...)` at
  `app/src/main/java/com/baghdad/edulife/core/web/UrlSecurityPolicy.java:53-90`
  treats the following as `BLOCK`:
  - `null` / blank URLs (`UrlSecurityPolicy.java:54-56`)
  - `URISyntaxException` / malformed URLs (`UrlSecurityPolicy.java:60-63`)
  - missing or unknown scheme (`UrlSecurityPolicy.java:65-74`) — covers
    `file://`, `javascript:`, `intent://`, `content://`
  - HTTP / cleartext (`UrlSecurityPolicy.java:76-80`)
  - missing host (`UrlSecurityPolicy.java:82-83`)
- HTTPS + allowlisted host → `ALLOW_IN_APP`; HTTPS off-allowlist → `ALLOW_EXTERNAL`
  (system browser). Allowlist is built case-insensitively in `trustedHosts(...)`.
- `LessonPlayerFragment.shouldOverrideUrlLoading(...)` at
  `LessonPlayerFragment.java:478-510` classifies every navigation: ALLOW_IN_APP
  proceeds, ALLOW_EXTERNAL hands off to `openExternalUrl(...)`, BLOCK toasts and
  cancels.
- `LessonPlayerFragment.openExternalUrl(...)` at
  `LessonPlayerFragment.java:373-400` reclassifies before launching
  `ACTION_VIEW`, so an HTTP / unknown-scheme URL cannot reach `Intent.ACTION_VIEW`.
- `LessonPlayerFragment.loadDetailInWebView(...)` at
  `LessonPlayerFragment.java:547-606` calls the classifier before loading the URL
  and explicitly defers PDF detection to the download path (see F6).
- Host JVM tests `UrlSecurityPolicyTest` and `LessonWebViewHostsTest` exercise
  every branch; both pass under `:app:testDebugUnitTest`.

### F5 — JavaScript off by default, only enabled for trusted-host video

**Status: Fixed.**

Evidence:

- `LessonPlayerFragment.configureWebView()` at `LessonPlayerFragment.java:464-477`
  sets `setJavaScriptEnabled(false)`, `setDomStorageEnabled(false)`,
  `setAllowFileAccess(false)`, `setAllowContentAccess(false)`,
  `setMediaPlaybackRequiresUserGesture(true)`.
- `loadDetailInWebView` only re-enables JS for the trusted-host video branch
  (`LessonPlayerFragment.java:572-583`), then disables it again before the inline
  body branch (`LessonPlayerFragment.java:598-601`).
- The trusted host list lives in
  `LessonWebViewHosts.forApiBaseUrl(BuildConfig.API_BASE_URL)` and is small and
  fixed: the backend host plus YouTube/Vimeo origins.

### F6 — Protected PDFs never sent to Google Docs Viewer

**Status: Fixed.**

Evidence:

- `LessonContentTypeResolver.resolveViewerUrl(...)` at
  `LessonContentTypeResolver.java:118-120` now returns the raw URL — the previous
  `https://docs.google.com/gview?embedded=true&url=…` wrapper is gone.
- `LessonContentTypeResolver.shouldDownloadInsteadOfInline(...)` at
  `LessonContentTypeResolver.java:126-130` flags any PDF lesson type or `.pdf`
  URL.
- `LessonPlayerFragment.loadDetailInWebView(...)` at
  `LessonPlayerFragment.java:557-565` bumps any PDF that slipped past the
  explicit case into `downloadAndOpenPdf(...)`.
- `LessonPlayerFragment.downloadAndOpenPdf(...)` at
  `LessonPlayerFragment.java:402-451` writes via `LessonPdfDownloader` and opens
  via `FileProvider` on the `lesson_downloads/` subtree.
- `LessonPdfDownloader.download(...)` at `LessonPdfDownloader.java:54-91`:
  - Rejects URLs that fail `UrlSecurityPolicy.classify(...)` (line 61-64).
  - Uses `ApiClient.authenticatedClient()` only when the URL host matches the
    backend (`LessonPdfDownloader.java:67`); otherwise it uses a dedicated
    unauthenticated client. The Firebase bearer never leaves the EduLife origin.

### F7 — Release OkHttp logging fully off (re-verified)

**Status: Fixed.** See "P1 — Release OkHttp logging disabled" above.

### F8 — Backup hardened

**Status: Fixed.**

Evidence:

- `AndroidManifest.xml:9` — `android:allowBackup="false"`. Auto Backup is
  disabled on every device.
- `backup_rules.xml:9-15` excludes every storage domain (`sharedpref`, `database`,
  `file`, `external`, `root`) so even a future flip back to `allowBackup="true"`
  cannot ship learner data without a rule change.
- `data_extraction_rules.xml:12-26` excludes every storage domain from both
  `<cloud-backup>` and `<device-transfer>`. API 31+ device-to-device transfer
  cannot carry session, planner, certificate, or onboarding data onto a new
  device.

### F9 — JitPack content-filtered

**Status: Partially Fixed (content filter applied; dependency verification
metadata still deferred).**

Evidence:

- `settings.gradle.kts:28-33` restricts the JitPack repository via
  `content { includeGroup("com.github.ibrahimsn98") }`. The only artifact that
  actually needs JitPack (`SmoothBottomBar`) resolves through it. Any other
  group — including `com.github.bumptech.glide`, which is on Maven Central — is
  explicitly excluded from JitPack, so a typo or hijacked transitive group cannot
  silently pull a JitPack jar.
- Gradle dependency verification metadata
  (`gradle/verification-metadata.xml`) is intentionally not added in this pass.
  The deferred-to-P3 note is documented in
  `docs/2026-06-18-android-security-p2-remediation.md` under "Deferred".

### F10 — Avatar bounds decoding with `inSampleSize`

**Status: Fixed.**

Evidence:

- `ProfileFragment.AVATAR_MAX_BYTES` at `ProfileFragment.java:58` is 10 MB.
- `ProfileFragment.compressImage(...)` at `ProfileFragment.java:166-225`:
  - Queries source size via `OpenableColumns.SIZE` and rejects sources above the
    byte ceiling before any decode work (`ProfileFragment.java:170-174`).
  - First pass `decodeBounds(...)` uses `inJustDecodeBounds = true`
    (`ProfileFragment.java:241-249`).
  - Computes `inSampleSize` via the standard doubling rule
    (`ProfileFragment.java:251-258`, `computeInSampleSize`).
  - Decodes once with that subsample factor and explicit `ARGB_8888`
    (`ProfileFragment.java:183-201`).
  - `scaleBitmap(...)` at `ProfileFragment.java:260-267` is still applied as a
    final crop down to `AVATAR_MAX_PX = 1024`.
- Photo Picker entry point unchanged (`ProfileFragment.java:73-79`).

### F11 — R8 keep rules narrowed

**Status: Partially Fixed (Retrofit catch-all replaced; DTO/model keep rules
remain broad until `@SerializedName` migration lands).**

Evidence:

- `app/proguard-rules.pro:22-39`:
  - The broad `-keep class retrofit2.** { *; }` is gone.
  - Replaced with the Retrofit-recommended
    `-keepclasseswithmembers,includedescriptorclasses class * { @retrofit2.http.* <methods>; }`
    plus `-keep,allowobfuscation,allowshrinking interface retrofit2.Call`,
    `-keep,allowobfuscation,allowshrinking class retrofit2.Response`,
    `-dontwarn retrofit2.KotlinExtensions(*)`.
  - `ApiService` itself is preserved only as a reachable interface
    (`-keep,allowobfuscation,allowshrinking interface com.baghdad.edulife.core.network.ApiService`),
    not a wholly-kept set of members.
- `app/proguard-rules.pro:60-63` keeps DTO/model packages broadly, with an
  explicit comment that the rule remains because most fields are bare field-name
  matches against the live backend JSON. The `@SerializedName` migration is
  tracked as P3 hardening. Verified release build still passes
  (`:app:assembleRelease` SUCCESS above), so R8 + lintVital are clean against the
  narrowed Retrofit rules.

### F12 — Email verification rejection on `/auth/sync`

**Status: Needs Backend Verification.**

The Android-side flow is unchanged from the original audit observation:
`AuthRepository.login(...)` returns success on Firebase sign-in without checking
`isEmailVerified()`, and the backend `/auth/sync` is expected to reject
unverified Firebase users. Verifying that backend behavior is outside the scope
of this Android verification pass.

### No regression to P0 / P1 fixes

**Status: Confirmed.**

The P2 work did not weaken any P0/P1 control:

- `*.logcat` still ignored in `.gitignore` and absent from the tracked file list.
- Release HTTPS gate still enforced (verified live with both negative tests).
- Release logging still gated behind `BuildConfig.DEBUG` only.
- `AuthRepository.failBackendSync(...)` still clears the synced identity on every
  failure path; the SessionStorage helper used (`clearAuthenticatedSession`)
  intentionally preserves the pending registration role, which is the documented
  intent.

---

## OWASP Mobile Top 10 — Post-Remediation Status

| ID | OWASP | Previous status | Current status | Notes |
|---|---|---|---|---|
| F1 | M1 / M6 | Fail | **Fixed** | Logcat files removed; `*.logcat` ignored. Historical Git content not rewritten — operational risk only. |
| F2 | M5 / M8 | Fail | **Fixed** | Release HTTPS gate enforced; cleartext config debug-only. Negative tests pass. |
| F3 | M3 / M9 | Fail | **Fixed** | `clearAuthenticatedSession()` called on every sync failure path; unit-tested. |
| F4 | M3 / M9 | Partial | **Fixed** | Retrofit-based download + FileProvider + sanitized filenames. No `DownloadManager`/`DIRECTORY_DOWNLOADS` in main. |
| F5 | M4 / M5 / M8 | Partial | **Fixed** | `UrlSecurityPolicy` rejects file/javascript/intent/content/http/unknown/malformed; JS off by default. |
| F6 | M6 / M9 | Fail | **Fixed** | GDocs wrapper removed; PDFs flow through authenticated downloader + FileProvider; non-backend hosts use unauthenticated client. |
| F7 | M6 / M7 | Partial | **Fixed** | Logging interceptor only attached inside `if (BuildConfig.DEBUG)`. |
| F8 | M6 / M9 | Partial | **Fixed** | `allowBackup="false"` + all backup/transfer domains excluded. |
| F9 | M2 | Partial | **Partially Fixed** | JitPack content-filtered to one group; Gradle dependency verification still deferred. |
| F10 | M4 | Partial | **Fixed** | Bounds decode + `inSampleSize` + 10 MB ceiling. |
| F11 | M7 | Partial | **Partially Fixed** | Retrofit catch-all replaced; DTO keep rule pending `@SerializedName` migration. |
| F12 | M3 | Needs verification | **Needs Backend Verification** | Backend `/auth/sync` must reject unverified users. |

---

## Remaining Risks

1. **Historical Git content (F1).** The original logcat files are no longer
   tracked, but anyone with a clone made before the deletion still has the
   exposed bearer / email / Firebase UID material. Rotation and history rewriting
   are operational follow-ups, not Android-code fixes.
2. **Gradle dependency verification (F9).** Without
   `gradle/verification-metadata.xml`, a maliciously pushed Maven Central or
   `com.github.ibrahimsn98` artifact would still be trusted. The content filter
   shrinks the surface; verification metadata closes it. Tracked in the P2
   "Deferred" section.
3. **DTO keep rules (F11).** Release R8 still cannot strip the
   `features.**.model.**` and `**.dto.**` packages. A future
   `@SerializedName` migration unlocks tighter rules. Until then, release
   binaries leak more structural metadata than ideal — no direct vulnerability.
4. **Backend email-verification gate (F12).** Until the backend audit confirms
   `/auth/sync` rejects `email_verified=false`, an unverified Firebase user could
   theoretically reach the sync path if the client check were ever bypassed.
   Android side blocks navigation only after sync success, so the backend remains
   the source of truth.
5. **Cache eviction for downloaded lesson PDFs.** `LessonPdfDownloader` writes
   to `cacheDir/lesson_downloads/`; Android may purge the file between the
   download callback and the FileProvider VIEW intent under storage pressure.
   Worst case is a "PDF viewer can't read file" toast. Documented in the P2
   remediation audit; not a security risk.
6. **`FirebaseAuthInterceptor` token attachment on the shared client.** The
   authenticated client always attaches the bearer. `LessonPdfDownloader`
   compensates by switching to an unauthenticated client for non-backend hosts.
   Long-term, attaching the bearer only for trusted backend hosts inside the
   interceptor itself would be defense-in-depth so any future helper that reuses
   `ApiClient.authenticatedClient()` cannot leak the token by accident. Already
   tracked in the P2 audit.

---

## Final Jury-Ready Summary

EduLife Android has fully closed every P0 and P1 finding from the original
2026-06-18 OWASP Mobile Top 10 audit and has fully or substantively closed every
P2 finding. The only items not marked Fixed are:

- F9 (Supply chain): JitPack is now content-filtered to one trusted group; the
  remaining Gradle dependency verification metadata is a deferred hardening step.
- F11 (Binary protections): Retrofit's broad keep rule was replaced with
  targeted rules; DTO/model packages remain broadly kept pending a
  `@SerializedName` migration.
- F12 (Authentication): backend-side `email_verified` enforcement on
  `/auth/sync` cannot be verified from Android alone.

Build evidence: `:app:testDebugUnitTest`, `:app:assembleDebug`, and
`:app:assembleRelease` (with HTTPS URL) all succeed. Both release negative paths
(missing URL, HTTP URL) fail with the exact, documented Gradle error messages.
Static checks confirm no `DownloadManager`, no `DIRECTORY_DOWNLOADS`, no live
`docs.google.com/gview`, no `cleartextTrafficPermitted="true"` in `main`, no
release-time `HttpLoggingInterceptor.Level.BASIC`/`BODY`, no `allowBackup="true"`,
and no tracked `*.logcat` files.

No new findings were introduced. No code was modified during this verification
pass.
