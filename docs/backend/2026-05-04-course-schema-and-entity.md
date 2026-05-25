# Task Audit - Course Schema And Entity

## Date
2026-05-04

## Task Summary
Created the Sprint 2 course catalog database shape for courses, sections, and lessons, documented the seed catalog data, and completed the JPA mapping for the `courses` table.

## Files Created
- docs/2026-05-04-course-schema-and-entity.md

## Files Modified
- backend/src/main/resources/db/migration/V2__courses.sql
- backend/src/main/resources/db/migration/V3__seed_courses.sql
- backend/src/main/java/com/edulife/courses/entity/Course.java

## What Was Done
Updated `V2__courses.sql` so the course catalog schema clearly includes the `courses`, `course_sections`, and `lessons` tables with low-complexity database rules for ordering and catalog safety.

Added supporting indexes for common Sprint 2 lookups:
- course status
- published date
- section lookup by `course_id`
- lesson lookup by `course_section_id`

Kept the ordering rules in the database with positive `display_order` checks and unique `(parent_id, display_order)` constraints so section and lesson ordering cannot become ambiguous.

Confirmed `V3__seed_courses.sql` contains 5 published courses with sections, lessons, and preview lessons, and added comments to keep the seed intent clear for future resets.

Completed the `Course` JPA entity mapping for the `courses` table by including the authored-by UUID column and simple lifecycle hooks for audit timestamps.

## Architecture Compliance
The schema work stays inside Flyway migrations under `backend/src/main/resources/db/migration`, which matches the backend-first Sprint 2 contract-first flow in the project instructions.

The entity work stays inside the `courses` domain module at `backend/src/main/java/com/edulife/courses/entity`, which respects the modular monolith structure and keeps course persistence logic inside the course module.

The entity uses a raw `UUID` for `created_by_user_id` instead of adding a cross-module relationship, which keeps complexity low to mid and avoids premature coupling.

## Code Comments Added
Added SQL comments in the migration files to explain:
- why the indexes exist
- why the ordering constraints are enforced
- why the seed dataset stays intentionally small

Added Java comments in `Course.java` to explain:
- why the author link is stored as a UUID instead of a JPA association
- why lifecycle hooks manage timestamps in the entity

## Validation / Testing
Ran `./mvnw -DskipTests compile` in `backend/` and the build succeeded.

Ran `./mvnw test` in `backend/`, but full tests could not complete because Flyway detected checksum mismatches for already-applied local migrations `V2` and `V3` in the existing `edulife` PostgreSQL database.

## Risks / Notes
Because `V2__courses.sql` and `V3__seed_courses.sql` already existed and had been applied in the local database before this task, Flyway validation now requires either:
- a fresh database for test runs, or
- a deliberate Flyway repair/reset decision

No repair was executed automatically to avoid mutating the existing local schema history without an explicit request.
