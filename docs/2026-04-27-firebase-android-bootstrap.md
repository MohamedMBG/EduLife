# Task Audit - Firebase Android Bootstrap

## Date
2026-04-27

## Task Summary
Wire Firebase Auth SDK into the Android app for Sprint 0. Ensure Firebase initializes safely on startup, Application class exists, google-services.json is excluded from source control.

## Files Created
- `app/src/main/java/com/baghdad/edulife/EduLifeApp.java`
- `docs/2026-04-27-firebase-android-bootstrap.md`

## Files Modified
- `app/.gitignore` — added `google-services.json` exclusion rule
- `app/src/main/AndroidManifest.xml` — registered `EduLifeApp` as `android:name`

## What Was Done

1. **Firebase BoM + firebase-auth dependency** — already present in `app/build.gradle.kts` (`firebase-bom:34.12.0` + `firebase-auth`). No change needed.

2. **Google Services plugin** — already declared in root `build.gradle.kts` (`version "4.4.4"`) and applied in `app/build.gradle.kts`. No change needed.

3. **Application class** — created `EduLifeApp.java`. Firebase auto-initializes via `FirebaseInitProvider` (a ContentProvider injected by the google-services plugin) before `Application.onCreate()`. The class adds a startup guard log: if `FirebaseApp.getApps()` is empty, it logs an error so developers catch a missing `google-services.json` immediately at launch rather than at first auth call-site.

4. **Manifest registration** — added `android:name=".EduLifeApp"` to the `<application>` tag.

5. **google-services.json gitignore** — added `google-services.json` to `app/.gitignore`. Ran `git rm --cached app/google-services.json` to stop tracking the file already in the index. The file remains on disk so the local build continues to work.

## Architecture Compliance

- `EduLifeApp.java` lives at the root package `com.baghdad.edulife`, which is the correct location for the Application subclass (not inside a feature folder).
- No feature logic was added to the Application class.
- Follows AGENTS.md §7 (Android structure) and §13 (Firebase auth foundation).

## Code Comments Added

- Class-level Javadoc on `EduLifeApp` explains why explicit `FirebaseApp.initializeApp()` is not called (auto-init via ContentProvider) and why the class exists (startup hook for Sprint 0 singletons).
- Inline comment in `onCreate` explains the guard log rationale (early failure visibility vs. silent auth crash).

## Validation / Testing

- Gradle plugin and BoM versions already present; no new dependency resolution needed.
- Build will succeed only when `app/google-services.json` is present locally (required blocker per AGENTS.md §18).
- Manual test: launch app with `google-services.json` present → Logcat should show `Firebase initialized successfully.`.
- Manual test: remove `google-services.json` and build → build fails at Gradle sync with google-services plugin error (expected and correct).

## Risks / Notes

- **SECURITY — git history contains google-services.json**: The file was committed in commit `af902c6`. It is now untracked, but the secret remains in git history. If this repository is public or will be shared, the Firebase project API keys should be rotated and the commit should be purged with `git filter-repo` or GitHub secret scanning remediation. Treat this as a required follow-up before any public push.
- `FirebaseAuth.getInstance()` is now safe to call from any auth repository or ViewModel after this bootstrap.
- No login/register logic was implemented per task scope.
