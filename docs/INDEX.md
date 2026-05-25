# EduLife Docs Index

Last updated: 2026-05-25

---

## plan/
Strategic docs, sprint planning, execution roadmap.

| File | What |
|------|------|
| `EXECUTIVE PLAN TO FOLLOW.md` | Full MVP execution plan — phases, sprint targets, risks, first 10 tasks |
| `2026-04-26-sprint-1-issue-planning.md` | Sprint 1 issue breakdown |
| `2026-04-30-project-analysis-next-tasks.md` | Mid-project analysis and priority re-order |
| `2026-05-21-master-plan-phases.md` | Phase map update (P0–P9 issue structure) |

---

## backend/
All backend task audits — auth, courses, enrollments, progress.

### Auth & Security
| File | What |
|------|------|
| `2026-04-25-security-gitignore-hardening.md` | .gitignore + secrets hygiene |
| `2026-04-26-add-firebase-admin-sdk-dependency.md` | Firebase Admin SDK added to pom.xml |
| `2026-04-26-fix-firebase-admin-bootstrap.md` | FirebaseApp init fix |
| `2026-04-26-publish-users-migration-pr.md` | V2 users table migration PR |
| `2026-04-27-implement-firebase-token-filter.md` | FirebaseTokenFilter — token validation, email_verified check |
| `2026-04-27-republish-users-branch.md` | Branch republish after rebase |
| `2026-04-30-global-api-error-contract.md` | GlobalApiExceptionHandler — { status, message, timestamp } |
| `2026-04-30-move-auth-sync-production.md` | /auth/sync endpoint moved to production config |

### Courses
| File | What |
|------|------|
| `2026-05-01-courses-migration.md` | V3 courses/sections/lessons migration |
| `2026-05-01-seed-courses-migration.md` | Seed data for 5 Moroccan courses |
| `2026-05-01-courses-list-endpoint.md` | GET /api/v1/courses — paginated, category filter |
| `2026-05-01-create-phase-2-branch.md` | Phase 2 branch setup |
| `2026-05-04-course-schema-and-entity.md` | Course JPA entity |
| `2026-05-04-course-section-entity.md` | CourseSection JPA entity |
| `2026-05-04-lesson-entity.md` | Lesson JPA entity |
| `2026-05-04-course-dtos.md` | CourseSummaryDto, CourseDetailDto, LessonSummaryDto |
| `2026-05-04-course-repositories.md` | CourseRepository, CourseSectionRepository, LessonRepository |
| `2026-05-04-published-course-list-and-detail.md` | GET /courses + GET /courses/{id} implementation |
| `2026-05-04-course-endpoint-tests-and-access-control.md` | CourseControllerTest |
| `2026-05-04-branch-and-commit-course-work.md` | Course module commit + PR |
| `2026-05-04-branch-and-commit-course-section-entity.md` | Section entity commit |
| `2026-05-04-verify-course-endpoints-exposed.md` | SecurityConfig route verification |
| `2026-05-24-sprint-2-course-discovery-contract-pr.md` | Sprint 2 discovery contract PR audit |

### Architecture Reviews
| File | What |
|------|------|
| `backend-architecture.md` | Live architecture reference — modules, contracts, decisions |
| `2026-05-15-backend-module-roots.md` | Module root package audit |
| `2026-05-15-backend-structure-review.md` | Full backend structure review |
| `2026-05-16-backend-verdict-review.md` | Post-review verdict and action items |

### Enrollments
| File | What |
|------|------|
| `enrollment-phase1-completion.md` | Closes #245–248 — enroll, unenroll, list, idempotency |

### Progress (P3)
| File | What |
|------|------|
| `2026-05-25-progress-course-detail-endpoint.md` | Closes #254 — GET /api/v1/progress/courses/{id}, DTO contract, completedAt |
| `2026-05-25-progress-tests.md` | Closes #255 — 17 tests: idempotency, 403, 100% completion, percent accuracy |

---

## android/
All Android task audits — auth flow, course UI, networking.

### Sprint 0–1: Auth & Setup
| File | What |
|------|------|
| `2026-04-25-redesign-onboarding-screen.md` | Onboarding screen redesign |
| `2026-04-26-finish-onboarding-flow.md` | Onboarding → Login nav |
| `2026-04-26-implement-login-screen.md` | LoginFragment UI |
| `2026-04-26-create-register-screen.md` | RegisterFragment UI |
| `2026-04-26-add-android-github-workflows.md` | GitHub Actions CI for Android |
| `2026-04-27-firebase-android-bootstrap.md` | Firebase SDK wired in Android |
| `2026-04-28-fix-android-ci-lint.md` | CI lint fix |
| `2026-04-28-check-firebase-sdk-config.md` | google-services.json verification |
| `2026-04-28-wire-login-fragment.md` | Firebase signIn wired to LoginFragment |
| `2026-04-28-firebase-auth-okhttp-interceptor.md` | OkHttp interceptor adds Bearer token |
| `2026-04-28-401-retry-token-refresh-guard.md` | 401 → force-refresh token + retry once |
| `2026-04-28-session-storage.md` | userId + role stored in SharedPreferences |

### Sprint 2: Course Discovery
| File | What |
|------|------|
| `2026-05-15-android-course-models.md` | Course/Section/Lesson model classes |
| `2026-05-15-android-course-api-service.md` | Retrofit ApiService course endpoints |
| `2026-05-15-android-course-repository.md` | CourseRepository wired to API |
| `2026-05-15-course-catalog-ui.md` | CourseCatalogFragment + ViewModel |

### Bug Fixes & Device Issues
| File | What |
|------|------|
| `2026-05-19-debug-cleartext-local-api.md` | Cleartext HTTP fix for local dev |
| `2026-05-19-fix-physical-device-login-api-url.md` | API URL fix for physical device |
| `2026-05-19-resolve-merge-conflicts.md` | Merge conflict resolution |
| `2026-05-23-login-sync-timeout-fix.md` | Login + /auth/sync timeout fix |
| `2026-05-24-improve-android-home-profile-ui.md` | Home + Profile UI polish |

---

## reports/
PFA academic report updates and web landing docs.

### PFA Report
| File | What |
|------|------|
| `2026-05-19-update-pfa-report-implemented-scope.md` | Added implemented scope section |
| `2026-05-19-improve-pfa-report-diagrams.md` | Diagram improvements |
| `2026-05-19-add-pfa-uml-diagrams.md` | UML class + sequence diagrams added |
| `2026-05-24-update-pfa-report-architecture-uiux.md` | Architecture + UI/UX chapter update |
| `2026-05-24-add-pfa-methodology-progress-chapter.md` | Methodology + progress tracking chapter |
| `2026-05-24-update-pfa-use-case-diagram.md` | Use case diagram update |
| `2026-05-24-add-pfa-api-contracts-and-local-env.md` | API contracts + local env section |
| `2026-05-25-update-pfa-report.md` | Latest full PFA report update |

### Web Version
| File | What |
|------|------|
| `2026-05-25-web-architecture-deep-dive.md` | guided-journey-lab architecture doc |
| `2026-05-25-web-version-report-sections.md` | Web version sections for PFA report |

---

## Open PRs (as of 2026-05-25)

| PR | Branch | Issue | Status |
|----|--------|-------|--------|
| #296 | feat/lesson-player-progress-and-web-rebrand | #249 #250 #251 #252 #253 | Open |
| #297 | feat/progress-course-detail-endpoint | #254 | Open |
| #298 | feat/progress-tests-255 | #255 | Open |
