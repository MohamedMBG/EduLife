# EduLife Teacher, Admin, and Group Workflows

## Workflow: Teacher Request Submission and Status

Role:
Learner requesting teacher access

Platform:
Android, Backend, Database

Status:
Partially working

Entry point:

- Android: `ProfileFragment`, `TeacherRequestFragment`
- Backend: `POST /api/v1/teacher-requests`, `GET /api/v1/teacher-requests/me`

End result:

- A learner can request teacher promotion and later see the latest request status.

Step-by-step:

1. Learner submits motivation text.
2. Backend rejects duplicate pending requests.
3. Backend records the request as `PENDING`.
4. Android surfaces pending, approved, or rejected states.

Backend code:

- file path: `backend/src/main/java/com/edulife/teacherrequests/controller/TeacherRequestController.java`
- class/method: submit and `me`
- file path: `backend/src/main/java/com/edulife/teacherrequests/service/TeacherRequestService.java`

Android code:

- file path: `app/src/main/java/com/baghdad/edulife/features/profile/ui/TeacherRequestFragment.java`
- file path: `app/src/main/java/com/baghdad/edulife/features/profile/viewmodel/TeacherRequestViewModel.java`

Web code:

- not implemented

Database:

- tables: `teacher_requests`
- migration files: `V15__teacher_requests.sql`

API contract:

- `POST /api/v1/teacher-requests`
  - request DTO: `SubmitTeacherRequestRequest`
  - response DTO: `TeacherRequestResponse`
- `GET /api/v1/teacher-requests/me`
  - response DTO: `TeacherRequestResponse`

Security:

- authentication: required
- authorization: learner path
- ownership checks: status endpoint returns only caller's latest request

Problems found:

- no web teacher-request page

Recommended next fix:

- add a web learner-facing teacher-request flow for parity with Android

## Workflow: Admin Teacher Request Moderation

Role:
Platform admin

Platform:
Android, Web, Backend, Database

Status:
Fully working

Entry point:

- Android: `TeacherRequestsFragment`
- Web: `/admin/teacher-requests`, `/admin/dashboard`
- Backend: `/api/v1/admin/teacher-requests`

End result:

- Admin approves or rejects teacher applications, and approval promotes the user role transactionally.

Step-by-step:

1. Admin lists requests, optionally filtered by status.
2. Admin approves or rejects a specific request.
3. On approval, backend updates request review fields and user role to `TEACHER`.
4. Client refreshes moderation lists and admin metrics.

Backend code:

- file path: `backend/src/main/java/com/edulife/teacherrequests/controller/AdminTeacherRequestController.java`
- file path: `backend/src/main/java/com/edulife/teacherrequests/service/TeacherRequestService.java`

Android code:

- file path: `app/src/main/java/com/baghdad/edulife/features/admin/ui/TeacherRequestsFragment.java`

Web code:

- file path: `guided-journey-lab/src/routes/admin.teacher-requests.tsx`
- file path: `guided-journey-lab/src/routes/admin.dashboard.tsx`

Database:

- tables: `teacher_requests`, `users`
- migration files: `V15__teacher_requests.sql`

API contract:

- `GET /api/v1/admin/teacher-requests`
- `PUT /api/v1/admin/teacher-requests/{id}/approve`
- `PUT /api/v1/admin/teacher-requests/{id}/reject`

Security:

- authentication: required
- authorization: `ADMIN`
- ownership checks: admin-only route; user promotion is server-side only

Problems found:

- no major workflow break found

Recommended next fix:

- add more explicit audit/history UI only if moderation complexity grows

## Workflow: Teacher CMS Course, Section, and Lesson Management

Role:
Teacher, Group Admin, Admin

Platform:
Android, Web, Backend, Database

Status:
Partially working

Entry point:

- Android: `TeacherDashboardFragment`, `CmsCourseDetailFragment`
- Web: `/teach`, `/teach/$courseId`
- Backend: `/api/v1/cms/courses`, `/api/v1/cms/courses/{courseId}/sections`, `/api/v1/cms/sections/{sectionId}/lessons`

End result:

- Staff users can create course drafts, add sections, and add/delete lessons.

Step-by-step:

1. Teacher creates a draft course.
2. Teacher adds ordered sections.
3. Teacher adds ordered lessons with article body or URL-backed content.
4. Group admin or admin later publishes the course.

Backend code:

- file path: `backend/src/main/java/com/edulife/admin/controller/CmsCourseController.java`
- file path: `backend/src/main/java/com/edulife/admin/controller/CmsSectionController.java`
- file path: `backend/src/main/java/com/edulife/admin/controller/CmsLessonController.java`

