# Fix: Enrollment Re-enrollment + Courses Display Bugs

## Goal

Fix enrolled courses not appearing in the Courses tab after re-enrollment, and allow users to re-enroll in courses they previously unenrolled from.

## What Changed

### Backend — re-enrollment support

`EnrollmentService.enroll()` previously called `existsByUserIdAndCourseId` (status-agnostic), which threw a 409 CONFLICT for any prior enrollment record regardless of status. Because the `enrollments` table has a `UNIQUE (user_id, course_id)` constraint, a user who unenrolled could never re-enroll — they received a 409, the Android app treated it as "already enrolled," navigated to CoursesFragment, but the course was absent (CANCELLED enrollment, not returned by `getMyEnrollments`).

Fix:
- `existsByUserIdAndCourseId` replaced with `existsByUserIdAndCourseIdAndStatus(..., ACTIVE)` — 409 only fires for an active duplicate.
- `findByUserIdAndCourseId` added to `EnrollmentRepository` to locate the cancelled record.
- If a cancelled enrollment exists, it is reactivated via `enrollment.reactivate()` (new method on `Enrollment` entity) instead of inserting a new row (which would violate the unique constraint).
- `Enrollment.reactivate()` sets status back to `ACTIVE`.

### Android — stale ViewModel after enrollment navigation (prior session)

`EnrollCourseFragment` was using `setRestoreState(true)` in its post-enrollment NavOptions. This restored the old ViewModel state (stale enrollment list) when navigating to CoursesFragment. Removing `setRestoreState(true)` forces a fresh CoursesFragment and a clean API fetch.

### Android — CertificatesFragment crash (prior session)

`certsEmpty` view is a `LinearLayout` in XML but was declared as `TextView` in the fragment, causing a `ClassCastException`. Changed the declaration to `View`.

### Android — Lesson player next/prev navigation (prior session)

- Prev button: `popBackStack()` instead of a Toast stub.
- Next button: reads the shared `CourseDetailViewModel` (scoped to the `courseDetailFragment` NavBackStackEntry) to find the next accessible lesson and navigate to it with full args.

## Files Touched

- `backend/.../enrollments/service/EnrollmentService.java` — status-aware duplicate check + reactivate path
- `backend/.../enrollments/entity/Enrollment.java` — `reactivate()` method
- `backend/.../enrollments/repository/EnrollmentRepository.java` — `findByUserIdAndCourseId()`
- `app/.../features/courses/ui/EnrollCourseFragment.java` — removed `setRestoreState(true)`
- `app/.../features/certificates/ui/CertificatesFragment.java` — `View` instead of `TextView`
- `app/.../features/courses/ui/LessonPlayerFragment.java` — next/prev nav
- `app/.../features/courses/ui/CourseDetailFragment.java` — ViewModel scoped to NavBackStackEntry

## Backend Impact

`POST /api/v1/enrollments` now reactivates cancelled enrollments rather than blocking them. Behaviour change: a user who unenrolled can re-enroll; the original `enrolledAt` timestamp is preserved. A 409 is only returned when an ACTIVE enrollment already exists.

## Android Impact

CoursesFragment is always recreated fresh after enrollment → `onResume()` fetches the current server state. Certificates screen no longer crashes. Lesson player supports unlimited chaining to the next lesson.

## Architecture Compliance

- Business logic stays in the service layer.
- Repository method follows Spring Data naming convention.
- No new migrations needed (no schema change — only service logic changed).
- Ownership check unchanged (`unenroll` still verifies `userId` match).

## Tests / Verification

Manual:
1. Unenroll from a course → go to Home tab → find that course → Enroll → confirm it appears in Courses tab.
2. Enroll in a course → confirm it appears in Courses tab immediately after enrollment.
3. Open CertificatesFragment — no crash.
4. Open any lesson → use Prev/Next buttons.

## Risks / Notes

- Re-enrollment preserves the original `enrolledAt` and any prior progress records. Progress is not reset on re-enroll; this is intentional for MVP (learners keep their progress history).
- The `existsByUserIdAndCourseId` method is still present in the repository (used nowhere now but kept to avoid compilation issues if referenced elsewhere). Safe to remove later.
