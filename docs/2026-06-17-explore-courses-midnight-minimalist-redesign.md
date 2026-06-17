# Explore Courses — Midnight Minimalist Redesign

## Goal
Redesign the `/explore` route to match the Midnight Minimalist reference screenshot with top navigation, featured course hero, filter bar, 3-column course grid, and footer.

## What Changed
- Replaced sidebar `AppShell` layout with a custom top navigation bar layout specific to the explore page
- Added featured course hero banner with gradient overlay and course data
- Redesigned filter bar with "FILTER BY" label, level and language pill filters
- Rebuilt course grid with image-based cards, level badges, hover effects
- Added skeleton loading cards (6-card grid)
- Added empty state and error state components
- Added page footer matching the reference
- Applied Midnight Minimalist design tokens throughout

## Files Touched
- guided-journey-lab/src/routes/explore.tsx (full rewrite)

## Backend Endpoints Used
- `GET /api/v1/courses` — paginated course list (unchanged)
- `GET /api/v1/enrollments/me` — user's enrollments (unchanged)
- `POST /api/v1/enrollments` — enroll in course (unchanged)

## Design Tokens Used
- Primary: #091426
- Background: #f6fafe
- Secondary text: #505f76
- Surface container low: #f0f4f8
- Outline variant: #c5c6cd
- On surface variant: #45474c
- Primary container: #1e293b
- On primary container: #8590a6
- White cards: #ffffff
- Muted surface: #eaeef2
- Montserrat typography (light 300, normal 400, semibold 600)
- Soft focus shadow: 0 32px 64px -12px rgba(9,20,38,0.06)

## Course Data Sources Preserved
- `listCourses` API call with search query and level filter
- `listMyEnrollments` API call for enrollment status
- `enrollInCourse` mutation for enrollment action
- Client-side language filtering
- Deferred search query

## Search/Filter/Enroll Behavior Preserved
- Search input sends query to API via `useDeferredValue`
- Level filters sent as `category` param to API
- Language filters applied client-side
- Enroll mutation triggers query invalidation
- Enrolled courses show "Continue" linking to course detail
- Non-enrolled courses show "Enroll Now" button

## States Handled
- [x] Loading (skeleton cards)
- [x] Error (error state component)
- [x] Empty (empty state with icon and message)
- [x] Success (course grid)

## Responsive Behavior
- Desktop: 3-column grid, full top nav, large hero
- Tablet: 2-column grid, nav links visible
- Mobile: 1-column grid, hamburger menu with dropdown, hero adjusts height

## Dark Mode Tested
N/A — this redesign uses inline styles matching the Midnight Minimalist spec (light theme). The page bypasses AppShell which provides dark mode toggle. Dark mode support could be added as follow-up.

## TypeScript Errors
None.

## Risks / Notes
- Explore page now uses its own layout instead of AppShell sidebar. Other pages are unaffected.
- The `useDarkMode` hook import was removed since the page uses its own layout without a dark mode toggle.
- Line ending (CRLF) lint warnings are pre-existing across the codebase, not introduced by this change.
- Footer links are placeholder `#` hrefs matching the reference.
