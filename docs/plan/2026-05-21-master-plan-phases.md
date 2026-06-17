# EduLife — Master Development Plan

**Date:** 2026-05-21  
**Scope:** Backend (Spring Boot) + Mobile (Android) — full phase-by-phase roadmap  
**Current state:** Phase 0 complete, Phase 1 in progress

---

## Legend

- ✅ Done
- 🔄 In Progress
- ⬜ Planned

---

## PHASE 0 — Foundation ✅

Both sides complete. No remaining work.

### Backend
- Spring Boot 3.5 + PostgreSQL + Flyway migrations
- Firebase Admin SDK + `FirebaseTokenFilter` (stateless auth)
- `POST /api/v1/auth/sync` — creates/fetches internal user from Firebase UID
- Course entities: `Course`, `CourseSection`, `Lesson`
- Flyway: `V1__init.sql`, `V2__courses.sql`, `V3__seed_courses.sql`
- Global error handler (`GlobalApiExceptionHandler` → `ApiError` JSON)

### Mobile
- Android MVVM skeleton (`EduLifeApp`, `MainActivity`)
- Firebase Auth + OkHttp interceptor + token refresh authenticator
- Onboarding → Login → Register flow
- Bottom nav: Home / Courses / Profile
- `SessionStorage` (userId, role via SharedPreferences)
- `ApiClient` singleton (Retrofit + Gson + logging)

---

## PHASE 1 — Course Browsing ✅ + Enrollment 🔄

### Backend
| Status | Endpoint | Notes |
|--------|----------|-------|
| ✅ | `GET /api/v1/courses` | Paginated, level filter |
| ✅ | `GET /api/v1/courses/{id}` | Detail + sections + lessons |
| ⬜ | `POST /api/v1/enrollments` | Enroll current user in a course |
| ⬜ | `GET /api/v1/enrollments` | List current user's enrolled courses |

**DB:** `V4__enrollments.sql`
```sql
enrollments (
  id           UUID PRIMARY KEY,
  user_id      UUID NOT NULL REFERENCES users(id),
  course_id    UUID NOT NULL REFERENCES courses(id),
  enrolled_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  UNIQUE (user_id, course_id)
)
```

### Mobile
| Status | Task |
|--------|------|
| ✅ | `CoursesFragment` + catalog with level filter |
| ✅ | `CourseDetailFragment` |
| ✅ | `EnrollCourseFragment` UI frame |
| ⬜ | Wire `EnrollCourseFragment` → `POST /api/v1/enrollments` |
| ⬜ | `HomeFragment` show enrolled courses from `GET /api/v1/enrollments` |

**Exit criteria:** User can browse catalog, open detail, enroll, see enrolled list on Home.

---

## PHASE 2 — Lesson Player ⬜

### Backend
| Status | Task |
|--------|------|
| ✅ | `Lesson` entity with `type`, `duration_minutes`, `is_preview`, `summary` |
| ⬜ | Add `content_url` column to `lessons` via `V5__lesson_content.sql` |
| ⬜ | `GET /api/v1/courses/{courseId}/lessons/{lessonId}` — full lesson with content URL |

### Mobile
| Status | Task |
|--------|------|
| ✅ | `LessonPlayerFragment` UI frame + nav args |
| ⬜ | Fetch full lesson from API |
| ⬜ | `VIDEO` type: integrate ExoPlayer |
| ⬜ | `TEXT` type: render HTML/markdown content |
| ⬜ | On finish → call `POST /api/v1/progress/lessons/{lessonId}/complete` |

**Exit criteria:** User can open any lesson, watch video or read text, trigger completion.

---

## PHASE 3 — Progress Tracking ⬜

### Backend
**DB:** `V6__progress.sql`
```sql
lesson_progress (
  id           UUID PRIMARY KEY,
  user_id      UUID NOT NULL REFERENCES users(id),
  lesson_id    UUID NOT NULL REFERENCES lessons(id),
  completed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (user_id, lesson_id)
)

course_progress (
  id                UUID PRIMARY KEY,
  user_id           UUID NOT NULL REFERENCES users(id),
  course_id         UUID NOT NULL REFERENCES courses(id),
  percent_complete  SMALLINT NOT NULL DEFAULT 0,
  last_accessed_at  TIMESTAMPTZ,
  UNIQUE (user_id, course_id)
)
```

