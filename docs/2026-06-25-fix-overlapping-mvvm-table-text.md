# Task Audit - Fix Overlapping MVVM Table Text

## Date
2026-06-25

## Task Summary
Fixed the overlapping text issue in Table 3.3 of the jury report by correcting the LaTeX table layout.

## Files Created
- docs/2026-06-25-fix-overlapping-mvvm-table-text.md

## Files Modified
- rapport PFA/edulife-pfa-jury.tex

## What Was Done
Reworked the LaTeX structure of the table `Correspondance entre écrans Android et couches MVVM`.
Reduced inter-column padding with a local `\tabcolsep` override and slightly tightened row height with `\arraystretch`.
Changed the table to percentage-based column widths so the full layout stays within the printable text area.
Applied monospaced formatting only to the code-oriented columns instead of forcing `\texttt{}` repeatedly in each cell.
Inserted explicit `\newline` breaks in long fragment, ViewModel, and repository lists so identifiers wrap cleanly instead of colliding across columns.
Scoped the formatting changes inside a local group so the rest of the document layout remains unchanged.

## Architecture Compliance
This task only changes the report presentation. It stays aligned with the actual Android MVVM architecture and does not modify any project implementation.

## Code Comments Added
No code comments were added because the task only modified LaTeX documentation layout.

## Validation / Testing
Validated the updated table block directly in `rapport PFA/edulife-pfa-jury.tex`.
No LaTeX compilation was run, so the final PDF still needs to be regenerated to confirm that the overlap is fully resolved.

## Risks / Notes
This fix addresses the underlying width overflow in the table definition, so it should be more stable than wording-only changes.
If the table is still visually dense after PDF generation, the next step should be splitting the table into two smaller tables rather than shrinking the font further.
