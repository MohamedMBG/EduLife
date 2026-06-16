# EduLife Auth Workflows

## Workflow: App Entry, Session Routing, and Role Portals

Role:
All roles

Platform:
Android, Web, Backend

Status:
Partially working

Entry point:

- Android: `MainActivity` + `app/src/main/res/navigation/nav_graph.xml`
- Web: `guided-journey-lab/src/lib/auth/auth-context.tsx`
- Backend: `backend/src/main/java/com/edulife/security/FirebaseTokenFilter.java`

End result:

- Existing signed-in users are routed to the correct learner, teacher, group admin, or admin surface.

Step-by-step:

1. Android checks onboarding completion, Firebase session state, and stored backend role.
2. Web restores Firebase `browserSessionPersistence` and then runs backend sync on auth state changes.
3. Backend validates the bearer token, enforces `email_verified`, loads the internal user, and resolves DB roles into Spring authorities.
4. Client route guards redirect to role-specific areas:
   - learner dashboard
   - teacher studio
   - group portal
   - admin portal

Backend code:

- file path: `backend/src/main/java/com/edulife/security/FirebaseTokenFilter.java`
- class/method: `FirebaseTokenFilter#doFilterInternal`
- important snippet:

```java
if (!Boolean.TRUE.equals(decodedToken.getEmailVerified())) {
    writeUnauthorized(response, "Email verification required");
    return;
}
```

Android code:

- file path: `app/src/main/java/com/baghdad/edulife/MainActivity.java`
- class/method: `MainActivity#onCreate`
- important snippet:

```java
if (currentUser == null) {
    navController.navigate(R.id.loginFragment);
} else if ("ADMIN".equals(role)) {
    navController.navigate(R.id.adminDashboardFragment);
}
```

Web code:

- file path: `guided-journey-lab/src/lib/auth/auth-context.tsx`
- component/hook/function: `RequireAuth`, `RequireTeacher`, `RequireGroupManager`, `RequireAdmin`
- important snippet:

```tsx
if (!session) {
  return <Navigate to="/login" search={{ redirect: location.pathname }} />
}
```

Database:

- tables: `users`
- migration files: `V1__init.sql`, `V16__add_role_constraint.sql`, `V19__seed_staff_roles.sql`

API contract:

- endpoint: all protected `/api/v1/**`
- request: Firebase bearer token
- response: protected DTO or `401/403`
- errors:
  - `401 Email verification required`
  - `401 Authentication required`
  - `403` from `@PreAuthorize`

Security:

- authentication: Firebase token validation in filter
- authorization: RBAC from internal DB role, not client claims
- ownership checks: downstream service-level checks for user/group/course ownership

Problems found:

- Android and web use different local session models.
- Android can still navigate as if login succeeded after a failed sync.

Missing pieces:

- no centralized cross-client contract for post-login routing behavior

Recommended next fix:

- make Android login fail closed unless backend sync returns a valid `userId` and role

## Workflow: Register and Login

Role:
Learner, Teacher, Group Admin, Admin bootstrap

Platform:
Android, Web, Backend, Firebase

Status:
Partially working

Entry point:

- Android: `LoginFragment`, `RegisterFragment`
- Web: `/login`, `/register`
- Backend: `/api/v1/auth/sync`

End result:

- User creates or signs into a Firebase account, then gets an EduLife internal UUID and role.

Step-by-step:

1. Client creates/signs in with Firebase Email/Password.
2. Client stores an intended EduLife role for first sync.
3. Client fetches a Firebase ID token.
4. Client calls `POST /api/v1/auth/sync`.
5. Backend upserts the internal user and returns `{ userId, role }`.
6. Client stores the internal UUID and resolved role for later routing.

Backend code:

- file path: `backend/src/main/java/com/edulife/auth/controller/AuthController.java`
- class/method: `AuthController#sync`
- important snippet:

```java
@PostMapping("/sync")
public AuthSyncResponse sync(@Valid @RequestBody(required = false) AuthSyncRequest request) {
    return authSyncService.sync(request);
}
```

- file path: `backend/src/main/java/com/edulife/auth/service/AuthSyncService.java`
- class/method: `AuthSyncService#sync`
- important snippet:

```java
if (existing != null) {
    return new AuthSyncResponse(existing.getId(), existing.getRole());
}
```

Android code:

- file path: `app/src/main/java/com/baghdad/edulife/features/auth/viewmodel/AuthViewModel.java`
- class/method: `AuthViewModel#login`, `AuthViewModel#register`
- important snippet:

```java
repository.signIn(email, password, roleCode, () -> {
    authState.postValue(new AuthState.Success());
}, error -> authState.postValue(new AuthState.Error(error)));
```

Web code:

- file path: `guided-journey-lab/src/routes/register.tsx`
- component/hook/function: register submit handler
- file path: `guided-journey-lab/src/routes/login.tsx`
- component/hook/function: login submit handler

Database:

- tables: `users`
- migration files: `V1__init.sql`, `V18__seed_admin_user.sql`, `V19__seed_staff_roles.sql`, `V20__promote_admin_role.sql`

API contract:

- endpoint: `POST /api/v1/auth/sync`
- request DTO:
  - `AuthSyncRequest`
  - `intendedRole`
- response DTO:
  - `AuthSyncResponse`
  - `userId`, `role`
- errors:
  - `401` unauthenticated / unverified token
  - `409/400` invalid first-sync role situations

Security:

- authentication: caller identity comes from `SecurityContext`, not request JSON
- authorization: `intendedRole` is only honored during initial sync and cannot self-assign `ADMIN`
- ownership checks: relinking by email is server-controlled

Problems found:

