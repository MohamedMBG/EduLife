# EduLife Web — Full Planning Document

**App:** guided-journey-lab  
**Stack:** React 19 + TypeScript + TanStack Start/Router + shadcn/ui + Tailwind v4  
**Deploy:** Cloudflare Workers via `wrangler.jsonc`  
**Backend:** Spring Boot modular monolith (all endpoints already built)  
**Last updated:** 2026-06-03

---

## Learner Flow (MVP)

```text
Landing → Register/Login
  → Email Verification
  → Dashboard
  → Explore Courses
  → Enroll
  → Lessons + Progress
  → Exam (MCQ, backend-scored)
  → Certificate (PDF download + public verify)
```

---

## Route Map

| Route | File | Purpose |
|---|---|---|
| `/` | `src/routes/index.tsx` | Landing page |
| `/login` | `src/routes/login.tsx` | Login |
| `/register` | `src/routes/register.tsx` | Registration |
| `/dashboard` | `src/routes/dashboard.tsx` | Learner home |
| `/explore` | `src/routes/explore.tsx` | Course catalog |
| `/courses` | `src/routes/courses.tsx` | My enrolled courses |
| `/courses/$courseId` | `src/routes/courses.$courseId.tsx` | Course detail + outline |
| `/learn/$courseId/$lessonId` | `src/routes/learn.$courseId.$lessonId.tsx` | Lesson content |
| `/certificates` | `src/routes/certificates.tsx` | My certificates |
| `/level` | `src/routes/level.tsx` | ⚠ Stub — fake gamification data |

Routes still needed:

| Route | Purpose | Priority |
|---|---|---|
| `/courses/$courseId/exam` | Exam questions page | High |
| `/courses/$courseId/exam/result` | Exam result + certificate trigger | High |
| `/profile` | Profile edit + avatar upload | Medium |
| `/forgot-password` | Firebase password reset | Medium |
| `/certificates/verify/$hash` | Public certificate verification | Medium |
| `/admin` | Admin dashboard (metrics, users) | Low |
| `/admin/cms` | Teacher CMS (create courses/exams) | Low |

---

## Current State

### Done ✅

#### Foundation
- Bun + TanStack Start + React 19 scaffolded
- TypeScript strict mode
- Tailwind v4 with full design token system (`src/styles.css`)
- shadcn/ui components integrated
- Dark mode CSS fully implemented (`.dark` class toggle)
- Framer Motion for animations
- Google Fonts (Fraunces + Figtree) loaded

#### Auth
- Firebase client SDK initialized (`src/lib/auth/firebase.ts`)
- `browserLocalPersistence` for session persistence
- Auth context with `login()`, `register()`, `logout()`, `getAccessToken()` (`src/lib/auth/auth-context.tsx`)
- Email verification enforced before dashboard access
- `RequireAuth` route guard component
- Login page — full implementation with error handling
- Register page — two-step: role selection + credentials
- `/api/v1/auth/sync` called after Firebase login → internal userId + role stored

#### API Layer
- Full fetch wrapper with `Authorization: Bearer <token>` (`src/lib/api/client.ts`)
- Auto token refresh on 401 → retry once
- `ApiClientError` class with status code
- TypeScript interfaces for all API responses (`src/lib/api/types.ts`)
- All major endpoints wired:
  - `GET /api/v1/courses` + `GET /api/v1/courses/{id}`
  - `POST/DELETE/GET /api/v1/enrollments`
  - `GET /api/v1/progress/courses/{courseId}`
  - `PUT /api/v1/progress/lessons/{id}/mark-complete`
  - `GET /api/v1/certificates`
  - `GET /api/v1/profiles/me`

#### Pages
- Landing page — hero, features, CTA sections
- Dashboard — profile summary, enrolled courses, progress for first 3 courses, recommended
- Explore — full course catalog, level/language filters, enroll action
- My Courses — enrolled list, filters (all/in-progress/completed/not-started), search
- Course Detail — sections + lessons outline, enrollment button, progress bar
- Lesson View — lesson content body or external URL, mark complete, prev/next navigation
- Certificates list — earned certificates with course title, date, download link

