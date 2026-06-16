# EduLife Web Workflows

## Workflow: Web Authentication and Session Lifecycle

Role:
All roles

Platform:
Web, Firebase, Backend

Status:
Mostly working

Entry point:

- `/login`
- `/register`
- `/forgot-password`
- `guided-journey-lab/src/lib/auth/auth-context.tsx`

End result:

- Web users authenticate with Firebase, sync to backend, persist session for the browser session, and route by EduLife role.

Step-by-step:

1. User signs in or registers through Firebase.
2. Intended EduLife role is stored in `localStorage` for first sync.
3. `auth-context.tsx` observes auth state changes.
4. Backend sync resolves internal `userId` and `role`.
5. Route guards redirect to learner, teacher, group, or admin surfaces.
6. `/forgot-password` calls Firebase directly and masks account-enumeration errors.

Web code:

- `guided-journey-lab/src/lib/auth/auth-context.tsx`
- `guided-journey-lab/src/routes/login.tsx`
- `guided-journey-lab/src/routes/register.tsx`
- `guided-journey-lab/src/routes/forgot-password.tsx`

Problems found:

- sync runs on auth state changes, but the client model is thinner than Android’s retry/authenticator model
- there is no web delete-account flow

Recommended next fix:

- add web delete-account to complete the account lifecycle

## Workflow: Web Learner Study Experience

Role:
Learner

Platform:
Web, Backend

Status:
Strong but not fully consistent with Android/backend rules

Entry point:

- `/dashboard`
- `/explore`
- `/courses`
- `/courses/$courseId`
- `/learn/$courseId/$lessonId`
- `/courses/$courseId/exam`
- `/certificates`

End result:

- Learner can discover courses, enroll, study lessons, take exams, and manage certificates on the web.

Step-by-step:

1. Dashboard loads profile, enrollments, progress, and course suggestions.
2. Explore lists the live catalog and supports enroll CTA.
3. Course detail loads sections and lessons.
4. Lesson route loads content and writes completion.
5. Exam route loads status first, then questions, then submits to backend.
6. Certificates routes show earned credentials and public verification pages.

Web code:

- `guided-journey-lab/src/routes/dashboard.tsx`
- `guided-journey-lab/src/routes/explore.tsx`
- `guided-journey-lab/src/routes/courses.$courseId.tsx`
- `guided-journey-lab/src/routes/learn.$courseId.$lessonId.tsx`
- `guided-journey-lab/src/routes/courses.$courseId.exam.tsx`
- `guided-journey-lab/src/routes/certificates.index.tsx`
- `guided-journey-lab/src/routes/certificates.$certificateId.tsx`
- `guided-journey-lab/src/routes/certificates.verify.$hash.tsx`

Problems found:

- course detail hides exam CTA until progress is 100%, but backend only requires enrollment today
- `/courses/$courseId/resources` is a client-side workaround, not a true backend resources API

Recommended next fix:

- align exam readiness rules with backend and Android

## Workflow: Web Staff Portals

Role:
Teacher, Group Admin, Admin

Platform:
Web, Backend

Status:
Partial but substantial

Entry point:

- `/teach`
- `/teach/$courseId`
- `/groups`
- `/groups/$groupId`
- `/approvals`
- `/admin/dashboard`
- `/admin/teacher-requests`
- `/admin/analytics`

End result:

- Staff users can manage courses, groups, approvals, and admin reporting from the web app.

Step-by-step:

1. Role guards enforce teacher/group/admin access.
2. Teacher studio lists owned courses and supports section/lesson CRUD.
3. Group portal lists owned groups, member management, and course assignment.
4. Approvals page publishes drafts scoped to managed teachers.
5. Admin routes expose metrics, analytics, and teacher moderation.

Web code:

- `guided-journey-lab/src/routes/teach.index.tsx`
- `guided-journey-lab/src/routes/teach.$courseId.tsx`
- `guided-journey-lab/src/routes/groups.index.tsx`
- `guided-journey-lab/src/routes/groups.$groupId.tsx`
- `guided-journey-lab/src/routes/approvals.tsx`
- `guided-journey-lab/src/routes/admin.dashboard.tsx`
- `guided-journey-lab/src/routes/admin.teacher-requests.tsx`
- `guided-journey-lab/src/routes/admin.analytics.tsx`

Problems found:

- no web admin user-management route
- no web teacher-request submission route
- no web CMS exam authoring route
- no group join-request route even though backend supports it

Recommended next fix:

- prioritize CMS exam authoring and admin user management because the backend is already present

## Workflow: Web Local-Only and Derived Features

Role:
Learner

Platform:
Web only

Status:
Local only / derived

Entry point:

- `/planner`
- `/level`
- fallback path in `/advisor`

End result:

- Learner gets polished UX for planning and progress, but these pages are not fully backend-owned.

Step-by-step:

1. Planner stores goals, tasks, hours, and selected study days in `localStorage`.
2. Level page derives XP and badges from profile, enrollments, progress, and certificates instead of using backend gamification endpoints.
3. Advisor can fall back to a local matcher when AI/backed recommendations are unavailable.

Web code:

- `guided-journey-lab/src/routes/planner.tsx`
- `guided-journey-lab/src/routes/level.tsx`
- `guided-journey-lab/src/routes/advisor.tsx`

Problems found:

- planner state is not shareable between devices
- web level can drift from backend/Android gamification state
- advisor fallback can hide infrastructure failures

Recommended next fix:

- decide which of these experiences are intentionally local and which should become backend products

## Web-Specific Gap Summary

| Gap | Current state |
| --- | --- |
| Delete account | Backend + Android exist, web missing |
| Teacher request submission | Backend + Android exist, web missing |
| Admin user management | Backend exists, web missing |
| CMS exam authoring | Backend exists, web missing |
| Group join requests | Backend exists, web missing |
| Gamification source of truth | Web derives locally instead of calling backend gamification endpoints |

