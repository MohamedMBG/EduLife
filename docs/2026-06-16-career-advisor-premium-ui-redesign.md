# Task Audit - Career Advisor Premium UI Redesign

## Date
2026-06-16

## Task Summary
Redesigned the web Career Advisor page to match the provided premium minimalist reference while keeping the existing advisor API, catalog fallback, enrollment action, and course-outline navigation intact.

## Files Created
- docs/2026-06-16-career-advisor-premium-ui-redesign.md

## Files Modified
- guided-journey-lab/src/routes/advisor.tsx
- guided-journey-lab/src/styles.css

## What Was Done
Rebuilt the `/advisor` route as a page-specific premium layout with a sticky top navigation, desktop advisor sidebar, numbered sections, split hero and command-panel prompt card, polished empty/loading/error/result states, advisor reasoning block, best-match course card, compact alternative-path card, and footer.

The textarea, context chips, example goal button, submit button, recommendation generation, history selection, course outline links, and enrollment mutation remain wired to the existing route state and API/data functions. The route still uses live catalog data, backend advisor responses when enabled, and the deterministic catalog fallback when the AI provider is unavailable.

Added Montserrat and Inter to the existing font import so this page can follow the supplied DESIGN.md typography direction without changing the app's global font tokens.

## Architecture Compliance
The change is scoped to the web app route that owns the Career Advisor experience. No backend files, advisor API contracts, course DTOs, enrollment logic, or shared backend behavior were changed.

The implementation preserves the current frontend data flow:
- `listCourses` supplies live catalog data.
- `requestAdvisorRecommendation` is used when AI advisor mode is enabled.
- `analyzeCareerGoal` remains the fallback ranking path.
- `enrollInCourse` still handles enrollment.
- course outline navigation still targets `/courses/$courseId`.

## Code Comments Added
Added comments around local advisor history and the AI-provider fallback path to explain why the page may use localStorage and deterministic catalog ranking without introducing fake course data.

## Validation / Testing
Ran:
- `npx eslint src/routes/advisor.tsx`
- `npm run build`
- `npm run lint`

Targeted lint for the changed route passed. Production build passed for client and server bundles.

Full repository lint still fails because of existing unrelated Prettier/encoding issues in files outside this task, such as `eslint.config.js`, `AppShell.tsx`, and several landing/lesson components.

Manual browser smoke checks:
- Started the Vite dev server on `127.0.0.1:5173`.
- Confirmed `/advisor` is auth-gated in normal mode.
- Started a separate demo-mode dev server on `127.0.0.1:5174`.
- Signed in with the demo flow.
- Verified desktop and mobile layout render without horizontal overflow.
- Submitted the example goal and confirmed the result, best-match card, alternative path, and footer render.
- Confirmed `View course outline` navigates to a real course detail route.

## Risks / Notes
The footer links are visual footer entries because dedicated privacy, terms, and help routes are not currently part of the web app.

The in-app Browser plugin was unavailable in this session (`iab` was missing), so visual verification used local Playwright as a fallback.
