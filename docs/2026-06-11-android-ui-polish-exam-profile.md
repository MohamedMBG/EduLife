# Android UI Polish — Exam Flow + Edit Profile

## Goal

Improve the UI of the Android app. A styling-density scan of all 22 layouts showed the exam screen, exam result screen, and edit profile screen were the least polished (1–2 custom backgrounds vs 9–22 on other screens), while being the highest-value screens for the MVP demo flow. Bring them up to the same premium standard as the rest of the app.

## What Changed

### Exam screen (`fragment_exam.xml` + `ExamFragment.java`)
- Top bar: rounded icon back button, answered-count pill (`examTopProgressPill`) on the right, hidden during loading/gate states.
- Header card: rounded card with "FINAL EXAM" eyebrow chip, 22sp title, pass-score and question-count rendered as mint pill chips.
- Question cards (built programmatically): rounded 20dp cards, "QUESTION N OF M" eyebrow pill, choice rows restyled as selectable rounded rows via new `bg_exam_choice` checked-state selector (mint surface + green border when selected), green radio tint.
- Footer: Material `LinearProgressIndicator` showing answered progress, rounded 52dp submit CTA with proper disabled state (`bg_exam_submit_button` selector).
- Gate card (already-passed / cooldown): rounded CTA, uppercase eyebrow, larger padding.

### Exam result screen (`fragment_exam_result.xml`)
- Score hero: circle now sits inside a decorative outer ring (`bg_score_ring`); pass/fail tinting from Java unchanged.
- Certificate card (pass state): gold-on-paper treatment reusing certificate tokens — `cert_paper` card with gold stroke (`bg_exam_cert_card`), gold seal icon in a pale-gold circle (`bg_exam_gold_icon`), certificate number in the existing `bg_cert_number_pill`.
- Retry/cooldown message (fail state): now rendered in the soft error card (`bg_login_error_card`).
- Done button: rounded brand CTA; layout uses `fillViewport` + weighted spacer so the button anchors to the bottom on tall screens.
- All view IDs preserved — `ExamResultFragment.java` untouched.

### Edit profile (`fragment_edit_profile.xml`)
- Section header added: eyebrow chip, "Personal information" title, subtitle explaining where the name appears.
- Inputs upgraded from `Widget.MaterialComponents` to `Widget.Material3.TextInputLayout.OutlinedBox` with 14dp corner radius, brand surface fill, brand-primary stroke/hint colors.
- Progress indicators tinted brand primary.

## Files Touched

- `app/src/main/res/layout/fragment_exam.xml`
- `app/src/main/res/layout/fragment_exam_result.xml`
- `app/src/main/res/layout/fragment_edit_profile.xml`
- `app/src/main/java/com/baghdad/edulife/features/courses/ui/ExamFragment.java`
- `app/src/main/res/values/strings.xml` (8 new strings)
- New drawables: `bg_exam_choice.xml`, `bg_exam_submit_button.xml`, `bg_score_ring.xml`, `bg_exam_cert_card.xml`, `bg_exam_gold_icon.xml`

## Backend Impact

None. No API contracts touched.

## Android Impact

Visual-only plus minimal Fragment wiring (progress bar + top pill). No ViewModel, Repository, or navigation changes. All existing view IDs preserved.

## Web Impact

None.

## Architecture Compliance

- No business logic added to UI classes.
- All colors via existing brand/cert tokens in `colors.xml` — zero hardcoded hex in layouts.
- Reused existing drawables where possible (`bg_catalog_card`, `bg_auth_eyebrow`, `bg_cert_icon`, `bg_cert_number_pill`, `bg_enroll_cta_button`, `bg_login_error_card`).

## Tests / Verification

- `gradlew assembleDebug` — clean build, no errors.
- States preserved on exam screen: loading, error/status, gate (passed/cooldown), content, submit-disabled.

## Risks / Notes

- `RadioButton` background selector relies on `state_checked` propagation — standard behavior, verified pattern.
- Submit button disabled state now visually distinct (pale green) instead of relying on default Material alpha.
- Remaining lower-priority polish candidates from the scan: `item_course_card.xml`, `item_certificate.xml`, `fragment_certificates.xml`, `item_onboarding.xml`.
