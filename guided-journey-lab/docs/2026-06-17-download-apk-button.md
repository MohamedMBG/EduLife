# Download APK Button

## Goal
Replace the landing-page "Google Play" CTA (toast: "coming soon") with a real
"Download apk EduLife" button that downloads the prerelease APK.

## What Changed
- Copied `app/build/outputs/apk/debug/EduLife-prerelease.apk` (14 MB) into
  `guided-journey-lab/public/` so it ships as a static asset.
- `PublicMobileLearningSection.tsx`: swapped the `<button onClick={toast}>` for an
  `<a href="/EduLife-prerelease.apk" download>` anchor. Label now "Download apk
  EduLife"; icon changed `Smartphone` → `Download`. Removed `toast` import and the
  `handleAndroidPilotClick` handler.
- `.gitignore`: added `!public/EduLife-prerelease.apk` to override the root
  `*.apk` ignore rule (root `../.gitignore:40`), else the APK would not commit and
  Vercel would 404 it.

## Files Touched
- guided-journey-lab/public/EduLife-prerelease.apk (new)
- guided-journey-lab/src/components/landing/PublicMobileLearningSection.tsx
- guided-journey-lab/.gitignore

## Backend Endpoints Used
None. Static file download only.

## Design Tokens Used
No new tokens. Reused existing `bg-primary` button styling.

## States Handled
- [ ] Loading — N/A (static download)
- [ ] Error — N/A (browser handles failed download)
- [ ] Empty — N/A
- [x] Success — browser downloads the APK

## Dark Mode Tested
N/A — button styling unchanged from prior light/dark behavior.

## TypeScript Errors
None. `tsc --noEmit` clean; production build succeeds (`built in 5.22s`),
APK confirmed copied to `dist/client/EduLife-prerelease.apk`.

## Risks / Notes
- This is the **debug** APK (`EduLife-prerelease.apk`), unsigned for Play Store —
  users get the Android "install from unknown sources" prompt. Fine for a pilot.
- 14 MB binary now lives in git. Each new APK build must be re-copied into
  `public/` to update the download; consider a release-signed APK before public
  launch.
- Vercel: `vercel.json` rewrites `/(.*) → /_shell.html`, but rewrites run only
  after the filesystem check, so the real static APK is served directly.
- Deploy via `git subtree push --prefix=guided-journey-lab web main`.
