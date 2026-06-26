# EduLife — Use Case Diagram Notes

## Diagram File

- **PlantUML source:** `diagrams/edulife-use-case-diagram.puml`
- **Rendered output:** `diagrams/edulife-use-case-diagram.png` / `.svg`

The current French diagram is intentionally more compact than the earlier exhaustive version: several closely related actions are grouped into broader use cases so the final shape stays less vertical and more rectangular for report use.

---

## Actors Identified

### Left Side — Human Actors

| Actor | Role | Source |
|-------|------|--------|
| **Learner (Apprenant)** | Registered user with LEARNER role. Core consumer of learning content. In the current French diagram, public discovery and certificate verification are grouped under this actor because the separate Visitor actor was removed. | `UserRole.LEARNER` — default role on registration |
| **Teacher (Enseignant)** | Content creator with TEACHER role. Creates courses, sections, lessons, and exams via CMS. | `UserRole.TEACHER` — granted after admin-approved teacher request |
| **Group Administrator** | Institute/organization manager with GROUP_ADMIN role. Manages groups, members, and course approvals within their institute. | `UserRole.GROUP_ADMIN` — assigned by platform admin |
| **Platform Administrator** | System-wide supervisor with ADMIN role. Manages users, reviews teacher applications, publishes courses, and monitors platform metrics. | `UserRole.ADMIN` — assigned via `StaffRoleProperties` config |

### Right Side — External Technical Systems

| System | Role | Source |
|--------|------|--------|
| **Firebase Authentication Service** | Handles user registration, login, email verification, and token generation. Backend validates tokens server-side via Firebase Admin SDK. In the current diagram it is rendered as a rectangle rather than a stickman. | `FirebaseConfig.java`, `FirebaseTokenFilter.java`, `FirebaseAuthInterceptor.java` |
| **Cloudinary Media Storage** | Stores course cover images and user avatar uploads. Backend integrates via Cloudinary SDK with local fallback for development. | `CloudinaryConfig.java`, `CloudinaryStorageService.java` |
| **Groq LLM AI Service** | Powers the Career Advisor feature. Backend sends learner goals + course catalog to Groq API, receives AI-generated course recommendations. | `GroqLlmClient.java`, `AdvisorService.java` |

---

## Two Principal Services

### 1. Learning & Certification Service

Covers the complete learner journey from registration to certification.

| Use Case | Justification |
|----------|--------------|
| Register | `AuthController.sync` — creates user on first Firebase login |
| Log In | Firebase auth + backend token validation |
| Verify Email | Firebase `email_verified` enforced before learner-flow access |
| Sync Identity with Backend | `POST /api/v1/auth/sync` — maps Firebase UID to internal user |
| Manage Profile | `ProfileController` — edit name, bio, upload avatar |
| Upload Avatar | `POST /api/v1/profile/avatar` — stored via Cloudinary |
| Browse Courses | `GET /api/v1/courses` — public catalog of published courses |
| View Course Details | `GET /api/v1/courses/{id}` — sections, lessons, metadata |
| Enroll in Course | `POST /api/v1/enrollments` — transactional with initial progress |
| Access Lessons | `GET /api/v1/courses/{courseId}/lessons/{lessonId}` |
| Complete Lesson | `POST /api/v1/courses/{courseId}/lessons/{lessonId}/complete` |
| Track Progress | `GET /api/v1/progress/courses/{courseId}` |
| Take MCQ Exam | `GET /api/v1/courses/{courseId}/exam` — delivers questions without answers |
| Submit Exam | `POST /api/v1/courses/{courseId}/exam/submit` — server-side scoring |
| View Exam Result | Returned in submit response + `GET /exam/status` |
| Generate Certificate | Auto-generated on exam pass (backend-only) |
| Download Certificate | `GET /api/v1/certificates/{id}/download` — PDF |
| Verify Certificate (Public) | `GET /api/v1/certificates/verify/{hash}` — no auth required |
| View XP & Level | `GET /api/v1/gamification/me` |
| View Badges | Badge catalog + earned badges in gamification response |
| View Leaderboard | `GET /api/v1/gamification/leaderboard` |
| View Learner Analytics | `GET /api/v1/analytics/me/summary` + cohort trends |
| Use Study Planner | Client-side weekly task planner (Android + Web) |
| Use Career Advisor | `POST /api/v1/advisor/recommend` — Groq LLM integration |

