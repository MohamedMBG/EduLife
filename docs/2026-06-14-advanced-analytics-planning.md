# Advanced Analytics — Post-MVP Planning Document

## Date
2026-06-14

## Status
**Planning only. Not scheduled. Out of MVP scope.** No code, backend, or Android changes are part of this document. This is a forward-looking capability plan to be revisited *after* the core learner flow is proven end to end (Sprint 0 → Sprint 7, then Sprint 2A).

---

## 1. Executive Summary

Advanced analytics is a high-value but non-foundational capability. It answers "how is learning going?" for four roles (student, teacher, group admin, platform admin) by turning the data the MVP already produces — enrollments, lesson progress, exam attempts, certificates — into dashboards and metrics.

It is explicitly **excluded from the MVP** (`AGENTS.md` §4, line 111) and must not block the learner loop:

```text
Discover course -> Enroll -> Learn -> Take exam -> Pass -> Receive certificate
```

The recommendation: build analytics **incrementally, as a single new modular-monolith module**, only after Sprint 7 hardening is done. Start with cheap read-only operational counts derived from existing tables. Do **not** add Kafka, event-streaming, microservices, a data warehouse, or AI/predictive analytics for the first iterations. Keep it realistic for a solo developer.

The single most important constraint: **analytics must never weaken or destabilize the learner flow, and must never leak one user's data to another across role and group boundaries.**

---

## 2. Why Advanced Analytics Is Out of MVP Scope

- **Explicitly excluded.** `AGENTS.md` §4 lists "Advanced analytics" under "Excluded from MVP" (line 111).
- **Not part of the core loop.** Nothing in `Discover -> Enroll -> Learn -> Exam -> Pass -> Certificate` requires analytics. It is observational, not operational.
- **Depends on real data.** Meaningful analytics needs real enrollments, progress, and exam history. Until the learner loop runs end to end with real data, analytics has nothing trustworthy to measure.
- **Cost vs. value timing.** Dashboards consume significant backend + Android + testing effort. Spending it before the learner loop is proven risks a polished dashboard over a fragile core — exactly the "fragile demo" `CLAUDE.md` warns against.
- **Risk of scope creep.** Analytics naturally pulls toward event pipelines, warehouses, and AI. The MVP forbids microservices, event-driven architecture, and AI recommendations. Planning now keeps those temptations boxed.

---

## 3. Recommended Post-MVP Phase Placement

Respect the locked sprint order:

```text
Sprint 0 -> Sprint 1 -> Sprint 2 -> Sprint 3 -> Sprint 4 -> Sprint 5 -> Sprint 6 -> Sprint 7 -> Sprint 2A
```

Analytics is placed **after Sprint 2A**, as a new post-MVP track. It does not interleave with any MVP sprint.

```text
[ MVP: Sprint 0 ... Sprint 7 ] -> [ Sprint 2A: Basic CMS ] -> [ POST-MVP: Analytics Phase A -> B -> C -> (D much later) ]
```

Preconditions before any analytics work begins:

1. Learner loop verified end to end with real (non-seed) data.
2. Sprint 7 hardening complete: error/empty states, security checklist, delete-account flow.
3. RBAC boundaries (student / teacher / group admin / platform admin) stable and tested.
4. Real usage exists — there is something worth measuring.

If none of the above is true, **do not start analytics.**

---

## 4. Product Goals for Analytics

- Give each role an honest, scoped view of learning activity and outcomes.
- Surface where learners drop off (discovery → enrollment → completion → exam pass).
- Help teachers see how their own courses perform.
- Help group admins see their own group's health.
- Give platform admins a global operational picture for moderation and growth decisions.
- Do all of the above **without** new privacy risk, without destabilizing the learner flow, and without overengineering.

Non-goals (now): predictive scoring, AI insights, recommendations, cross-platform behavioral tracking, real-time streaming dashboards.

---

## 5. User Roles and Analytics Needs

Role definitions follow `AGENTS.md` §5. Analytics must enforce the same boundaries.

### Student
- Own progress across enrolled courses.
- Own exam attempts, scores, pass/fail, cooldown status.
- Own certificates earned.
- Personal streak / activity history (already a client-side gamification concept — analytics can later mirror, not replace, it).
- **Must never** see other students' data or platform metrics.

### Teacher
- Performance of **their own** courses only: enrollment counts, completion rate, average exam score, pass rate, certificates issued.
- Lesson-level drop-off within their courses.
- **Must never** see other teachers' courses or student PII beyond aggregate counts within their own courses.

