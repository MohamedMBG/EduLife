# Android Security Audit + Hardening

## Goal

Perform a full OWASP MASVS/MASTG-style security audit of the EduLife Android app
(`app/`), identify every real vulnerability, risky pattern, and misconfiguration,
then apply the safe, high-value fixes. No backend or web changes.

## What Changed

Static audit of the entire `app/` module (manifest, Gradle, network configs,
auth/session, interceptors, WebView, downloaders, FileProvider, exam/gamification
trust boundaries, logging, secrets, supply chain). The app was already strongly
hardened (prior 2026-06 OWASP pass). Findings: **no P0/P1**, one P2, six P3.

Fixes applied this task:

- **P2-1 — Inline lesson TEXT links bypassed `UrlSecurityPolicy`.**
  `renderTextContent` rendered teacher-authored body HTML with `Html.fromHtml` +
  `LinkMovementMethod`, so embedded `<a href>` links launched via a raw
  `ACTION_VIEW`, escaping the player's URL policy (a teacher could embed
  `intent://`/`javascript:`/`file://` links shown to learners). Now every
  `URLSpan` is re-wrapped in a `ClickableSpan` that routes the tap through
  `openExternalUrl()` → `UrlSecurityPolicy.classify()`.

- **P3-1 — `android.util.Log` shipped in release with no stripping.** Added
  `-assumenosideeffects` for `Log.d`/`Log.v` in release ProGuard rules; scrubbed
  the `content://` URI value out of all `ProfileFragment` avatar `Log.w`/`Log.e`
  messages (Log.w/e are retained for crash triage but must not carry sensitive
  values).

- **P3-2 — Avatar client cap 10 MB → 5 MB.** `AVATAR_MAX_BYTES` now matches the
  AGENTS.md product limit. Backend remains the authoritative enforcer (requires
  backend verification).

- **P3-4 — Debug OkHttp logging `BODY` → `HEADERS`.** Debug logcat no longer
  carries response bodies (emails, names, internal userIds, exam payloads).
  Release still attaches no logger. Authorization stays redacted.

- **P3-6 — Retrofit `2.9.0` → `2.11.0`** (+ `converter-gson`). Removes a
  2020-era dependency.

- **P3-3 — TLS certificate pinning.** Added a ready-to-fill, **inactive**
  `<domain-config>`/`<pin-set>` template with rotation guidance to
  `network_security_config.xml`. Deliberately not activated: a wrong/expired SPKI
  pin hard-fails every API call and bricks installs, and the backend sits behind
  Render's ~90-day-rotating Let's Encrypt certs — real pins + a rotation strategy
  (pin the intermediate, two pins, expiration) must be set before activating.

- **P3-5 — `SmoothBottomBar` (JitPack) replaced with Material `BottomNavigationView`.**
  The only JitPack-sourced (GitHub-built) artifact is gone, so the **entire JitPack
  repository was removed** from `settings.gradle.kts` — every dependency now resolves
  from `google()`/`mavenCentral()` only. The floating rounded look is preserved via
  `@drawable/bg_bottom_nav` + margins/elevation; the active-pill colour comes from
  the theme's `colorSecondaryContainer` (= `brand_primary_surface`) and icon/label
  tint from `@color/bottom_nav_tint`. Menu item ids already equal the nav-graph
  destination ids, so selection maps straight onto `navigate()`; a guard plus
  `getSelectedItemId()` check prevents a selection↔navigation feedback loop.

## Files Touched

- `app/src/main/java/com/baghdad/edulife/features/courses/ui/LessonPlayerFragment.java` — P2-1 (URLSpan re-wrap + imports).
- `app/src/main/java/com/baghdad/edulife/features/profile/ui/ProfileFragment.java` — P3-2 (5 MB cap) + P3-1 (URI scrub from logs).
- `app/proguard-rules.pro` — P3-1 (strip Log.d/v in release).
- `app/src/main/java/com/baghdad/edulife/core/network/ApiClient.java` — P3-4 (HEADERS-level debug logging).
- `app/build.gradle.kts` — P3-6 (Retrofit 2.11.0).
- `app/src/main/res/xml/network_security_config.xml` — P3-3 (inactive pinning template).
- `app/src/main/res/layout/activity_main.xml` — P3-5 (Material BottomNavigationView).
- `app/src/main/java/com/baghdad/edulife/MainActivity.java` — P3-5 (Material nav wiring; removed kotlin/SmoothBottomBar imports).
- `app/build.gradle.kts` — P3-5 (dropped SmoothBottomBar dependency).
- `settings.gradle.kts` — P3-5 (removed the JitPack repository entirely).
- Reused pre-staged `app/src/main/res/drawable/bg_bottom_nav.xml` and `app/src/main/res/color/bottom_nav_tint.xml`.

## Backend Impact

None (no backend code changed). Open items requiring backend verification:
- Avatar upload >5 MB rejected server-side.
- Exam pass threshold (80%), 2-fail→72h cooldown, certificate gating enforced
  server-side (Android correctly sends only `{questionId, choiceId}` and trusts
  the server's `ExamResultResponse`).
- Admin/teacher/group-admin RBAC enforced server-side (the Android role-based
  routing in `MainActivity` is cosmetic; tampered local roles only reach screens
  that 403).

## Android Impact

P2-1 changes the tap behavior of links inside text lessons (now policy-checked).
P3-1/P3-4 reduce logcat output. P3-2 rejects 5–10 MB avatar sources client-side.
P3-6 bumps Retrofit. No public API or user-visible flow removed.

## Web Impact

None.

## Architecture Compliance

Feature-first MVVM preserved. No API calls moved into Fragments; the P2-1 change
reuses the existing `openExternalUrl`/`UrlSecurityPolicy` seam. Java-only, XML
layouts unchanged, manual DI unchanged.

## Tests / Verification

- `./gradlew :app:compileDebugJavaWithJavac` — compile the edited Java.
- `./gradlew testDebugUnitTest` — existing host-JVM suites (SessionStorage,
  UrlSecurityPolicy, LessonContentTypeResolver, exam/profile mapping).
- Recommended new tests: TEXT-render link routes `intent:`/`javascript:` hrefs to
  `BLOCK`; `compressImage` rejects a 6 MB source; release dex contains no avatar
  log strings; `assembleRelease` with an HTTP base URL fails the build.

## Risks / Notes

- **P3-3 pinning is intentionally inert** — must not be activated with
  placeholder pins.
- **P3-5 / P3-6** changed the dependency graph (SmoothBottomBar removed, JitPack
  repo removed, Retrofit bumped). Verified with `./gradlew :app:assembleDebug`
  (passes); run `assembleRelease` before shipping. The bottom bar is slightly
  taller (`wrap_content` vs the old fixed 64dp) and uses "selected" label
  visibility — worth a quick visual check on device.
- Production go-live still depends on the backend verification items above plus
  activating TLS pinning.
