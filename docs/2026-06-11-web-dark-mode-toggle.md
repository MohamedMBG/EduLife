# Dark Mode Toggle in AppShell

## Goal
Add a persistent dark / light mode toggle to the app shell header that respects the
CSS design system's `.dark` class convention.

## What Changed
`AppShell.tsx`: added `useDarkMode()` hook that reads initial state from the DOM class
(set by the flash-prevention script), toggles `document.documentElement.classList`
on click, and persists to `localStorage` under key `'edulife-dark'`. Sun / Moon icons
from lucide-react. Toggle button placed in the main sticky header to the right of the
header slot.

`__root.tsx`: added an inline `<script>` in `<head>` before React hydration that reads
`localStorage` and applies `.dark` to `<html>` synchronously. Prevents flash of wrong
colour scheme on page load.

## Files Touched
- `guided-journey-lab/src/components/app/AppShell.tsx`
- `guided-journey-lab/src/routes/__root.tsx`

## Backend Endpoints Used
None.

## Design Tokens Used
- `bg-accent`, `text-foreground` — toggle hover state
- `text-muted-foreground` — toggle resting state

## States Handled
- [x] Dark (Moon icon visible, click → light)
- [x] Light (Sun icon visible, click → dark)
- [x] Persists across page reloads via localStorage
- [x] No flash on initial load (inline script in `<head>`)

## Dark Mode Tested
Yes — toggle switches all semantic tokens (background, card, sidebar, borders) via the
`.dark` CSS variable overrides already defined in `styles.css`.

## TypeScript Errors
None.

## Risks / Notes
`useEffect` initialises state from DOM — one render before the icon reflects the stored
preference (imperceptible in practice because the class is already applied by the inline
script). SSR renders without the `.dark` class; the inline script corrects it client-side
before paint.
