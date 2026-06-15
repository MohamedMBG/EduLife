# Task Audit - Course Cards Split Design

## Date
2026-06-14

## Task Summary
Redesigned the layout of course cards displayed in the Home Fragment section. Transitioned from a dark image overlay scrim card to a clean split card design to optimize legibility and visual hierarchy.

## Files Created
- [2026-06-14-course-card-split-design.md](file:///c:/Users/pc/AndroidStudioProjects/EduLife/docs/2026-06-14-course-card-split-design.md)

## Files Modified
- [item_course_summary.xml](file:///c:/Users/pc/AndroidStudioProjects/EduLife/app/src/main/res/layout/item_course_summary.xml)

## What Was Done
1. **Layout Restructuring (Split Card):**
   - Transformed `item_course_summary.xml` from a single `FrameLayout` containing text overlaid on top of a dark image scrim, to a vertical `LinearLayout` containing two halves:
     - **Top Half:** Standardized header image height (`140dp`) with clean cropping, featuring the Level Badge placed nicely in the top-left corner.
     - **Bottom Half:** A clean white card body (`brand_surface` background) featuring high-contrast dark text (`brand_text_primary` and `brand_text_secondary`) for details.
   
2. **Details & Action Row updates:**
   - Positioned course title and description on the high-contrast light background to solve text readability issues.
   - Updated the course progress bar to use the custom rounded green progress drawable (`@drawable/bg_planner_progress_bar`) and set its height to a clean `6dp`.
   - Restyled the rating badge to sit inside a light pill capsule (`bg_badge_dialog_pill` tinted with `brand_surface_muted`).
   - Integrated the language icon and text layout inline.
   - Updated the open course button to a solid green CTA action button (`@drawable/bg_enroll_cta_button` with white text), which is visible and matches standard app buttons.

## Architecture Compliance
The changes only modify layout definitions inside the resource folder (`item_course_summary.xml`) and preserve all view binds. No controllers, adapters, repositories, or models were affected, ensuring complete architecture compliance.

## Code Comments Added
Added structural notes inside the layout XML file explaining the card composition.

## Validation / Testing
Ran compile tasks through Gradle wrapper successfully to make sure layout bindings compile cleanly.

## Risks / Notes
Layout bounds are constrained to avoid clipping. The card elevation and corner radius are balanced to match the Material Design system used throughout the app.
