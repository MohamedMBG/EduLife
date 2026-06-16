# Advisor + Learner Dashboard UI Refresh

## Goal
Apply the Taste skills (`design-taste-frontend`, `redesign-existing-projects`,
`high-end-visual-design`) to the Career Advisor route and the learner Dashboard route,
matching the premium language landed in the earlier landing pass.

## What Changed

### Advisor (`src/routes/advisor.tsx`)
- Page-level intro with eyebrow tag + italic emphasis instead of a header-only entry.
- Chat workspace switched from `glass grain` blur stack (perf-banned on scrolling
  containers by the high-end-visual-design skill) to a single hairline + elevated card.
- Chat header rebuilt: bezel-wrapped bot icon, mono-uppercase status line ("Online ·
  grounded on catalog"), live "N courses indexed" pill on the right.
- Removed the hardcoded `emerald-600 → teal-500` gradient on user bubbles (broke brand
  token system); user bubbles now use `bg-foreground` with `shadow-bezel`. Bot bubbles
  use `bg-surface` + hairline.
- Killed the noisy `animate-pulse` Sparkles spam; only the bot eyebrow keeps a small
  Sparkles glyph.
- Send button: pure black foreground pill with diagonal-translate arrow on hover instead
  of the rotating-on-tap glow chip.
- Quick-start "Try" pills now use hairline + magnetic hover (`-translate-y-0.5`).
- `RecommendationCard` rebuilt: Best Match gets the double-bezel architecture
  (`bezel` outer + `bezel-inner`), Alt Option gets the hairline card. Course image lifts
  on hover with a 4% scale tween. Match reasoning sits in a tinted hairline panel with a
  mono eyebrow. "Enroll now" uses the button-in-button pattern with the trailing arrow
  in its own nested circle.
- Removed invalid `text-teal-650` / `text-teal-450` classes (Tailwind has no `650`/`450`
  shades).
- Right rail rebuilt as three sections: live data (catalog + enrollment counts), a
  prompt-writing tip list, and a deep gradient promo block linking to `/courses` with a
  button-in-button CTA.

### Learner Dashboard (`src/routes/dashboard.tsx`)
- `QuickActionCard` component replaces the two near-identical Advisor + Planner cards
  with one shared component that varies accent (primary vs gold), bezel icon, and embeds
  an inline progress bar when the planner has hours logged.
- "Continue learning" card rewritten as a 1.4/0.8 split: hero title + body on the left,
  a `ProgressRing` SVG component on the right showing the active course's percent with
  the brand gradient stroke and the `cubic-bezier(0.16,1,0.3,1)` stroke-dashoffset
  tween.
- Empty state for "Continue learning" gets a centred Compass icon block instead of the
  generic StateCard.
- Profile snapshot card rewritten with `/04` mono numbering, `SnapshotRow` helper
  (email rendered in mono, bio in muted body, role as a primary-tinted pill).
- "Recommended from the live catalog" header now uses mono numbering (`/05 · Discover`),
  italic kicker copy, and the same button-in-button "Browse all" CTA as the rest of the
  app.
- Suggested course cards rebuilt: 16:10 image with hover-scale, mono "Pick {n}" badge
  pinned top-left with Sparkles glyph, `text-display` title, line-clamped description,
  outline + Enroll button-in-button row pinned to the bottom.

## Files Touched
- `guided-journey-lab/src/routes/advisor.tsx` — full rewrite of the chat workspace +
  side panels + recommendation card. Removed unused imports (`ArrowRight`, `Sparkles`'s
  pulsing usage). Added `ArrowUpRight`, `Lightbulb`.
- `guided-journey-lab/src/routes/dashboard.tsx` — replaced ArrowRight with ArrowUpRight,
  added Sparkles + Target imports, swapped the dual quick-action cards for
  `QuickActionCard`, rebuilt the continue-learning + profile-snapshot section, rebuilt
  the recommended catalog grid, added `QuickActionCard`, `ProgressRing`, `SnapshotRow`
  helpers.

## Backend Endpoints Used
None changed. Pure presentation pass; queries against
`listCourses`, `listMyEnrollments`, `getCourseProgress`, `getProfile`,
`enrollInCourse`, `requestAdvisorRecommendation`, and `analyzeCareerGoal` continue
unchanged.

## Design Tokens Used
- Existing: `--color-primary`, `--color-gold`, `--color-teal`, `--color-foreground`,
  `--color-surface`, `--color-surface-elevated`, `--color-border`,
  `--gradient-primary`, `--gradient-gold`.
- Existing utilities: `.bezel`, `.bezel-inner`, `.hairline`, `.shadow-bezel`,
  `.shadow-elevated`, `.shadow-luxury`, `font-mono`, `eyebrow`.
- No new tokens were added — both pages live entirely on the design system shipped in
  `styles.css` from the earlier landing pass.

## States Handled
- [x] Loading — advisor bot bubble shows a three-dot bounce with "Reading catalog" copy;
  dashboard uses `StateCard` for explore-query loading.
- [x] Error — advisor renders an inline Retry button-in-button inside the same bot
  bubble; dashboard renders `StateCard` for the error case.
- [x] Empty — advisor zero-match copy lives under the bubble; dashboard renders a
  centred Compass empty-state for "no active enrollment" and `StateCard` for "no
  suggestions yet".
- [x] Success — recommendation grid renders the Best Match double-bezel card + Alt
  option hairline card; suggested-courses tiles render with hover lift.

## Dark Mode Tested
Yes. Every new surface reads from tokens — bezel/hairline/shadow utilities ship `.dark`
variants in `styles.css`. The gradient promo block in the advisor right rail uses
`bg-gradient-to-br from-primary via-primary to-primary-glow`, which adapts via the dark
token reassignment.

## TypeScript Errors
None. `bun run build` completes (worker + server bundles, 33s). Pre-existing prettier
CRLF noise across the repo continues to dominate `bun run lint`; no new lint errors
introduced by this pass.

## Risks / Notes
- The advisor chat container is no longer a `glass grain` overlay. `backdrop-blur` was
  removed from the scrolling content area per the high-end-visual-design skill's
  "blur-constraints" rule — backdrop-blur on a scrolling region causes continuous GPU
  repaints and visible mobile frame drops.
- The advisor Best Match card uses the double-bezel architecture; the Alt option uses a
  flat hairline so the rank hierarchy is communicated by depth, not just by a "Best
  match" label.
- `ProgressRing` is a small inline SVG — no chart library. Stroke-dashoffset transition
  honours the same spring-bezier used elsewhere.
- Bio "No bio added yet." copy uses sentence case + period (matches the rest of the
  surface) instead of "Not set" / placeholder noise.
- No new dependencies. Pure Tailwind v4 + existing `framer-motion` + existing
  `lucide-react`.
