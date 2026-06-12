# Android + Web Full Plan

Derived from 2026-06-12 full-stack audit. Backend stable — both plans assume no backend changes except the two backend items already planned (cooldown-gate getExam, admin seed runbook) and the pass-threshold decision.

Effort scale: S = under half day, M = 1–2 days, L = 3+ days.

---

## ANDROID PLAN

### Current state

MVP screens all exist and consume real API: login/register, onboarding, catalog, course detail, enroll, lesson player, exam + result, certificates list/detail, profile + edit + avatar. Teacher CMS + admin dashboard built (scope-blessed or frozen per decision 0.2). Architecture compliant; network/auth core solid.

### Phase A1 — Fixes (S each, parallel, one commit each)

| # | Task | Files | Done when |
|---|---|---|---|
| A1.1 | Remove Google sign-in placeholder button | `features/auth/ui/LoginFragment.java:57` + layout XML | No dead UI on login screen |
| A1.2 | Dedupe `recyclerview` dependency | `app/build.gradle.kts` | Single declaration, build passes |
| A1.3 | Pass-threshold display copy | exam screens if any hardcode "70%/80%" | Copy reads from `passScore` field only, never hardcoded |

### Phase A2 — State + flow verification pass (M)

Walk every screen against the four-state rule. Known UiState classes exist; verify each renders all states, not just declares them.

| # | Screen | Verify |
|---|---|---|
| A2.1 | Catalog (`CoursesFragment`, `HomeFragment`) | empty catalog, network error retry, pagination loading |
| A2.2 | Course detail + enroll | 409 already-enrolled, unpublished course 404 |
| A2.3 | Lesson player | missing content, mark-complete failure rollback |
| A2.4 | Exam (`ExamFragment`) | cooldown 429 on fetch (new backend behavior), already-passed 409, time-limit handling, submit failure mid-exam (answers preserved?) |
| A2.5 | Exam result | pass with certificate number, fail with cooldown countdown |
| A2.6 | Certificates | empty list, detail load failure, download/share action |
| A2.7 | Profile | avatar upload 413 (>5MB), upload progress, update failure |
| A2.8 | Session expiry | `SessionEventBus` → login redirect from every screen, no crash mid-flight |

Output: checklist doc; each gap found becomes its own S fix commit.

### Phase A3 — Hardening (M)

| # | Task | Notes |
|---|---|---|
| A3.1 | Dep upgrades: Retrofit 2.9→2.11, lifecycle 2.7→2.8.x, ViewPager2 1.0→1.1 | One PR, full build + A2 smoke pass after |
| A3.2 | `security-crypto` alpha watch | Move to stable when 1.1.0 final ships; until then pin stays |
| A3.3 | ProGuard release verification | Build release, test serialization (Gson models survive R8), exam + auth flows on minified build |
| A3.4 | Prod API base URL path | `edulife.apiBaseUrl` gradle property documented for release builds; no localhost in release |

### Phase A4 — Release readiness (M–L)

| # | Task |
|---|---|
| A4.1 | Versioning: bump versionCode/versionName scheme, document in CLAUDE.md |
| A4.2 | Signing config (release keystore, not in repo, env/local.properties) |
| A4.3 | Device QA matrix: minSdk 24 device/emulator + recent API 35/36 device; rotation, process death on exam screen |
| A4.4 | Full E2E on device against deployed backend: register → verify email → enroll → lessons → exam fail ×2 → cooldown → pass → certificate |
| A4.5 | Play Store prep (listing, privacy policy) — only if launch decided; otherwise APK distribution |

Dependencies: A2 before A3.3 and A4. A1 anytime.

---

## WEB PLAN

### Current state

Full MVP routes consuming real API; demo mode fenced; auth tokens session-only; certificate public verify works. No CMS/admin UI (decision 0.3). Deploys to Cloudflare Workers, pushed via git subtree.

### Phase W1 — Fixes (S each)

