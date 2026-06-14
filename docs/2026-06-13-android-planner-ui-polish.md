# Android Study Planner — UI Polish

## Goal

Refine the Study Planner screen so it matches the premium minimal aesthetic defined in `app/CLAUDE.md`: generous spacing, refined typography, correct iconography, and clearer visual hierarchy on the progress card.

## What Changed

- Stepper buttons now use proper minus / plus glyphs instead of left / right arrows (the arrows were misleading and read as navigation).
- Added a brand-tinted percentage chip on the progress card so users can see weekly completion at a glance.
- Day selectors are now evenly distributed across the row via `layout_weight` and `FrameLayout` centering, with a larger 44dp tap target.
- Log-time buttons fill the row (`weight=1`) at 44dp height; matches add-task input height.
- Header padding, card padding (24dp), and inter-card gaps (18dp) increased for a calmer, more spacious layout.
- Task row gets a subtle border, larger radius, and a 56dp minimum height for a more substantial feel.
- Title typography tightened (letter-spacing -0.02, slightly larger).
- Course focus checkboxes now use density-scaled padding/margins instead of raw pixels.

## Files Touched

- `app/src/main/res/layout/fragment_planner.xml`
- `app/src/main/res/layout/item_planner_task.xml`
- `app/src/main/res/drawable/bg_planner_task_item.xml`
- `app/src/main/res/drawable/bg_planner_percentage_chip.xml` (new)
- `app/src/main/res/drawable/ic_planner_minus.xml` (new)
- `app/src/main/res/drawable/ic_planner_plus.xml` (new)
- `app/src/main/java/com/baghdad/edulife/features/courses/ui/PlannerFragment.java`

## Backend Impact

None.

## Android Impact

UI-only. View IDs preserved (`progressHoursText`, `plannerProgressBar`, `targetHoursText`, day IDs, etc.). New view ID `progressPercentageChip` bound in `PlannerFragment` and updated alongside the progress bar. No behavioral or data-flow changes; ViewModel and persistence untouched.

## Web Impact

None.

## Architecture Compliance

- Feature-first MVVM untouched: Fragment still observes `PlannerViewModel` LiveData and delegates writes back to the ViewModel.
- No business logic added to UI classes.
- No new dependencies, no Kotlin, no Hilt — Java + XML only.

## Tests / Verification

- `./gradlew :app:compileDebugJavaWithJavac` succeeds.
- Manual: rebuild and inspect Planner tab — stepper shows ± icons, percentage chip updates as hours are logged, day pills space evenly, task rows feel less cramped.

## Risks / Notes

- The header padding grew; on very small devices verify the title + Reset button still fit on one line. Reset uses `wrap_content`, so it should adapt.
- Percentage chip text is plain ASCII (`"25%"`) — no locale formatting. Acceptable for MVP; revisit when localizing.