#### Config
- `.env.example` with all required variables
- `src/lib/env.ts` validates env vars at startup
- `wrangler.jsonc` configured for Cloudflare Workers deploy
- `VITE_DEMO_MODE` flag for testing without backend

---

### Missing ❌

---

#### 1. Exam Flow — HIGH PRIORITY

Backend endpoints ready. No web UI exists.

**What to build:**

**`/courses/$courseId/exam`**
- Check exam status first: `GET /api/v1/courses/{courseId}/exam/status`
  - If `passed: true` → show "Already passed" state with certificate link
  - If `inCooldown: true` → show cooldown timer with `cooldownEndsAt` countdown
  - If `attemptsUsed >= 0` → show attempt count warning, allow start
- Fetch questions: `GET /api/v1/courses/{courseId}/exam`
- Render MCQ questions — one page or scrollable list
- Track selected answers in local state
- Submit: `POST /api/v1/courses/{courseId}/exam/submit`
- Navigate to result page

**`/courses/$courseId/exam/result`**
- Display score (`score` field, e.g. 85%)
- Pass: show certificate number, link to `/certificates`
- Fail: show attempt count, cooldown info if locked
- Handle `409` (already passed) and `429` (cooldown) from submit

**Key rules:**
- Never store or display correct answers
- Disable submit until all questions answered
- Handle network errors gracefully (don't lose answers on retry)

---

#### 2. Profile Edit Page — MEDIUM PRIORITY

**`/profile`**
- Fetch: `GET /api/v1/profiles/me`
- Edit form: display name, bio
- Avatar upload: `POST /api/v1/profiles/me/avatar` (multipart, max 5MB)
  - Enforce 5MB limit in UI before sending
  - Show preview after upload
- Save: `PUT /api/v1/profiles/me`
- All 4 states: loading, error, empty, success

---

#### 3. Password Reset — MEDIUM PRIORITY

**`/forgot-password`**
- Email input form
- Call Firebase `sendPasswordResetEmail(auth, email)`
- Show success message ("Check your inbox")
- Handle error: user not found, invalid email
- Login page "Forgot password?" link currently points to `#` — fix it

---

#### 4. Public Certificate Verification — MEDIUM PRIORITY

**`/certificates/verify/$hash`**
- No auth required
- Fetch: `GET /api/v1/certificates/verify/{hash}`
- Display: student name, course title, issue date, certificate number
- 404 state: "Certificate not found or invalid"
- This page is linked from QR codes on PDF certificates

---

#### 5. Dark Mode Toggle — LOW PRIORITY

- CSS is 100% ready
- No UI button exists to switch modes
- Add toggle button in `AppShell` header
- Persist choice in `localStorage`
- Toggle `.dark` class on `<html>`

---

#### 6. Level Route — DEPRIORITIZE

`/level` is 760 lines of fully hardcoded demo data:
- Fake leaderboard (hardcoded users + XP)
- Fake badges (hardcoded achievement list)
- Fake daily quests
- Fake streak counter

**Do not wire to backend** — gamification endpoints do not exist. Either:
- Remove route from nav until backend supports it, OR
- Leave as demo/placeholder clearly marked

---

#### 7. Admin Dashboard — LOW PRIORITY (post-MVP)

**`/admin`**
- `GET /api/v1/admin/metrics` → user counts, enrollment counts, certificate counts, course status breakdown
- `GET /api/v1/admin/users` → user list with role
- `PUT /api/v1/admin/users/{id}/role` → change user role
- `PUT /api/v1/admin/courses/{id}/publish` → publish course
- `PUT /api/v1/admin/courses/{id}/archive` → archive course
- Only accessible to ADMIN role

---

#### 8. Teacher CMS — LOW PRIORITY (post-MVP)

**`/admin/cms`**
- Create/edit courses, sections, lessons, exams, questions
- Only accessible to TEACHER | GROUP_ADMIN | ADMIN
- Full CRUD endpoints already on backend

---

## Sprint Plan

### Sprint W1 — Exam Flow ✅

All tasks use real backend endpoints. No mocked data.

- [x] Pre-exam status check (passed / cooldown / allowed)
- [x] Exam questions page with MCQ selection
- [x] Submit to backend, receive result
- [x] Exam result page (pass + certificate link, fail + attempt count, cooldown timer)
- [x] Handle 409 (already passed) and 429 (cooldown)
- [x] Audit doc: `docs/2026-06-05-web-exam-flow.md`

---

### Sprint W2 — Profile + Password Reset ✅

- [x] Profile view + edit form
- [x] Avatar upload (5MB enforced in UI)
- [x] Forgot password page
- [x] Fix "Forgot password?" link on login page
- [x] Audit docs

---

### Sprint W3 — Certificate Verify + Dark Mode Toggle ⬜

- [ ] Public `/certificates/verify/$hash` page (no auth)
- [ ] Dark mode toggle button in AppShell
- [ ] Persist preference in localStorage
- [ ] Audit docs

---

### Sprint W4 — Hardening ⬜

- [ ] Error boundaries on all routes
- [ ] Loading skeleton states (replace spinners with skeletons)
- [ ] Empty states for all lists (no courses, no certificates, no enrollments)
- [ ] Offline/network error handling
- [ ] TypeScript strict — zero `any` types
- [ ] Audit docs

---

### Sprint W5 — Deploy ⬜

- [ ] Set all env vars in Cloudflare dashboard
- [ ] `bun run build` passes clean
- [ ] `git subtree push --prefix=guided-journey-lab web main`
- [ ] Smoke test full learner flow on production URL
- [ ] Audit doc: `docs/YYYY-MM-DD-deploy.md`

---

### Post-MVP (do not start until W1–W5 done)

- [ ] Admin dashboard
- [ ] Teacher CMS
- [ ] Fix `/level` gamification with real backend data

---

## Environment Variables

```text
# Firebase (client-side, not secret)
VITE_FIREBASE_API_KEY=
VITE_FIREBASE_AUTH_DOMAIN=
VITE_FIREBASE_PROJECT_ID=
VITE_FIREBASE_STORAGE_BUCKET=
VITE_FIREBASE_MESSAGING_SENDER_ID=
VITE_FIREBASE_APP_ID=

# Backend
VITE_API_BASE_URL=https://your-backend-url.com

# Optional
VITE_DEMO_MODE=false
```

Copy `.env.example` to `.env` and fill in values. Firebase values can be obtained from the Firebase Console → Project Settings → Your apps.

---

## Dev Setup

```bash
bun install
cp .env.example .env   # fill in VITE_API_BASE_URL
bun run dev            # http://localhost:3000
```

Demo mode (no backend needed):
```bash
VITE_DEMO_MODE=true bun run dev
```

Build:
```bash
bun run build
```

Deploy to Cloudflare:
```bash
git subtree push --prefix=guided-journey-lab web main
```

---

## Key Files

| File | Purpose |
|---|---|
| `src/styles.css` | All design tokens — colors, typography, shadows, gradients |
| `src/lib/auth/firebase.ts` | Firebase SDK init |
| `src/lib/auth/auth-context.tsx` | Auth state, login/register/logout |
| `src/lib/api/client.ts` | API fetch wrapper with token auth |
| `src/lib/api/types.ts` | TypeScript interfaces for all API responses |
| `src/lib/env.ts` | Env var validation |
| `wrangler.jsonc` | Cloudflare Workers config |
| `CLAUDE.md` | AI agent rules — read before writing any code |

---

## Rules

1. Never hardcode colors — use tokens from `src/styles.css`
2. Never mock data once a backend endpoint exists
3. Every feature needs: loading, error, empty, success states
4. Every task needs an audit file in `/docs/YYYY-MM-DD-task-name.md`
5. TypeScript — no `any` types
6. Dark mode — every component must work in both light and dark
7. Never score exams on client — backend only
8. Never expose Firebase UID — use internal userId from `/auth/sync`
