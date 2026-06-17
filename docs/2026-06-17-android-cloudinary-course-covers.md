# Android Cloudinary Course Cover Images

## Goal

Display Cloudinary-hosted course cover images across all Android screens.

## What Changed

Added ImageView elements to four layouts and wired Glide image loading in their
corresponding adapters/fragments. No model changes required — all models already
use `imageUrl` which matches the backend field name.

### Layouts modified

- `item_enrolled_course.xml` — added `enrolledCourseImage` ImageView above course info
- `item_cms_course.xml` — added `cmsCourseImage` ImageView above title
- `item_approval_course.xml` — added `approvalCourseImage` ImageView above title
- `fragment_course_detail.xml` — added `courseDetailHeroImage` full-width hero image;
  removed parent horizontal padding so image bleeds edge-to-edge, added per-element
  margins for text content below

### Java files modified

- `CoursesFragment.java` (EnrolledCourseAdapter) — added ImageView binding and Glide
  loading with level-based fallback
- `TeacherCourseAdapter.java` — added ImageView binding and Glide loading
- `ApprovalCourseAdapter.java` — added ImageView binding and Glide loading
- `CourseDetailFragment.java` — added Glide hero image loading in `bindCourseDetail()`

### Resources (pre-existing, no changes needed)

- `dimens.xml` — `detail_hero_image_height`, `cms_course_image_height`,
  `enrolled_course_image_height` already defined
- `strings.xml` — `course_cover_content_desc` already defined
- `network_security_config.xml` — HTTPS-only default; Cloudinary URLs work without changes

## Files Touched

- `app/src/main/res/layout/item_enrolled_course.xml`
- `app/src/main/res/layout/item_cms_course.xml`
- `app/src/main/res/layout/item_approval_course.xml`
- `app/src/main/res/layout/fragment_course_detail.xml`
- `app/src/main/java/com/baghdad/edulife/features/courses/ui/CoursesFragment.java`
- `app/src/main/java/com/baghdad/edulife/features/teacher/ui/TeacherCourseAdapter.java`
- `app/src/main/java/com/baghdad/edulife/features/groupadmin/ui/ApprovalCourseAdapter.java`
- `app/src/main/java/com/baghdad/edulife/features/courses/ui/CourseDetailFragment.java`

## Backend Impact

None. Backend already returns `imageUrl` with Cloudinary secure URLs.

## Android Impact

- Course covers now visible on: catalog (already worked), My Learning, course detail,
  teacher courses, approval queue
- Glide loads absolute Cloudinary HTTPS URLs directly — no localhost prefixing
- Fallback to level-specific hero drawables on null/empty/error

## Web Impact

None.

## Architecture Compliance

- No Cloudinary secrets in Android
- No direct Cloudinary upload from Android
- Image loading via Glide (existing dependency)
- Models unchanged — `imageUrl` field already matched backend DTO

## Tests / Verification

- `./gradlew assembleDebug` — BUILD SUCCESSFUL
- `./gradlew test` — BUILD SUCCESSFUL

## Risks / Notes

- `GroupCourseAdapter` not updated — `GroupCourse` model lacks `imageUrl` field
  (backend group-course DTO does not include it). Would need backend change first.
- Career advisor recommendation cards don't show images — adapter uses separate
  model without direct image field. Low priority.
- No cover image upload UI on Android yet. When added, should POST to
  `/api/v1/cms/courses/{courseId}/cover-image` (multipart) and refresh from response.
