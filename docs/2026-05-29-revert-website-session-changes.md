# Task Audit - Revert Website Session Changes

## Date
2026-05-29

## Task Summary
Reverted the website work added during this session for `guided-journey-lab`, including the new backend-aligned routes, shared shell, mock backend data, and website-specific task audit files.

## Files Created
- docs/2026-05-29-revert-website-session-changes.md

## Files Modified
- guided-journey-lab/src/routeTree.gen.ts
- guided-journey-lab/src/routes/__root.tsx
- guided-journey-lab/src/routes/courses.tsx
- guided-journey-lab/src/routes/dashboard.tsx
- guided-journey-lab/src/routes/explore.tsx
- guided-journey-lab/src/styles.css

## What Was Done
Removed the new website files created during this session:

- `guided-journey-lab/src/components/web/AppShell.tsx`
- `guided-journey-lab/src/data/mock-backend.ts`
- `guided-journey-lab/src/routes/certificates.tsx`
- `guided-journey-lab/src/routes/courses.$courseId.exam.tsx`
- `guided-journey-lab/src/routes/courses.$courseId.tsx`
- `guided-journey-lab/src/routes/groups.tsx`
- `guided-journey-lab/src/routes/learn.$courseId.$lessonId.tsx`
- `guided-journey-lab/src/routes/profile.tsx`
- website audit docs created during this session

Restored the original content of the tracked website files that were replaced during this session:

- `__root.tsx`
- `courses.tsx`
- `dashboard.tsx`
- `explore.tsx`
- `styles.css`

Ran a website build so the generated route tree would be rebuilt against the original route set.

## Architecture Compliance
The rollback was limited to the website under `guided-journey-lab` and the website-specific task audit files. Unrelated Android, backend, and pre-existing documentation changes were left untouched.

## Code Comments Added
No new product code comments were added. This task removed session-added website files and restored original content.

## Validation / Testing
Ran:

- `pnpm build` in `guided-journey-lab`

The build completed successfully after the rollback.

## Risks / Notes
The session-added website surfaces were removed and the tracked website files were restored to their original visible content, but Git still reports the restored tracked files as modified. The remaining mismatch appears to be file metadata or line-ending normalization rather than remaining session functionality. The shell environment prevented completing a final low-level normalization step automatically.