### Group Admin
- Aggregate health of **their own group only**: teachers in group, courses by those teachers, enrollments inside the group, group completion/pass summaries.
- **Must never** see other groups or global platform metrics.

### Platform Admin
- Global operational metrics: total users by role, active courses, enrollment volume, exam pass rates, certificate issuance, teacher verification backlog.
- Cross-group and cross-teacher views (the only role allowed to).
- Used for moderation and operational decisions, not marketing/behavioral profiling.

---

## 6. Analytics Feature Options by Priority

### Must-have (first post-MVP analytics iteration)
- Student: own progress summary + own exam/certificate history (mostly already available; aggregate cleanly).
- Teacher: per-course enrollment count, completion rate, pass rate, certificates issued.
- Platform admin: global counts (users by role, courses, enrollments, exams passed, certificates).
- All computed **on read**, from existing tables. No new pipelines.

### Should-have (later)
- Lesson-level drop-off (where students stop) per course.
- Group admin group summaries.
- Time-windowed trends (last 7/30 days) using existing timestamps.
- Exam attempt distribution and cooldown-impact view.

### Could-have (long term)
- Cohort analytics (group learners by enrollment month, compare completion).
- Pre-aggregated/materialized summary tables for performance at scale.
- Exportable reports (CSV/PDF) for admins.
- Funnel visualization across the full learner loop.

### Explicitly avoid for now
- Predictive / AI-assisted analytics (dropout prediction, recommendations).
- Real-time streaming dashboards.
- Event-sourcing / Kafka / message bus.
- A separate analytics microservice or data warehouse.
- Cross-user behavioral tracking, heatmaps, session replay.
- Third-party analytics SDKs that ship student PII off-platform.
- Payment/revenue analytics (no payments in product).

---

## 7. Metrics Catalog

Each metric below is derivable, at least in basic form, from MVP entities. "Source" names the existing entities.

### Course discovery metrics
- Course catalog views *(requires a lightweight view counter; see §8 — defer until Should-have)*.
- Courses listed / active / pending approval. Source: `Course`.
- Discovery → enrollment conversion (needs view counter to be meaningful).

### Enrollment metrics
- Total enrollments, enrollments per course, per teacher, per group.
- New enrollments over time window. Source: `Enrollment` (+ timestamps).
- Active vs. unenrolled. Source: `Enrollment`.

### Lesson progress metrics
- Lessons completed per learner / per course.
- Average course completion %.
- Lesson-level drop-off (last lesson reached). Source: `Progress`, `Lesson`, `CourseSection`.

### Exam metrics
- Attempts per exam, pass rate, fail rate.
- Average score, score distribution.
- Cooldown incidence (learners hitting 2-fail / 72h rule). Source: `ExamAttempt` (+ `Exam`). **Never** read/expose correct answers.

### Certificate metrics
- Certificates issued total / per course / per teacher / per group.
- Certificate issuance over time. Source: `Certificate`.

### Teacher / course performance metrics
- Per teacher: enrollments, completion rate, pass rate, certificates, number of active courses.
- Per course: same, scoped to one course. Source: join `Course` ↔ `Enrollment` ↔ `Progress` ↔ `ExamAttempt` ↔ `Certificate`.

### Group-level metrics
- Teachers in group, courses by group teachers, enrollments in group, group pass/completion summary. Source: `Group`, `GroupMembership`, plus the teacher/course metrics scoped to the group.

### Platform-level metrics
- Users by role, total active courses, total enrollments, global pass rate, certificates issued, teacher verification backlog. Source: `User`, `Role`, `UserRole`, `Course`, `Enrollment`, `ExamAttempt`, `Certificate`, `TeacherVerification`.

---

## 8. Data Model Impact

### Existing MVP entities that already support analytics (read-only)
- `User`, `Role`, `UserRole` — counts by role.
- `Course`, `CourseSection`, `Lesson` — catalog structure, lesson-level breakdown.
- `Enrollment` — enrollment volume, per-course/teacher/group (with timestamps for trends).
- `Progress` — completion %, lesson drop-off.
- `Exam`, `ExamAttempt` — pass/fail, scores, attempt distribution, cooldown. (`ExamQuestion`/`ExamChoice`/`ExamAnswer` exist but correct answers stay server-only.)
- `Certificate` — issuance metrics.
- `Group`, `GroupMembership` — group scoping.
- `TeacherVerification` — verification backlog.

Most Must-have and several Should-have metrics need **no schema change** — only read queries with aggregation.