| Status | Endpoint |
|--------|----------|
| ⬜ | `POST /api/v1/progress/lessons/{lessonId}/complete` |
| ⬜ | `GET /api/v1/progress/courses/{courseId}` — percent complete per section |

Auto-compute `course_progress.percent_complete` whenever a lesson is marked complete.

### Mobile
| Status | Task |
|--------|------|
| ⬜ | `CourseDetailFragment`: progress bar per section |
| ⬜ | `HomeFragment`: percent complete on enrolled course cards |
| ⬜ | `LessonPlayerFragment`: mark complete on video end / text scroll-bottom |

**Exit criteria:** Progress persists server-side and surfaces everywhere in the UI.

---

## PHASE 4 — Profile ⬜

### Backend
**DB:** `V7__profiles.sql`
```sql
profiles (
  user_id      UUID PRIMARY KEY REFERENCES users(id),
  display_name VARCHAR(100),
  avatar_url   TEXT,
  bio          TEXT,
  updated_at   TIMESTAMPTZ
)
```

| Status | Endpoint |
|--------|----------|
| ⬜ | `GET /api/v1/profile` — current user profile + stats |
| ⬜ | `PUT /api/v1/profile` — update display_name, bio |
| ⬜ | `POST /api/v1/profile/avatar` — upload avatar (Firebase Storage or S3) |

### Mobile
| Status | Task |
|--------|------|
| ⬜ | `ProfileFragment`: display_name, email, avatar, enrolled count |
| ⬜ | Edit profile screen |
| ⬜ | Avatar pick + upload |
| ⬜ | Stats: courses enrolled, lessons completed, certificates earned |

**Exit criteria:** Profile page fully functional with real data and editable fields.

---

## PHASE 5 — Exams & Certificates ⬜

### Backend
**DB:** `V8__exams.sql`
```sql
exams (
  id                  UUID PRIMARY KEY,
  course_id           UUID NOT NULL REFERENCES courses(id),
  title               VARCHAR(200),
  pass_score          SMALLINT NOT NULL DEFAULT 80,
  time_limit_minutes  SMALLINT
)

exam_questions (
  id            UUID PRIMARY KEY,
  exam_id       UUID NOT NULL REFERENCES exams(id),
  question_text TEXT NOT NULL,
  order_index   SMALLINT NOT NULL
)

exam_choices (
  id           UUID PRIMARY KEY,
  question_id  UUID NOT NULL REFERENCES exam_questions(id),
  choice_text  TEXT NOT NULL,
  is_correct   BOOLEAN NOT NULL DEFAULT false
)

exam_attempts (
  id        UUID PRIMARY KEY,
  user_id   UUID NOT NULL REFERENCES users(id),
  exam_id   UUID NOT NULL REFERENCES exams(id),
  score     SMALLINT NOT NULL,
  passed    BOOLEAN NOT NULL,
  taken_at  TIMESTAMPTZ NOT NULL DEFAULT now()
)
```

**DB:** `V9__certificates.sql`
```sql
certificates (
  id                 UUID PRIMARY KEY,
  user_id            UUID NOT NULL REFERENCES users(id),
  course_id          UUID NOT NULL REFERENCES courses(id),
  issued_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
  certificate_number VARCHAR(50) UNIQUE NOT NULL,
  UNIQUE (user_id, course_id)
)
```

| Status | Endpoint |
|--------|----------|
| ⬜ | `GET /api/v1/courses/{id}/exam` — questions (shuffled choices, `is_correct` hidden) |
| ⬜ | `POST /api/v1/courses/{id}/exam/submit` — auto-score, return result, issue cert on pass |
| ⬜ | `GET /api/v1/certificates` — list user's certificates |

### Mobile
| Status | Task |
|--------|------|
| ⬜ | `ExamFragment`: timed MCQ screen |
| ⬜ | Submit → score screen with pass/fail |
| ⬜ | `CertificateFragment`: view certificate, share as image |
| ⬜ | Profile: certificates count + list |

**Exit criteria:** Full exam flow. Certificate issued on pass. Shareable from app.

---

## PHASE 6 — Groups / Cohorts ⬜

