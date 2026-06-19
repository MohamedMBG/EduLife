# Task Audit - Android Security P2 Remediation

## Date
2026-06-18

## Goal
Address the remaining P2 items from `docs/2026-06-18-android-security-audit-report.md` (F4, F5,
F6, F8, F9, F10, F11) without weakening any of the P0/P1 fixes already shipped: certificate
PDFs must be private by default, WebView and lesson URL handling must enforce a real scheme +
host policy, protected PDFs must never be sent to Google Docs Viewer, backups must be safe for
MVP, the JitPack dependency surface must be narrowed, avatar decoding must be memory-bounded,
and the broadest R8 keep rule must be tightened where it is safe to do so.

## What Was Done

### F4 - Certificate downloads through OkHttp + app-private storage + FileProvider
- New `ApiService.downloadCertificatePdf(certificateId)` streams the PDF body through the
  same Retrofit/OkHttp pipeline as every other call. The Firebase bearer is attached by
  `FirebaseAuthInterceptor` and a 401 triggers the existing
  `FirebaseTokenAuthenticator` one-shot refresh — `DownloadManager` bypassed both.
- New `core/network/ApiClient.authenticatedClient()` exposes the configured OkHttp client so
  download helpers can reuse the same pipeline (with the explicit comment that callers must
  only hand it backend-origin URLs to avoid leaking the bearer to third parties).
- New `features/certificates/data/CertificateDownloader` writes the streamed PDF to
  `context.getFilesDir()/certificates/certificate-<sanitized>.pdf`. The file name is
  sanitized (`[^a-z0-9._-]` collapsed to `_`, `.`/`..` rejected) so a future backend format
  change cannot escape the certificates directory.
- New `features/certificates/data/CertificatePdfIntents` builds per-grant `content://` URIs
  via `FileProvider` for both VIEW and SEND intents, with `FLAG_GRANT_READ_URI_PERMISSION`
  so no other app can read the file unless explicitly handed the URI.
- `CertificatesFragment` and `CertificateDetailFragment` no longer use `DownloadManager`,
  `Environment.DIRECTORY_DOWNLOADS`, or manual `Authorization` header construction. They no
  longer register a `DownloadManager.ACTION_DOWNLOAD_COMPLETE` BroadcastReceiver. Both
  fragments call `CertificateDownloader.download(...)` and route the resulting `File` through
  `CertificatePdfIntents.viewIntent(...)` for open and `shareIntent(...)` for share.
- `CertificateDetailFragment.shareCert(...)` now attaches the PDF (private URI) plus the
  existing share-summary text, so sharing surfaces the credential file instead of a plain
  string copy in the public Downloads folder.
- New manifest `<provider>` declares `${applicationId}.fileprovider` (`exported=false`,
  `grantUriPermissions=true`) backed by `app/src/main/res/xml/file_paths.xml`, which
  whitelists only `files/certificates/` and `cache/lesson_downloads/`.

### F5 - WebView and lesson URL allowlisting
- New `core/web/UrlSecurityPolicy` is a pure, host-JVM-testable scheme + host classifier.
  Decisions: `ALLOW_IN_APP` (HTTPS + allowlisted host), `ALLOW_EXTERNAL` (HTTPS, anything
  else), `BLOCK` (cleartext, `file://`, `javascript:`, `intent://`, `content://`, unknown
  schemes, malformed URLs, missing host).
- New `features/courses/model/LessonWebViewHosts` exposes the in-app WebView allowlist:
  the backend host (derived from `BuildConfig.API_BASE_URL`) plus the deliberately-small set
  of trusted video providers (`www.youtube.com`, `youtube.com`, `m.youtube.com`, `youtu.be`,
  `www.youtube-nocookie.com`, `player.vimeo.com`, `vimeo.com`).
- `LessonPlayerFragment`:
  - WebView config: `setJavaScriptEnabled(false)` and `setDomStorageEnabled(false)` by
    default. JS is re-enabled only right before loading a trusted-host video embed in
    `loadDetailInWebView(...)`. Inline body HTML is loaded with JS explicitly off.
  - `WebViewClient.shouldOverrideUrlLoading` now classifies every navigation through the
    policy. Trusted hosts load inside the WebView; non-allowlisted HTTPS targets are handed
    off to the system browser; everything else is rejected with a toast.
  - `openExternalUrl(...)` rejects unsafe URLs (`file://`, `javascript:`, `intent://`,
    `http://`, etc.) up front and only launches `ACTION_VIEW` for URLs the policy accepts.

