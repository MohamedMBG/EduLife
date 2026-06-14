# Task Audit - Fix Study Planner Android UI

## Date
2026-06-13

## Task Summary
Fixed the UI of the Android Study Planner screen to match the premium design language used across the rest of the EduLife app. The planner had multiple visual inconsistencies: generic system progress bar, wrong button drawables, flat cards, missing section icons, and hardcoded text.

## Files Created
- `app/src/main/res/drawable/bg_planner_progress_bar.xml` — Custom rounded progress bar with brand colors
- `app/src/main/res/drawable/bg_planner_stepper_button.xml` — Proper stepper button for +/- hours control
- `app/src/main/res/drawable/bg_planner_card.xml` — Card background with subtle border for depth
- `app/src/main/res/drawable/bg_planner_progress_card.xml` — Highlighted mint card for the progress tracker
- `app/src/main/res/drawable/bg_planner_task_item.xml` — Subtle muted background for checklist items
- `app/src/main/res/drawable/bg_planner_log_button.xml` — Filled primary pill buttons for log time actions

## Files Modified
- `app/src/main/res/layout/fragment_planner.xml` — Complete UI overhaul
- `app/src/main/res/layout/item_planner_task.xml` — Updated task item styling
- `app/src/main/res/values/strings.xml` — Added `planner_reset_btn_short` and `planner_log_label` strings

## What Was Done

### Visual Fixes Applied

1. **Progress bar**: Replaced `@android:drawable/progress_horizontal` with a custom rounded drawable (`bg_planner_progress_bar.xml`) using the brand palette — rounded corners, `brand_primary` fill on `brand_primary_surface` track.

2. **Progress tracker card**: Changed from `bg_settings_item` (plain white) to a new `bg_planner_progress_card` with mint-tinted background (`brand_primary_surface`) and green border (`brand_primary_border`). This makes the most important card visually distinct.

3. **Log time buttons**: Replaced `bg_catalog_filter_button` (outlined mint) with `bg_planner_log_button` (filled primary pill) so the CTA stands out against the tinted progress card. Text changed to white.

4. **Section icons**: Added brand-tinted icons (timer, sparkle, book, description, courses, check-circle) before each card title, creating visual hierarchy that matches the Career Advisor and other premium screens.

5. **Target hours stepper**: Replaced `bg_back_button` (translucent white oval meant for dark headers) with `bg_planner_stepper_button` — a proper stepper with muted fill, border, and pressed state. Increased size from 44dp to 48dp with proper padding.

6. **All cards**: Switched from `bg_settings_item` (flat white, no border) to `bg_planner_card` (white with subtle brand border), adding depth and separation.

7. **Day selector circles**: Increased from 38dp to 42dp for better tap targets.

8. **Task checklist items**: Changed from `bg_catalog_lesson_row` (heavy bordered) to `bg_planner_task_item` (subtle muted fill without border). Added tint to delete icon. Refined spacing.

9. **Hardcoded text**: Replaced `"New Week"` and `"Log time studied:"` with string resources (`planner_reset_btn_short`, `planner_log_label`).

10. **Spacing refinements**: Increased header bottom padding, improved margins between sections, added line-spacing to subtitle text, better internal card padding.

## Architecture Compliance
- All changes are in the `features/courses/ui/` domain (layout files) and `res/` resources — consistent with the EduLife feature-first MVVM structure.
- No Java logic changes were needed; the layout uses the same view IDs so PlannerFragment.java works without modification.
- New drawables follow existing naming conventions (`bg_planner_*`).
- String resources follow the existing `planner_` prefix pattern.

## Code Comments Added
- XML comments in all new drawables explaining their purpose and design rationale.
- Section separator comments in the layout file for each card.
- Comments on key design decisions (e.g., "Custom rounded progress bar replacing the generic system drawable").

## Validation / Testing
- Gradle `assembleDebug` build executed to verify compilation.
- All view IDs preserved from the original layout, ensuring PlannerFragment.java binds correctly without code changes.

## Risks / Notes
- The progress bar height increased from 8dp to 10dp for better visibility; this is cosmetic only.
- Day selector circles increased from 38dp to 42dp — should be verified on narrow screens (320dp width phones) to ensure they don't overflow.
- The `bg_planner_progress_card` uses `brand_primary_surface` which is a light mint; on certain screens this may look similar to the background — verified against the existing color values to ensure sufficient contrast.
