# EduLife Course and Learning Workflows

## Workflow: Course Discovery and Catalog Search

Role:
Learner and any verified authenticated user

Platform:
Android, Web, Backend, Database

Status:
Fully working

Entry point:

- Android: `HomeFragment`, `CoursesFragment`
- Web: `/dashboard`, `/explore`
- Backend: `GET /api/v1/courses`

End result:

- User sees published courses, can search by `q`, and can filter by category/level-like metadata on the client.

Step-by-step:

1. Client loads `/api/v1/courses`.
2. Backend serves only `PUBLISHED` courses.
3. If `q` is present, backend uses course FTS search support.
4. Android and web render course cards and detail entry points.

Backend code:

- file path: `backend/src/main/java/com/edulife/courses/controller/CourseController.java`
- class/method: `CourseController#listCourses`
- important snippet:

```java
return courseService.listPublishedCourses(category, q, pageable);
```

Android code:

- file path: `app/src/main/java/com/baghdad/edulife/features/courses/viewmodel/CourseCatalogViewModel.java`
- class/method: load/search flow
- file path: `app/src/main/java/com/baghdad/edulife/features/courses/ui/HomeFragment.java`

Web code:

- file path: `guided-journey-lab/src/routes/explore.tsx`
- component/hook/function: catalog loader and enroll CTA

Database:

- tables: `courses`
- migration files: `V2__courses.sql`, `V3__seed_courses.sql`, `V5__course_image_url.sql`, `V13__course_fts.sql`

API contract:

- endpoint: `GET /api/v1/courses`
- request params:
  - `category` optional
  - `q` optional
  - Spring pagination params
- response DTO:
  - paged `CourseSummaryDto`

Security:

- authentication: required
- authorization: all verified users
- ownership checks: not applicable, but unpublished courses are hidden

Problems found:

- Android repository still contains fallback demo courses, although the main flow no longer depends on them.

Missing pieces:

- no public unauthenticated catalog

Recommended next fix:

- remove or isolate fallback catalog data so production behavior always reflects backend truth

## Workflow: Course Detail and Resource Viewing

Role:
Learner and other verified users

Platform:
Android, Web, Backend, Database

Status:
Partially working

Entry point:

- Android: `CourseDetailFragment`
- Web: `/courses/$courseId`, `/courses/$courseId/resources`
- Backend: `GET /api/v1/courses/{courseId}`

End result:

- User sees course metadata, sections, lessons, and can jump to lessons or resources.

Step-by-step:

1. Client requests `GET /api/v1/courses/{courseId}`.
2. Backend returns course metadata with nested sections and lesson summaries ordered by `displayOrder`.
3. Android renders the outline and an enroll/exam CTA based on enrollment state.
4. Web renders the outline and builds a separate resources page by regrouping lesson summaries by `lessonType`.

Backend code:

- file path: `backend/src/main/java/com/edulife/courses/service/CourseService.java`
- class/method: `getPublishedCourseDetail`

Android code:

- file path: `app/src/main/java/com/baghdad/edulife/features/courses/ui/CourseDetailFragment.java`
- class/method: outline rendering and exam CTA logic

Web code:

- file path: `guided-journey-lab/src/routes/courses.$courseId.tsx`
- file path: `guided-journey-lab/src/routes/courses.$courseId.resources.tsx`

Database:

- tables: `courses`, `course_sections`, `lessons`
- migration files: `V2__courses.sql`, `V6__lesson_content.sql`

API contract:

- endpoint: `GET /api/v1/courses/{courseId}`
- response DTO:
  - `CourseDetailDto`
  - nested `CourseSectionDto`
  - nested `LessonSummaryDto`

Security:

- authentication: required
- authorization: published courses only
- ownership checks: not required for discovery detail

Problems found:

- Web resources page is a client-side regrouping because there is no dedicated resources endpoint.
- Exam CTA rules are inconsistent across clients.

Missing pieces:

- no explicit teacher public profile or issuer block on course detail

Recommended next fix:

- add a shared exam-eligibility rule and expose it consistently in course detail responses

## Workflow: Enrollment and My Courses

Role:
Learner path

Platform:
Android, Web, Backend, Database, Tests

Status:
Fully working

Entry point:

- Android: `EnrollCourseFragment`, `CoursesFragment`, `HomeFragment`
- Web: `/explore`, `/courses`
- Backend: `/api/v1/enrollments`

End result:

- Learner enrolls into a published course, gets progress initialized, and later can list or cancel the enrollment.

Step-by-step:

1. Client posts `courseId` to `POST /api/v1/enrollments`.
2. Backend verifies the course is published and reuses/reactivates an existing enrollment if needed.
3. Backend creates or refreshes `course_progress`.
4. Backend emits gamification enrollment XP.
5. Client refreshes learner dashboard and my-courses queries.
6. Unenroll uses `DELETE /api/v1/enrollments/{id}` with owner checks.

Backend code:

- file path: `backend/src/main/java/com/edulife/enrollments/controller/EnrollmentController.java`
- class/method: enrollment endpoints
- file path: `backend/src/main/java/com/edulife/enrollments/service/EnrollmentService.java`
- class/method: `enroll`, `unenroll`, `listMyEnrollments`
- important snippet:

```java
CourseProgress progress = courseProgressRepository
        .findByUserIdAndCourseId(userId, courseId)
        .orElseGet(() -> new CourseProgress(userId, courseId, 0, totalLessons));
```

