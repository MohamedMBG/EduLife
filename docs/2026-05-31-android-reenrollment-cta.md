# Task Audit - Android Re-enrollment CTA

## Date
2026-05-31

## Task Summary
Improved the My Courses empty state so learners who unenroll or have no active enrollments now see an explicit `Browse Courses` CTA that takes them back to Home, where re-enrollment already exists.

## Files Created
- docs/2026-05-31-android-reenrollment-cta.md

## Files Modified
- app/src/main/java/com/baghdad/edulife/features/courses/ui/CoursesFragment.java
- app/src/main/res/layout/fragment_courses.xml
- app/src/main/res/values/strings.xml

## What Was Done
Added a dedicated empty-state button to the My Courses screen instead of relying on a passive text hint that told the learner to use the Home tab.

Updated `CoursesFragment` so:
- the button appears only when the learner has no active enrollments
- filtered-empty states still show text only, because the learner already has courses and just needs to change the filter
- tapping the CTA navigates directly to `homeFragment` using the same main-tab navigation behavior already used by the app shell

Moved the old hardcoded empty-state strings into string resources for consistency with the rest of the Android UI.

## Architecture Compliance
This change stays entirely in the existing courses UI layer and resources:
- screen logic in `features/courses/ui`
- layout in `res/layout`
- copy in `res/values`

No repository, ViewModel, or backend changes were needed.

## Code Comments Added
Added a comment in `CoursesFragment` explaining why the CTA navigates to Home: re-enrollment already exists there, and the goal is to make that path explicit rather than invent a second enrollment flow.

## Validation / Testing
Ran `./gradlew.bat :app:compileDebugJavaWithJavac` successfully.

Manual QA recommended for:
- unenroll from the last course and confirm the `Browse Courses` button appears
- tap the CTA and confirm it lands on Home with bottom navigation synced
- filtered-empty states still hide the CTA

## Risks / Notes
This keeps a single enrollment entry point on Home instead of duplicating enrollment controls in My Courses. That is simpler for MVP but still leaves Home as the only actual re-enrollment flow.
