# CLAUDE.md — guided-journey-lab (EduLife Web)

Operational guide for AI agents working on the EduLife web app.

Product and architecture rules live in the root `AGENTS.md`.
Backend API is already built and running — never fake data once the endpoint exists.

---

## Stack

- React 19
- TypeScript
- TanStack Start / Router
- shadcn/ui
- Tailwind v4 (CSS variables via `@theme inline`)
- Bun

---

## Routes

```text
src/routes/index.tsx       → Landing / home
src/routes/login.tsx       → Login
src/routes/register.tsx    → Registration
src/routes/dashboard.tsx   → Learner dashboard
src/routes/courses.tsx     → Course catalog
src/routes/explore.tsx     → Explore / discovery
src/routes/level.tsx       → Level / lesson view
```

---

## Design System — READ THIS BEFORE TOUCHING ANY UI

All colors, typography, shadows, gradients, and utilities are defined in:

```text
src/styles.css
```

**Never hardcode hex, rgb, or oklch values inline. Always use the CSS variable tokens.**

---

### Color Tokens (Tailwind classes map 1:1 to these)

Use `bg-primary`, `text-primary`, `border-primary`, etc.

#### Brand Colors

| Token | Tailwind class | Light value | Dark value | Use for |
|---|---|---|---|---|
| `--primary` | `bg-primary` / `text-primary` | deep teal-green | bright teal | CTAs, active states, brand |
| `--primary-foreground` | `text-primary-foreground` | near-white | near-black | text ON primary bg |
| `--primary-glow` | `bg-primary-glow` | lighter teal | bright teal | gradient accents |
| `--gold` | `bg-gold` / `text-gold` | warm amber | lighter amber | premium, certificates, CTAs |
| `--gold-foreground` | `text-gold-foreground` | dark brown | very dark | text ON gold bg |
| `--teal` | `bg-teal` / `text-teal` | cyan-teal | bright cyan | accent highlights |
| `--teal-foreground` | `text-teal-foreground` | dark teal | very dark | text ON teal bg |

#### Semantic Colors

| Token | Tailwind class | Use for |
|---|---|---|
| `--background` | `bg-background` | page background |
| `--foreground` | `text-foreground` | body text |
| `--card` | `bg-card` | card backgrounds |
| `--card-foreground` | `text-card-foreground` | card text |
| `--surface` | `bg-surface` | subtle section bg |
| `--surface-elevated` | `bg-surface-elevated` | modal, dropdown bg |
| `--secondary` | `bg-secondary` | secondary buttons, badges |
| `--secondary-foreground` | `text-secondary-foreground` | text on secondary |
| `--muted` | `bg-muted` | disabled, skeleton |
| `--muted-foreground` | `text-muted-foreground` | captions, placeholders |
| `--accent` | `bg-accent` | hover states, subtle highlights |
| `--accent-foreground` | `text-accent-foreground` | text on accent |
| `--destructive` | `bg-destructive` | errors, delete |
| `--destructive-foreground` | `text-destructive-foreground` | text on error |
| `--border` | `border-border` | dividers, outlines |
| `--input` | `border-input` | form field borders |
| `--ring` | `ring-ring` | focus rings |

#### Exact CSS Variable Values (for reference, not for inline use)

Light mode (`:root`):
```css
--primary:              oklch(0.40 0.19 152);
--primary-foreground:   oklch(0.99 0 0);
--primary-glow:         oklch(0.60 0.24 148);
--secondary:            oklch(0.958 0.016 145);
--secondary-foreground: oklch(0.17 0.09 150);
--muted:                oklch(0.952 0.009 128);
--muted-foreground:     oklch(0.48 0.030 148);
--accent:               oklch(0.942 0.018 138);
--accent-foreground:    oklch(0.17 0.09 150);
--gold:                 oklch(0.74 0.17 77);
--gold-foreground:      oklch(0.22 0.07 57);
--teal:                 oklch(0.66 0.15 194);
--teal-foreground:      oklch(0.17 0.07 214);
--destructive:          oklch(0.58 0.23 25);
--destructive-foreground: oklch(0.985 0 0);
--background:           oklch(0.985 0.006 95);
--foreground:           oklch(0.11 0.038 155);
--card:                 oklch(1 0 0);
--card-foreground:      oklch(0.11 0.038 155);
--surface:              oklch(0.968 0.008 105);
--surface-elevated:     oklch(1 0 0);
--border:               oklch(0.88 0.012 138);
--input:                oklch(0.88 0.012 138);
--ring:                 oklch(0.50 0.21 148);
--radius:               1rem;
```

