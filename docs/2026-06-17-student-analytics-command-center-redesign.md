# Student Analytics — Academic Command Center Redesign

## Goal

Redesign the student analytics page into a premium "Academic Command Center" dashboard matching the provided reference screenshot, using the Midnight Minimalist design system.

## What Changed

Complete visual overhaul of the `StudentAnalyticsPanel` in `analytics.tsx`. The page now renders:

1. **Header** — "Academic Command Center" title (font-light 300), v2.4.0 badge, subtitle, dynamic last-updated timestamp, disabled "Download Full Report" button (coming soon).
2. **KPI Cards Row** — 5 metric cards (Active Courses, Lessons Done, Exam Attempts, Success Rate, Certificates) with icons, uppercase labels, large values, helper text, and computed trend indicators (green/red/gray).
3. **Skill Growth & Projection** — Recharts `LineChart` using real `lessonsByMonth` data (solid dark actual line + dashed gray target projection). Empty state if no data.
4. **Career Alignment** — Custom SVG radar/pentagon chart showing 5 dimensions derived from analytics data. Overlay score percentage. Skill level rows, dynamic advisor note, functional "View Career Roadmap" link to `/advisor`.
5. **Academic Milestones** — Summary milestone rows derived from analytics counts (certificates, exams, lessons). Polished empty state when no activity.
6. **Footer** — Lightweight in-page footer with EduLife branding and legal links.
7. **Loading skeleton** — Shimmer skeleton matching the full layout.
8. **Error state** — Centered error card with retry button.

Teacher and Group analytics panels remain **unchanged**.

## Files Touched

- `guided-journey-lab/src/routes/analytics.tsx` — full rewrite of student panel

## Backend Endpoints Used

- `GET /api/v1/analytics/me/summary` — `StudentAnalyticsSummary`
- `GET /api/v1/analytics/me/progress-trend` — `StudentProgressTrend`

No endpoints added, removed, or modified.

## Design Tokens Used

- Colors: `bg-primary`, `text-primary`, `bg-surface-elevated`, `text-foreground`, `text-muted-foreground`, `bg-muted`, `border-border`, `bg-accent`, `bg-destructive`
- Shadows: `shadow-soft`
- Typography: `font-light` (300 weight for title), `font-semibold`, `tracking-tight`, `tracking-[0.12em]`
- Radius: `rounded-xl`, `rounded-lg`, `rounded-md`
- No hardcoded hex values in components; only CSS variable tokens

## States Handled

- [x] Loading — full skeleton with animate-pulse
- [x] Error — error card with message + retry
- [x] Empty — chart empty state, milestones empty state
- [x] Success — full dashboard with real data

## Dark Mode Tested

Design tokens support dark mode via CSS variables. All colors use semantic tokens.

## TypeScript Errors

None — `tsc --noEmit` passes clean.

## Lint

Passes after prettier auto-fix.

## Build

`vite build` succeeds.

## Data Sources Preserved

All data derived from existing `StudentAnalyticsSummary` and `StudentProgressTrend` APIs:
- KPI values: direct from summary fields
- Success Rate: `examsPassed / examAttempts * 100`
- Chart: `lessonsByMonth` array
- Radar: derived from lesson ratio, exam ratio, course engagement, cert count, monthly consistency
- Skill levels: derived from ratios
- Milestones: derived from summary counts (no activity feed API exists)

## Responsive Behavior

- Desktop: 5 KPI columns, chart + career side by side (1.6fr/1fr)
- Tablet: KPI wraps to 3 columns, main grid stacks
- Mobile: KPI 2 columns, everything stacks, button inline

## Risks / Notes

- "Download Full Report" button disabled — no report generation endpoint exists
- "View History" link non-functional — no activity feed API
- Career alignment metrics derived from summary data (no dedicated career API)
- Target line on chart is a computed projection, not real curriculum benchmarks
- Milestones section shows aggregate counts, not timestamped events (no API)