| # | Task | Files | Done when |
|---|---|---|---|
| W1.1 | Dark-mode key unify | `components/landing/Nav.tsx` (`"theme"` → `"edulife-dark"`), extract shared `useDarkMode` hook used by Nav + AppShell | One key, no desync, no wrong-theme flash |
| W1.2 | Commit `routeTree.gen.ts` | — | Clean working tree |
| W1.3 | `wrangler.jsonc` name → `edulife-web` | Check existing Cloudflare worker binding first — rename may create new worker; coordinate routes/domain before merge |
| W1.4 | Pass-threshold copy | any hardcoded "70/80%" in exam UI | Reads `passScore` from API only |

### Phase W2 — Auth guards (S–M)

| # | Task | Notes |
|---|---|---|
| W2.1 | `beforeLoad` redirect on protected routes: dashboard, courses.$courseId.* (exam, resources), learn.*, profile, certificates.index/$id | Redirect to /login with `redirect` search param; keep `RequireAuth` as render fallback |
| W2.2 | Post-login redirect honors `redirect` param | Deep link to exam → login → back to exam |
| W2.3 | 401 handling sweep | Confirm every API error path triggers re-auth not silent spinner |

### Phase W3 — State + flow verification pass (M)

Same discipline as Android A2:

| # | Route | Verify |
|---|---|---|
| W3.1 | courses.index / explore | empty, error, pagination/search states |
| W3.2 | courses.$courseId | 404 course, enroll 409, unenroll confirm |
| W3.3 | learn.$courseId.$lessonId | mark-complete failure, notes localStorage edge (private browsing throws — wrap) |
| W3.4 | exam + result | cooldown 429 on fetch (new backend), countdown display, already-passed 409, submit network failure preserves answers |
| W3.5 | certificates + verify.$hash | invalid hash, download failure, empty list |
| W3.6 | profile | avatar >5MB → 413 message, update failure |
| W3.7 | demo mode | every demo-disabled feature shows clear message, not broken UI |

Each gap → S fix commit.

### Phase W4 — Deploy pipeline (M)

| # | Task |
|---|---|
| W4.1 | Cloudflare env vars: all `VITE_*` set for prod build; document in `.env.example` parity check |
| W4.2 | Backend `APP_CORS_ALLOWED_ORIGINS` includes deployed web origin (exact, no wildcard) |
| W4.3 | Subtree push runbook validated: `git subtree push --prefix=guided-journey-lab web main` |
| W4.4 | Prod smoke: full MVP E2E on deployed site against deployed backend |
| W4.5 | Custom domain + HTTPS confirm (HSTS already sent by backend) |

### Phase W5 — Optional: CMS/admin UI (L, only if decision 0.3 = build)

| # | Task |
|---|---|
| W5.1 | Role-aware routing: role from auth/sync response gates /admin, /teach sections |
| W5.2 | Teacher dashboard: my courses list (GET /admin/cms/courses scope) |
| W5.3 | Course editor: create/update course, sections, lessons (existing CMS endpoints) |
| W5.4 | Exam builder: create exam + questions/choices |
| W5.5 | Admin: metrics page, user role management, teacher request review |

Reuses backend endpoints 1:1 — zero backend work. Mirror Android teacher/admin feature scope for parity.

---

## CROSS-CUTTING ORDER

1. Decision 0.1 (70 vs 80) before A1.3/W1.4 — copy tasks trivial either way.
2. Backend cooldown-gate `getExam` ships before A2.4/W3.4 verification (clients must handle 429 on fetch).
3. A-phases and W-phases independent — parallelizable.
4. W4 (deploy) before A4.4 (device E2E needs deployed backend) if testing against prod; otherwise local backend fine.

## Suggested milestone grouping

- **Milestone 1 (this week):** A1 + W1 + W2 — all fixes and guards. ~2 days.
- **Milestone 2:** backend cooldown patch → A2 + W3 verification passes + gap fixes. ~3-4 days.
- **Milestone 3:** A3 + W4 — hardening + deploy pipeline. ~3 days.
- **Milestone 4:** A4 release readiness; W5 if blessed.
