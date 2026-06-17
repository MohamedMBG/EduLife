# Career Advisor — Midnight Minimalist Redesign

## Goal
Redesign the Career Advisor page to match the Midnight Minimalist design system (DESIGN.md) and the uploaded reference screenshot, while preserving all existing data flow, API calls, and user actions.

## What Changed
Full visual overhaul of the advisor page from teal-green to Midnight Minimalist palette:
- Large editorial numbered sections (01, 02, 03) with decorative phase numbers
- Premium prompt card with rounded containers, subtle shadows, uppercase labels
- AI Analysis section with strategic reasoning card + dark confidence score card
- Selected Path section with split image/content recommendation card
- Glass overlay on recommendation image with curated score
- Page-specific footer matching reference design
- Loading/error/empty states restyled to match
- All Material Symbols replaced with Lucide icons (no external font dependency)

## Files Touched
- guided-journey-lab/src/routes/advisor.tsx

## Backend Endpoints Used
No changes. Same endpoints consumed:
- `GET /api/v1/courses` (paginated catalog)
- `GET /api/v1/enrollments/me` (user enrollments)
- `POST /api/v1/enrollments` (enroll in course)
- `POST /api/v1/advisor/recommend` (AI advisor, when enabled)

## Design Tokens Used
Midnight Minimalist palette via inline `MM` constant:
- primary: #091426
- primaryContainer: #1e293b
- secondary: #505f76
- surface: #f0f4f8
- surfaceContainer: #eaeef2
- outlineVariant: #c5c6cd
- outline: #75777d
- onSurface: #171c1f
- onSurfaceVariant: #45474c
- background: #f6fafe

Typography: Montserrat (inherited from app font stack).

## States Handled
- [x] Loading (skeleton pulse + "Synthesizing Insights" spinner)
- [x] Error (premium error card with retry button)
- [x] Empty (dashed container waiting for brief)
- [x] Success (full 3-section editorial layout)

## Dark Mode Tested
N/A — Midnight Minimalist is a light-only design system per DESIGN.md. Page lives within AppLayout which handles dark mode toggle visibility.

## TypeScript Errors
None.

## Lint Errors
None (prettier auto-formatted).

## Advisor Data Flow Preserved
- `handleAnalyze` → `requestAdvisorRecommendation` (AI) or `analyzeCareerGoal` (fallback) → `briefs` state
- `currentBrief` drives sections 02/03 visibility
- `enrollMutation` → enroll CTA
- `Link to="/courses/$courseId"` → syllabus/course detail navigation
- `BriefHistory` → localStorage persistence
- Context chips → `handleContextChip` appends to goal text
- All original helper functions preserved: `getFitScore`, `getGainBullets`, `formatLevel`, `formatLanguage`

## Responsive Behavior
- Desktop: 12-column grid hero, split recommendation card
- Tablet: columns collapse, prompt card stacks below hero
- Mobile: full-width everything, stacked CTAs, phase numbers scale down

## Risks / Notes
- Page uses inline Midnight Minimalist hex values rather than CSS variable tokens. This is intentional per the redesign spec — the MM palette differs from the app's teal-green system.
- Footer is page-specific (not the shared landing Footer) to match the reference design exactly.
- `dangerouslySetInnerHTML` used in reasoning bullets for keyword highlighting — input is from trusted advisor response, not user-controlled.