### F6 - Stop sending protected PDFs to Google Docs Viewer
- `LessonContentTypeResolver.resolveViewerUrl(...)` no longer wraps PDF URLs in
  `https://docs.google.com/gview?embedded=true&url=...`. It now returns the raw URL so the
  caller can choose a safer rendering path.
- New `LessonContentTypeResolver.shouldDownloadInsteadOfInline(...)` flags any PDF lesson
  type or `.pdf` URL so the player routes it through the download flow even if it slipped
  past the explicit PDF case.
- New `features/courses/data/LessonPdfDownloader` writes the PDF to
  `context.getCacheDir()/lesson_downloads/lesson-<sanitized>.pdf` and exposes a small
  callback interface. It uses `ApiClient.authenticatedClient()` only when the PDF URL host
  matches the backend host; otherwise it uses an internal, un-authenticated OkHttp instance
  so the Firebase bearer cannot leak to a third-party origin. URLs failing
  `UrlSecurityPolicy.classify(...)` are rejected before any network call.
- `LessonPlayerFragment.bindLessonContent` PDF case and `loadDetailInWebView(...)` PDF
  fallback both call `downloadAndOpenPdf(...)`, which opens the resulting private file via
  `FileProvider` and a VIEW intent (mirroring the certificate flow).

### F8 - Backup policy hardened for MVP
- `AndroidManifest.xml`: `android:allowBackup="false"`. Auto Backup is disabled on every
  device; no learner data, planner data, certificate cache, or onboarding state ships to
  Google Drive.
- `data_extraction_rules.xml`: every storage domain (`sharedpref`, `database`, `file`,
  `external`, `root`) is excluded from both `<cloud-backup>` and `<device-transfer>`. This
  closes the API-31+ device transfer path so a switch-to-new-phone flow cannot carry the
  previous learner's encrypted session, planner data, or downloaded artifacts onto the new
  device.
- `backup_rules.xml`: kept (still referenced via `android:fullBackupContent`) and rewritten
  to explicitly exclude every domain. If anyone flips `allowBackup` back to `true` later,
  the data still does not leave the device without an intentional rule change.

### F9 - JitPack content filter
- `settings.gradle.kts`: JitPack is restricted to the `com.github.ibrahimsn98` group, which
  is the only artifact (`SmoothBottomBar`) that actually needs JitPack. Glide is published
  on Maven Central under `com.github.bumptech.glide` and is intentionally not allowlisted
  through this filter. A typo or a hijacked transitive group can no longer silently resolve
  a GitHub-built jar.
- Gradle dependency verification metadata was NOT added in this pass; see "Deferred" below.

### F10 - Avatar image bounds-decoded with a byte ceiling
- `ProfileFragment.compressImage(...)`:
  - Queries the source URI's `OpenableColumns.SIZE` and rejects sources above
    `AVATAR_MAX_BYTES = 10 MB` before any decode work.
  - First decode pass uses `BitmapFactory.Options.inJustDecodeBounds = true` to read width
    + height without allocating a bitmap.
  - Computes `inSampleSize` via the standard "double until the largest side is &le; max"
    rule (`AVATAR_MAX_PX = 1024`), then decodes once with that subsample factor and the
    explicit `ARGB_8888` config.
  - `scaleBitmap(...)` is still applied as a final crop down to the exact `AVATAR_MAX_PX`
    bound. The Photo Picker entry point is unchanged.

### F11 - R8 keep rules narrowed where safe
- `proguard-rules.pro`: the broad `-keep class retrofit2.** { *; }` was replaced with the
  Retrofit-recommended targeted pattern (`-keepclasseswithmembers ... @retrofit2.http.*
  <methods>;`, plus `interface retrofit2.Call` and `class retrofit2.Response` allowed to be
  shrunk/obfuscated, plus `-dontwarn` for the optional `KotlinExtensions`). Added an
  explicit `-keep,allowobfuscation,allowshrinking interface com.baghdad.edulife.core.network.ApiService`
  so Retrofit can still build its dynamic proxy.
- The broad `com.baghdad.edulife.features.**.model.**` and `**.dto.**` keep rules were kept
  with an explicit comment that they exist because most DTO fields do not carry
  `@SerializedName` and would break Gson deserialization against the live backend if
  renamed. Migrating each DTO to `@SerializedName` is tracked as P3.

