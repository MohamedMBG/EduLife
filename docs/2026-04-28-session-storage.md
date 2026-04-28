# Task Audit - Session Storage for Synced User Identity

## Date
2026-04-28

## Task Summary
Implement a minimal, production-safe local session storage layer that persists the
internal EduLife `userId` and `role` returned by `POST /api/v1/auth/sync`, and
integrates that storage into the authentication flow without modifying UI logic.

---

## Files Created

| File | Purpose |
|---|---|
| `app/src/main/java/com/baghdad/edulife/core/storage/SessionStorage.java` | SharedPreferences wrapper — single source of truth for session identity |
| `app/src/main/java/com/baghdad/edulife/features/auth/model/AuthSyncResponse.java` | Retrofit response model for `/api/v1/auth/sync` |

---

## Files Modified

| File | What Changed |
|---|---|
| `app/src/main/java/com/baghdad/edulife/core/network/ApiService.java` | Converted from empty class to Retrofit interface; added `syncUser()` endpoint |
| `app/src/main/java/com/baghdad/edulife/features/auth/data/AuthRepository.java` | Replaced `prepareBackendSyncToken` placeholder with real `syncWithBackend` that calls the backend and saves/clears session; added `Context` dependency for `SessionStorage`; wired `signOut` to also clear session |
| `app/src/main/java/com/baghdad/edulife/features/auth/viewmodel/AuthViewModel.java` | Changed base class from `ViewModel` to `AndroidViewModel` to pass `Application` context safely to `AuthRepository`; updated sync call name from `prepareBackendSyncToken` to `syncWithBackend` |

---

## What Was Done

### 1. SessionStorage
Created `core/storage/SessionStorage.java` using `SharedPreferences` (MODE_PRIVATE).

Stored keys:
- `user_id` → internal EduLife UUID from backend
- `role`    → user role string (e.g. `STUDENT`)

Exposed methods:
- `saveSession(userId, role)` — write after sync success
- `getUserId()` — read user ID
- `getRole()` — read role
- `hasSession()` — check if both keys are present
- `clearSession()` — wipe all keys; called on logout and sync failure

### 2. AuthSyncResponse
Created `features/auth/model/AuthSyncResponse.java` with `userId` and `role` string
fields matching the JSON keys returned by the backend.

### 3. ApiService (now a Retrofit interface)
The file was an empty class stub. Converted to a proper `interface` with:
```java
@POST("auth/sync")
Call<AuthSyncResponse> syncUser();
```
The `FirebaseAuthInterceptor` injects the `Authorization: Bearer <token>` header
automatically on every request.

### 4. AuthRepository — syncWithBackend
Replaced the `prepareBackendSyncToken` placeholder with `syncWithBackend(callback)`:

- Force-refreshes the Firebase token (so the interceptor has a valid one).
- Calls `apiService.syncUser()` via Retrofit.
- On HTTP 2xx with valid body → `sessionStorage.saveSession(userId, role)`.
- On HTTP error, network failure, or incomplete body → `sessionStorage.clearSession()`.
- `signOut()` now calls both `FirebaseAuth.signOut()` and `sessionStorage.clearSession()`.

`AuthRepository` now takes a `Context` parameter in its constructor. Application context
is used to avoid Activity memory leaks.

### 5. AuthViewModel — AndroidViewModel
Changed base class from `ViewModel` to `AndroidViewModel` so the constructor receives
an `Application` reference, which is safe to hold for the ViewModel's lifetime.
`LoginFragment` uses `new ViewModelProvider(this).get(AuthViewModel.class)` which
automatically delegates to `AndroidViewModelFactory` — no factory change needed.

The sync method call was renamed from `prepareBackendSyncToken` to `syncWithBackend`.

---

## Architecture Compliance

| Concern | Location |
|---|---|
| Session storage helper | `core/storage/` — shared utility, correct location |
| Response model | `features/auth/model/` — scoped to auth feature |
| API endpoint definition | `core/network/ApiService` — correct Retrofit interface location |
| Business logic (save/clear session) | `features/auth/data/AuthRepository` — repository layer only |
| State emission | `features/auth/viewmodel/AuthViewModel` — ViewModel only |
| UI untouched | `LoginFragment` — zero modifications |

No controllers contain business logic. No duplication of storage logic.

---

## Code Comments Added

| File | Comment Purpose |
|---|---|
| `SessionStorage.java` | Class-level Javadoc explains security intent (no tokens), class-level and method-level why |
| `AuthSyncResponse.java` | Security note on `userId` never being a Firebase UID |
| `ApiService.java` | Note that Bearer header is injected by interceptor, not manually |
| `AuthRepository.java` | Why sync clears session on failure; why token is force-refreshed; why both signOut steps are coupled |
| `AuthViewModel.java` | Why `AndroidViewModel` is used instead of `ViewModel`; why signOut atomically clears both sources |

---

## Security Considerations

- **Never stored**: Firebase ID token, refresh token, or password.
- **Only stored**: `userId` (internal UUID) and `role` (role string).
- `SessionStorage` uses `MODE_PRIVATE` — data is inaccessible to other apps.
- Session is always cleared on sync failure → prevents stale identity from leaking into future sessions.
- Logout clears both Firebase session and local session atomically.
- Correct answer to exams is never on the client (unrelated but noted).

---

## Validation Performed

```
./gradlew build
```

Result: **BUILD SUCCESSFUL** — 92 tasks, 23 executed, 69 up-to-date. Exit code: 0.

Manual validation checklist (requires running emulator + backend):
- [ ] Log in with valid credentials → observe `user_id` and `role` in SharedPreferences via Device File Explorer
- [ ] Restart app → confirm `SessionStorage.hasSession()` returns `true`
- [ ] Log out → confirm SharedPreferences keys are cleared
- [ ] Force sync failure (kill backend) → confirm session is cleared
- [ ] Confirm no Firebase token appears in SharedPreferences

---

## Risks / Notes

- **Backend not yet running**: `syncWithBackend` will call `sessionStorage.clearSession()` on network
  failure, so login will succeed at the Firebase level but `AuthUiState.success` will not be set.
  This is correct behavior; the learner flow is gated behind a real backend response.
- **`isBlank()` requires API 26**: The project `minSdk` should be ≥ 26; if not, replace with
  `isEmpty()` or `TextUtils.isEmpty()`.
- **No encryption**: SharedPreferences is private but unencrypted. For higher-security
  requirements in future sprints, consider `EncryptedSharedPreferences` from Jetpack Security.
- **No session refresh logic yet**: After a backend role change, the local role will be stale
  until the user logs out and back in. This is acceptable for Sprint 1 scope.
