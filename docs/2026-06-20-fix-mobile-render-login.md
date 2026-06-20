# Task Audit - Fix Mobile Render Login

## Date
2026-06-20

## Task Summary
Diagnosed why the same teacher account works through the deployed backend but the Android app reports that Render is unreachable. Rebuilt and replaced the stale website APK that predates the Android auth-sync reliability fix.

## Files Created
- docs/2026-06-20-fix-mobile-render-login.md

## Files Modified
- app/build.gradle.kts
- guided-journey-lab/public/EduLife-prerelease.apk

## What Was Done
Verified the live Render health endpoint and authenticated `POST /api/v1/auth/sync` using the same Firebase account. The backend returned HTTP 200 with the account's internal UUID and `TEACHER` role in under one second, proving that the account, Firebase token validation, and backend endpoint are working for non-browser clients.

Inspected the APK published by the website and found it was built on 2026-06-17. It predates the 2026-06-19 Android change that reuses the freshly fetched Firebase token for `/auth/sync` instead of performing a second token fetch inside OkHttp. The currently built APK and the website APK also had different hashes.

Incremented the Android prerelease version from code 1 / name 1.0 to code 2 / name 1.0.1. Rebuilt the current debug APK with `https://edulife-2bro.onrender.com/api/v1/` injected as its API base URL, then replaced the website's stale `EduLife-prerelease.apk` asset.

## Architecture Compliance
The existing mobile auth fix remains in `features/auth/data` and shared token transport remains in `core/network`. This task does not bypass backend sync, weaken Firebase validation, or add client-owned role logic. The website only distributes the rebuilt Android artifact.

## Code Comments Added
Added a Gradle comment explaining why the prerelease version was incremented: Android must recognize the corrected downloadable APK as newer than the stale artifact.

## Validation / Testing
- Live `GET /actuator/health`: HTTP success with `UP`.
- Live authenticated `POST /api/v1/auth/sync`: HTTP 200, role `TEACHER`.
- `:app:assembleDebug`: passed using the installed JDK 21.
- `AuthSyncDecisionTest`: passed.
- Final APK metadata: package `com.baghdad.edulife`, version code `2`, version name `1.0.1`, minimum SDK 24, target SDK 36.
- Final APK API base URL: `https://edulife-2bro.onrender.com/api/v1/`.
- APK Signature Scheme v2 verification: passed with one Android debug signer.
- Published APK SHA-256: `41141E799637F78A5234FF9AF9EF3F8C7E46766A91741DDF34FC8190CD909141`.

## Risks / Notes
- The corrected APK asset must be committed and the Vercel website redeployed before website downloads serve it.
- Existing users must install the new 1.0.1 APK. If Android rejects an in-place update because the previous APK used another signing key, uninstall the old prerelease first and then install the new file.
