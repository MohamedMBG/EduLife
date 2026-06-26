# Task Audit - Fix Use Case Label Overflow Copy

## Date
2026-06-25

## Task Summary
Adjusted the copied PlantUML use case diagram so long labels stay inside the use case ovals instead of spilling outside.

## Files Created
- docs/2026-06-25-fix-use-case-label-overflow-copy.md

## Files Modified
- diagrams/edulife-use-case-diagram copy.puml
- diagrams/edulife-use-case-diagram copy.png

## What Was Done
Updated the use case labels in `diagrams/edulife-use-case-diagram copy.puml`.
Inserted explicit line breaks in long use case names such as:
- `Synchroniser l'identite`
- `Telecharger le certificat`
- `Consulter les analytics plateforme`
- `Importer l'image de couverture`
- and the other long learner, teacher, group, and admin actions.
These line breaks force PlantUML to increase the height of the ellipse instead of letting the text extend beyond the use case boundary.
Rendered a fresh PNG and updated `diagrams/edulife-use-case-diagram copy.png` to match the patched source.

## Architecture Compliance
This task only changes the copied diagram source and its rendered asset. It does not affect the EduLife application architecture or implementation.

## Code Comments Added
No code comments were added because this task only modified PlantUML and rendered image files.

## Validation / Testing
Rendered the updated diagram from `diagrams/edulife-use-case-diagram copy.puml`.
Visually checked the generated PNG to confirm that the long labels now wrap inside the use case ovals.

## Risks / Notes
The labels now fit inside the use case shapes, but the diagram remains dense because of the number of actors and links.
If you want a cleaner overall composition later, the next step should be reducing the number of direct associations or splitting the diagram by service area.