### Additional tables that may be needed *later* (not now)
- `course_view_event` (or a simple per-course view counter) — only when discovery-conversion metrics become a real requirement. Prefer a counter column or a tiny append table over an event pipeline.
- `analytics_daily_snapshot` / materialized summary tables — only when on-read aggregation becomes too slow at real scale (Could-have, Phase C).
- Optional `report_export` record if admins need saved/exportable reports.

All later tables are added via **new Flyway migrations only** (`AGENTS.md` Flyway rules / `CLAUDE.md`). Never edit applied migrations.

### What should NOT be added yet
- No event/event-log tables for streaming.
- No data warehouse / OLAP store.
- No denormalized analytics DB separate from the monolith DB.
- No per-click/behavioral tracking tables.
- No AI/feature-store tables.

Default first iteration: **zero new tables.** Compute everything from existing data.

---

## 9. Backend Architecture Plan

### Keep the modular monolith
No microservices, no separate analytics service, no event bus. Analytics is one more domain module inside the existing Spring Boot monolith (`AGENTS.md` §3, §6; `CLAUDE.md`).

### Suggested future analytics module
```text
backend/src/main/java/com/edulife/analytics/
  controller/
  service/
  repository/
  dto/
  exception/
```
No new `entity/` initially — it reads from other modules' data via dedicated read repositories / projection queries. It does not own write models in Phase A.

### Services, repositories, DTOs, controllers
- **Controllers** — thin. Resolve caller identity/role server-side, delegate to service. One controller per audience scope (e.g. teacher analytics, group analytics, admin analytics) or one controller with role-guarded endpoints.
- **Services** — all aggregation and scoping logic. Apply ownership filters here.
- **Repositories** — read-only aggregate queries (counts, averages, group-by). Projections/DTO mappings, not entity exposure.
- **DTOs** — every response is a dedicated analytics DTO. Never expose JPA entities, never expose `firebase_uid`, never expose correct exam answers (`CLAUDE.md` security rules).

### Security / RBAC rules
- Every endpoint validates the Firebase token and `email_verified`, resolves the internal user server-side.
- Role and ownership scoping is enforced **in the service**, derived from the resolved internal user — never from a client-supplied `userId`, `role`, `teacherId`, or `groupId`.
- Teacher endpoints filter to courses owned by the resolved teacher.
- Group-admin endpoints filter to the resolved admin's own group.
- Platform-admin endpoints are the only ones allowed unrestricted aggregate scope, guarded by an explicit admin role check.
- Read-only: analytics endpoints perform no writes (except, later, an isolated view-counter increment if added).

---

## 10. Android App Plan

### Future dashboard screens
Add later under feature-first MVVM (`AGENTS.md` §7), e.g.:
```text
app/.../features/analytics/
  ui/
  viewmodel/
  data/
  model/
```
Flow stays: `Fragment -> ViewModel -> Repository -> ApiService -> Backend` (`CLAUDE.md`). No API calls in Fragments, no business logic in UI.

### Role-specific views
- **Student:** "My Progress" overview (own completion, exam history, certificates). Largely a presentation layer over data already fetched.
- **Teacher:** "My Courses Performance" — enrollment, completion, pass rate, certificates per owned course.
- **Group admin:** "Group Overview" — group-scoped summaries.
- **Platform admin:** "Platform Overview" — global counts. (Heavy admin dashboards may stay web-first per `CLAUDE.md` MVP boundaries; keep Android lean.)

Each view shows only what the backend returns for that role — the app never decides scope; the backend does.

### Empty / error / loading states
Every screen must implement all four states (`CLAUDE.md` Error Handling):
- **Loading** — spinner/skeleton while fetching.
- **Empty** — "No data yet" (common early, when little activity exists).
- **Error** — controlled message + retry, with 401 → token refresh + single retry per existing auth rules.
- **Success** — the dashboard content.

---

## 11. Privacy and Security Rules

Non-negotiable, mirrors `AGENTS.md` §13 and §20:

- **No cross-group student data leakage.** A group admin sees only their own group; analytics queries are hard-scoped to the resolved group.
- **Teachers see only their own course analytics.** Scope by owned courses, server-side.
- **Group admins see only their own group.** No global or other-group access.
- **Platform admins** are the only role with global analytics, behind an explicit admin check.
- Students see only their own data.
- Never expose `firebase_uid`.
- Never expose correct exam answers, even in aggregates.
- Prefer aggregates over raw PII; when listing individuals (e.g. admin user management), expose the minimum needed.
- Scope is always derived from the server-resolved internal user — never trusted from the client.
- No third-party analytics SDK that exfiltrates student PII.
- Any new metric is privacy-reviewed before shipping (see effort, §13).

