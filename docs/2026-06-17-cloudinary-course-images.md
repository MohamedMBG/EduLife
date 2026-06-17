# Cloudinary Course Cover Images

## Goal
Replace local filesystem image storage with Cloudinary so course cover images display correctly on all clients (web, Android) regardless of backend host.

## What Changed
- Added Cloudinary Java SDK dependency to backend
- Created `CloudinaryProperties`, `CloudinaryConfig` for Cloudinary bean setup
- Created `CloudinaryStorageService` to upload/delete images via Cloudinary API
- Created `CloudinaryUploadResult` record to carry both secure URL and public ID
- Added Flyway migration `V25__course_cover_cloudinary.sql` — adds `cover_image_public_id` column
- Updated `Course` entity with `coverImagePublicId` field
- Updated `CmsCourseService` to use `CloudinaryStorageService` instead of `LocalCourseCoverStorage`
- Updated `CourseCoverUploadResponse` to include `coverImagePublicId`
- Disabled `LocalCourseCoverStorage` (removed `@Component`)
- Added Cloudinary config section to `application.yaml`
- Added `onError` image fallbacks across all frontend pages displaying course images
- Updated frontend `CourseCoverUploadResponse` type to include `coverImagePublicId`
- Added `CmsCoverImageUploadTest` with 6 test cases

## Files Touched

### Backend
- `backend/pom.xml` — added cloudinary-http44 dependency
- `backend/src/main/java/com/edulife/config/CloudinaryProperties.java` — new
- `backend/src/main/java/com/edulife/config/CloudinaryConfig.java` — new
- `backend/src/main/java/com/edulife/courses/storage/CloudinaryStorageService.java` — new
- `backend/src/main/java/com/edulife/courses/storage/CloudinaryUploadResult.java` — new
- `backend/src/main/java/com/edulife/courses/storage/LocalCourseCoverStorage.java` — disabled
- `backend/src/main/java/com/edulife/courses/entity/Course.java` — added coverImagePublicId
- `backend/src/main/java/com/edulife/courses/dto/CourseCoverUploadResponse.java` — added field
- `backend/src/main/java/com/edulife/admin/service/CmsCourseService.java` — swapped storage
- `backend/src/main/resources/application.yaml` — added cloudinary section
- `backend/src/main/resources/db/migration/V25__course_cover_cloudinary.sql` — new
- `backend/src/test/java/com/edulife/admin/CmsCoverImageUploadTest.java` — new

### Frontend
- `guided-journey-lab/src/lib/api/types.ts` — updated response type
- `guided-journey-lab/src/components/cms/CourseCoverImageUploader.tsx` — onError fallback
- `guided-journey-lab/src/routes/explore.tsx` — onError fallback
- `guided-journey-lab/src/routes/dashboard.tsx` — onError fallback
- `guided-journey-lab/src/routes/courses.index.tsx` — onError fallback
- `guided-journey-lab/src/routes/advisor.tsx` — onError fallback
- `guided-journey-lab/src/routes/planner.tsx` — onError fallback

## Required Environment Variables
```
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret
CLOUDINARY_FOLDER=edulife
```

## Backend Endpoint
`POST /api/v1/cms/courses/{id}/cover-image` — unchanged URL, now uploads to Cloudinary.

## Upload Flow
1. Teacher/group admin selects image in web UI
2. Frontend validates type (JPG/PNG/WebP) and size (<5MB)
3. Frontend sends `multipart/form-data` with `file` field to backend
4. Backend validates again, uploads to Cloudinary folder `edulife/course-covers`
5. Cloudinary returns `secure_url` and `public_id`
6. Backend stores `secure_url` in `image_url` column, `public_id` in `cover_image_public_id`
7. Old Cloudinary asset deleted by public ID (failure logged, not fatal)
8. Response returns both URL and public ID
9. Frontend invalidates query cache, image displays via absolute Cloudinary URL

## Database Fields
- `courses.image_url` — existing TEXT column, now stores Cloudinary secure_url
- `courses.cover_image_public_id` — new TEXT column (V25), stores Cloudinary public ID for deletion

## Accepted File Types
- image/jpeg
- image/png
- image/webp

## Max File Size
5MB (validated both frontend and backend)

## Permission Rules
- TEACHER: can upload for own courses only
- GROUP_ADMIN: can upload for courses by teachers in their groups
- ADMIN: can upload for any course
- LEARNER: 403 Forbidden

## Frontend Display Behavior
- Course images use absolute Cloudinary URLs — no localhost prefix
- All `<img>` elements have `onError` handlers that hide broken image and show gradient fallback
- Fallback gradient: `linear-gradient(135deg, #1e293b, #091426)` with icon placeholder

## How to Test Locally
1. Set Cloudinary env vars (get free account at cloudinary.com)
2. Start backend: `cd backend && ./mvnw spring-boot:run`
3. Start web: `cd guided-journey-lab && bun run dev`
4. Login as teacher, navigate to course management
5. Upload JPG/PNG/WebP image
6. Verify Cloudinary URL in response
7. Verify image displays on explore, dashboard, course cards

## Troubleshooting
- **Image not showing**: Check browser DevTools Network tab — `img src` should be `https://res.cloudinary.com/...`
- **Upload fails**: Verify CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET env vars
- **Old images broken**: Pre-Cloudinary images stored `http://localhost:8080/...` URLs, they won't display. Re-upload to fix.
- **403 on upload**: Check user role and course ownership

## Architecture Compliance
- Backend-only upload (no frontend direct Cloudinary upload)
- No secrets exposed to frontend
- Cloudinary credentials via environment variables only
- Role/ownership checks unchanged
- Flyway migration for schema change

## Risks / Notes
- Existing courses with old localhost image URLs will show fallback gradient until re-uploaded
- Cloudinary free tier has upload limits (25 credits/month) — sufficient for MVP
- Old `LocalCourseCoverStorage` kept in codebase (disabled) for reference
- `CourseCoverStorageConfig` still registers static resource handler for backward compatibility
