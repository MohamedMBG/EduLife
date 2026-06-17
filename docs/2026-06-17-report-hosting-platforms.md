# Task Audit - Report Hosting Platforms

## Date
2026-06-17

## Task Summary
Updated the LaTeX report to mention the deployment and hosting platforms used around EduLife: Render for the backend, Vercel for the frontend, and Neon for the PostgreSQL database layer.

## Files Created
- docs/2026-06-17-report-hosting-platforms.md

## Files Modified
- rapport PFA/edulife-academic-report.tex
- rapport PFA/edulife-academic-report.pdf

## What Was Done
Extended the global architecture chapter to explain the intended deployment split between backend, frontend, and database hosting.

Updated the technologies chapter so the hosting stack is now explicitly documented in:

- the global technologies summary table;
- the backend technologies subsection;
- a dedicated hosting and deployment subsection;
- the web technologies subsection.

The report now states clearly that:

- the Spring Boot backend is hosted on `Render`;
- the React/TanStack frontend is hosted on `Vercel`;
- the PostgreSQL database is hosted on `Neon`.

## Architecture Compliance
The change stays entirely inside the documentation/reporting layer. It does not modify the backend, Android, or web implementation, and it respects the existing report structure by expanding the already-added technologies chapter with deployment details.

## Code Comments Added
No code comments were added because the task only modified LaTeX documentation.

## Validation / Testing
Recompiled the report with:

- `C:\Users\pc\AppData\Local\Programs\MiKTeX\miktex\bin\x64\pdflatex.exe -interaction=nonstopmode -halt-on-error edulife-academic-report.tex`

An intermediate parallel LaTeX run temporarily corrupted the `.toc` file, so the report was regenerated again in a clean serial pass. Final compilation succeeded and produced the updated PDF.

## Risks / Notes
The document still emits existing LaTeX warnings such as `Overfull \\hbox` on long routes and technical strings.

The duplicate page-destination warning from the front-matter numbering scheme is still present and was not changed in this task.