### 2. Content, Group & Administration Service

Covers content creation, institutional group management, and platform administration.

| Use Case | Justification |
|----------|--------------|
| Apply as Teacher | `POST /api/v1/teacher-requests` — learner submits application |
| Review Teacher Requests | `GET/PUT /api/v1/admin/teacher-requests` — admin approves/rejects |
| Create Course | `POST /api/v1/cms/courses` — teacher creates draft course |
| Manage Sections & Lessons | CMS CRUD for sections and lessons within a course |
| Upload Course Cover Image | `POST /api/v1/cms/courses/{id}/cover-image` — via Cloudinary |
| Create Final Exam | `POST /api/v1/cms/courses/{courseId}/exam` — MCQ exam builder |
| Publish Course | `PUT /api/v1/cms/courses/{id}/publish` — admin/group-admin approval |
| Archive Course | `PUT /api/v1/cms/courses/{id}/archive` — admin removes from catalog |
| View Teacher Analytics | `GET /api/v1/analytics/teacher/courses` |
| Create Group | `POST /api/v1/groups` — institute/organization creation |
| Manage Group Members | Add/remove members in a group |
| Approve Join Requests | Teachers request to join group, group-admin approves |
| Attach Courses to Group | `POST /api/v1/groups/{groupId}/courses` |
| Approve Group Courses | Group-admin publishes courses from group teachers |
| View Group Analytics | `GET /api/v1/analytics/group/{groupId}/cohorts` |
| Manage Users & Roles | `GET/PUT /api/v1/admin/users` — role assignment |
| View Platform Metrics | `GET /api/v1/admin/metrics` — user/enrollment/certificate counts |
| View Platform Analytics | `GET /api/v1/analytics/platform` + cohort data |
| Delete Account | `DELETE /api/v1/account` — Play Store compliance |

---

## External Systems Used

| System | Integration Point | Notes |
|--------|-------------------|-------|
| Firebase Authentication | Registration, login, email verification, token validation | Admin SDK for server-side verification |
| Cloudinary | Course cover images, user avatars | With local filesystem fallback |
| Groq LLM | Career Advisor recommendations | REST API via `GroqLlmClient` |

---

## Features Excluded (Not Confirmed in Codebase)

| Feature | Reason for Exclusion |
|---------|---------------------|
| Payment / Billing | Not implemented — no payment endpoints, entities, or integrations |
| Email / Notification Service | No email sender or notification service found in codebase |
| Real-time Chat / Discussions | Not implemented |
| AI Tutoring | Advisor is recommendation-only, not interactive tutoring |
| Offline Downloads | Not implemented |
| Content Moderation (lesson-level) | Only course-level publish/archive exists |

---

## How to Include in Report

### LaTeX

```latex
\begin{figure}[H]
    \centering
    \includegraphics[width=\textwidth]{diagrams/edulife-use-case-diagram.png}
    \caption{Diagramme de cas d'utilisation — EduLife System}
    \label{fig:use-case-diagram}
\end{figure}
```

### Markdown

```markdown
![EduLife Use Case Diagram](diagrams/edulife-use-case-diagram.png)
```

### Word / Google Docs

Insert the PNG image directly. Use landscape orientation if needed for readability.

---

## Density Warning

This diagram contains **4 human actors**, **3 external systems**, and **~40 use cases** across two service groups. On a single A4 page in portrait mode, labels may become small.

**Recommendations:**
1. Use **landscape A4** for the single combined diagram
2. Alternatively, split into **two separate diagrams**:
   - Diagram A: Learning & Certification Service (Learner + external systems)
   - Diagram B: Content, Group & Administration Service (Teacher, Group Admin, Platform Admin)
3. For the report, the combined diagram provides better overview; split diagrams provide better readability
