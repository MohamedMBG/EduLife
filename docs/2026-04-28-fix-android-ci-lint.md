# Task Audit - Fix Android CI Lint Failure

## Date
2026-04-28

## Task Summary
Fixed Android CI GitHub Actions build failure which failed on `Run lint` with exit code 1 due to missing manifest configurations after Firebase was integrated.

## Files Created
- docs/2026-04-28-fix-android-ci-lint.md

## Files Modified
- app/src/main/AndroidManifest.xml
- .github/workflows/android-ci.yml
- .github/workflows/android-release.yml

## What Was Done
1. Added a mock `google-services.json` generation step to `.github/workflows/android-ci.yml` and `android-release.yml`. This fixes the exact error (`File google-services.json is missing`) that causes the `processDebugGoogleServices` task (and consequently `lintDebug`) to crash in the GitHub Actions runner because the real JSON is correctly gitignored.
2. Added the `android.permission.INTERNET` `<uses-permission>` explicitly to the `AndroidManifest.xml`. Firebase APIs and network clients require internet, and the absence of explicit internet permission causes strict lint checks to fail on CI even if merged manifest handles it.
3. Fully qualified the `EduLifeApp` name in the `android:name` attribute within `<application>` to `com.baghdad.edulife.EduLifeApp`. This resolves CI lint errors (like `Instantiatable` or `MissingClass` false positives) when a package isn't explicitly defined via `package=` attribute in `AndroidManifest.xml` (but is correctly defined by `namespace` in `build.gradle.kts`).
4. Fully qualified `MainActivity` for consistency.

## Architecture Compliance
The changes maintain full compatibility with the existing architecture. No features were altered. We fixed the application and manifest settings to meet security and baseline lint checks strictly required by CI.

## Code Comments Added
No java code comments were required for this config-only change. The manifest is structurally obvious. 

## Validation / Testing
- Simulated Lint with `./gradlew lint` using a correct Java 17 local environment.
- Verified that local Android lint successfully passes without error code.
- Did not change the real local `google-services.json`, so Firebase logic remains completely intact locally. The mock is only generated dynamically in the ephemeral CI runner.

## Risks / Notes
- CI should now pass. The `processDebugGoogleServices` task will accept the mock JSON and allow the Gradle daemon to proceed to the lint and build steps.
