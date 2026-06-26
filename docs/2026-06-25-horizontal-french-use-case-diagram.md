# Task Audit - Horizontal French Use Case Diagram

## Date
2026-06-25

## Task Summary
Remplacement du diagramme de cas d'utilisation image par une version LaTeX native, horizontale, en français, avec acteurs en stickmans et services en rectangles.

## Files Created
- docs/2026-06-25-horizontal-french-use-case-diagram.md

## Files Modified
- rapport PFA/edulife-pfa-jury.tex

## What Was Done
Le rapport jury a été mis à jour pour dessiner directement le diagramme de cas d'utilisation avec TikZ.
La solution ajoute les packages `tikz` et `pdflscape`, définit des styles dédiés au diagramme, puis remplace l'ancienne image PNG par une figure vectorielle en mode paysage.
Le nouveau diagramme organise les domaines fonctionnels horizontalement à l'intérieur du système EduLife :
- parcours étudiant
- espace enseignant
- pilotage de groupe
- administration plateforme

Les acteurs sont maintenant représentés par des stickmans UML simplifiés et les services par des rectangles arrondis avec libellés en français.
Le paragraphe explicatif a aussi été harmonisé pour utiliser une terminologie française cohérente, notamment `responsable de groupe` à la place de `group admin`.

## Architecture Compliance
La modification reste localisée au document de rapport dans `rapport PFA/` sans toucher au backend ni aux clients applicatifs.
Elle respecte l'organisation actuelle du projet en apportant une amélioration de présentation uniquement dans le bon artefact documentaire.

## Code Comments Added
Des commentaires ont été ajoutés dans le fichier LaTeX pour expliquer :
- pourquoi le diagramme passe en vectoriel TikZ plutôt qu'en image raster
- pourquoi la mise en page paysage est utilisée
- pourquoi une macro stickman dédiée est définie

## Validation / Testing
Une relecture ciblée de la zone modifiée a été effectuée après édition.
La compilation locale n'a pas pu être exécutée dans cet environnement car aucun moteur LaTeX (`pdflatex`, `xelatex`, `lualatex`, `latexmk`, `tectonic`) n'est disponible dans le shell courant.

## Risks / Notes
Le rendu final doit être vérifié dans un environnement disposant d'un moteur LaTeX, car la taille visuelle exacte dépendra de la distribution installée.
Si l'impression jury montre encore des labels trop serrés, l'ajustement à faire sera principalement sur `minimum width`, `below=...` ou le facteur de `resizebox`.
