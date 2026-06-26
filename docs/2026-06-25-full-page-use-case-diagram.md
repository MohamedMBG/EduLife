# Task Audit - Full Page Use Case Diagram

## Date
2026-06-25

## Task Summary
Adjusted the use case diagram in the jury report so section `3.13.1 Diagramme de cas d'utilisation` uses a dedicated page and renders the image much larger.

## Files Created
- docs/2026-06-25-full-page-use-case-diagram.md

## Files Modified
- rapport PFA/edulife-pfa-jury.tex

## What Was Done
Replaced the inline `figure[H]` block with a page-oriented `figure[p]`.
Added `\clearpage` before and after the figure so the use case diagram is isolated on its own page.
Replaced the `\fullimg` macro call with a direct `\includegraphics` call sized to `width=\textwidth,height=0.88\textheight,keepaspectratio` so the image occupies most of the page while leaving room for the caption.

## Architecture Compliance
This task only changes LaTeX presentation in the report. It does not affect the EduLife application architecture or implementation.

## Code Comments Added
No code comments were added because the task only modified documentation layout.

## Validation / Testing
Validated the targeted LaTeX block directly in `rapport PFA/edulife-pfa-jury.tex`.
No PDF compilation was run, so the rendered page layout still needs visual confirmation.

## Risks / Notes
The added page breaks may shift nearby pagination and figure numbering placement in the generated PDF.
If the rendered caption still compresses the image too much, the next adjustment should be a slightly larger height such as `0.90\textheight` or moving the caption to a separate page note if your formatting rules allow it.