---

## 12. Implementation Roadmap

### Phase A — Basic operational metrics (read-only)
- New `analytics/` module, read-only aggregate queries over existing tables.
- Must-have metrics: student own-summary, teacher per-course basics, platform global counts.
- Zero new tables. Role/ownership scoping enforced in services.
- Minimal or no UI (could surface via web admin first); Android optional.

### Phase B — Dashboards
- Android (and/or web) dashboards for the roles, full loading/empty/error/success states.
- Should-have metrics: lesson drop-off, group-admin summaries, 7/30-day trends.
- Still read-on-demand; introduce view counter only if discovery conversion is genuinely needed.

### Phase C — Cohort / progress analytics
- Cohort grouping, funnel across the learner loop, deeper trend analysis.
- Introduce materialized/snapshot summary tables (new Flyway migrations) **only if** on-read aggregation is measurably too slow.

### Phase D — Predictive / AI-assisted (much later, maybe never)
- Dropout-risk signals, suggested interventions.
- Explicitly out of this plan's commitment. Requires separate approval, separate design, and a privacy review. Do not start without explicit user request.

---

## 13. Estimated Effort

Relative sizing for a solo developer (S = small, M = medium, L = large).

| Area | Phase A | Phase B | Phase C | Phase D |
|------|---------|---------|---------|---------|
| Backend | M (read queries, scoping, DTOs) | M | L (snapshots, cohorts) | L |
| Android | S (or skip) | M (dashboards + 4 states per role) | M | M |
| Testing | M (RBAC/ownership tests critical) | M | M | L |
| Data / privacy review | S | M | M | L (predictive raises real risk) |
| **Overall** | **Small–Medium** | **Medium** | **Large** | **Large** |

Notes:
- Phase A is intentionally cheap — mostly queries + DTOs + role guards.
- The biggest non-code cost is **RBAC/ownership test coverage** — under-testing scope leaks is the main risk.
- Phase D effort is dominated by data/privacy/ML concerns, not UI.

---

## 14. Risks and Tradeoffs

- **Privacy leakage (highest risk).** A missed ownership filter exposes one teacher's/group's/student's data to another. Mitigation: scope server-side from resolved identity; dedicated RBAC tests per endpoint.
- **Scope creep toward pipelines/AI.** Analytics invites Kafka/warehouse/AI. Mitigation: this plan caps Phase A–B at read-on-demand over existing tables; D is gated behind explicit approval.
- **Performance at scale.** On-read aggregation may slow as data grows. Tradeoff: accept simplicity first; add snapshot tables (Phase C) only when measured, not speculatively.
- **Distraction from the learner loop.** Time on dashboards is time not hardening the core. Mitigation: no analytics work until after Sprint 7.
- **Misleading early metrics.** Tiny data makes rates noisy. Mitigation: prominent empty states; avoid over-interpreting early numbers.
- **Exam integrity.** Aggregating exam data risks accidentally exposing answer keys. Mitigation: never query/return correct-answer columns in analytics.

---

## 15. Clear Recommendation — What NOT to Build Yet

Do **not**, now or during the MVP:

- Do not build any analytics during Sprints 0–7 or 2A.
- Do not add microservices or a separate analytics service.
- Do not add Kafka, event streaming, or event-driven architecture.
- Do not add a data warehouse / OLAP store / separate analytics DB.
- Do not build predictive or AI-assisted analytics (Phase D) without explicit, separate approval.
- Do not add per-click/behavioral/session-replay tracking.
- Do not add third-party analytics SDKs that send student PII off-platform.
- Do not add new analytics tables in the first iteration — compute from existing data.
- Do not build payment/revenue analytics (no payments in product).
- Do not let any analytics work destabilize or delay the learner flow.

**Build first, only after the learner loop is proven:** Phase A read-only operational metrics, scoped strictly by role and group, as one new module in the existing modular monolith.

---

## Appendix — Compliance Check

- Respects MVP scope: analytics confirmed excluded (`AGENTS.md` §4). ✅
- Respects sprint order: placed after Sprint 2A, blocks no MVP sprint. ✅
- No microservices, no Kafka/event-driven, no AI recommendations in plan. ✅
- No payments / revenue / social. ✅
- Modular monolith + feature-first MVVM preserved. ✅
- Privacy/RBAC boundaries enforced per role and group. ✅
- Realistic for a solo developer (phased, cheap first). ✅
- No code, backend, or Android source modified by this document. ✅
