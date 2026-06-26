# Task Audit - Maximize Use Case Diagram Page

## Date
2026-06-25

## Task Summary
Adjusted the use case diagram so it occupies more of the dedicated page in the jury report.

## Files Created
- docs/2026-06-25-maximize-use-case-diagram-page.md

## Files Modified
- rapport PFA/edulife-pfa-jury.tex

## What Was Done
Updated the `Diagramme de cas d'utilisation principal` figure block.
Replaced the previous image sizing with a centered `\makebox` wrapper and enlarged the image constraints to `width=1.04\textwidth` and `height=0.94\textheight`.
Kept `keepaspectratio` so the image scales up safely without distortion.
Preserved the dedicated-page layout and caption.

## Architecture Compliance
This task only changes LaTeX report presentation and does not affect the EduLife application architecture or implementation.

## Code Comments Added
No code comments were added because the task only modified documentation layout.

## Validation / Testing
Validated the updated figure block directly in `rapport PFA/edulife-pfa-jury.tex`.
No PDF compilation was run, so the final rendered page still needs visual confirmation.

## Risks / Notes
Because the image now slightly exceeds `\textwidth`, final PDF rendering should be checked to ensure it remains visually balanced and does not clip unexpectedly.
If you want an even more aggressive full-page effect after rendering, the next step would be to move the caption off the figure page or reduce its vertical spacing.