## Files Created
- `app/src/main/java/com/baghdad/edulife/core/web/UrlSecurityPolicy.java`
- `app/src/main/java/com/baghdad/edulife/features/certificates/data/CertificateDownloader.java`
- `app/src/main/java/com/baghdad/edulife/features/certificates/data/CertificatePdfIntents.java`
- `app/src/main/java/com/baghdad/edulife/features/courses/data/LessonPdfDownloader.java`
- `app/src/main/java/com/baghdad/edulife/features/courses/model/LessonWebViewHosts.java`
- `app/src/main/res/xml/file_paths.xml`
- `app/src/test/java/com/baghdad/edulife/core/web/UrlSecurityPolicyTest.java`
- `app/src/test/java/com/baghdad/edulife/features/courses/LessonWebViewHostsTest.java`
- `docs/2026-06-18-android-security-p2-remediation.md`

## Files Modified
- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/xml/backup_rules.xml`
- `app/src/main/res/xml/data_extraction_rules.xml`
- `app/src/main/res/values/strings.xml`
- `app/proguard-rules.pro`
- `settings.gradle.kts`
- `app/src/main/java/com/baghdad/edulife/core/network/ApiClient.java`
- `app/src/main/java/com/baghdad/edulife/core/network/ApiService.java`
- `app/src/main/java/com/baghdad/edulife/features/certificates/ui/CertificatesFragment.java`
- `app/src/main/java/com/baghdad/edulife/features/certificates/ui/CertificateDetailFragment.java`
- `app/src/main/java/com/baghdad/edulife/features/courses/model/LessonContentTypeResolver.java`
- `app/src/main/java/com/baghdad/edulife/features/courses/ui/LessonPlayerFragment.java`
- `app/src/main/java/com/baghdad/edulife/features/profile/ui/ProfileFragment.java`
- `app/src/test/java/com/baghdad/edulife/features/courses/LessonContentTypeResolverTest.java`

## Architecture Compliance
- Network code (Retrofit interface, OkHttp client, download helpers) stays under
  `core/network` and `features/certificates/data` / `features/courses/data`.
- Pure URL safety logic stays under `core/web` with no Android API dependency (tests run on
  host JVM, no Robolectric).
- WebView trusted host list lives next to the rest of the lesson model (`features/courses/model`).
- No business logic was moved into UI classes. Fragments call helpers and observe ViewModels
  exactly as before.

## Backend Impact
None. The certificate download endpoint (`GET /api/v1/certificates/{id}/download`) is
already used by the previous `DownloadManager` flow; the new Retrofit path hits the same
URL.

## Web Impact
None.

## Tests / Verification
- New host-JVM tests:
  - `UrlSecurityPolicyTest` covers HTTPS allowlisted host, untrusted HTTPS, HTTP, `file`,
    `javascript`, `intent`, `content`, unknown scheme, null/blank/malformed input,
    null allowlist, host case-insensitivity, and the `trustedHosts(...)` builder.
  - `LessonWebViewHostsTest` verifies the backend host is derived from the API base URL and
    that the static video providers are always present.
- Updated `LessonContentTypeResolverTest` to lock in the new "PDF URLs are returned raw, not
  wrapped in Google Docs Viewer" behavior and to cover the new
  `shouldDownloadInsteadOfInline(...)` helper.
- Existing `SessionStorageTest` and `LessonContentTypeResolverTest` cases continue to pass.

Gradle commands run:
- `./gradlew.bat :app:testDebugUnitTest` — see "Validation / Testing" below.
- `./gradlew.bat :app:assembleDebug` — see "Validation / Testing" below.
- `./gradlew.bat :app:assembleRelease -Pedulife.apiBaseUrl=https://example.com/api/v1/`
  — see "Validation / Testing" below.

Static checks (`rg` / Grep):
- `android.app.DownloadManager`, `DownloadManager.Request`, `Environment.DIRECTORY_DOWNLOADS`
  — no matches under `app/`.
- `docs.google.com/gview` — no matches in production code (only doc comments and tests that
  prove the new behavior).
- `HttpLoggingInterceptor.Level.BASIC` / release-time logging — no regression (`ApiClient`
  still attaches the logger only inside `if (BuildConfig.DEBUG)`).
