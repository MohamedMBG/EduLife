# Task Audit - Web Career Goal Advisor UI Polish

## Date
2026-06-13

## Task Summary
Redesigned the "Career Goal Advisor" page (`/advisor`) in the `guided-journey-lab` project to deliver a highly premium, modern, and conversational workspace dashboard.

## Files Created
None

## Files Modified
- [advisor.tsx](file:///c:/Users/pc/AndroidStudioProjects/EduLife/guided-journey-lab/src/routes/advisor.tsx)

## What Was Done
1. **Hero/Header Banner**: Converted into a premium glassmorphic section (`glass shadow-luxury grain`) with a decorative background `BrainCircuit` vector grid, serif headlines (`font-display`), and redesigned interactive sub-metric and step widgets (`StepPill`, `InfoMetric`).
2. **Goal Input Form**: Upgraded the text area with focus glow rings and inner shadows. Restyled character counter indicators, quick-start cards with hover transitions, and replaced the plain submit action with a premium glowing gradient button.
3. **AdvisorResponse**: Redesigned the chat-like card with an AI branding label, pulsing indicator rings, and a custom bouncing dots typing simulator for catalog loadings.
4. **RecommendationCard**: Remodeled match panels with smooth scale-up image zooms, oklch badges, custom dividers, and high-fidelity action outlines.
5. **EmptyResult**: Styled as a clean glassmorphic card with a decorative slow-spinning compass SVG.

## Architecture Compliance
The changes are fully compliant with the TanStack Start routing architecture and are modularized within the `src/routes/advisor.tsx` route file. Component state hooks (`useQuery`, `useMutation`) and parameters remain fully decoupled from layout enhancements.

## Code Comments Added
Added helpful comments detailing:
- Hover scale and slide tap parameters for `motion` components.
- Color gradient variables mapping the Best match/Secondary course tags.
- Bouncing delay offsets for typing loader elements.

## Validation / Testing
- Verified via `bun run build:dev` in the `guided-journey-lab` directory. Both the client and server environment built cleanly with zero compilation errors.

## Risks / Notes
- Bouncing dot loaders and pulsing indicator rings use Framer Motion keyframe arrays; these should be monitored for performance on low-power devices.