Android code:

- file path: `app/src/main/java/com/baghdad/edulife/features/courses/viewmodel/EnrollmentViewModel.java`
- file path: `app/src/main/java/com/baghdad/edulife/features/courses/ui/EnrollCourseFragment.java`

Web code:

- file path: `guided-journey-lab/src/routes/explore.tsx`
- file path: `guided-journey-lab/src/routes/courses.index.tsx`

Database:

- tables: `enrollments`, `course_progress`
- migration files: `V4__enrollments.sql`, `V7__progress.sql`

API contract:

- `POST /api/v1/enrollments`
  - request DTO: `EnrollRequest`
  - response DTO: `EnrollmentResponse`
- `DELETE /api/v1/enrollments/{id}`
  - response: empty `204`
- `GET /api/v1/enrollments/me`
  - response DTO: list of `EnrolledCourseDto`

Security:

- authentication: required
- authorization: owner only
- ownership checks:
  - only caller enrolls self
  - only caller can cancel own enrollment
  - course must be published

Error states:

- duplicate or cancelled enrollment reuse is handled, not surfaced as hard failure
- unpublished course returns controlled error
- unknown enrollment id or ownership mismatch returns error

Problems found:

- none in the core backend flow

Missing pieces:

- no group-driven bulk enrollment workflow

Recommended next fix:

- keep this flow as the contract baseline before adding cohort automation

## Workflow: Lesson Access

Role:
Learner and preview visitors with authenticated session

Platform:
Android, Web, Backend, Database

Status:
Fully working

Entry point:

- Android: `LessonPlayerFragment`
- Web: `/learn/$courseId/$lessonId`
- Backend: `GET /api/v1/courses/{courseId}/lessons/{lessonId}`

End result:

- Learner opens lesson content, sees content body or URL, and can navigate between lessons.

Step-by-step:

1. Client asks for lesson detail.
2. Backend loads the lesson, course section, and current completion state.
3. Preview lessons are accessible without enrollment.
4. Non-preview lessons require active enrollment.
5. Android renders lesson content in a `WebView`; web uses `LessonContentRenderer`.

Backend code:

- file path: `backend/src/main/java/com/edulife/courses/service/LessonService.java`
- class/method: `getLessonDetail`

Android code:

- file path: `app/src/main/java/com/baghdad/edulife/features/courses/ui/LessonPlayerFragment.java`

Web code:

- file path: `guided-journey-lab/src/routes/learn.$courseId.$lessonId.tsx`

Database:

- tables: `lessons`, `lesson_progress`
- migration files: `V2__courses.sql`, `V6__lesson_content.sql`, `V7__progress.sql`

API contract:

- endpoint: `GET /api/v1/courses/{courseId}/lessons/{lessonId}`
- response DTO:
  - `LessonDetailDto`
  - includes `contentUrl`, `contentBody`, `preview`, `completed`

Security:

- authentication: required
- authorization:
  - preview lessons bypass enrollment check
  - non-preview lessons require enrollment
- ownership checks: completion state is always for the current user

Problems found:

- none severe in lesson read flow

Missing pieces:

- no comments/Q&A layer

Recommended next fix:

- keep the endpoint stable and only add metadata, not a separate incompatible lesson model

## Workflow: Lesson Completion and Course Progress

Role:
Learner

Platform:
Android, Web, Backend, Database, Tests

Status:
Fully working with one rule ambiguity

Entry point:

- Android: `LessonPlayerFragment` mark done / close handling
- Web: `/learn/$courseId/$lessonId` mark-as-done button
- Backend: `POST /api/v1/courses/{courseId}/lessons/{lessonId}/complete`, `GET /api/v1/progress/courses/{courseId}`

End result:

- Lesson completion is idempotent, course aggregates update, and progress feeds dashboards plus exam readiness UI.

Step-by-step:

1. Client submits mark-complete.
2. Backend writes `lesson_progress` once.
3. Backend recalculates `course_progress`.
4. Backend emits XP for lesson completion and course completion when applicable.
5. Client invalidates lesson, progress, courses, dashboard, and profile views.

Backend code:

- file path: `backend/src/main/java/com/edulife/progress/service/ProgressService.java`
- class/method: `markLessonComplete`, `getCourseProgress`
- important snippet:

```java
if (!lesson.isPreview()) {
    requireActiveEnrollment(userId, courseId);
}
```

Android code:

- file path: `app/src/main/java/com/baghdad/edulife/features/courses/ui/LessonPlayerFragment.java`
- class/method: completion button and close behavior

Web code:

- file path: `guided-journey-lab/src/routes/learn.$courseId.$lessonId.tsx`
- component/hook/function: `markCompleteMutation`

Database:

- tables: `lesson_progress`, `course_progress`
- migration files: `V7__progress.sql`

API contract:

- `POST /api/v1/courses/{courseId}/lessons/{lessonId}/complete`
  - response DTO: `CourseProgressDto`
- `GET /api/v1/progress/courses/{courseId}`
  - response DTO: `CourseProgressDto`

Security:

- authentication: required
- authorization: current learner only
- ownership checks:
  - progress is resolved by caller identity
  - course progress read requires active enrollment

Problems found:

- Preview lessons can currently be marked complete because the enrollment check is skipped for previews.

Missing pieces:

- explicit product decision on whether preview completion should count toward progress

Recommended next fix:

- decide whether previews should be countable; if not, block completion or exclude them from aggregates