- `cleartextTrafficPermitted="true"` — only present under `app/src/debug` (no regression).
- WebView `setJavaScriptEnabled(true)` — only inside the trusted-host video-load branch of
  `LessonPlayerFragment.loadDetailInWebView`; configureWebView and the inline body path
  explicitly set it to `false`.

## Validation / Testing
- `./gradlew.bat :app:testDebugUnitTest` — **BUILD SUCCESSFUL in 46s** after a one-line fix
  (`retrofit2.Callback` was shadowed by the inner `CertificateDownloader.Callback`; resolved
  by fully qualifying the inner callback). Includes the new
  `UrlSecurityPolicyTest`, `LessonWebViewHostsTest`, and updated
  `LessonContentTypeResolverTest`.
- `./gradlew.bat :app:assembleDebug` — **BUILD SUCCESSFUL in 49s**.
- `./gradlew.bat :app:assembleRelease "-Pedulife.apiBaseUrl=https://example.com/api/v1/"`
  — **BUILD SUCCESSFUL in 3m 5s**. R8/lintVital ran clean against the narrowed Retrofit
  keep rules; release minification still produces a packaged APK.

Static checks (rg / Grep against `app/src/main`):
- `cleartextTrafficPermitted="true"` — no matches.
- `HttpLoggingInterceptor` / `Level.BODY` / `Level.BASIC` — only inside
  `if (BuildConfig.DEBUG)` in `ApiClient.java`.
- `DownloadManager`, `Environment.DIRECTORY_DOWNLOADS`, `docs.google.com` — only in doc
  comments documenting the removed behavior; no live code references.
- `allowBackup` — `"false"` in `AndroidManifest.xml`; only doc comments in
  `backup_rules.xml`/`data_extraction_rules.xml`.

## Deferred
- **Authenticated PDF lessons via a dedicated backend endpoint.** Today
  `LessonPdfDownloader` uses the authenticated client only when the lesson PDF URL matches
  the backend host. If a future deployment serves auth-gated PDFs from a different host
  (signed S3, CDN with bearer), we will need a dedicated backend "download lesson resource"
  endpoint to keep the bearer scoped.
- **In-app PDF renderer.** The audit explicitly allowed "download privately + open via
  FileProvider" as the safe fallback. A real in-app renderer (PdfRenderer or
  WebView+PDF.js) is still future work.
- **Gradle dependency verification metadata.** Generating
  `gradle/verification-metadata.xml` is a multi-step process (`--write-verification-metadata
  pgp,sha256`, manual review, periodic refresh on every dependency bump). Recommended next
  pass; not enabled here to avoid silently breaking CI on the next stable Gradle/AGP bump.
- **DTO `@SerializedName` migration.** Required before the broad
  `com.baghdad.edulife.features.**.model.**` and `**.dto.**` keep rules can be removed.
- **WebView SSL pin set.** Network security config still leaves `<pin-set>` empty until the
  production domain's certificate chain is finalized (already noted in the file).

## Remaining P3 Hardening
- Add screenshot protection (`FLAG_SECURE`) on certificate, profile, and exam screens if
  product confirms the requirement.
- Add advisor prompt client-side guardrails (max length, content-class checks) in front of
  the existing backend rate limiter.
- Migrate avatar pipeline to Glide's `downsampleAt`/`override` so a single library handles
  the bounds decode + display.
- Add Android instrumentation tests for the certificate download path (no public Downloads
  side effect, FileProvider URI grant lifetime).
- Migrate DTOs to `@SerializedName` and narrow the model keep rule (see Deferred above).

## Risks / Notes
- Backup is now off entirely. If product later wants per-feature backup (e.g., planner
  preferences), the right answer is a strict include-list in `data_extraction_rules.xml`
  rather than flipping `allowBackup` back to `true` and reintroducing the broad cloud
  surface.
- `LessonPdfDownloader` writes to `cacheDir`. Android may purge cache entries under storage
  pressure between the download callback and the FileProvider VIEW intent; the worst case
  is a "PDF viewer can't read file" toast and a re-tap. If this becomes a UX issue, move to
  `filesDir/lesson_downloads/` and add an explicit eviction policy.
- The Bearer token is unconditionally attached by `FirebaseAuthInterceptor` to every request
  on the authenticated client. `LessonPdfDownloader` works around this by using a separate
  un-authenticated client for non-backend hosts. Long-term we should change the interceptor
  to only attach for trusted backend hosts so any future helper that reuses
  `ApiClient.authenticatedClient()` cannot leak the token by accident.
