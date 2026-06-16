# Premium Web UI Pass (guided-journey-lab)

## Goal
Lift the web app (`guided-journey-lab`) from a competent shadcn/ui template feel to a
production-grade, non-generic premium aesthetic. Keep all existing routes, backend
contracts, design tokens, and brand identity intact.

## What Changed

A full visual refresh of the landing page and auth + dashboard cards using three Taste
skills (`design-taste-frontend`, `redesign-existing-projects`, `high-end-visual-design`).

**Design read.** Premium consumer / agency-leaning edtech landing for Moroccan learners.
Kept the project's intentional brand language: serif display (Fraunces) + Figtree sans,
deep teal-green primary, gold accent. These are explicit brand choices in `CLAUDE.md` so
they override the skill's default font/colour preferences.

**Anti-AI patterns removed.**
- 3×2 equal feature card grid (the LLM-default layout) replaced with an asymmetric
  Bento where the lead "Structure" cell spans 4 cols / 2 rows.
- "Three equal pillar" WhyEduLife row replaced with an editorial split: tall lead panel
  + stacked secondaries.
- Edge-to-edge sticky nav glued to the top replaced with a detached floating glass pill
  that swaps to `glass + shadow-elevated` only after scroll.
- Hamburger replaced with a two-line glyph that morphs to an X via rotate/translate.
- `ease-in-out` / `linear` motion replaced with `cubic-bezier(0.16,1,0.3,1)` and
  `cubic-bezier(0.32,0.72,0,1)` for spring-like physics.
- `h-screen` replaced with `min-h-[100dvh]` on hero/auth shells.

**Premium techniques added.**
- "Double-bezel" nested card architecture (`.bezel` outer + `.bezel-inner` core with
  inset highlight) on the hero device, certificate image, login form, journey step icons,
  and feature lead icon.
- "Button-in-button" CTA: trailing arrow lives inside its own nested circle and
  translates diagonally on hover (Hero, Nav, FinalCTA, Login).
- Eyebrow micro-tags (`.eyebrow`, `.eyebrow-dot`) on every section, mono-spaced section
  numbers (`/01`, `01 · Structure`).
- Hero asymmetric grid (7/5 split) with two floating proof cards (80% pass threshold +
  certificate identifier) overlapping the device shell.
- Page-level fixed grain overlay (`.noise-overlay`) to break digital flatness.
- Tinted shadows (`--shadow-bezel`, `--shadow-elevated`) replace generic black drop
  shadows.
- Refined transitions / staggered ascent (`ascend` keyframe), italic emphasis in the
  same Fraunces family (no random serif/sans mixing).
- Editorial split layout on Problem (before/after) with a centred nested gradient
  connector instead of two equal cards.
- Marquee + ascend utility keyframes added for future use.
- Certificate gets a watermark "Verified / 80+ / Score" stamp in gold.

**State surfaces.**
- `MetricCard` / `StateCard` rewritten across `dashboard.tsx` and `courses.index.tsx`
  with the new hairline + bezel hover language.
- Dashboard welcome hero replaced with a deeper gradient card that picks up the same
  warm gold/teal ambient orbs used in FinalCTA — visual consistency across surfaces.

## Files Touched

**Tokens & globals**
- `guided-journey-lab/src/styles.css` — added `.eyebrow`, `.bezel`, `.bezel-inner`,
  `.hairline`, `.ring-hairline`, `.noise-overlay`, `.glass` (light + dark), `.focus-ring`,
  refined shadows (`--shadow-elevated`, `--shadow-bezel`, `--shadow-bezel-dark`,
  `--shadow-luxury`), three custom cubic-bezier easings, `ascend` + `marquee` keyframes,
  `::selection` styling, `text-wrap: balance/pretty`.

**Landing**
- `guided-journey-lab/src/components/landing/Nav.tsx` — floating pill, scroll-triggered
  glass, button-in-button CTA, morphing hamburger.
- `guided-journey-lab/src/components/landing/Hero.tsx` — asymmetric 7/5 split, double-
  bezel device shell, floating proof cards, italic emphasis, button-in-button CTA, trust
  row icons.
- `guided-journey-lab/src/components/landing/Section.tsx` — `SectionLabel` now uses the
  eyebrow style; new `SectionKicker` helper for max-65ch description copy.
- `guided-journey-lab/src/components/landing/Problem.tsx` — 5/2/5 layout with a centred
  nested gradient connector, struck-through old-way headline, bezel iconography.
- `guided-journey-lab/src/components/landing/Journey.tsx` — six-step ordered list with
  bezel icons, mono step numbers (`01..06`), a gradient track line with primary start dot
  + gold end dot.
- `guided-journey-lab/src/components/landing/Features.tsx` — full rebuild as an
  asymmetric Bento; lead cell spans 4×2, secondary cells span 2 / 3 / 3 / 6, distinct
  iconography per cell.
- `guided-journey-lab/src/components/landing/WhyEduLife.tsx` — editorial split: tall
  Direction panel (col-span-5 row-span-2) next to two stacked panels, per-card metric
  footer.
