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

EduLife is being built as a realistic MVP. The focus is on delivering the core learner loop reliably before expanding into broader platform features.

## Repository Overview

This repository contains the main EduLife product codebase:

- `app/` - Android application built with Java, XML, and pragmatic MVVM
- `backend/` - Spring Boot modular monolith with PostgreSQL and Flyway
- `guided-journey-lab/` - React web client for the platform experience
- `docs/` - task audits, architecture notes, and implementation records

## Product Surfaces

EduLife is delivered through three coordinated application layers:

- **Android app** for the primary learner experience
- **Backend API** for business rules, data access, security, exams, and certificates
- **Web client** for the browser-based platform experience and operational workflows

All clients depend on the same backend contracts and security rules.

## Architecture

EduLife keeps the MVP intentionally simple:

- **Backend**: modular monolith, not microservices
- **Android**: feature-first MVVM
- **Web**: React + TypeScript client integrated with the same backend
- **Auth**: Firebase on the client, validated server-side with internal user resolution

The backend is the source of truth for permissions, enrollments, exams, and certificates.

### Backend Design

The backend is one Spring Boot application organized by domain modules instead of separate services. This keeps deployment and development simple while still maintaining clean boundaries between areas such as:

- authentication and user identity
- profiles and roles
- courses, lessons, and enrollments
- progress tracking
- exams and certificates
- admin and group operations

This approach fits the current MVP stage: one deployable system, clear module ownership, and no unnecessary distributed-system complexity.

### Android Design

The Android app follows pragmatic MVVM with a feature-first structure:

- `ui/` for screens and rendering
- `viewmodel/` for state handling
- `data/` for repositories and API calls
- `model/` for feature models and UI state

The Android client is meant to stay thin. Business rules remain on the backend, while the app focuses on navigation, state, and user interaction.

### Web Design

The web client is a React and TypeScript application that consumes the same backend APIs. It is used for the browser experience and complements the mobile flow with a modern frontend architecture centered on routing, reusable components, and shared API utilities.

### Security Model

EduLife uses Firebase for authentication on the client side, but access control is enforced on the backend. The backend validates Firebase tokens, resolves the internal user record, checks role permissions, and protects learner data and exam integrity server-side.

## Repository Structure

```text
EduLife/
|-- app/
|-- backend/
|-- guided-journey-lab/
|-- docs/
|-- diagrams/
|-- README.md
|-- AGENTS.md
|-- CLAUDE.md
```

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

## Principles

- Keep the learner journey first
- Keep business logic on the backend
- Keep architecture clean but pragmatic
- Prefer vertical slices over disconnected mock work
- Avoid overbuilding beyond MVP scope

## Where To Look Next

- [AGENTS.md](./AGENTS.md) for product and architecture rules
- [CLAUDE.md](./CLAUDE.md) for repository workflow guidance
- [`backend/`](./backend) for API and business logic
- [`app/`](./app) for Android client work
- [`guided-journey-lab/`](./guided-journey-lab) for the web client

## Author

Mohamed Baghdad  
GitHub: [@MohamedMBG](https://github.com/MohamedMBG)
