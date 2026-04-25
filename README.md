# EduLife

EduLife is a mobile-first educational platform for Moroccan learners. The MVP focuses on a simple, structured learning journey:

```text
Discover course -> Enroll -> Learn -> Take exam -> Pass -> Receive certificate
```

The project currently contains the Android application. The backend vision is a modular monolith, but backend modules are not part of this repository yet.

## Product Scope

EduLife is designed to replace fragmented learning across random videos, PDFs, and chat groups with one organized course experience.

MVP features planned for the product include:

- Authentication and role-based access
- Student and teacher profiles
- Course catalog and course details
- Course enrollment
- Lesson and resource access
- Progress tracking
- Final MCQ exams with automatic scoring
- Certificate generation after passing
- Course discussion and Q&A threads
- Basic notifications
- Basic admin and teacher verification flows

Post-MVP features such as payments, real-time chat, mentorship booking, advanced analytics, gamification, and AI recommendations are intentionally out of scope until the learning core is stable.

## Architecture

### Android

The Android app follows a pragmatic MVVM approach using Java and XML layouts.

Current package namespace:

```text
com.baghdad.edulife
```

Recommended feature-first structure:

```text
app/src/main/java/com/baghdad/edulife/
  core/
    network/
    storage/
    utils/
    ui/
    navigation/
  features/
    auth/
      ui/
      viewmodel/
      data/
      model/
    onboarding/
      ui/
```

Current implemented areas include:

- `core/network` for shared API setup
- `features/auth` for login/register models, repository, ViewModel, and UI
- `features/onboarding` for the onboarding UI
- XML layouts and navigation resources under `app/src/main/res`

### Backend Direction

When backend work is added, EduLife should use a modular monolith. Domain modules should remain inside one deployable backend application rather than being split into microservices during the MVP.

Planned backend domains:

```text
auth, users, profiles, roles, courses, lessons, resources,
enrollments, progress, exams, certificates, discussions,
notifications, groups, admin
```

## Tech Stack

- Java
- XML layouts
- Android Gradle Plugin
- AppCompat
- Material Design Components
- ConstraintLayout
- AndroidX Lifecycle ViewModel
- AndroidX LiveData
- Retrofit
- Gson converter
- OkHttp logging interceptor
- AndroidX Navigation Component
- JUnit, AndroidX Test, Espresso

## Requirements

- Android Studio
- JDK 11 or newer
- Android SDK with compile SDK 36 support
- Gradle wrapper included in the repository

## Getting Started

Clone the project and open it in Android Studio:

```bash
git clone <repository-url>
cd EduLife
```

Sync Gradle, then run the `app` configuration on an emulator or physical Android device.

You can also build from the command line:

```bash
./gradlew assembleDebug
```

On Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

## Project Layout

```text
EduLife/
  app/
    src/
      main/
        java/
        res/
        AndroidManifest.xml
    build.gradle.kts
  docs/
  gradle/
  build.gradle.kts
  settings.gradle.kts
  gradle.properties
```

## Development Rules

- Keep the MVP focused on the core learning loop.
- Use feature-first MVVM for Android code.
- Put UI logic in feature `ui` packages.
- Put state logic in feature `viewmodel` packages.
- Put API and repository logic in feature `data` packages or shared `core/network` when cross-feature.
- Keep business logic out of UI classes.
- Use JWT and RBAC for protected backend endpoints when backend work begins.
- Store heavy files such as videos, PDFs, and generated certificates in external storage, not directly in the database.
- Add clear comments for non-obvious validation, security, permission, API, transaction, or state-handling decisions.
- Add a task audit file in `docs/` for every completed coding, setup, refactor, or architecture task.

## Documentation

Task-level implementation notes live in `docs/`.

Each completed task should add a file using this format:

```text
docs/YYYY-MM-DD-task-name.md
```

## Status

EduLife is in early MVP development. The current repository is focused on the Android client foundation, including onboarding, authentication screens, navigation setup, and network dependencies.
