# Task Audit - Fix Android CI Lint Failure

## Date
2026-04-28

## Task Summary
Fixed Android CI GitHub Actions build failure which failed on `Run lint` with exit code 1 due to missing manifest configurations after Firebase was integrated.

## Files Created
- docs/2026-04-28-fix-android-ci-lint.md

## Files Modified
- app/src/main/AndroidManifest.xml

## What Was Done
1. Added the `android.permission.INTERNET` `<uses-permission>` explicitly to the `AndroidManifest.xml`. Firebase APIs and network clients require internet, and the absence of explicit internet permission causes strict lint checks to fail on CI even if merged manifest handles it.
2. Fully qualified the `EduLifeApp` name in the `android:name` attribute within `<application>` to `com.baghdad.edulife.EduLifeApp`. This resolves CI lint errors (like `Instantiatable` or `MissingClass` false positives) when a package isn't explicitly defined via `package=` attribute in `AndroidManifest.xml` (but is correctly defined by `namespace` in `build.gradle.kts`).
3. Fully qualified `MainActivity` for consistency.

## Architecture Compliance
The changes maintain full compatibility with the existing architecture. No features were altered. We fixed the application and manifest settings to meet security and baseline lint checks strictly required by CI.

## Code Comments Added
No java code comments were required for this config-only change. The manifest is structurally obvious. 

## Validation / Testing
- Simulated Lint with `./gradlew lint` using a correct Java 17 local environment.
- Verified that local Android lint successfully passes without error code.
- Did not change `google-services.json` so Firebase logic remains completely intact.

## Risks / Notes
- CI should now pass. If CI still reports problems, it may be due to other missing API keys or CI pipeline problems rather than Android lint configuration itself.
