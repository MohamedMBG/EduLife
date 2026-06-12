# Seed Staff Accounts (Teacher, Group Admin, Admin)

## Goal

Provision ready-to-use staff accounts for local development and testing: one TEACHER, one GROUP_ADMIN, and one ADMIN, with verified emails and known passwords, wired into both Firebase Auth and the backend `users` table.

## What Changed

- Created two new Firebase Auth users via the Admin REST API (service account), both with `emailVerified=true`:
  - `teacher@edulife.test`
  - `groupadmin@edulife.test`
- Reset the password of the existing `admin@edulife.test` Firebase user (password was unknown) and confirmed `emailVerified=true`.
- Inserted matching rows into the local `users` table with the correct roles and real Firebase UIDs, so no first-login auth-sync dance is needed.
- Added `V19__seed_staff_roles.sql`: promotes `teacher@edulife.test` to TEACHER and `groupadmin@edulife.test` to GROUP_ADMIN by email (same no-op-safe pattern as `V18__seed_admin_user.sql`). This keeps roles correct on any environment where these accounts log in before being promoted.
- Seeded production (Neon, via the Render backend at `edulife-2bro.onrender.com`) without direct DB access: called `POST /api/v1/auth/sync` with each account's Firebase token. `intendedRole` gave teacher/group-admin their roles immediately; the admin row was created as LEARNER because ADMIN is not self-assignable.
- Added `V20__promote_admin_role.sql`: re-promotes `admin@edulife.test` to ADMIN. Needed because V18 already ran on Neon before the admin row existed (no-op at the time). Applies on the next Render deploy.

Credentials were handed to the project owner in the session; they are intentionally **not** recorded in this doc.

## Files Touched

- `backend/src/main/resources/db/migration/V19__seed_staff_roles.sql` (new)
- `backend/src/main/resources/db/migration/V20__promote_admin_role.sql` (new)
- `docs/2026-06-12-seed-staff-accounts.md` (this file)
- One-off seeding script ran from a temp directory and was deleted afterwards; no script lives in the repo.

## Backend Impact

- New Flyway migration V19 (data-only UPDATEs, no schema change, no-op when accounts are absent).
- No code changes.

## Android Impact

None. Accounts log in through the normal email/password flow.

## Web Impact

None. Same accounts work on the web client.

## Architecture Compliance

- ADMIN role is not self-assignable through registration (enforced by `AuthSyncService`); the admin account was promoted via migration (V18) and its Firebase password reset server-side.
- No `firebase_uid` exposed to clients; UIDs only live in the DB and Firebase.
- Email verification enforced: all three accounts have `emailVerified=true` in Firebase.

## Tests / Verification

- `signInWithPassword` verified for all three accounts against Firebase (all returned valid tokens with the expected UIDs).
- Local `users` table verified: roles TEACHER / GROUP_ADMIN / ADMIN present with matching UIDs.
- Backend started locally; Flyway applied V18 and V19 cleanly.
- Local end-to-end: `/api/v1/auth/sync` returned TEACHER / GROUP_ADMIN / ADMIN for the three accounts.
- Production end-to-end: prod `/api/v1/auth/sync` returned TEACHER / GROUP_ADMIN; admin currently LEARNER on Neon until V20 deploys.

## Risks / Notes

- These are test-domain accounts (`@edulife.test`) intended for development. Rotate or delete before any production launch.
- V19/V20 only cover role promotion, not account creation — fresh environments still need the Firebase users to exist (created once via the Admin API or console).
- `admin@edulife.test` stays LEARNER on production until V19+V20 reach main and Render redeploys. Alternative: run `UPDATE users SET role = 'ADMIN' WHERE email = 'admin@edulife.test';` once in the Neon SQL console for an immediate fix.
