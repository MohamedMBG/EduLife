# Task Audit - Seed Courses Migration

## Date
2026-05-01

## Task Summary
Created `V3__seed_courses.sql` to seed five published courses with ordered sections, lessons, and preview lessons for Sprint 2 course discovery.

## Files Created
- backend/src/main/resources/db/migration/V3__seed_courses.sql
- docs/2026-05-01-seed-courses-migration.md

## Files Modified
- None

## What Was Done
Added a new Flyway seed migration that inserts:
- 5 published courses
- 10 course sections
- 20 lessons
- 1 preview lesson per course

The seed data uses deterministic UUID values so later seed adjustments and API tests can reference stable identifiers.

The inserted courses are marked `PUBLISHED` and use `CURRENT_TIMESTAMP` for `published_at` so the Android catalog can treat them as live content immediately after migration.

The lesson data respects the ordering constraints introduced in `V2__courses.sql` by:
- assigning positive `display_order` values
- avoiding duplicate section order inside a course
- avoiding duplicate lesson order inside a section

## Architecture Compliance
This task respects the EduLife backend-first Sprint 2 flow by using Flyway seed data instead of long-lived mock APIs.

It stays within the current schema boundaries:
- no CMS tables
- no exam data
- no certificate data
- no deferred discussion or notification features

## Code Comments Added
Added SQL comments in `V3__seed_courses.sql` to explain:
- why real seed catalog data is needed for Sprint 2 integration
- why section ordering is inserted explicitly
- why preview lessons are included in every course

These comments document the product reason for the seed structure rather than repeating the insert syntax.

## Validation / Testing
Validated the new seed file against the existing `V2__courses.sql` schema to ensure:
- course status values are valid
- lesson types match allowed values
- section and lesson ordering constraints are respected
- preview flags are supported by the schema

Manual follow-up recommended:
- run Flyway migrations against the local PostgreSQL instance
- query the seeded tables to confirm all 5 courses and related rows were inserted

## Risks / Notes
The seed data intentionally avoids resource URLs and lesson content payloads because lesson content hosting is a later decision in the execution plan.

If the Android catalog contract later needs localized titles or richer metadata, that should be added in a follow-up migration instead of mutating this baseline seed by hand.
