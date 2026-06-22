# Task Audit - Fix Missing Info Outline Drawable

## Date
2026-06-22

## Task Summary
Fixed the Android resource-linking failure from GitHub Actions where `fragment_gamification.xml` and `fragment_leaderboard.xml` referenced `@drawable/ic_info_outline`, but that drawable resource did not exist in the project.

## Files Created
- app/src/main/res/drawable/ic_info_outline.xml
- docs/2026-06-22-fix-missing-info-outline-drawable.md

## Files Modified
- None

## What Was Done
Added a new vector drawable resource named `ic_info_outline.xml` under `app/src/main/res/drawable/`.

This is the smallest clean fix because:
- the layouts already intentionally depend on `@drawable/ic_info_outline`
- adding the missing shared asset preserves the intended UI
- no layout logic or navigation behavior needed to change

The new drawable covers all failing references reported by CI:
- `fragment_gamification.xml`
- `fragment_leaderboard.xml`

## Architecture Compliance
The change stays inside Android shared UI resources under `app/src/main/res/drawable/`, which matches the existing Android resource organization. No feature boundaries, MVVM responsibilities, or backend contracts were changed.

## Code Comments Added
Added a short XML comment inside the new drawable file to explain why the shared icon exists and where it is used. This keeps the resource intention clear without adding noise.

## Validation / Testing
Validated the reported failure against the CI log:
- `:app:processDebugResources` failed because `@drawable/ic_info_outline` was missing
- the missing drawable resource now exists at the expected path

Local full Gradle verification remains blocked by a separate machine-specific JDK/toolchain issue already noted in the previous task audit. That local issue is unrelated to the GitHub resource-linking failure fixed here.

## Risks / Notes
- This fix is targeted to the reported CI failure only.
- If GitHub Actions still fails after this patch, the next error will likely be a different missing resource or lint issue rather than this `ic_info_outline` reference.