Dark mode (`.dark`):
```css
--primary:              oklch(0.67 0.22 148);
--primary-foreground:   oklch(0.07 0.03 155);
--primary-glow:         oklch(0.77 0.25 144);
--background:           oklch(0.09 0.026 158);
--foreground:           oklch(0.96 0.007 110);
--card:                 oklch(0.15 0.036 153);
--surface:              oklch(0.12 0.030 156);
--surface-elevated:     oklch(0.15 0.036 153);
--secondary:            oklch(0.18 0.052 153);
--secondary-foreground: oklch(0.88 0.014 140);
--muted:                oklch(0.18 0.034 153);
--muted-foreground:     oklch(0.62 0.032 148);
--accent:               oklch(0.20 0.044 153);
--accent-foreground:    oklch(0.88 0.014 140);
--gold:                 oklch(0.84 0.18 80);
--gold-foreground:      oklch(0.12 0.06 58);
--teal:                 oklch(0.74 0.16 196);
--teal-foreground:      oklch(0.12 0.06 216);
--destructive:          oklch(0.65 0.25 25);
--border:               oklch(0.24 0.040 153);
--input:                oklch(0.24 0.040 153);
--ring:                 oklch(0.65 0.21 148);
```

---

### Typography

Two font families. Both loaded from Google Fonts.

| Variable | Font | Weight | Use for |
|---|---|---|---|
| `--font-display` | Fraunces (serif) | 400, 500, 600 | h1, h2, h3, h4, hero text |
| `--font-sans` | Figtree (sans) | 300–700 | body text, UI labels |

Rules:
- All headings (`h1`–`h4`) automatically use `font-display` via global base styles.
- Use `.text-display` utility class for non-heading display text.
- Body uses `font-sans` automatically — no class needed.
- Headings have `letter-spacing: -0.02em` and `font-weight: 500` by default.

Never use `font-serif` or `font-mono` unless you have a specific reason.

---

### Border Radius

| Token | Value | Tailwind |
|---|---|---|
| `--radius-sm` | 12px | `rounded-sm` |
| `--radius-md` | 14px | `rounded-md` |
| `--radius-lg` | 16px | `rounded-lg` |
| `--radius-xl` | 20px | `rounded-xl` |
| `--radius-2xl` | 24px | `rounded-2xl` |
| `--radius-3xl` | 32px | `rounded-3xl` |
| `--radius-4xl` | 40px | `rounded-4xl` |

---

### Shadows

Use these utility classes — do not write custom `box-shadow`:

| Class | Use for |
|---|---|
| `shadow-soft` | Subtle card lift |
| `shadow-elevated` | Modal, dropdown, popover |
| `shadow-glow` | Primary-branded elements on hover |
| `shadow-luxury` | High-contrast premium cards |
| `shadow-gold` | Gold/certificate elements |

---

### Gradients

Use these utility classes — do not write custom `background`:

| Class | Use for |
|---|---|
| `bg-hero-gradient` | Hero section radial background |
| `bg-gradient-primary` | Primary branded backgrounds |
| `bg-gradient-gold` | Premium/certificate sections |
| `bg-gradient-aurora` | Multi-color ambient sections |
| `text-gradient-primary` | Gradient-filled headline text |

---

### Glass Effect

```html
<div class="glass">...</div>
```

Applies frosted glass: translucent background + `backdrop-filter: blur(16px)`.
Use for nav overlays, modals on image backgrounds.

---

### Animations

| Class | Effect | Duration |
|---|---|---|
| `animate-float` | Slow vertical float | 8s |
| `animate-glow` | Opacity + scale pulse | 6s |
| `animate-shimmer` | Left-to-right shimmer | 2.2s |

---

### Texture

Add `.grain` class to a `position: relative` container for a subtle film grain overlay.

---

## Component Rules

- Use shadcn/ui components first. Do not reinvent buttons, inputs, dialogs.
- Extend shadcn components with the design tokens above.
- Every feature needs: loading state, error state, empty state, success state.
- Never show a spinner forever — always resolve to an error or success state.
- Dark mode is supported via `.dark` class on `<html>`. Design every component for both modes.

---

## API Consumption Rules

The backend is a Spring Boot modular monolith. Base URL is configured via environment variable.

```text
VITE_API_BASE_URL or equivalent
```

Key endpoints (all require `Authorization: Bearer <firebase_id_token>`):

