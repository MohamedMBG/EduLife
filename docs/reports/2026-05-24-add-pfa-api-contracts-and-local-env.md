# Task Audit - Add PFA API Contracts And Local Env

## Date
2026-05-24

## Task Summary
Added two missing report chapters to the PFA LaTeX document: one describing the backend API contracts with JSON examples and error/status rules, and one describing the local environment and deployment/testing prerequisites (Firebase Admin credentials, Android-to-backend URL setup, and cleartext/network pitfalls).

## Files Created
- docs/2026-05-24-add-pfa-api-contracts-and-local-env.md

## Files Modified
- rapport PFA/untitled-1.tex

## What Was Done
Inserted two new chapters before the existing "Technologies utilisées" chapter:

1. `Contrats API (exemples JSON)`
- Documented shared conventions: `/api/v1` prefix and `Authorization: Bearer <firebase-id-token>`.
- Documented the shared error response contract `{status, message, timestamp}` and included a concrete JSON example.
- Added endpoint examples with sample JSON payloads:
  - `POST /api/v1/auth/sync`
  - `GET /api/v1/courses`
  - `GET /api/v1/courses/{courseId}`
- Added the documented status rules for `401`, `403`, `404`, and `400`, and explicitly stated that these cases must emit the global JSON error contract.

2. `Environnement et déploiement local`
- Documented backend Firebase Admin credential setup using environment variables:
  - `FIREBASE_ADMIN_CREDENTIALS_PATH`
  - `FIREBASE_ADMIN_CREDENTIALS_JSON`
- Documented local backend prerequisites (PostgreSQL + Flyway + reachable port).
- Documented Android → backend base URL considerations for emulator vs physical device.
- Documented Android cleartext HTTP restrictions and the existence of debug-only allowances for local development hosts.
- Listed common local pitfalls (firewall, network reachability, token/log secrecy, secret management).

## Architecture Compliance
This is documentation-only work and does not change runtime behavior. The chapters align with the EduLife architecture and security rules by:
- keeping protected endpoints behind Firebase Bearer token validation;
- enforcing the global API error contract as the consistent error response shape;
- documenting secret handling rules (no credential JSON in repo) and debug-only cleartext allowances.

## Code Comments Added
No production code was modified, so no code comments were added.

## Validation / Testing
Validated by deriving the inserted content from the existing documentation sources:
- `docs/backend-architecture.md` (course discovery contract, protected endpoints, status rules, error contract)
- `docs/2026-04-30-global-api-error-contract.md` (global `{status,message,timestamp}` contract context)
- `docs/2026-04-26-fix-firebase-admin-bootstrap.md` (Firebase Admin credentials via environment variables)
- `docs/2026-05-19-debug-cleartext-local-api.md` (debug-only cleartext config motivation)
- `docs/2026-05-19-fix-physical-device-login-api-url.md` (device reachability and API base URL considerations)

No PDF regeneration was run in this environment.

## Risks / Notes
The LaTeX report now includes accurate contract and environment notes, but the generated PDF will remain outdated until `untitled-1.tex` is recompiled.

The JSON examples are intentionally representative/simplified; the authoritative reference remains the backend contract documentation in `docs/backend-architecture.md`.
