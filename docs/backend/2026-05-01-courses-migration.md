# Task Audit - Courses Migration

## Date
2026-05-01

## Task Summary
Created Flyway migration `V2__courses.sql` to add the core course catalog tables for courses, course sections, and lessons, including ordering constraints and supporting indexes.

## Files Created
- backend/src/main/resources/db/migration/V2__courses.sql
- docs/2026-05-01-courses-migration.md

## Files Modified
- None

## What Was Done
Added a new Flyway migration for Sprint 2 course discovery data.

The migration creates:
- `courses`
- `course_sections`
- `lessons`

It also adds:
- foreign keys from sections to courses and lessons to sections
- a foreign key from courses to `users` for future teacher ownership without forcing CMS work now
- check constraints for valid course status values
- check constraints that require positive `display_order` values
- unique constraints that prevent duplicate section order inside one course
- unique constraints that prevent duplicate lesson order inside one section
- indexes for course status lookups and creator-based queries

## Architecture Compliance
This change respects the EduLife backend modular monolith plan by extending the database foundation needed for Sprint 2 course discovery without introducing microservices, CMS-heavy tables, or deferred features.

It also follows the execution order in `AGENTS.md`:
- backend-first progression
- contract-friendly schema preparation
- learner flow before CMS

## Code Comments Added
Added SQL comments inside `V2__courses.sql` to explain:
- why the migration avoids early CMS and hosting decisions
- why status values are constrained
- why `display_order` must be positive
- why parent-scoped uniqueness is enforced for sections and lessons
- why the explicit indexes exist

These comments explain the business and architectural reason for the schema rules rather than restating the SQL.

## Validation / Testing
Validated the migration against the existing Flyway folder structure and baseline `V1__init.sql` naming conventions.

Manual follow-up recommended:
- run Flyway against the local PostgreSQL instance
- verify the new tables, foreign keys, and indexes are created successfully
- add seed data in a later migration for Sprint 2 course discovery

## Risks / Notes
Lesson content URLs were intentionally not added here because `AGENTS.md` marks lesson content hosting as a decision that must be finalized before Sprint 4.

`created_by_user_id` is nullable so seed-data-backed catalog work can proceed before teacher CMS is scheduled.