Android code:

- file path: `app/src/main/java/com/baghdad/edulife/features/teacher/ui/TeacherDashboardFragment.java`
- file path: `app/src/main/java/com/baghdad/edulife/features/teacher/ui/CmsCourseDetailFragment.java`
- file path: `app/src/main/java/com/baghdad/edulife/features/teacher/viewmodel/CmsCourseDetailViewModel.java`

Web code:

- file path: `guided-journey-lab/src/routes/teach.index.tsx`
- file path: `guided-journey-lab/src/routes/teach.$courseId.tsx`

Database:

- tables: `courses`, `course_sections`, `lessons`
- migration files: `V2__courses.sql`, `V5__course_image_url.sql`, `V6__lesson_content.sql`

API contract:

- course DTOs:
  - `CreateCourseRequest`, `UpdateCourseRequest`, `CourseAdminDto`
- section DTOs:
  - `CreateSectionRequest`, `UpdateSectionRequest`, `SectionAdminDto`
- lesson DTOs:
  - `CreateLessonRequest`, `UpdateLessonRequest`, `LessonAdminDto`

Security:

- authentication: required
- authorization: `TEACHER`, `GROUP_ADMIN`, `ADMIN`
- ownership checks:
  - teacher is limited to owned courses
  - group admin scope is limited to teachers in owned groups
  - admin can operate across platform scope

Problems found:

- Android and web support create/list/delete well, but update flows are incomplete compared with backend support.
- No client exposes course archive.

Missing pieces:

- fuller edit metadata flows
- explicit teacher student-monitoring beyond analytics

Recommended next fix:

- finish update/edit parity before adding more CMS scope

## Workflow: Teacher Exam Authoring

Role:
Teacher, Group Admin, Admin

Platform:
Backend only

Status:
Backend only

Entry point:

- Backend: `GET/POST /api/v1/cms/courses/{courseId}/exam`

End result:

- Backend can read or create/update a course exam with validated MCQ structure, but no Android or web surface currently uses it.

Step-by-step:

1. Staff calls CMS exam endpoint.
2. Backend validates `passScore`, `timeLimitMinutes`, question order, and exactly one correct choice per question.
3. Backend replaces the course exam definition.

Backend code:

- file path: `backend/src/main/java/com/edulife/admin/controller/CmsExamController.java`
- file path: `backend/src/main/java/com/edulife/admin/service/CmsExamService.java`

Database:

- tables: `exams`, `exam_questions`, `exam_choices`
- migration files: `V9__exams.sql`

API contract:

- request DTO: `CreateExamRequest`
- response DTO: `ExamAdminDto`

Security:

- authentication: required
- authorization: staff roles only
- ownership checks: same course ownership rules as other CMS endpoints

Problems found:

- no Android UI
- no web UI
- no visible author workflow despite fully implemented backend endpoint

Recommended next fix:

- add exam authoring to teacher CMS before expanding certificate or analytics requirements

## Workflow: Admin Dashboard and Platform Metrics

Role:
Platform admin

Platform:
Android, Web, Backend, Database

Status:
Fully working

Entry point:

- Android: `AdminDashboardFragment`, `PlatformAnalyticsFragment`
- Web: `/admin/dashboard`, `/admin/analytics`
- Backend: `/api/v1/admin/metrics`, `/api/v1/analytics/platform`, `/api/v1/analytics/platform/cohorts`

End result:

- Admin sees user/course/enrollment/certificate counts plus platform learning funnel analytics.

Step-by-step:

1. Client loads `admin/metrics` for operational counts.
2. Client loads platform analytics summary and cohorts.
3. Admin dashboard displays review queues and course publishing health.

Backend code:

- file path: `backend/src/main/java/com/edulife/admin/controller/AdminMetricsController.java`
- file path: `backend/src/main/java/com/edulife/analytics/controller/AnalyticsController.java`
- file path: `backend/src/main/java/com/edulife/analytics/controller/CohortAnalyticsController.java`

Android code:

- file path: `app/src/main/java/com/baghdad/edulife/features/admin/ui/AdminDashboardFragment.java`
- file path: `app/src/main/java/com/baghdad/edulife/features/analytics/ui/PlatformAnalyticsFragment.java`

Web code:

- file path: `guided-journey-lab/src/routes/admin.dashboard.tsx`
- file path: `guided-journey-lab/src/routes/admin.analytics.tsx`

Database:

