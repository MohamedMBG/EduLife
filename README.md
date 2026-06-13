# EduLife

> A mobile-first educational platform for Moroccan learners. Centralizes fragmented learning into a structured, guided journey with courses, exams, and certificates.

---

## Table of Contents

- [Overview](#overview)
- [Product Vision](#product-vision)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Repository Structure](#repository-structure)
- [Getting Started](#getting-started)
- [Development](#development)
- [API Documentation](#api-documentation)
- [Deployment](#deployment)
- [Contributing](#contributing)

---

## Overview

EduLife solves **learning fragmentation**. Students currently rely on scattered resources: YouTube videos, WhatsApp groups, PDFs, and disconnected platforms. EduLife unifies the entire learning experience into one structured system.

### Core Learning Flow

```text
Discover Course → Enroll → Learn → Take Exam → Pass → Receive Certificate
```

### Key Features

- **Unified Course Catalog**: Browse and discover courses in one place
- **Structured Lessons**: Video-based lessons with progress tracking
- **MCQ Exams**: Final exams with automatic scoring and pass/fail validation
- **Certificates**: Automatic certificate generation upon passing
- **Multi-role System**: Support for Students, Teachers, Admins
- **Firebase Auth**: Secure authentication with email verification
- **Backend-driven**: All business logic centralized on secure backend
- **Multilingual**: Ready for Darija, French, English

---

## Product Vision

### Problem Statement

Today’s learning is:
- **Scattered** across multiple platforms
- **Unstructured** and confusing
- **Unvalidated** (no exams, no credentials)
- **Isolated** (no teacher guidance)

Students don’t know: what to learn, where to start, how to stay consistent.

### Solution

EduLife provides:
1. **Centralized platform** for all learning resources
2. **Structured learning paths** with clear progression
3. **Validated learning** through exams and certificates
4. **Teacher guidance** and mentorship
5. **Progress visibility** for students and educators

### Target Users

**Primary:**
- Students (all ages, career-focused)
- Young professionals upskilling
- Moroccan learners seeking credible education
- Teachers and trainers

**Secondary:**
- Group administrators
- Platform administrators

---

## Architecture

### Deployment Model

Three separate, independently deployable components:

| Component | Tech | Target | Repo |
|-----------|------|--------|------|
| **Android App** | Java + XML, MVVM | `origin` | This repo |
| **Backend** | Spring Boot (Modular Monolith) | `origin` | This repo |
| **Web Dashboard** | React 19, TypeScript, TanStack | `web` remote | [guided-journey-lab](https://github.com/MohamedMBG/guided-journey-lab) |

### Backend Architecture

**Modular Monolith** (single Spring Boot deployable, organized into domain modules):

```
backend/src/main/java/com/edulife/
├── auth/              (Firebase integration, JWT)
├── users/             (User CRUD, roles)
├── roles/             (Role definitions, RBAC)
├── profiles/          (Student/Teacher profiles)
├── courses/           (Course catalog, creation)
├── enrollments/       (Enrollment business logic)
├── lessons/           (Lesson content, resources)
├── progress/          (Student progress tracking)
├── exams/             (MCQ exams, scoring)
├── certificates/      (Certificate generation)
├── groups/            (User groups, classes)
├── admin/             (Admin operations)
├── security/          (Auth filters, ownership checks)
├── common/            (Shared utilities)
└── config/            (Spring Boot config)
```

**Database**: PostgreSQL with Flyway migrations (never use `ddl-auto`).

### Android Architecture

**Feature-first MVVM**:

```
app/src/main/java/com/baghdad/edulife/
├── core/
│   ├── network/       (Retrofit, OkHttp, interceptors)
│   ├── session/       (Auth token management)
│   └── storage/       (SharedPreferences, local caching)
├── features/
│   ├── auth/          (Login, Register, Email verification)
│   ├── courses/       (Browse, Discover, Details)
│   ├── enrollments/   (Enroll flow)
│   ├── lessons/       (Watch, track progress)
│   ├── exams/         (Take exam, view results)
│   ├── certificates/  (View, download)
│   ├── profile/       (User profile management)
│   └── onboarding/    (First-time user setup)
```

### Web Architecture

**React 19 + TanStack Start**:

```
guided-journey-lab/
├── src/routes/       (TanStack Router file-based routes)
├── src/components/   (Reusable UI components)
├── src/features/     (Feature modules)
├── src/lib/
│   ├── api/          (Backend API client)
│   ├── auth/         (Auth state & context)
│   └── utils/        (Helpers)
└── src/types/        (TypeScript definitions)
```

### Security Model

- **Firebase Authentication**: Client-side auth, backend validates tokens
- **Server-side User Resolution**: Backend resolves `firebase_uid` to internal `userId`
- **Ownership Validation**: Backend enforces ownership on enrollments, progress, exams
- **Exam Security**: Correct answers never sent to client; scoring always server-side
- **CORS**: Explicit origins, no wildcards
- **Email Verification**: Enforced before learner access

---

## Tech Stack

### Backend
- **Language**: Java 17+
- **Framework**: Spring Boot 3.x
- **Database**: PostgreSQL 14+
- **Migrations**: Flyway
- **Auth**: Firebase Admin SDK
- **Build**: Maven

### Android
- **Language**: Java
- **Min SDK**: API 24 (Android 7.0)
- **Target SDK**: Latest
- **Architecture**: MVVM
- **HTTP Client**: Retrofit 2.x + OkHttp
- **JSON**: Gson
- **Navigation**: Android Navigation Component
- **Async**: ViewModel, LiveData
- **UI**: Material Design 3

### Web
- **Framework**: React 19
- **Language**: TypeScript
- **Router**: TanStack Start (File-based routing)
- **State**: React hooks + Context
- **HTTP Client**: Fetch or TanStack Query
- **UI**: shadcn/ui components
- **Styling**: Tailwind CSS v4
- **Build**: Vite + Cloudflare Workers
- **Package Manager**: Bun

### Shared
- **Auth**: Firebase (client SDKs)
- **Deployment**: Docker (backend)

---

## Repository Structure

```
EduLife/
├── app/                        # Android app (Java + XML)
│   ├── src/
│   ├── build.gradle
│   └── google-services.json    (required, see setup)
├── backend/                    # Spring Boot monolith
│   ├── src/main/java/com/edulife/
│   ├── src/main/resources/db/migration/  (Flyway migrations)
│   ├── pom.xml
│   └── .env.example
├── guided-journey-lab/         # React web app
│   ├── src/
│   ├── package.json
│   ├── .env.example
│   └── wrangler.jsonc          (Cloudflare Workers)
├── docs/                       # Audit & architecture docs
├── diagrams/                   # System diagrams
├── AGENTS.md                   # Product & architecture spec
├── CLAUDE.md                   # Claude Code operational guide
└── README.md                   # This file
```

---

## Getting Started

### Prerequisites

- **Git** (with `git subtree` for web deployments)
- **Java 17+** (Android & Backend)
- **Android Studio** (Android development)
- **Maven 3.8+** (Backend)
- **Node.js 18+** or **Bun** (Web)
- **PostgreSQL 14+** (Backend database)
- **Firebase Project** (Authentication)

### Clone the Repository

```bash
git clone https://github.com/MohamedMBG/EduLife
cd EduLife
```

### Configure Firebase

1. Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com)
2. Enable Email/Password authentication
3. Download credentials:
   - **Android**: `google-services.json` → `app/`
   - **Backend**: Service account JSON → set `FIREBASE_ADMIN_CREDENTIALS_JSON` env var
   - **Web**: Copy config to `guided-journey-lab/.env`

---

## Development

### Backend Setup

```bash
cd backend

# Configure environment variables
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/edulife
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=yourpassword
export FIREBASE_ADMIN_CREDENTIALS_PATH=/path/to/service-account.json
export APP_CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
export EDULIFE_AVATAR_STORAGE_DIR=/tmp/avatars
export EDULIFE_AVATAR_PUBLIC_BASE_URL=http://localhost:8080/avatars

# Run the application
./mvnw spring-boot:run
```

Backend runs on `http://localhost:8080`.

### Android Setup

1. Open `app/` in Android Studio
2. Ensure `google-services.json` is placed in `app/`
3. Sync Gradle
4. Run on emulator or device

### Web Setup

```bash
cd guided-journey-lab

# Copy environment template
cp .env.example .env

# Install dependencies
bun install  # or npm install

# Start dev server
bun run dev
```

Web runs on `http://localhost:5173`.

**Demo Mode** (without backend):
```bash
VITE_DEMO_MODE=true bun run dev
```

This keeps all data browser-local for testing UI/UX.

---

## Development Guidelines

See **[CLAUDE.md](./CLAUDE.md)** for operational rules and **[AGENTS.md](./AGENTS.md)** for product & architecture decisions.

### Key Principles

1. **Backend is the foundation** — all business logic, validation, security
2. **Never trust client data** — validate and resolve server-side
3. **No fake data** — use real backend or mark as demo-only
4. **Migrations are immutable** — never edit applied Flyway migrations
5. **Conventional Commits** — `feat(android):`, `fix(backend):`, `docs:`, etc.
6. **No microservices in MVP** — monolith only
7. **Definition of Done**:
   - Code compiles
   - Tests pass (if applicable)
   - API works with real backend
   - Flyway migrations run cleanly
   - Security/ownership checks in place
   - Audit doc created in `/docs`

---

## API Documentation

**Base URL**: `http://localhost:8080/api`

Key endpoints (see backend code for full list):

- **Auth**: `POST /auth/register`, `POST /auth/login`, `POST /auth/verify-email`
- **Courses**: `GET /courses`, `GET /courses/{id}`, `POST /courses/{id}/enroll`
- **Progress**: `GET /progress/{enrollmentId}`, `PUT /progress/{enrollmentId}/lesson/{lessonId}`
- **Exams**: `GET /exams/{examId}`, `POST /exams/{examId}/submit`
- **Certificates**: `GET /certificates/{id}/download`

See `backend/HELP.md` for detailed API specs.

---

## Deployment

### Backend (Spring Boot)

```bash
# Build JAR
cd backend
./mvnw clean package -DskipTests

# Run JAR
java -Dspring.config.location=classpath:,file:/etc/edulife/ -jar target/edulife-api.jar
```

**Docker** (recommended):
```dockerfile
FROM eclipse-temurin:21-jdk
COPY backend/target/edulife-api.jar /app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Android (Google Play)

1. Build signed APK in Android Studio
2. Upload to Google Play Console
3. Roll out through testing → staging → production

### Web (Cloudflare Workers)

Push using git subtree:

```bash
git subtree push --prefix=guided-journey-lab web main
```

(See [CLAUDE.md](./CLAUDE.md) for web deployment rules.)

---

## Contributing

This is a focused MVP project. Follow the guidelines in [CLAUDE.md](./CLAUDE.md) and [AGENTS.md](./AGENTS.md) before contributing.

**Commit Message Format**:
```
feat(android): add lesson video player
fix(backend): correct exam scoring logic
docs: update API documentation
chore: upgrade dependencies
```

**No Co-Authored-By trailers** — attribution handled separately.

---

## License

Proprietary. All rights reserved.

---

## Author

**Mohamed Baghdad**  
GitHub: [@MohamedMBG](https://github.com/MohamedMBG)  
Email: [mohamed.baghdad.dev@gmail.com](mailto:mohamed.baghdad.dev@gmail.com)

---

## Status

**Current Phase**: MVP Development  
**Latest**: Backend modular monolith + Android MVVM + React web dashboard  
**Next**: Core learner flow validation (enroll → learn → exam → certificate)
