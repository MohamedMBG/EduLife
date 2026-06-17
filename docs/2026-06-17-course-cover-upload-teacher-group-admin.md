# Course Cover Image Upload — Teacher & Group Admin

## Goal

Allow TEACHER and GROUP_ADMIN users to upload/change a cover image for courses they own or manage. Cover images display on course cards and detail pages across the platform.

## What Changed

### Backend

- New `POST /api/v1/cms/courses/{courseId}/cover-image` endpoint (multipart/form-data)
- New `LocalCourseCoverStorage` service — stores images on local filesystem, returns public URL
- New `CourseCoverStorageProperties` — configurable storage dir, public base URL, max file size
- New `CourseCoverStorageConfig` — static resource handler for `/uploads/course-covers/**`
- Extended `CmsCourseService.uploadCoverImage()` with ownership + group admin permission check
- Updated `SecurityConfig` — permits `/uploads/course-covers/**` without auth
- Updated `FirebaseTokenFilter` — skips course cover static paths
- Updated `application.yaml` — added `edulife.course-covers` config block

### Frontend

- New `uploadCourseCoverImage()` API client method
- New `CourseCoverUploadResponse` type
- New `CourseCoverImageUploader` component (drag-and-drop, preview, validation)
- Integrated uploader into `teach.$courseId.index.tsx` course management page

## Endpoint Added

```
POST /api/v1/cms/courses/{courseId}/cover-image
Content-Type: multipart/form-data
Field: file

Response:
{
  "courseId": "uuid",
  "coverImageUrl": "http://host/uploads/course-covers/uuid-uuid.jpg",
  "message": "Course cover image updated successfully"
}
```

## Accepted File Types

- image/jpeg
- image/png
- image/webp

## Max File Size

5MB (5,242,880 bytes)

## Storage Strategy

- Local filesystem storage at configurable `uploads/course-covers/` directory
- Filename: `{courseId}-{randomUUID}.{extension}` — prevents enumeration and path traversal
- Old cover deleted on replacement
- Served as static resource at `/uploads/course-covers/**`
- Path traversal defense: normalized path checked against base directory

## Permission Rules

| Role | Allowed? | Condition |
|------|----------|-----------|
| TEACHER | Yes | Must own the course (`createdByUserId` matches) |
| GROUP_ADMIN | Yes | Course author must be a member of one of their managed groups |
| ADMIN | Yes | Always |
| LEARNER | No | 403 Forbidden |

## Files Touched

### Backend
- `backend/src/main/java/com/edulife/courses/config/CourseCoverStorageProperties.java` (new)
- `backend/src/main/java/com/edulife/courses/config/CourseCoverStorageConfig.java` (new)
- `backend/src/main/java/com/edulife/courses/storage/LocalCourseCoverStorage.java` (new)
- `backend/src/main/java/com/edulife/courses/dto/CourseCoverUploadResponse.java` (new)
- `backend/src/main/java/com/edulife/admin/controller/CmsCourseController.java` (modified)
- `backend/src/main/java/com/edulife/admin/service/CmsCourseService.java` (modified)
- `backend/src/main/java/com/edulife/security/SecurityConfig.java` (modified)
- `backend/src/main/java/com/edulife/security/FirebaseTokenFilter.java` (modified)
- `backend/src/main/resources/application.yaml` (modified)

### Frontend
- `guided-journey-lab/src/components/cms/CourseCoverImageUploader.tsx` (new)
- `guided-journey-lab/src/lib/api/types.ts` (modified)
- `guided-journey-lab/src/lib/api/client.ts` (modified)
- `guided-journey-lab/src/routes/teach.$courseId.index.tsx` (modified)

## Frontend Screens Updated

- Teaching Studio course management page (`/teach/$courseId`) — uploader integrated
- Explore/Catalog — already displays `imageUrl` from course data
- Course detail — already displays `imageUrl` from course data
- My Learning — already displays `imageUrl` from enrollment data

## Commands Run

- `./mvnw compile` — backend compiles clean
- `npx tsc --noEmit` — zero TypeScript errors
- `npx eslint` — zero lint errors after prettier formatting

## Manual Verification Steps

1. Login as teacher → open owned course → upload JPG cover → confirm preview → refresh → confirm persists
2. Open Explore/Catalog → confirm cover image appears on course card
3. Open course detail → confirm cover image appears
4. Login as different teacher → confirm cannot upload cover to another teacher's course (403)
5. Login as group admin → confirm can upload cover for courses authored by teachers in their group
6. Login as group admin → confirm cannot upload cover for courses outside their group (403)
7. Login as learner → confirm upload endpoint returns 403
8. Try uploading .gif or .pdf → confirm 400 error
9. Try uploading file > 5MB → confirm 413 error
10. Upload new cover over existing one → confirm old file deleted, new URL returned
11. Check mobile layout — uploader should be responsive

## Risks / Notes

- No Flyway migration needed — `image_url` column already exists (V5)
- Existing course DTOs already include `imageUrl` — no changes needed
- Old cover images from external URLs (e.g., Unsplash seed data) are NOT deleted from remote servers, only locally-stored covers are cleaned up
- Production should set `EDULIFE_COURSE_COVERS_PUBLIC_BASE_URL` to the deployed domain