- tables: derived reads across `users`, `courses`, `enrollments`, `exam_attempts`, `certificates`, `teacher_requests`

Security:

- authentication: required
- authorization: `ADMIN`
- ownership checks: platform-wide admin scope

Problems found:

- no major backend/client disconnect here

Recommended next fix:

- keep admin metrics thin and derived; avoid adding a second reporting data model too early

## Workflow: Admin User Management

Role:
Platform admin

Platform:
Backend, Android placeholder

Status:
Backend only

Entry point:

- Backend: `/api/v1/admin/users`
- Android: user-management CTA placeholder toast

End result:

- Backend can list users and change roles, but there is no completed client experience.

Backend code:

- file path: `backend/src/main/java/com/edulife/admin/controller/AdminUserController.java`
- file path: `backend/src/main/java/com/edulife/admin/service/AdminUserService.java`

Android code:

- file path: `app/src/main/java/com/baghdad/edulife/features/admin/ui/AdminDashboardFragment.java`
- note: CTA exists but is not implemented

Web code:

- not implemented

Database:

- tables: `users`
- migration files: `V1__init.sql`, `V16__add_role_constraint.sql`

Problems found:

- clear backend/client parity gap

Recommended next fix:

- implement web or Android admin user management against the existing backend endpoints

## Workflow: Group Management and Course Approvals

Role:
Group Admin, Admin, Teacher for some join paths

Platform:
Android, Web, Backend, Database

Status:
Partially working

Entry point:

- Android: `GroupAdminDashboardFragment`, `GroupDetailFragment`, `CourseApprovalsFragment`
- Web: `/groups`, `/groups/$groupId`, `/approvals`
- Backend: `/api/v1/groups/*`, `/api/v1/cms/courses/{id}/publish`

End result:

- Group managers can create groups, add/remove members, attach courses, and approve draft courses from teachers in their scope.

Step-by-step:

1. Group admin creates or lists owned groups.
2. Group detail shows members and attached courses.
3. Group admin adds members by email and attaches published courses.
4. Group approval screen loads scoped CMS drafts and can publish them.

Backend code:

- file path: `backend/src/main/java/com/edulife/groups/controller/GroupController.java`
- file path: `backend/src/main/java/com/edulife/groups/service/GroupService.java`
- file path: `backend/src/main/java/com/edulife/admin/service/CmsCourseService.java`

Android code:

- file path: `app/src/main/java/com/baghdad/edulife/features/groupadmin/ui/GroupAdminDashboardFragment.java`
- file path: `app/src/main/java/com/baghdad/edulife/features/groupadmin/ui/GroupDetailFragment.java`
- file path: `app/src/main/java/com/baghdad/edulife/features/groupadmin/ui/CourseApprovalsFragment.java`

Web code:

- file path: `guided-journey-lab/src/routes/groups.index.tsx`
- file path: `guided-journey-lab/src/routes/groups.$groupId.tsx`
- file path: `guided-journey-lab/src/routes/approvals.tsx`

Database:

- tables: `groups`, `group_members`, `group_courses`
- migration files: `V11__groups.sql`

API contract:

- `CreateGroupRequest`
- `AddMemberRequest`
- `AttachCourseRequest`
- `GroupSummaryDto`, `GroupDetailDto`, `GroupMemberDetailDto`, `GroupCourseDetailDto`

Security:

- authentication: required
- authorization:
  - `GROUP_ADMIN/ADMIN` for create/manage/approve
  - `TEACHER` allowed into the broader group controller for join-request paths
- ownership checks:
  - creator/admin ownership enforced on group reads and writes
  - publish scope restricted to managed teachers

Problems found:

- no detach-course workflow exists in backend or clients
- Android and web do not expose teacher join-request flows even though backend supports them

Recommended next fix:

- add join-request and detach-course UX before expanding group analytics further

## Workflow: Group Join Requests

Role:
Teacher requester, Group Admin reviewer

Platform:
Backend only

Status:
Backend only

Entry point:

- `GET /api/v1/groups/join-requests/mine`
- `POST /api/v1/groups/{groupId}/join-requests`
- `GET /api/v1/groups/{groupId}/join-requests`
- `PUT /api/v1/groups/{groupId}/join-requests/{requestId}/approve|reject`

End result:

- Teachers can request access to groups, and group owners can approve or reject those requests.

Database:

- tables: `group_join_requests`
- migration files: `V21__group_join_requests.sql`

Problems found:

- schema, service, and controller support exist, but Android and web do not expose the workflow

Recommended next fix:

- implement the join-request UI before introducing more teacher/group complexity

