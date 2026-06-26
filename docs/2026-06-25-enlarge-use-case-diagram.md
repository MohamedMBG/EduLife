# Task Audit - Enlarge Use Case Diagram

## Date
2026-06-25

## Task Summary
Increased the rendered size of the use case diagram in the jury report.

## Files Created
- docs/2026-06-25-enlarge-use-case-diagram.md

## Files Modified
- rapport PFA/edulife-pfa-jury.tex

## What Was Done
Updated the figure block for the main use case diagram.
Raised the `max totalheight` constraint from `0.85\textheight` to `0.94\textheight` so LaTeX can display the image larger on the page while still respecting page bounds.

## Architecture Compliance
This task only changes report presentation. It does not affect the EduLife application architecture or implementation.

## Code Comments Added
No code comments were added because the task only modified LaTeX layout content.

## Validation / Testing
Validated the targeted figure block directly in `rapport PFA/edulife-pfa-jury.tex`.
No PDF compilation was run, so the final rendered size and page balance still need visual confirmation.

## Risks / Notes
If the figure caption or surrounding spacing feels too tight after PDF generation, the next adjustment should be a slightly lower height such as `0.90\textheight` or a dedicated page break before the figure.
