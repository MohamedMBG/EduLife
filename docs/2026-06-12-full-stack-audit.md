# Full-Stack Audit — Backend, Android, Web

## Goal

Complete audit of all three deployables against CLAUDE.md / AGENTS.md rules: architecture, security, quality, MVP coverage.

## Backend (Spring Boot)

### Module inventory

Present: `account, admin, auth, certificates, common, config, courses, enrollments, exams, groups, profiles, progress, roles, security, teacherrequests, users`.
Notes: `lessons` lives inside `courses/` (LessonController/LessonService) rather than its own module; `roles/` is package-info only (UserRole enum lives in `users/model`). Extra modules beyond spec: `account`, `teacherrequests`. All modules follow controller/service/repository/dto/entity/exception layout.

### Security audit

| Rule | Verdict | Evidence |
|---|---|---|
| Firebase token validation on protected routes | PASS | `security/FirebaseTokenFilter.java` + `SecurityConfig.java:94-107` — `/api/v1/**` authenticated, `anyRequest().denyAll()` |
| email_verified enforced | PASS | `FirebaseTokenFilter.java:80-87` |
| Internal user resolved server-side | PASS | every service has `resolveCurrentUser()` from SecurityContext; role authorities loaded from users table, never from token claims (`FirebaseTokenFilter.java:92-96`) |
| firebase_uid never exposed | PASS | no DTO contains it; `UserSummaryDto` documents intentional omission |
| Ownership checks | PASS | enrollments (`EnrollmentService.java:87`), exams (enrollment-gated), certificates (access-denied exception + tests), CMS course/section/lesson/exam ownership via `createdByUserId` in all Cms*Service |
| Correct answers never serialized | PASS | `ExamDto.ChoiceDto(id, choiceText)` only; choices shuffled server-side; `ExamAnswerSecurityTest` exists |
| Scoring backend-only | PASS | `ExamService.submitExam` |
| 2 failures → 72h cooldown | PASS | `ExamService.java:150-159` |
| Certificates backend-only, after pass | PASS | generated inside `submitExam` transaction only on pass |
| Enrollment transactional + initial progress | PASS | `EnrollmentService.enroll` `@Transactional`, calls `progressService.initializeCourseProgress` |
| Avatar 5MB | PASS | `application.yaml` multipart 5MB/6MB + `max-file-bytes` + 413 handler |
| CORS explicit | PASS | `CorsProperties` allowlist, localhost defaults, prod override documented |
| ddl-auto | PASS | `ddl-auto: none`, Flyway enabled, V1–V18 sequential |
| Error contract | PASS | `GlobalApiExceptionHandler` — stable `{status,message,timestamp}`, generic 500, no stack traces |

### Issues (ranked)

1. **Pass threshold mismatch.** CLAUDE.md/AGENTS.md say 80%; code uses 70 everywhere — `V9__exams.sql:5` (`DEFAULT 70`), all 5 seeded exams 70, `CreateExamRequest.java:18` comment "Plan specifies 70 as default". Spec or code wrong — needs decision.
2. **V18 seed admin fragile** — `UPDATE users SET role='ADMIN' WHERE email='admin@edulife.test'` is a no-op if the account doesn't exist at migration time; test-domain email baked into a permanent migration. Not exploitable (runs once), but admin may silently never be seeded.
3. **Exam fetch not cooldown-gated** — `getExam` allows fetching questions while in cooldown (submit is blocked, status endpoint exists). Minor: lets a cooled-down user farm question/choice text.
4. **Scope creep vs MVP** — admin metrics, CMS (courses/sections/lessons/exams), groups, teacher requests, account anonymization all built. Well-secured, but CLAUDE.md MVP boundaries exclude CMS/admin dashboards.

### Tests

19 test classes incl. security-critical: `ExamAnswerSecurityTest`, `FirebaseTokenFilterSecurityTest`, `SecurityDefaultCorsTest`, `SecurityHardeningTest`, `RateLimitRetryAfterTest`, avatar storage, progress, enrollment, certificate controllers.

### Verdict

Strongest component. Production-grade security posture: hardened headers (HSTS, frame deny, no-referrer, server header cleared), rate limiting with Retry-After, health-only actuator, env-driven secrets, clean Flyway chain.

## Android (Java + XML, MVVM)

### Compliance