- Android login success path does not require successful backend sync.
- Admin self-assignment is blocked correctly on backend, but clients do not surface this nuance clearly.

Missing pieces:

- no Android forgot-password flow

Recommended next fix:

- return explicit sync failure to Android UI and block navigation when sync fails

## Workflow: Password Reset

Role:
All Firebase users

Platform:
Android, Web, Firebase

Status:
Partial

Entry point:

- Android: `LoginFragment` forgot-password click
- Web: `/forgot-password`

End result:

- Web users can request a Firebase password reset email.
- Android users currently cannot.

Step-by-step:

1. Web user enters email on `/forgot-password`.
2. Web calls Firebase `sendPasswordResetEmail`.
3. Account-enumeration errors are intentionally swallowed so the UI always behaves as if email was sent.
4. Android currently shows only a placeholder toast from `LoginFragment`.

Android code:

- file path: `app/src/main/java/com/baghdad/edulife/features/auth/ui/LoginFragment.java`
- class/method: forgot-password click handler
- important snippet:

```java
forgotPasswordText.setOnClickListener(v ->
        Toast.makeText(requireContext(), "Forgot password coming soon", Toast.LENGTH_SHORT).show());
```

Web code:

- file path: `guided-journey-lab/src/routes/forgot-password.tsx`
- component/hook/function: `handleSubmit`
- important snippet:

```tsx
await firebaseAuth.sendPasswordResetEmail(auth, email.trim());
```

API contract:

- no EduLife backend endpoint
- Firebase-only operation

Problems found:

- Android and web do not have parity.

Missing pieces:

- Android reset screen
- manual test coverage in the repo

Recommended next fix:

- implement Android Firebase reset to match the web behavior, including account-enumeration-safe UX

## Workflow: Firebase Auth Sync

Role:
All verified users

Platform:
Android, Web, Backend, Database

Status:
Fully working on backend; partial cross-client behavior

Entry point:

- `POST /api/v1/auth/sync`

End result:

- Firebase identity is mapped to an internal EduLife UUID and trusted server role.

Step-by-step:

1. `FirebaseTokenFilter` authenticates the request.
2. `AuthSyncService` reads `firebase_uid`, email, and intended role.
3. Existing users are returned immediately.
4. If the Firebase UID changed but email matches, backend relinks the account by email.
5. New users are created with a restricted initial role.
6. Staff seed-role mapping can promote trusted preconfigured accounts.

Backend code:

- file path: `backend/src/main/java/com/edulife/auth/service/AuthSyncService.java`
- class/method: `AuthSyncService#sync`
- important snippet:

```java
if (request != null && request.intendedRole() != null) {
    resolvedRole = normalizeInitialRole(request.intendedRole());
}
```

Database:

- tables: `users`
- migration files: `V1__init.sql`, `V19__seed_staff_roles.sql`

API contract:

- endpoint: `POST /api/v1/auth/sync`
- request:
  - `intendedRole`
- response:
  - `userId`
  - `role`
- errors:
  - `401` token missing/invalid/unverified
  - `409` impossible identity conflicts

Security:

- authentication: token validated with Firebase Admin
- authorization: backend decides role assignment
- ownership checks: no client-supplied `userId` or role is trusted

Problems found:

- Android client handling is weaker than backend guarantees.

Recommended next fix:

- add explicit client tests for sync-failure handling on Android and web

## Workflow: Profile, Avatar, and Account Deletion

Role:
Owner only

Platform:
Android, Web, Backend, Database, File storage

Status:
Profile/avatar full; account deletion partial because web is missing

Entry point:

- Android: `ProfileFragment`, `EditProfileFragment`
- Web: `/profile`
- Backend: `/api/v1/profile`, `/api/v1/profile/avatar`, `/api/v1/account`

End result:

- User can view/update profile, upload avatar, and on Android can delete the account.

Step-by-step:

1. Client requests `GET /api/v1/profile`; backend lazily creates a profile row if missing.
2. Client updates `displayName` and `bio` through `PUT /api/v1/profile`.
3. Avatar uploads go through multipart `POST /api/v1/profile/avatar`; backend writes a new file and removes the previous avatar file.
4. Android delete-account flow calls `DELETE /api/v1/account`.
5. Backend anonymizes local records, deletes the Firebase user, and clears session context.

Backend code:

- file path: `backend/src/main/java/com/edulife/profiles/controller/ProfileController.java`
- class/method: profile endpoints
- file path: `backend/src/main/java/com/edulife/account/service/AccountService.java`
- class/method: `AccountService#deleteCurrentAccount`

Android code:

- file path: `app/src/main/java/com/baghdad/edulife/features/profile/ui/ProfileFragment.java`
- class/method: edit/avatar/delete handlers

Web code:

- file path: `guided-journey-lab/src/routes/profile.tsx`
- component/hook/function: profile edit and avatar upload

Database:

- tables: `profiles`, `users`
- migration files: `V8__profiles.sql`, `V12__account_anonymization.sql`

API contract:

- `GET /api/v1/profile`
  - response DTO: `ProfileDto`
- `PUT /api/v1/profile`
  - request DTO: `UpdateProfileRequest`
  - response DTO: `ProfileDto`
- `POST /api/v1/profile/avatar`
  - response DTO: `AvatarUploadResponse`
- `DELETE /api/v1/account`
  - response: empty `204`

Security:

- authentication: required
- authorization: owner only
- ownership checks: user resolved from security context; no path user id exists

Problems found:

- web has no delete-account path at all

Missing pieces:

- web account deletion UI
- direct automated tests for web profile flow

Recommended next fix:

- add web account deletion to match backend capability and Android compliance flow

