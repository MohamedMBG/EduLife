# Task Audit - Switch Profile Avatar Storage To Cloudinary

## Date
2026-06-22

## Task Summary
Fixed profile avatar uploads so they no longer use localhost-backed filesystem URLs and instead use Cloudinary-hosted public image URLs, matching the deployed media strategy already used for course cover uploads.

## Files Created
- backend/src/main/java/com/edulife/profiles/storage/CloudinaryAvatarStorage.java
- backend/src/test/java/com/edulife/profiles/storage/CloudinaryAvatarStorageTest.java
- docs/2026-06-22-switch-profile-avatar-storage-to-cloudinary.md

## Files Modified
- backend/src/test/java/com/edulife/profiles/ProfileAvatarControllerTest.java
- backend/src/main/java/com/edulife/profiles/storage/CloudinaryAvatarStorage.java

## What Was Done
Added a new `CloudinaryAvatarStorage` implementation of `AvatarStorage` and marked it as the primary backend avatar storage bean.

The new implementation:
- validates avatar size against the configured 5MB limit
- validates allowed MIME types
- checks image magic bytes so renamed HTML/script files are rejected
- uploads avatars to Cloudinary under the `edulife/avatars` folder
- returns the Cloudinary `secure_url` as the public `avatarUrl`
- deletes prior Cloudinary avatar assets when a profile avatar is replaced

Updated controller-level test expectations so the profile avatar API contract now reflects Cloudinary URLs instead of localhost upload URLs.

## Architecture Compliance
This change stays inside the backend profile storage layer:
- `profiles/storage/` owns avatar persistence details
- `ProfileService` continues to depend on the `AvatarStorage` abstraction
- controller and UI layers were not given storage-specific logic

This respects the EduLife modular-monolith structure and keeps storage concerns out of controllers and client code.

## Code Comments Added
Added comments in `CloudinaryAvatarStorage.java` for:
- the reason versioned Cloudinary URL prefixes are stripped before deriving a public id
- the delete-path behavior so only expected avatar-folder assets are destroyed

These comments explain the storage and cleanup intent rather than restating code.

## Validation / Testing
Ran focused backend tests:

```text
./mvnw.cmd "-Dtest=CloudinaryAvatarStorageTest,ProfileAvatarControllerTest" test
```

Result:
- **BUILD SUCCESS**

Validated:
- avatar upload endpoint returns a Cloudinary URL contract
- Cloudinary avatar uploads succeed through the storage adapter
- spoofed image uploads are rejected
- previous Cloudinary avatars can be deleted safely

## Risks / Notes
- This change requires a **Render backend redeploy** because avatar storage is selected server-side.
- Render must already have valid Cloudinary environment variables configured:
  - `CLOUDINARY_CLOUD_NAME`
  - `CLOUDINARY_API_KEY`
  - `CLOUDINARY_API_SECRET`
- Existing old localhost avatar URLs already stored in profiles will remain old values until each affected user uploads a new avatar or those rows are migrated separately.
