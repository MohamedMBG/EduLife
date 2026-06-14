# Task Audit - Conversational Career Advisor & Collapsible Sidebar

## Date
2026-06-13

## Task Summary
Redesigned the "Career Goal Advisor" page (`/advisor`) to function as a clean, conversational AI chatbot interface. Added a collapsible sidebar menu to the main web app wrapper (`AppShell.tsx`) to optimize desktop space.

## Files Created
None

## Files Modified
- [advisor.tsx](file:///c:/Users/pc/AndroidStudioProjects/EduLife/guided-journey-lab/src/routes/advisor.tsx)
- [AppShell.tsx](file:///c:/Users/pc/AndroidStudioProjects/EduLife/guided-journey-lab/src/components/app/AppShell.tsx)

## What Was Done
1. **Collapsible Sidebar (`AppShell.tsx`)**:
   - Added React state `isCollapsed` to track sidebar width toggles on desktop screens.
   - Built a chevron toggle next to the top logo that animates between chevron pointing states.
   - Adapted aside panel layouts: collapsed width shrinks smoothly from `w-72` to `md:w-20` on desktop, hiding names, labels, and text descriptions while centering link icons with hover tooltips.
   - Adapted profile detail footers to render initials and logout button in a collapsed stack.
2. **Conversational Advisor Chat (`advisor.tsx`)**:
   - Transformed the page into a unified chatbot messaging workspace.
   - Added a vertical chat stream displaying:
     - Bot welcoming guidelines bubble.
     - User goal bubble on the right with initials.
     - Bot response explanation bubble on the left with a bot avatar and status dot.
     - Actionable course recommendation cards rendered inline directly inside the chat log container.
   - Redesigned chat input bar: a clean single-line bar with a messaging send icon.
   - Placed quick-start suggestion capsules ("Software path", "French writing") directly above the input bar that populate and submit prompts instantly.
   - Moved metadata, catalog checking stats, and advice tips to a separate clean card on the right on desktop to keep the chat interface clean and distraction-free.

## Architecture Compliance
The changes align perfectly with the TanStack Router structure in the `guided-journey-lab` web module. Navigation routes, states, and client API services are preserved and decoupled from UI layout restructures.

## Code Comments Added
Added code comments describing:
- Aside layout transitions and responsiveness widths constraints.
- Hover title tooltips on collapsible navigation links.
- Keyframe delays for bubble and loader components.

## Validation / Testing
- Verified via `bun run build:dev` in the `guided-journey-lab` directory. The client environment and SSR server bundles compiled successfully with zero TypeScript or syntax warnings.

## Risks / Notes
- Monitor layout sizing constraints on tablets or mid-sized screens when the sidebar transitions.
