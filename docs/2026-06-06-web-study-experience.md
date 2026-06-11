# Web Study Experience Upgrades

## Goal

Make the learning side of the website feel like a real course player instead of plain text. Render video, PDF, and article content per lesson type. Add a course-wide resources index and per-lesson personal notes.

## What Changed

### 1. Type-aware lesson player

`learn.$courseId.$lessonId.tsx` now renders content based on `lesson.lessonType` and the shape of `contentUrl`. Detection logic lives in `src/lib/lesson/media.ts`:

- YouTube URLs (`youtube.com/watch`, `youtu.be`, `/embed/`, `/shorts/`) → `youtube-nocookie.com/embed` iframe with `rel=0&modestbranding=1`.
- Vimeo URLs (`vimeo.com`, `player.vimeo.com`) → `player.vimeo.com/video` iframe.
- Direct video files (`.mp4|.webm|.ogg|.mov|.m4v`) → native `<video controls>` with `preload="metadata"`.
- `.pdf` URLs → embedded `<iframe>` PDF viewer with a Download button.
- Anything else → external link card.

Article body (`contentBody`) renders through a tiny inline markdown subset — headings (`#`, `##`, `###`), bullet lists, `**bold**`, inline `code`, and `[text](url)` links. Chose to inline this instead of pulling `react-markdown` for ~5 formatting cases.

### 2. Course resources screen

New route `src/routes/courses.$courseId.resources.tsx`. Reads `getCourseDetail` and groups lessons into three buckets (Videos, PDFs and downloads, Articles and reading). Each card links straight to the lesson player so users can hop between media without going back through the section list.

Course detail header now shows a `Resources` pill so enrolled learners can jump to the index.

Note: the course summary endpoint does not ship `contentUrl` per lesson. Grouping uses `lessonType` only. A dedicated `GET /api/v1/courses/{id}/resources` endpoint that returns URLs would let us render direct download buttons here in a follow-up.

### 3. Per-lesson notes side panel

New `src/components/lesson/LessonNotes.tsx`, backed by `src/lib/lesson/notes.ts`. Notes live in `localStorage` keyed by `edulife_lesson_notes:<lessonId>`. Writes are debounced 400ms. Empty notes delete the key. Shown in the lesson page's right rail under "Course progress" + "Lesson actions". Status line shows last save time (`aria-live="polite"` so screen readers announce it).

LocalStorage chosen instead of a backend table to keep this iteration scoped to frontend-only changes. If notes need to follow the learner across devices, add a `/api/v1/notes` module later.

## Files Touched

- `guided-journey-lab/src/lib/lesson/media.ts` (new)
- `guided-journey-lab/src/lib/lesson/notes.ts` (new)
- `guided-journey-lab/src/components/lesson/LessonContentRenderer.tsx` (new)
- `guided-journey-lab/src/components/lesson/LessonNotes.tsx` (new)
- `guided-journey-lab/src/routes/courses.$courseId.resources.tsx` (new)
- `guided-journey-lab/src/routes/learn.$courseId.$lessonId.tsx` (rewrote content area, added notes panel + resources link, narrowed `lessonQuery.data` undefined branch)
- `guided-journey-lab/src/routes/courses.$courseId.tsx` (added Resources pill in hero)

## Backend Endpoints Used

- `GET /api/v1/courses/{id}` — for resources screen grouping and lesson navigation.
- `GET /api/v1/courses/{courseId}/lessons/{lessonId}` — unchanged, drives the player.
- `GET /api/v1/progress/courses/{courseId}` — unchanged, drives progress bar.
- `PUT /api/v1/progress/lessons/{lessonId}/mark-complete` — unchanged, lesson actions.

No new endpoints added. No backend changes.

## Design Tokens Used

All existing tokens. No new color/shadow/gradient added.

- Cards: `bg-surface-elevated`, `border-border`, `shadow-soft`/`shadow-elevated`
- Hero buttons: `bg-white/10`, `border-white/30`, `text-primary-foreground`
- PDF chrome: `bg-foreground text-background` download button
- Notes textarea: `bg-background border-border focus:border-primary focus:ring-primary/10`

## States Handled

- [x] Loading — `StateCard` on lesson + resources pages.
- [x] Error — `lessonQuery.error.message`, `courseQuery.error.message`.
- [x] Empty — resources screen shows per-section "no items" cards; lesson page shows "no content configured" fallback.
- [x] Success — type-aware renderer.

## Dark Mode Tested

Yes — all tokens cascade. Embedded YouTube/Vimeo iframes stay black, PDF viewer surface uses `bg-background`.

## TypeScript Errors

None in touched files. Verified via filtered `tsc --noEmit`. Pre-existing errors in `demo.ts`, `certificates.tsx`, `courses.$courseId.tsx` (the section I didn't change), unrelated.

## Risks / Notes

- **PDF embedding** uses `<iframe src="...pdf#toolbar=1">`. Works in Chrome/Edge/Firefox natively; Safari users get the browser's built-in viewer. Some CORS-protected PDF hosts may refuse framing — fallback download button stays accessible.
- **YouTube nocookie** still loads Google fonts/CSS. Privacy-strict tenants may want to gate behind a click-to-load placeholder; out of scope here.
- **Markdown subset** is intentional. If teachers expect tables, images, or footnotes, swap `MarkdownLite` for `react-markdown` + `remark-gfm` in a follow-up.
- **Notes** are device-local. Switching browsers/devices loses them. A backend table is the next step if cross-device sync becomes a requirement.
- **Resources grouping** is heuristic on `lessonType`. A teacher who mislabels a video as `ARTICLE` will see it in the wrong bucket. Backend-side validation on lessonType + future resources endpoint would tighten this.
- **No per-lesson quizzes** in this batch. End-of-course MCQ exam remains the only quiz surface. Per-lesson quiz needs new backend module (table, DTOs, scoring, migration) — deferred to a follow-up task per the user's earlier go-ahead.