```text
POST   /api/v1/auth/sync                          → register/login, get internal userId + role
GET    /api/v1/courses                            → paginated course list
GET    /api/v1/courses/{id}                       → course detail with sections/lessons
POST   /api/v1/enrollments                        → enroll in course
DELETE /api/v1/enrollments/{id}                   → unenroll
GET    /api/v1/enrollments/me                     → user's enrollments
PUT    /api/v1/progress/lessons/{lessonId}/mark-complete → mark lesson done
GET    /api/v1/progress/courses/{courseId}        → full progress breakdown
GET    /api/v1/courses/{courseId}/exam            → exam questions (no answers)
GET    /api/v1/courses/{courseId}/exam/status     → pre-exam check (passed? in cooldown?)
POST   /api/v1/courses/{courseId}/exam/submit     → submit answers, get result
GET    /api/v1/certificates/me                    → user's certificates
GET    /api/v1/certificates/{id}                  → certificate detail
GET    /api/v1/certificates/{id}/download         → PDF download
GET    /api/v1/certificates/verify/{hash}         → public verification (no auth)
GET    /api/v1/profiles/me                        → user profile
PUT    /api/v1/profiles/me                        → update profile
POST   /api/v1/profiles/me/avatar                 → upload avatar (multipart, max 5MB)
```

Admin-only endpoints (role: ADMIN):
```text
GET    /api/v1/admin/metrics
GET    /api/v1/admin/users
PUT    /api/v1/admin/users/{id}/role
PUT    /api/v1/admin/courses/{id}/publish
PUT    /api/v1/admin/courses/{id}/archive
```

Teacher/CMS endpoints (role: TEACHER | GROUP_ADMIN | ADMIN):
```text
POST   /api/v1/admin/cms/courses
PUT    /api/v1/admin/cms/courses/{id}
POST   /api/v1/admin/cms/sections
POST   /api/v1/admin/cms/lessons
POST   /api/v1/admin/cms/exams
```

Backend error contract — every error returns:
```json
{
  "status": 400,
  "message": "Validation failed",
  "timestamp": "2026-06-03T00:00:00Z"
}
```

HTTP codes used:
- `401` — missing or invalid Firebase token
- `403` — authenticated but wrong role or ownership violation
- `404` — resource not found
- `409` — conflict (already enrolled, already passed exam, etc.)
- `429` — rate limit or exam cooldown

Exam cooldown response additionally includes `cooldownEndsAt` field.

---

## Security Rules

- Never store the Firebase token in `localStorage` — use memory or `sessionStorage`.
- Never expose `firebase_uid` — the backend returns an internal `userId`.
- Never send userId or role in request body — backend resolves from token.
- Always handle 401 by triggering re-authentication.

---

## What NOT To Build

- Do not duplicate backend business logic (exam scoring, certificate generation, progress calculation).
- Do not add hardcoded/mocked responses once a backend endpoint exists.
- No local auth bypass.
- No UI-only feature pretending to be complete.
- No microservices, no direct DB access, no fake data in production paths.
- No post-MVP features: payments, AI recommendations, real-time chat.

---

## Gamification (Shared Spec)

Gamification is a first-class feature and **must match Android exactly**.

Authoritative spec lives in root `CLAUDE.md` → `## Gamification (Shared Spec)`.

Web-specific notes:

- XP, level thresholds, level names, badge ids/labels/conditions, streak rules **must not** diverge from the root spec.
- XP is event-driven (lesson=25, course=100, exam_pass=150, cert=200, enrollment=10, daily_login=5, streak bonuses +30/+75) — do not derive XP as `lessons × 50` style shortcuts.
- Backend has no gamification endpoints yet — derive state from `/enrollments/me`, `/certificates/me`, `/progress/courses/{id}`. If a backend gamification API ships, both clients switch to it together.
- Any change to constants (XP values, level thresholds, level names, badge defs) must land in Android + Web in the same PR and update root `CLAUDE.md` spec.
- Web has richer UI (quests, weekly chart, skill tree) — extra UI surfaces are fine as long as underlying constants match.

---

## Mandatory Audit File

After every task — feature, fix, refactor, config change, anything — create a file in `/docs`.

File name format:
```text
docs/YYYY-MM-DD-task-name.md
```

Use today's date. Use kebab-case for the task name. If `/docs` does not exist, create it.

The file must contain:

```markdown
# Task Title

## Goal
What was asked for.

## What Changed
Specific description of what was implemented or fixed.

## Files Touched
- path/to/file

## Backend Endpoints Used
List any backend endpoints this task calls.

## Design Tokens Used
List any new color tokens, gradients, shadows, or typography used.

## States Handled
- [ ] Loading
- [ ] Error
- [ ] Empty
- [ ] Success

## Dark Mode Tested
Yes / No / N/A

## TypeScript Errors
None / list any known issues.

## Risks / Notes
Any known limitations, follow-up work, or edge cases.
```

Do not skip this. The audit file is part of the task, not optional.

---

## Definition of Done

A web feature is done only when:

- It consumes the real backend endpoint.
- It handles loading, error, empty, and success states.
- It respects the design tokens above (no hardcoded colors).
- It works in both light and dark mode.
- It passes TypeScript compile with no errors.
- Audit doc created at `docs/YYYY-MM-DD-task-name.md`.
