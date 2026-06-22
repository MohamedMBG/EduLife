# EduLife

EduLife is a mobile-first learning platform built for Moroccan learners. It turns scattered learning resources into one guided path: discover a course, enroll, study lessons, pass an exam, and earn a certificate.

## What EduLife Is

EduLife is focused on a practical MVP for structured digital learning. The platform is designed around:

- guided course discovery
- secure authentication and role-based access
- lesson progression tracking
- server-scored MCQ exams
- certificate delivery after passing

Core learner journey:

```text
Discover -> Enroll -> Learn -> Take exam -> Pass -> Receive certificate
```

## Repository Overview

This repository contains the main EduLife product codebase:

- `app/` - Android application built with Java, XML, and pragmatic MVVM
- `backend/` - Spring Boot modular monolith with PostgreSQL and Flyway
- `guided-journey-lab/` - React web client for the platform experience
- `docs/` - task audits, architecture notes, and implementation records

## Architecture Snapshot

EduLife keeps the MVP intentionally simple:

- **Backend**: modular monolith, not microservices
- **Android**: feature-first MVVM
- **Web**: React + TypeScript client integrated with the same backend
- **Auth**: Firebase on the client, validated server-side with internal user resolution

The backend is the source of truth for permissions, enrollments, exams, and certificates.

## Tech Stack

- **Android**: Java, XML, ViewModel, LiveData, Retrofit, Navigation Component
- **Backend**: Spring Boot, PostgreSQL, Flyway, Firebase Admin SDK
- **Web**: React, TypeScript, TanStack Router/Query, Vite
- **Storage / Media**: Cloudinary for hosted images, filesystem or external storage where appropriate

## Getting Started

### Prerequisites

- Java 21
- Android Studio
- PostgreSQL
- Node.js or Bun
- Firebase project credentials

### Run the Backend

```bash
cd backend
./mvnw spring-boot:run
```

### Run the Web App

```bash
cd guided-journey-lab
npm install
npm run dev
```

### Run the Android App

Open the project in Android Studio, add the required Firebase configuration, then run the app on an emulator or device.

## Project Status

EduLife is under active MVP development. The current direction prioritizes the end-to-end learner flow before expanding into broader CMS, social, payment, or AI-heavy features.

## Where To Look Next

- [AGENTS.md](./AGENTS.md) for product and architecture rules
- [CLAUDE.md](./CLAUDE.md) for repository workflow guidance
- [`backend/`](./backend) for API and business logic
- [`app/`](./app) for Android client work
- [`guided-journey-lab/`](./guided-journey-lab) for the web client

## Author

Mohamed Baghdad  
GitHub: [@MohamedMBG](https://github.com/MohamedMBG)
