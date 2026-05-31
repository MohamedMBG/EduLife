# Task Audit - Android Register Role Selection

## Date
2026-05-31

## Task Summary
Implemented a two-step Android registration flow so users choose a role first, then enter credentials. The selected role is now preserved locally and sent to the backend on the first verified `/api/v1/auth/sync`, which matches the current Spring Boot auth contract.

## Files Created
- app/src/main/java/com/baghdad/edulife/features/auth/model/AuthSyncRequest.java
- app/src/main/res/drawable/bg_auth_role_option.xml
- docs/2026-05-31-android-register-role-selection.md

## Files Modified
- app/src/main/java/com/baghdad/edulife/core/network/ApiService.java
- app/src/main/java/com/baghdad/edulife/core/storage/SessionStorage.java
- app/src/main/java/com/baghdad/edulife/features/auth/data/AuthRepository.java
- app/src/main/java/com/baghdad/edulife/features/auth/model/RegisterRequest.java
- app/src/main/java/com/baghdad/edulife/features/auth/ui/RegisterFragment.java
- app/src/main/java/com/baghdad/edulife/features/auth/viewmodel/AuthViewModel.java
- app/src/main/res/layout/fragment_register.xml
- app/src/main/res/values/strings.xml

## What Was Done
Added a role-selection step ahead of the credential form in `RegisterFragment` and replaced the old single-screen register layout with a two-step UI that keeps the existing EduLife visual style.

Expanded the Android registration model so it carries `fullName`, `email`, `password`, and `intendedRole`, then updated the ViewModel and repository to pass that object through the registration flow.

Added local persistence for the selected registration role in `SessionStorage`. This is necessary because email verification can happen outside the app, so the intended role must survive until the first verified login.

Updated `AuthRepository.syncWithBackend()` to read any pending role and call `POST /api/v1/auth/sync` with a body when needed. The pending role is cleared after a successful sync because the backend only applies it on first user creation.

Added an Android `AuthSyncRequest` DTO and an overloaded Retrofit `syncUser(@Body ...)` method to match the backend controller that already accepts optional `intendedRole`.

## Architecture Compliance
The UI changes stay inside `features/auth/ui/`, state changes stay in `features/auth/viewmodel/`, auth API/session logic stays in `features/auth/data/` and `core/`, and no unrelated architecture was introduced.

The implementation respects the EduLife auth plan by keeping Firebase registration as the identity source and using the backend sync endpoint, rather than inventing a separate Android-side registration API.

## Code Comments Added
Added comments in the auth repository explaining why the selected role is stored before email verification and why the pending role is cleared after a successful sync.

Added comments in the register fragment explaining the non-obvious Student-to-`LEARNER` mapping and why the selected role is surfaced again on the credentials step.

## Validation / Testing
Ran `./gradlew.bat :app:compileDebugJavaWithJavac` successfully after the changes.

Manual QA still recommended for:
- selecting each role and moving between step 1 and step 2
- registering, verifying email, then logging in to confirm `/auth/sync` assigns the intended first-time role
- rotating the device during the register flow to confirm state restoration feels correct

## Risks / Notes
The backend role enum uses `LEARNER` while the product copy says `Student`. The Android UI now maps that intentionally, but this mismatch remains a naming footgun for future work.

If a first sync partially succeeds server-side but the client never receives the response, the pending role may still be stored locally. This is safe because the backend ignores `intendedRole` after the user already exists.