### Backend
**DB:** `V10__groups.sql`
```sql
groups (
  id          UUID PRIMARY KEY,
  name        VARCHAR(200) NOT NULL,
  created_by  UUID NOT NULL REFERENCES users(id),
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
)

group_members (
  group_id   UUID NOT NULL REFERENCES groups(id),
  user_id    UUID NOT NULL REFERENCES users(id),
  role       VARCHAR(20) NOT NULL DEFAULT 'STUDENT',
  joined_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (group_id, user_id)
)

group_enrollments (
  group_id   UUID NOT NULL REFERENCES groups(id),
  course_id  UUID NOT NULL REFERENCES courses(id),
  PRIMARY KEY (group_id, course_id)
)
```

| Status | Endpoint | Role required |
|--------|----------|---------------|
| ⬜ | `POST /api/v1/groups` | TEACHER / ADMIN |
| ⬜ | `POST /api/v1/groups/{id}/members` | TEACHER / ADMIN |
| ⬜ | `POST /api/v1/groups/{id}/courses` | TEACHER / ADMIN |
| ⬜ | `GET /api/v1/groups/{id}/progress` | TEACHER / ADMIN |

### Mobile
| Status | Task |
|--------|------|
| ⬜ | Groups screen (under Profile or new tab) |
| ⬜ | Group detail: member list + progress table |
| ⬜ | Instructor view: who completed what |

**Exit criteria:** Teacher creates group, assigns course, monitors member progress.

---

## PHASE 7 — Admin Panel ⬜

### Backend
All endpoints gated to `ADMIN` role (or `TEACHER` where noted).

| Status | Endpoint | Purpose |
|--------|----------|---------|
| ⬜ | `GET /api/v1/admin/users` | List users, filter by role |
| ⬜ | `PUT /api/v1/admin/users/{id}/role` | Promote/demote user |
| ⬜ | `POST /api/v1/admin/courses` | Create course (TEACHER+) |
| ⬜ | `PUT /api/v1/admin/courses/{id}` | Update/publish/archive |
| ⬜ | `POST /api/v1/admin/courses/{id}/sections` | Add section |
| ⬜ | `POST /api/v1/admin/sections/{id}/lessons` | Add lesson |
| ⬜ | `POST /api/v1/admin/courses/{id}/exam` | Create exam + questions |

### Mobile / Web
Admin is better served by a web dashboard than mobile screens.  
Options:
- **Recommended:** React / Next.js admin panel (separate repo)
- **Alternative:** Role-gated `AdminFragment` in-app for ADMIN users only

**Exit criteria:** Admin can create full course with sections, lessons, and exam. Teacher can manage their own courses.

---

## PHASE 8 — Polish & Production ⬜

### Backend
| Area | Task |
|------|------|
| Performance | Redis cache for `GET /api/v1/courses` (public catalog) |
| Search | `GET /api/v1/courses?q=` using PostgreSQL `tsvector` full-text search |
| Pagination | Cursor-based pagination for large catalogs |
| Rate limiting | Bucket4j per-user rate limits on exam submit, enrollment |
| Notifications | Firebase Cloud Messaging for new courses, exam results |
| Deployment | Docker Compose → Railway / Render / AWS |

### Mobile
| Area | Task |
|------|------|
| Offline | Room DB cache for enrolled courses + lesson progress |
| Search | Search bar in `CoursesFragment` |
| Push | FCM integration for backend notifications |
| UX | Dark mode support |
| Release | ProGuard config, signing config, Play Store listing |

**Exit criteria:** App ready for public release on Play Store.

---

## Database Migration Timeline

| Migration | Content |
|-----------|---------|
| V1 | users |
| V2 | courses, course_sections, lessons |
| V3 | seed data |
| V4 | enrollments |
| V5 | lessons.content_url |
| V6 | lesson_progress, course_progress |
| V7 | profiles |
| V8 | exams, exam_questions, exam_choices, exam_attempts |
| V9 | certificates |
| V10 | groups, group_members, group_enrollments |

---

## Recommended Build Order

```
Phase 1 (finish)  →  Enrollment API + mobile wiring
Phase 2           →  Lesson player (ExoPlayer + content_url)
Phase 3           →  Progress tracking (unblocks Phase 5)
Phase 4           →  Profile (real data + avatar)
Phase 5           →  Exams + Certificates  ← high value for PFA demo
Phase 6           →  Groups / Cohorts
Phase 7           →  Admin panel
Phase 8           →  Polish + Play Store release
```

Phase 5 (Exams + Certificates) is the highest-value feature for academic demos and PFA reports. Prioritize it after progress tracking is stable.
