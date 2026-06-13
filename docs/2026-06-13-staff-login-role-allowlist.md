# Staff Login Fix — deterministic role allowlist in auth-sync

## Goal
Login was "not working for all three types of users" — the seeded staff accounts
`admin@edulife.test` (ADMIN), `teacher@edulife.test` (TEACHER), `groupadmin@edulife.test`
(GROUP_ADMIN). Inspect the full login chain, find the real cause, fix it.

## Root Cause (multi-lens inspection + adversarial verification)
A 4-lens parallel audit (frontend, backend auth/security, seeding/migrations, config/CORS)
plus an adversarial verifier converged on one verified cause. It is **not** a "cannot sign in"
bug — Firebase sign-in and `/auth/sync` succeed (emails are verified; CORS allows localhost).
It is a **wrong-role / wrong-portal** bug:

- Staff roles are provisioned only by Flyway migrations V18/V19/V20, which are pure
  `UPDATE users SET role=... WHERE email=...`. They **never create** the row.
- The only row-creating path is `AuthSyncService.createUserIfAbsent`, which inserts every new
  user as **LEARNER** (login sends no `intendedRole`, and ADMIN is never self-assignable) via
  `INSERT ... ON CONFLICT (firebase_uid) DO NOTHING` — so a repeat login never re-promotes.
- Therefore role correctness depends entirely on whether the staff row already existed when
  Flyway ran the UPDATE. On a **fresh DB** the migrations run on an empty `users` table → all
  three UPDATEs match zero rows → every staff account is created later as LEARNER and stays
  LEARNER → lands on the learner portal. On current prod (Neon) specifically, teacher/group
  admin were seeded correctly but `admin@` is still LEARNER until a redeploy runs V20.

Disproven during verification: auth-sync overwriting role to LEARNER (it is `DO NOTHING`),
rate-limit blocking, Firebase Admin misconfig, dashboard redirect loop.

## What Changed (the fix)
A **server-trusted, config-driven staff-email → role allowlist**, consulted on every
`/auth/sync`. This removes the migration-ordering dependency and self-heals existing LEARNER
rows on the next login, on any environment (fresh, local, prod) without a redeploy.

- New `edulife.staff.roles` config (in `application.yaml`) mapping each staff email to its role,
  bound by `StaffRoleProperties` (`@ConfigurationProperties(prefix="edulife.staff")`, registered
  by `AuthConfig`).
- `AuthSyncService.syncCurrentUser` now calls `reconcileStaffRole(user, email)`: if the
  **verified Firebase email** (from the token, never the request body) is in the allowlist and
  the stored role differs, it `setRole(...)` and persists.
- The client-side ADMIN self-assignment block in `resolveIntendedRole` is untouched — the
  allowlist is a separate, server-controlled source, so clients still cannot self-assign a role.

## Files Touched
Created:
- `backend/.../auth/config/StaffRoleProperties.java`
- `backend/.../auth/config/AuthConfig.java`

Modified:
- `backend/.../auth/service/AuthSyncService.java` (inject + `reconcileStaffRole`)
- `backend/src/main/resources/application.yaml` (`edulife.staff.roles` block)
- `backend/src/test/java/com/edulife/auth/AuthSyncControllerTest.java` (3 new tests)

## Backend Endpoints
- `POST /api/v1/auth/sync` — role now reconciled against the staff allowlist on every call.

## Architecture Compliance
- Assignment driven only off the Firebase-verified email; client input cannot influence role.
- DTO-only response unchanged; no `firebase_uid` exposed.
- Business logic stays in the service; config externalized and env-overridable.
- No migration edited; no schema change.

## Tests / Verification
- `./mvnw test-compile` → exit 0 (main + tests compile).
- `AuthSyncControllerTest` on a clean throwaway Postgres DB (`edulife_authtest`, created + dropped
  via JDBC so the dev DB was untouched): **Tests run: 10, Failures: 0, Errors: 0 — BUILD SUCCESS.**
  New tests: admin@ → ADMIN without intent; groupadmin@ → GROUP_ADMIN (stable on repeat login,
  single row); non-staff email → LEARNER. Existing 7 tests still green (no regression).
- Note: running the suite against the shared local `edulife` DB fails in `@BeforeEach`
  `userRepository.deleteAll()` due to leftover `groups` rows (FK `groups_created_by_fkey`) — a
  pre-existing test-isolation weakness unrelated to this fix; the clean DB run confirms the logic.

## Both Clients Benefit
Same backend `/auth/sync` powers Android and web. With correct roles returned, the web dashboard
redirects each staff user to their portal and the Android app routes to the matching dashboard
(the role routing + Group Admin UI added earlier today).

## Risks / Notes
- `edulife.staff.roles` lists test-domain accounts; rotate/clear before a real production launch
  (override via env, e.g. `EDULIFE_STAFF_ROLES_0_EMAIL=...`).
- Existing stale staff sessions must re-login once to pick up the corrected role.
- Immediate prod unblock (optional, parallel): redeploy `origin/main` (runs V20) or run
  `UPDATE users SET role='ADMIN' WHERE email='admin@edulife.test';` in Neon — but the allowlist
  now makes prod self-heal on the admin account's next login regardless.
