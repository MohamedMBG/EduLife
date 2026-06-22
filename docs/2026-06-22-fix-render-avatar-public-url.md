# Task Audit - Fix Render Avatar Public URL

## Date
2026-06-22

## Task Summary
Fixed the backend avatar upload flow so deployed uploads no longer emit `http://localhost:8080/uploads/avatars/...` URLs when the public base URL environment variable is unset. This was causing the website to treat the uploaded image as a blocked or unsafe resource under the production browser security policy.

## Files Created
- docs/2026-06-22-fix-render-avatar-public-url.md

## Files Modified
- backend/src/main/java/com/edulife/profiles/storage/LocalAvatarStorage.java
- backend/src/main/resources/application.yaml
- backend/src/test/java/com/edulife/profiles/storage/LocalAvatarStorageTest.java

## What Was Done
Updated `LocalAvatarStorage` so avatar upload responses prefer the current request origin when the configured avatar public base URL is blank or still pointing at a localhost fallback.

This means:
- local development can still use localhost URLs
- deployed requests on Render generate HTTPS avatar URLs from the real public backend origin
- the web app no longer has to depend only on client-side URL rewriting to display a newly uploaded avatar

Also updated avatar cleanup logic so old locally stored avatars are still deletable even when their public URL origin changes between localhost and the deployed backend host.

Added:
- request-origin based avatar URL generation
- localhost fallback detection
- safer stored-filename extraction for deletes
- `server.forward-headers-strategy: framework` so Spring respects `X-Forwarded-*` headers behind Render and uses the external HTTPS origin instead of an internal container host

## Architecture Compliance
The fix stays inside the backend profile storage/configuration layer:
- avatar URL generation belongs in `profiles/storage/`
- reverse-proxy URL handling belongs in backend configuration

No UI business logic was moved into controllers or routes, and no unrelated modules were changed.

## Code Comments Added
Added focused comments in:
- `LocalAvatarStorage.java` to explain why request-origin URLs are preferred over the localhost fallback in deployed environments
- `application.yaml` to explain why forwarded headers must be honored behind Render

These comments document the deployment/security reason for the behavior instead of restating code.

## Validation / Testing
Ran focused backend tests:

```text
./mvnw.cmd "-Dtest=LocalAvatarStorageTest,ProfileAvatarControllerTest" test
```

Result:
- **BUILD SUCCESS**
- verified avatar upload controller behavior
- verified local avatar storage still writes files
- verified request-origin avatar URLs are generated for deployed-style requests
- verified cleanup still deletes prior locally stored avatars even when the public origin differs

## Risks / Notes
- This fix requires a **Render backend redeploy** because the avatar URL is generated server-side.
- The existing web client-side media URL normalization remains useful as defense in depth, but the backend should now emit the correct public avatar URL by default.
- If Render already has `EDULIFE_AVATAR_PUBLIC_BASE_URL` set correctly, this change is still safe and should preserve that explicit configuration.