- 0 Kotlin files; Java + XML only. No Hilt/Dagger; manual DI.
- Feature-first MVVM: `core/{network,session,storage}` + `features/{admin,auth,certificates,courses,onboarding,profile,teacher}` each with data/model/ui/viewmodel.
- No API calls in fragments — grep for ApiClient/ApiService/retrofit in `ui/*.java` returns zero.
- Token refresh synchronized: `FirebaseTokenAuthenticator` serializes forced refreshes behind static lock, retry capped at 1, session-expired posted via `SessionEventBus`. `FirebaseAuthInterceptor` uses cached token with bounded timeout.
- Per-feature UiState classes (loading/error/empty/success) present across courses, exams, enrollment, admin, teacher.
- Session: EncryptedSharedPreferences (`security-crypto`). Release: R8 minify + resource shrink + ProGuard rules. Logging redacts Authorization; BODY level debug-only.
- `google-services.json` gitignored.
- API base URL injected via gradle property / local.properties (`http://10.0.2.2:8080/api/v1/` default).
- No fake/hardcoded data found where real endpoints exist.

### Issues (ranked)

1. **Scope creep** — teacher CMS feature module + admin dashboard/teacher-requests outside MVP boundaries (mirrors backend creep; consistent, deliberate).
2. **Google sign-in button is a visual placeholder** (`LoginFragment.java:57`) — UI element with no function; either wire or remove.
3. Old library versions: Retrofit 2.9 (2.11 current), lifecycle 2.7, ViewPager2 1.0 — works, not urgent.
4. `security-crypto 1.1.0-alpha06` — alpha dependency in a security-sensitive path (documented reason in build file).
5. Duplicate `recyclerview` dependency declared twice in `app/build.gradle.kts`.

### Verdict

Clean, rule-compliant. Network/auth core is solid. MVP screens all present: login/register, onboarding, catalog, course detail, enroll, lesson player, exam + result, certificates, profile.

## Web (React 19 + TanStack)

### Compliance

- Routes cover MVP and more: index, login, register, forgot-password, dashboard, explore, courses (catalog/detail/resources/exam/result), learn.$courseId.$lessonId, level, profile, certificates (list/detail/verify).
- `lib/api/client.ts` implements full backend contract: auth/sync, courses, enrollments, progress mark-complete, exam get/status/submit, certificates incl. download + public verify, profile + avatar. Paths match backend controllers.
- Auth: Firebase with session persistence — tokens NOT in localStorage (`lib/auth/firebase.ts:35`). `RequireAuth` wrapper guards authed routes. Errors typed via `ApiClientError`; env validation with actionable message (`lib/env.ts`).
- Demo mode is explicit opt-in (`VITE_DEMO_MODE`); in demo, network requests hard-disabled and exams return 501 "not available in demo mode" — fake data clearly fenced, not masquerading.
- `.env` gitignored; `.env.example` provided; no secrets in repo.

### Issues (ranked)

1. **Dark-mode key desync** — `components/landing/Nav.tsx` reads/writes localStorage key `"theme"` while `AppShell.tsx` + `__root.tsx` inline script use `"edulife-dark"`. Landing toggle and app toggle can disagree; flash-of-wrong-theme on landing.
2. **Route guards are component-level only** — no TanStack `beforeLoad` redirects; unauthenticated users mount the route shell before `RequireAuth` kicks in. Works, but router-level guard is cleaner.
3. `intendedRole` stored in localStorage (`auth-context.tsx:33,333`) — UX hint only, backend resolves real role, so not a security hole; keep it untrusted.
4. `wrangler.jsonc` name is generic `tanstack-start-app`.
5. `src/routeTree.gen.ts` modified but uncommitted (generated file housekeeping).

### Verdict

Real-backend integration throughout; demo mode properly fenced. Contract alignment with backend confirmed (endpoints, 429 cooldown payload with `cooldownEndsAt`).

## Cross-cutting findings

1. Pass threshold 70 vs documented 80 — single source of truth needed; affects all three clients' display copy too.
2. Both clients consume identical contracts — shared-API rule respected.
3. Scope creep (CMS/admin/groups/teacher-requests) is consistent and well-secured across backend+Android, but web has no CMS/admin UI — asymmetry to either accept or document.

## Files Touched

- docs/2026-06-12-full-stack-audit.md (this file only — read-only audit)

## Tests / Verification

Static review only; no builds run.

## Risks / Notes

See ranked issues per component above.
