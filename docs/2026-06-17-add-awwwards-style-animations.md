# Add Awwwards-Style Animations to Visitor Landing Page

## Goal
Add high-end cinematic animations (Awwwards-level) to the public visitor landing page at `/`.

## What Changed

### New Dependencies
- `gsap` v3.15.0 - for ScrollTrigger-powered scroll-driven animations
- `lenis` v1.3.23 - for buttery smooth scroll

### New Animation Components (`src/components/landing/animations/`)
- `SmoothScroll.tsx` - Lenis smooth scroll wrapper (respects reduced-motion)
- `ScrollProgress.tsx` - thin scroll progress indicator bar at page top
- `MagneticButton.tsx` - cursor-tracking magnetic effect on CTAs using motion values
- `SplitText.tsx` - word-by-word reveal with blur + rotateX + stagger
- `TextClipReveal.tsx` - GSAP ScrollTrigger clip-path wipe reveal for section headings
- `Parallax.tsx` - scroll-driven parallax offset using framer-motion useScroll
- `HorizontalScroll.tsx` - GSAP ScrollTrigger pin + scrub horizontal scroll section
- `ScrollReveal.tsx` - enhanced entrance wrapper with blur, rotateX, multi-direction support
- `index.ts` - barrel export

### Updated Sections
- **PublicLandingPage** - wrapped in SmoothScroll, added ScrollProgress
- **PublicNavbar** - scroll-driven morph (shadow + bg opacity), animated underline on nav links, staggered mobile menu items
- **PublicHeroSection** - split-text word stagger with blur/rotateX/perspective, magnetic CTA buttons, parallax on phone mockup, animated progress bar, breathing glow effects
- **PublicConflictSection** - TextClipReveal on heading, ScrollReveal wrappers, animated decorative circles, staggered list items
- **PublicMethodologySection** - converted to GSAP horizontal scroll (pinned section, scrub-driven pan), TextClipReveal heading
- **PublicCertificatesSection** - scroll-driven parallax tilt on certificate card, floating score badge with parallax, breathing glow, staggered stats
- **PublicWaitlistCTA** - scale-up entrance, TextClipReveal heading, staggered form elements, magnetic submit button
- **PublicMobileLearningSection** - TextClipReveal heading, parallax phone mockup, animated progress bar, staggered card reveals, magnetic CTA
- **PublicFooter** - staggered column reveals, spring hover on social icons, slide-on-hover footer links

### Styles
- Removed `scroll-behavior: smooth` (Lenis handles scroll)
- Added Lenis CSS overrides (lenis-smooth, lenis-stopped)

## Files Touched
- `guided-journey-lab/package.json` (deps)
- `guided-journey-lab/src/styles.css`
- `guided-journey-lab/src/components/landing/PublicLandingPage.tsx`
- `guided-journey-lab/src/components/landing/PublicNavbar.tsx`
- `guided-journey-lab/src/components/landing/PublicHeroSection.tsx`
- `guided-journey-lab/src/components/landing/PublicConflictSection.tsx`
- `guided-journey-lab/src/components/landing/PublicMethodologySection.tsx`
- `guided-journey-lab/src/components/landing/PublicCertificatesSection.tsx`
- `guided-journey-lab/src/components/landing/PublicWaitlistCTA.tsx`
- `guided-journey-lab/src/components/landing/PublicMobileLearningSection.tsx`
- `guided-journey-lab/src/components/landing/PublicFooter.tsx`
- `guided-journey-lab/src/components/landing/animations/` (8 new files)

## Backend Endpoints Used
None - landing page is static/presentational.

## Design Tokens Used
Existing tokens only (primary, border, shadows). No new tokens.

## States Handled
- [x] Loading - N/A (static page)
- [x] Error - N/A
- [x] Empty - N/A
- [x] Success - page renders with animations

## Dark Mode Tested
N/A - landing page uses its own light-mode color scheme.

## TypeScript Errors
None. Zero tsc errors.

## Risks / Notes
- GSAP ScrollTrigger horizontal scroll on methodology section: works with vertical scroll input. On mobile (< lg), section renders as stacked cards without horizontal scroll.
- Lenis smooth scroll is disabled when `prefers-reduced-motion: reduce` is active.
- All GSAP animations check `prefers-reduced-motion` before initializing.
- Framer Motion animations degrade gracefully.
- GSAP and framer-motion are not mixed within the same component tree (GSAP only in TextClipReveal and HorizontalScroll leaf components).
