# Task Audit - Fix Flyway Checksum Mismatch

## Date
2026-04-27

## Task Summary
Reset the local PostgreSQL `edulife` database to clear a stale Flyway history entry for `V1__init.sql`, then verified that the backend test suite passes and that Spring Boot starts cleanly with the current migration checksum.

## Files Created
- docs/2026-04-27-fix-flyway-checksum-mismatch.md

## Files Modified
- None

## What Was Done
- Inspected the Flyway migration folder and confirmed there is a single migration file at `backend/src/main/resources/db/migration/V1__init.sql`.
- Inspected the local database and confirmed `flyway_schema_history` contained version `1` with checksum `1960459403`, while the current local migration resolves to checksum `-132914803`.
- Inspected the existing `users` table and confirmed it reflected an older shape than the current migration, which proves the local database was created from an earlier version of `V1__init.sql`.
- Reset the local development database by dropping and recreating `edulife`.
- Ran `mvn test` successfully.
- Ran `mvn spring-boot:run` with a temporary local Firebase credential fixture provided through environment variables only, so no secrets were added to the repository.
- Verified Flyway validated and applied the current schema history cleanly on the fresh database.
- Cleared one stale local Java process that was temporarily holding port `8080` from an earlier verification run, then reran startup successfully on the default port.

## Architecture Compliance
This task respects the EduLife architecture by keeping Flyway enabled, preserving the existing modular monolith backend setup, and avoiding any workaround that would weaken migration validation. No new architecture, migration strategy, or security shortcut was introduced.

## Code Comments Added
No code changes were required, so no code comments were added in this task.

## Validation / Testing
- Verified `backend/src/main/resources/db/migration/V1__init.sql` is the only migration.
- Verified stale local checksum before reset:
  - applied checksum: `1960459403`
  - current resolved checksum: `-132914803`
- Ran `mvn test` successfully.
- Verified `mvn spring-boot:run` startup logs included:
  - `Successfully validated 1 migration`
  - `Current version of schema "public": 1`
  - `Schema "public" is up to date. No migration necessary.`
  - `Tomcat started on port 8080 (http)`
  - `Started BackendApplication`
- Verified `flyway_schema_history` after reset contains:
  - version `1`
  - script `V1__init.sql`
  - checksum `-132914803`
- Verified the recreated `users` table matches the current migration shape, including `created_at TIMESTAMPTZ` and no UUID default on `id`.

## Risks / Notes
- This reset is acceptable only because this is still early local development and there is no production database to preserve.
- Editing an already applied Flyway migration is dangerous because every existing database that recorded the old checksum will fail validation or drift from the SQL now stored in source control.
- For future schema changes, keep `V1__init.sql` immutable after it has been applied anywhere and add a new `V2__...sql` migration instead.
- Local reset commands used:
  - `C:\Program Files\PostgreSQL\17\bin\dropdb.exe -U postgres --if-exists edulife`
  - `C:\Program Files\PostgreSQL\17\bin\createdb.exe -U postgres edulife`
