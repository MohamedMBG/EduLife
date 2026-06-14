# Task Audit - Advanced Analytics Post-MVP Planning

## Date
2026-06-14

## Task Summary
Create a full future-planning document for Advanced Analytics as a post-MVP capability. Planning only — no code, no backend changes, no Android source changes. Plan must respect the locked sprint order and keep the learner flow as top priority.

## Files Created
- docs/2026-06-14-advanced-analytics-planning.md
- docs/2026-06-14-advanced-analytics-planning-audit.md

## Files Modified
- None

## What Was Done
Inspected AGENTS.md (full product/architecture spec) and CLAUDE.md before writing. Confirmed advanced analytics is explicitly excluded from MVP (AGENTS.md §4, line 111), confirmed locked sprint order (Sprint 0 → 7 → 2A, §16), and confirmed the four operational roles and their access boundaries (§5) plus security rules (§13).

Wrote a planning document with all 15 requested sections: executive summary; why out of MVP scope; post-MVP phase placement (after Sprint 2A); product goals; per-role analytics needs (student, teacher, group admin, platform admin); feature options by priority (must/should/could/avoid); a full metrics catalog (discovery, enrollment, lesson progress, exam, certificate, teacher/course, group, platform); data model impact (existing entities, future tables, what not to add); backend architecture plan (modular monolith, future `analytics/` module, services/repos/DTOs/controllers, RBAC); Android plan (dashboard screens, role views, four UI states); privacy/security rules; implementation roadmap (Phase A–D); effort estimates; risks/tradeoffs; and a clear "do not build yet" recommendation. Added a compliance-check appendix.

## Architecture Compliance
- No source code, backend, or Android files were modified — planning document only.
- Plan keeps the modular monolith (no microservices, no Kafka/event-driven) per AGENTS.md §3/§20 and CLAUDE.md.
- Future analytics module follows the standard module layout (controller/service/repository/dto) with thin controllers and service-layer logic.
- Android plan follows feature-first MVVM and the Fragment → ViewModel → Repository → ApiService flow.
- Sprint order preserved: analytics placed strictly after Sprint 2A; blocks no MVP sprint.
- Excludes AI recommendations, payments, revenue, and social features as required.

## Code Comments Added
None — no code was written or modified.

## Validation / Testing
- Verified analytics is listed under "Excluded from MVP" in AGENTS.md.
- Verified sprint order and role definitions against AGENTS.md.
- Document is Markdown only; no build/compile impact. No tests applicable.

## Risks / Notes
- Document is forward-looking; not a commitment to build. Phase D (predictive/AI) is explicitly gated behind separate future approval.
- If/when analytics is scheduled, the highest risk is RBAC/ownership scope leakage — flagged in the plan as requiring dedicated tests.
- No follow-up code work is implied or authorized by this task.
