# Phase 4 — Edit Profile + Avatar Upload

## Goal
Complete Phase 4 remaining gap: EditProfileFragment (displayName + bio form) and avatar upload via PickVisualMedia.

## What Changed

- **New models**: `UpdateProfileRequest.java`, `AvatarUploadResponse.java`
- **ApiService**: `PUT profile` + `@Multipart POST profile/avatar`
- **ProfileRepository**: `updateProfile()` + `uploadAvatar(File)` (creates MultipartBody.Part internally)
- **ProfileViewModel**: `updateProfile()`, `uploadAvatar()`, and associated LiveData streams (saving, saveSuccess, saveError, uploading, uploadedAvatarUrl, uploadError)
- **EditProfileFragment**: prefilled form (displayName + bio), validation, save → PUT /profile → toast + pop back
- **fragment_edit_profile.xml**: top bar with back + save button, two Material TextInputLayouts
- **ProfileFragment**: avatar FrameLayout now clickable → PickVisualMedia picker; on selection compresses to 1024px JPEG → uploadAvatar(); settingsEditProfile row → nav to EditProfileFragment; avatar ImageView shows real image via Glide when URL available
- **fragment_profile.xml**: added `avatarContainer` id, avatar `ImageView` (hidden by default, shown after upload/load)
- **nav_graph.xml**: added `editProfileFragment` destination + action from profileFragment
- **strings.xml**: edit profile + avatar strings

## Files Touched

- `features/profile/model/UpdateProfileRequest.java` (new)
- `features/profile/model/AvatarUploadResponse.java` (new)
- `core/network/ApiService.java`
- `features/profile/data/ProfileRepository.java`
- `features/profile/viewmodel/ProfileViewModel.java`
- `features/profile/ui/ProfileFragment.java`
- `features/profile/ui/EditProfileFragment.java` (new)
- `res/layout/fragment_edit_profile.xml` (new)
- `res/layout/fragment_profile.xml`
- `res/navigation/nav_graph.xml`
- `res/values/strings.xml`

## Backend Impact

None — consumes existing `PUT /api/v1/profile` and `POST /api/v1/profile/avatar` endpoints.

## Android Impact

- Profile screen: tap avatar → pick image → compress → upload → Glide shows result
- Profile screen: "Edit Profile" row → EditProfileFragment → form → save → updates name + bio
- ProfileViewModel scoped to Activity so EditProfileFragment and ProfileFragment share state

## Architecture Compliance

- Image compression in Fragment (UI concern); multipart creation in Repository (transport concern)
- ViewModel holds all loading/error/success state — Fragments only observe and render
- Avatar max 5MB constraint met: 1024px JPEG at 88% quality is well under 1MB

## Tests / Verification

- Open Profile → tap avatar → pick → compressed → uploaded → image shown
- Open Profile → Edit Profile → form pre-filled → change name → Save → toast + back → name updated
- Empty displayName → error shown, no network call

## Risks / Notes

- `PickVisualMedia` requires `ActivityResultContracts.PickVisualMedia` (Photo Picker API, available API 33+ or via Jetpack backport). Already available via existing AndroidX dependencies.
- Avatar ImageView uses `circleCrop()` in Glide to match the circular `bg_profile_avatar` background.