- `guided-journey-lab/src/components/landing/Certificate.tsx` — bezel-wrapped cert image,
  three-up proof tiles (Standard / Identifier / Verification), animated gold "Verified
  80+" watermark stamp.
- `guided-journey-lab/src/components/landing/Morocco.tsx` — staircased language cards
  with progressive left-margin offset, accent gradient bar, brand-coloured live-pilot
  status row.
- `guided-journey-lab/src/components/landing/Stats.tsx` — Bento 5/7 layout: lead stat
  large with the gradient text, the three secondary stats in a 3-col strip.
- `guided-journey-lab/src/components/landing/FinalCTA.tsx` — rebuilt as a 7/5 panel,
  italic gold emphasis, button-in-button waitlist submit, inline success state, noise
  overlay, second CTA collapses to a text link.
- `guided-journey-lab/src/components/landing/Footer.tsx` — 4/8 column layout with three
  named link groups, mono column headers, magnetic-hover social pills.

**Routes / app surfaces**
- `guided-journey-lab/src/routes/index.tsx` — adds page-level `.noise-overlay` and
  `min-h-[100dvh]`.
- `guided-journey-lab/src/routes/login.tsx` — split into form panel + brand panel,
  double-bezel form card, button-in-button submit, noise overlay, brand panel mirrors
  FinalCTA gradient language for cross-surface consistency.
- `guided-journey-lab/src/routes/dashboard.tsx` — welcome hero now uses the luxury
  gradient + ambient orbs, italic gold first-name accent, `MetricCard` / `StateCard`
  rewritten with mono labels, bezel icons, magnetic hover.
- `guided-journey-lab/src/routes/courses.index.tsx` — `MetricCard` + course tile inherit
  the same hairline + magnetic-hover language.
- `guided-journey-lab/src/components/app/AppShell.tsx` — sidebar logo bezel,
  primary-gradient instead of teal flat, active-nav state gets `shadow-bezel` +
  `ring-primary/15` for a soft "pressed-in" feel.

## Backend Endpoints Used
None changed. Pure presentation pass.

## Design Tokens Used
- New: `--shadow-bezel`, `--shadow-bezel-dark`, `--ease-out-expo`, `--ease-out-quint`,
  `--ease-spring`.
- Refined: `--shadow-soft`, `--shadow-elevated`, `--shadow-glow`, `--shadow-luxury`,
  `--shadow-gold`, `--gradient-hero`, `--gradient-aurora`.
- New utilities: `.eyebrow`, `.eyebrow-dot`, `.bezel`, `.bezel-inner`, `.hairline`,
  `.ring-hairline`, `.noise-overlay`, `.focus-ring`, `.ease-spring`, `.ease-out-expo`,
  `.shadow-bezel`, `animate-ascend`, `animate-marquee`.
- Existing brand tokens (`--primary`, `--gold`, `--teal`, `--surface`,
  `--surface-elevated`, font families) are unchanged — every new utility reads from
  them.

## States Handled
- [x] Loading — dashboard `StateCard` and courses `StateCard` rewritten with hairline.
- [x] Error — login form error region kept (destructive token palette).
- [x] Empty — courses + dashboard empty states refreshed.
- [x] Success — FinalCTA submit-success and login post-auth flow preserved.

## Dark Mode Tested
Yes. Every new utility ships a `.dark` variant: `.eyebrow`, `.bezel`, `.bezel-inner`,
`.hairline`, `.ring-hairline`, `.shadow-bezel`, `.glass`. All colours use the token
system so dark mode follows automatically.

## TypeScript Errors
None. `bun run build` completes (web app + worker entry, both bundles emit clean). The
prior `lint` failures (12442 prettier `Delete CRLF` errors) are pre-existing Windows
line-ending noise across the whole repository — not introduced by this pass. The one
real parse error introduced (`bg-[url(...)]` arbitrary value with escaped quotes inside
JSX className) was fixed by moving the noise SVG to a `style={{ backgroundImage }}`
attribute in `FinalCTA.tsx` and `login.tsx`.

## Risks / Notes
- The pass intentionally preserves Fraunces + Figtree (project brand) even though some
  Taste-skill rules ban Fraunces as a default display serif. The brand decision lives in
  `guided-journey-lab/CLAUDE.md` and overrides the skill default.
- Some routes outside the landing surface (admin.\*, teach.\*, planner, analytics,
  level, learn.\*, certificates.\*) were not touched in this pass — they continue to use
  the prior `rounded-3xl border border-border ...` card pattern. They are unblocked by
  this change (the new utilities are additive). A follow-up pass can roll the same
  hairline / bezel / magnetic-hover language across those surfaces.
- The fixed `.noise-overlay` is mounted only on the landing route to avoid GPU cost on
  long-scrolling product surfaces. Same trade-off the Taste skill calls out for grain.
- No new dependencies added. Everything uses Tailwind v4 utilities, existing
  `framer-motion`, and existing `lucide-react`.
