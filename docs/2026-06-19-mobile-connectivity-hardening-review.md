# Task Audit - Mobile Connectivity Hardening Review

## Date
2026-06-19

## Task Summary
Reviewed the recent Android and backend security-hardening reports to verify whether they could block a mobile app from reaching the Render-hosted backend, then tightened the Android login diagnostics so wrong-target APKs and host-specific sync failures are visible immediately.

## Files Created
- docs/2026-06-19-mobile-connectivity-hardening-review.md

## Files Modified
- app/src/main/java/com/baghdad/edulife/core/network/ApiClient.java
- app/src/main/java/com/baghdad/edulife/features/auth/data/AuthRepository.java
- app/src/main/java/com/baghdad/edulife/features/auth/ui/LoginFragment.java
- app/src/main/res/values/strings.xml

## What Was Done
Reviewed these security/hardening artifacts and the live config they describe:

- `docs/2026-06-19-p3-hardening.md`
- `docs/2026-06-19-android-security-post-remediation-verification.md`
- `app/src/main/res/xml/network_security_config.xml`
- `app/src/debug/res/xml/debug_network_security_config.xml`
- `app/build.gradle.kts`
- `backend/src/main/resources/application.yaml`
- `backend/src/main/java/com/edulife/security/SecurityConfig.java`

Findings from the review:

- The Android main network-security config is HTTPS-only and does not block normal TLS traffic to Render.
- TLS pinning in `network_security_config.xml` is still commented out, so there is no active pin mismatch that could hard-fail the app.
- The Render backend is reachable and healthy. Direct checks returned `UP` from `/actuator/health` and a JSON `401 Authentication required` from protected `/api/v1/...` endpoints, which proves transport reachability and Spring security responsiveness.
- The user-facing login error was too generic. It hid whether the APK was targeting the real Render host, emulator localhost (`10.0.2.2`), or a placeholder release host (`example.com`).
- The generated debug `BuildConfig` points at `https://edulife-2bro.onrender.com/api/v1/`, but the generated release `BuildConfig` in the workspace was `https://example.com/api/v1/`. That means a release APK built with the placeholder host would never reach the real backend even though the security hardening is correct.

Implemented Android-side diagnostics improvements:

- `ApiClient` now exposes the configured base URL so auth flows can report the real backend target.
- `AuthRepository` now includes the configured API host in sync timeout and network-failure messages.
- `LoginFragment` now:
  - shows host-specific unreachable/timeout messages,
  - explicitly warns when the installed APK is still pointed at `example.com`.
- Added string resources for the new host-aware auth errors.

## Architecture Compliance
The changes stay inside the Android architecture boundaries defined for EduLife:

- shared network environment reporting remains in `core/network/`
- auth sync transport diagnostics remain in `features/auth/data/`
- user-facing auth-state rendering remains in `features/auth/ui/`

No backend business rules, controller logic, or unrelated feature modules were changed.

## Code Comments Added
Added comments in:

- `ApiClient.java` to explain why exposing the build-time API URL helps catch wrong-environment APKs.
- `AuthRepository.java` to explain why the configured host must be surfaced in sync failures.
- `LoginFragment.java` to explain why the `example.com` placeholder case deserves a dedicated error.

These comments document the non-obvious operational reason for the change: distinguishing real connectivity failures from a bad app build target.

## Validation / Testing
Validated with:

- direct HTTPS check to `https://edulife-2bro.onrender.com/actuator/health` -> backend returned `UP`
- direct POST to `https://edulife-2bro.onrender.com/api/v1/auth/sync` without auth -> backend returned JSON `401 Authentication required`
- direct GET to `https://edulife-2bro.onrender.com/api/v1/courses?page=0&size=1` without auth -> backend returned JSON `401 Authentication required`
- `.\gradlew.bat :app:assembleDebug` -> `BUILD SUCCESSFUL`

Manual validation still recommended:

- reinstall the APK currently on the phone after rebuilding
- log in again and confirm any failure now names the target host
- if the phone shows `example.com`, rebuild the release APK with `-Pedulife.apiBaseUrl=https://edulife-2bro.onrender.com/api/v1/`

## Risks / Notes
- The main blocker is likely not the security hardening itself. The stronger candidates are:
  - the phone is running an older APK still pointed at `10.0.2.2`
  - the phone is running a release APK built against the `example.com` placeholder
  - Render cold-start latency is exceeding the auth-sync timeout
- The generic legacy string `auth_error_server_unreachable` still exists for any older call paths not updated in this task, but login now uses the host-aware variants.
- The backend-side security hardening reviewed here does not indicate a transport-level block for Android-to-Render HTTPS traffic.
