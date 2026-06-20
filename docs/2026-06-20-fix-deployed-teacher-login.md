# Task Audit - Fix Deployed Teacher Login

## Date
2026-06-20

## Task Summary
Diagnosed the deployed website login failure for the supplied teacher account and hardened backend CORS configuration so a stale Render environment override cannot remove the first-party Vercel origin.

## Files Created
- docs/2026-06-20-fix-deployed-teacher-login.md

## Files Modified
- backend/src/main/java/com/edulife/security/CorsProperties.java
- backend/src/main/java/com/edulife/security/SecurityConfig.java
- backend/src/main/resources/application.yaml
- backend/src/test/java/com/edulife/security/SecurityHardeningTest.java

## What Was Done
Verified the account directly against the Firebase project used by the deployed website. Firebase accepted the supplied credentials and reported that the email is verified. The subsequent production request to `POST /api/v1/auth/sync` returned HTTP 403 without an `Access-Control-Allow-Origin` header for `https://guided-journey-lab.vercel.app`, proving that backend CORS—not the password—blocked completion of login.

The repository already listed the Vercel origin in the default CORS value, but Render can replace that entire value through `APP_CORS_ALLOWED_ORIGINS`. Updated `CorsProperties` to merge the known first-party Vercel origin into the effective allowlist after external configuration is bound, so it remains allowed even when Render has an older override. `SecurityConfig` now consumes that effective allowlist.

Added a regression test that supplies an explicit environment-style allowlist override and verifies that the deployed Vercel origin can still preflight `/api/v1/auth/sync`.

## Architecture Compliance
The change remains in centralized backend security configuration. Firebase continues to authenticate credentials, `/api/v1/auth/sync` continues to resolve the internal UUID and server-owned role, and no authentication or role logic was moved into the website UI.

## Code Comments Added
Added a CORS property comment explaining why the first-party origin is merged after environment binding. Updated the application configuration comment and added a test comment explaining the Render override regression being protected.

## Validation / Testing
- Live Firebase credential verification: successful; email verified.
- Live production `/api/v1/auth/sync`: reproduced HTTP 403 with no CORS allow-origin header before deployment of this fix.
- Backend regression test: `SecurityHardeningTest` verifies the Vercel origin survives an explicit CORS override.
- `backend\\mvnw.cmd '-Dtest=SecurityHardeningTest,SecurityDefaultCorsTest' test`: passed (8 tests, 0 failures).

## Risks / Notes
- The source fix must be deployed to Render before the live website behavior changes.
- The supplied email is not present in the backend's static staff allowlist. Its effective role remains whatever is stored in the backend database; this CORS fix restores login but does not grant or change roles.
