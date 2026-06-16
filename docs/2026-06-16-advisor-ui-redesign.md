# Career Advisor UI Redesign

## Goal
Redesign the advisor page to be modern, minimalist, and easy UX while staying compatible with the project design system.

## What Changed
- Replaced the AI Persona / Avatar card (gimmicky) with a clean 3-bullet feature list explaining what the advisor does
- Pulled the Prompt Helper chip rows out of the textarea card — they now live as a separate, breathing section below the composer
- Simplified the textarea card: clean, no internal dividers or nested sections
- Reduced ambient orb intensity (opacity) for a subtler dark background
- Grid changed from `lg:grid-cols-12` split (6/6) to a clean `lg:grid-cols-2` — simpler and equivalent
- History drawer moved below the main grid (full-width) instead of inside the right column
- Top meta strip simplified: removed `enrolledCount` display, cleaner mono label
- Chip rows in prompt builder now show a short inline category label (16-char column) for scannability
- Starters row simplified — no "Starters /" label awkwardness, just a mono prefix
- Removed `Compass` import (unused after AI persona card removal)
- All logic (builder state, analyze handler, localStorage persistence) unchanged

## Files Touched
- `guided-journey-lab/src/routes/advisor.tsx`

## Backend Endpoints Used
None new. Existing: `GET /api/v1/courses`, `GET /api/v1/enrollments/me`, `POST /api/v1/advisor/recommend`.

## Design Tokens Used
- `bg-gold`, `text-gold-foreground`, `shadow-bezel` — Draft Brief CTA
- `bg-primary-glow/70` — feature list bullet dots
- `bg-teal` — live status pulse indicator
- `text-display` — display font on textarea and headings
- Ambient: `bg-primary-glow/20`, `bg-gold/10` — orbs (reduced from /25, /15)

## States Handled
- [x] Loading (BriefSkeleton)
- [x] Error (BriefError + retry)
- [x] Empty (EmptyAdvisorState + BriefEmpty)
- [x] Success (BriefSpread)

## Dark Mode Tested
N/A — hero section uses hardcoded dark gradient (zinc-950/slate-950), not affected by dark mode toggle. Brief section below uses semantic tokens, unchanged.

## TypeScript Errors
One pre-existing error in `StreamingText` framer-motion variant typing (line 727) — not introduced by this change.

## Risks / Notes
- No logic changes, only JSX restructure — all existing brief/history/builder behavior preserved
- `enrolledCount` prop still passed from parent but no longer rendered in the stage (was minor UX info that added clutter)
