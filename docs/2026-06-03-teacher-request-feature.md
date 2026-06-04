# Teacher Request Feature — Android

## Goal

Allow LEARNER-role users to apply to become a teacher directly from the Profile screen. The backend module was already fully implemented.

## What Changed

- **TeacherRequestResponse.java** — model mirroring backend `TeacherRequestResponse` (id, userId, userEmail, status, motivation, adminNote, requestedAt, reviewedAt)
- **SubmitTeacherRequestBody.java** — request body model with optional `motivation` field
- **ApiService** — added `POST teacher-requests` and `GET teacher-requests/me`
- **TeacherRequestRepository** — `getMyRequest()` (handles 204 = no request) + `submitRequest()` (handles 409 = already pending)
- **TeacherRequestViewModel** — loads current request state on open, guards against re-fetch, exposes `loading`, `noRequest`, `request`, `error`, `submitting`, `submitError`; `reload()` for retry
- **TeacherRequestFragment** — multi-state screen: form (no request or rejected), pending status card, approved card, error + retry; handles 4 backend status values (NONE/204, PENDING, REJECTED, APPROVED)
- **fragment_teacher_request.xml** — header + state views (loading/error/form/pending/approved) with rejection detail card nested inside form
- **fragment_profile.xml** — added `profileBecomeTeacherRow` (Learning section, `visibility="gone"`)
- **ProfileFragment** — shows "Become a Teacher" row only when `SessionStorage.getRole() == "LEARNER"`; navigates to teacherRequestFragment on tap
- **nav_graph.xml** — `teacherRequestFragment` destination + action from `profileFragment`
- **strings.xml** — all teacher request UI strings

## Files Touched

- `features/profile/model/TeacherRequestResponse.java` (new)
- `features/profile/model/SubmitTeacherRequestBody.java` (new)
- `features/profile/data/TeacherRequestRepository.java` (new)
- `features/profile/viewmodel/TeacherRequestViewModel.java` (new)
- `features/profile/ui/TeacherRequestFragment.java` (new)
- `res/layout/fragment_teacher_request.xml` (new)
- `core/network/ApiService.java`
- `features/profile/ui/ProfileFragment.java`
- `res/layout/fragment_profile.xml`
- `res/navigation/nav_graph.xml`
- `res/values/strings.xml`

## Backend Impact

None — consumes existing endpoints:
- `POST /api/v1/teacher-requests`
- `GET /api/v1/teacher-requests/me`

## Android Impact

- Profile screen: "Become a Teacher" row visible only for LEARNER role
- TeacherRequestFragment handles all states: no request (form), PENDING (status card), REJECTED (rejection detail + re-submit form), APPROVED (confirmation card)
- 409 from POST is handled gracefully with a toast
- 204 from GET is handled as "no request exists" → shows form

## Architecture Compliance

- ViewModel scoped to Fragment (not Activity — teacher request state is screen-local)
- Repository handles both 204 and normal 200/201 responses
- No business logic in Fragment — all state managed in ViewModel
- `motivation` is optional per backend spec; sent as null when empty

## Tests / Verification

- LEARNER role → profile shows "Become a Teacher" row
- Tap → TeacherRequestFragment opens → loading → form shown (no prior request)
- Submit with/without motivation → PENDING card shows with submitted date
- If previously REJECTED → rejection card + new form shown
- If APPROVED → approved confirmation card shown
- Network error → error view + retry button

## Risks / Notes

- Role row stays visible until logout even if request is APPROVED (role sync happens on next login)
- TEACHER/ADMIN/GROUP_ADMIN users never see the row
