# Task Audit - Lesson Entity

## Date
2026-05-04

## Task Summary
Created a JPA `Lesson` entity mapped to the `lessons` table for the Sprint 2 course catalog domain.

## Files Created
- backend/src/main/java/com/edulife/courses/entity/Lesson.java
- docs/2026-05-04-lesson-entity.md

## Files Modified
- None

## What Was Done
Added a new `Lesson` JPA entity in the backend `courses` module.

Mapped the entity to the `lessons` table with these fields:
- `id`
- `courseSectionId`
- `title`
- `summary`
- `lessonType`
- `estimatedDurationMinutes`
- `displayOrder`
- `preview`
- `createdAt`
- `updatedAt`

Kept the entity intentionally flat by storing the parent section reference as a `UUID` instead of adding a JPA relationship. This matches the current low-complexity approach used in the course catalog domain and avoids introducing entity graph behavior before the lesson flow needs it.

Added JPA lifecycle hooks for `createdAt` and `updatedAt` so application-managed writes keep audit timestamps aligned with the migration defaults.

## Architecture Compliance
The new entity was placed in `backend/src/main/java/com/edulife/courses/entity`, which follows the EduLife modular monolith structure and keeps persistence models inside the `courses` domain module.

The implementation respects the current project guidance to prefer simple, pragmatic mappings over unnecessary abstractions.

## Code Comments Added
Added comments in `Lesson.java` to explain:
- why `course_section_id` is stored as a `UUID` instead of a JPA association
- why timestamp lifecycle hooks are handled in the entity

These comments explain design intent rather than restating obvious field names.

## Validation / Testing
Ran `./mvnw -DskipTests compile` in `backend/` and the build succeeded.

## Risks / Notes
The entity currently uses `String` for `lessonType` to keep the mapping simple and close to the database contract. If lesson-type behavior grows later, this can be tightened into an enum with a clear use case.
