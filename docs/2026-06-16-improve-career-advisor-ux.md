# Task Audit - Improve Career Advisor UX (Website)

## Date
2026-06-16

## Task Summary
Improved the UX/UI of the Career Advisor page in the website client (Vite web application) to make it look premium, reduce cognitive load for users constructing prompts, add local persistence for previous recommendations, allow side-by-side comparison of alternative paths, and introduce high-end micro-interactions.

## Files Created
None (modifications to existing files only)

## Files Modified
- [__root.tsx](file:///c:/Users/pc/AndroidStudioProjects/EduLife/guided-journey-lab/src/routes/__root.tsx): Mounted the global `<Toaster />` component from `sonner` inside `RootComponent`.
- [advisor.tsx](file:///c:/Users/pc/AndroidStudioProjects/EduLife/guided-journey-lab/src/routes/advisor.tsx): Redesigned the Career Advisor layout into a two-column grid on desktop, added local storage persistence for briefs, added dynamic AI Advisor messages, implemented a 3-step prompt helper selection panel, added a compare paths side-by-side mode, built a word-by-word typewriter fade-in component, added copy brief (to clipboard) and print (PDF layout) actions, and normalized the fallback matching score to realistic percentages.

## What Was Done
1. **Toaster Setup**: Mounted `<Toaster />` globally to support feedback notifications (e.g. copying text, deleting history).
2. **AI Avatar Card**: Integrated a pulsing radial-glow avatar representing the AI Advisor personality with speech bubbles that change contextually (Scrutinizing, loaded, default instructions).
3. **Dynamic Prompt Helper**: Built a selection panel with choices for background level, interests, and languages. Clicking choices dynamically writes a natural goal sentence in the textarea.
4. **localStorage Persistence**: Automated saving completed briefs to browser storage. Briefs persist on reload.
5. **Interactive History**: Redesigned the history list. Clicking a item loads that brief as the active brief and scrolls to the results. Trash buttons on hover and "Clear All" have been integrated.
6. **Staggered Typewriter Animation**: Built a `StreamingText` component using Framer Motion to stagger words with a spring physics slide-up, blur reduction, and opacity fade.
7. **Path Comparison**: Programmed a structured compare view. Users can toggle comparing the best-match and the secondary path side-by-side on a clean, responsive layout.
8. **Document Export Actions**: Added "Copy Text" (copies formatted markdown of the brief and recommendations) and "Print/PDF" (uses dynamic CSS injection to print *only* the brief document, hiding client side navigation).
9. **Score Normalization**: Corrected the fallback matching score rendering (mapped local integer score 0-40 to a realistic 50-99% range).

## Architecture Compliance
- Changes are fully self-contained inside the web routes (`src/routes/` folder) and custom components, keeping layout logic separated from global models, in compliance with EduLife MVVM and modular frontend patterns.
- Global Toast notifications utilize the existing project package `sonner` and Radix.

## Code Comments Added
Added comprehensive inline comments explaining:
- Local storage serialization and filters.
- Natural sentence compile mapping from prompt chips.
- Framer Motion staggered staggerChildren parameters.
- Dynamic CSS injection and print layout overrides.
- Fit score integer-to-percentage scaling logic.

## Validation / Testing
- Verified compilation and bundle generation using the Vite builder.
- Manually check the layout on desktop view for responsive two-column behavior and test prompt builder compiling logic.
