# Hero Credential Image

## Goal
Replace the CSS-built phone mockup in the landing hero with a supplied image.

## What Changed
- Copied `ChatGPT Image Jun 17, 2026, 07_12_47 PM.png` → `public/hero-credential.png` (2.3 MB).
- `PublicHeroSection.tsx`: removed the `w-[340px]` phone-frame mockup (Final exam /
  Credential review card, progress bar, notch/button decorations) and replaced it
  with an `<img src="/hero-credential.png">` keeping the same width, rounded corners,
  and shadow. Floating "Authentic proof" card and glow kept.
- Dropped now-unused `Languages` import (only the mockup used it).

## Files Touched
- guided-journey-lab/public/hero-credential.png (new)
- guided-journey-lab/src/components/landing/PublicHeroSection.tsx

## Backend Endpoints Used
None. Static asset only.

## Design Tokens Used
No new tokens. Reused existing `w-[340px]`, `rounded-[3.2rem]`, and shadow utility.

## States Handled
- [x] Success — image renders in hero (desktop only, `hidden lg:block` wrapper unchanged)
- [ ] Loading / Error / Empty — N/A (static image)

## Dark Mode Tested
N/A — image is fixed art; hero wrapper styling unchanged.

## TypeScript Errors
None. Production build succeeds (`built in 4.79s`); image confirmed copied to
`dist/client/hero-credential.png`.

## Risks / Notes
- 2.3 MB PNG — consider compressing / converting to WebP before public launch.
- Image only shows on `lg+` (hero right column is `hidden lg:block`), same as the
  old mockup.
- Deploy via `git subtree push --prefix=guided-journey-lab web main`.
