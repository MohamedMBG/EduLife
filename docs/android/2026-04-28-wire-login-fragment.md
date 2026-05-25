# Task Audit - Wire LoginFragment to Firebase sign-in and backend sync

## Date
2026-04-28

## Task Summary
Connected the `LoginFragment` to Firebase Authentication and the backend sync process via `AuthViewModel`. Placeholder behaviors were replaced with actual ViewModel integrations, ensuring only verified users can access the app while correctly dispatching API sync and updating navigation logic.

## Files Created
- `app/src/main/res/layout/fragment_home.xml` (Stub for Home destination)
- `app/src/main/java/com/baghdad/edulife/features/courses/ui/HomeFragment.java` (Stub fragment class)
- `docs/2026-04-28-wire-login-fragment.md`

## Files Modified
- `app/src/main/java/com/baghdad/edulife/features/auth/ui/LoginFragment.java`
- `app/src/main/res/navigation/nav_graph.xml`

## What Was Done
1. **Instantiated AuthViewModel**: Initialized the ViewModel in `LoginFragment.onViewCreated()`.
2. **Replaced Placeholders**: Removed the Toast placeholder logic from the login button and hooked it to `handleLogin()`.
3. **Trigger Auth Process**: Implemented `handleLogin()` which retrieves the inputs, validates them (not empty), and calls `authViewModel.login(email, password)`.
4. **State Observation**: Added `renderAuthState(AuthUiState state)` observer to listen to `AuthUiState`.
    - Shows "Logging in..." message when loading and disables the button.
    - Handles `emailVerificationRequired` state to block access and display a Toast message advising to verify the email.
    - Handles generic API/Firebase errors with Toast messages.
    - On success, triggers navigation to the stub Home destination and resets the ViewModel state.
5. **Stub Home Navigation**: Added a stub `HomeFragment` and action inside the `nav_graph.xml` to fulfill the success state navigation.

## Architecture Compliance
The changes fully align with the MVVM architecture principles set in `AGENTS.md`. 
- UI handles the user events (LoginFragment) and forwards them to the ViewModel.
- ViewModel manages the data manipulation via Repository, and controls the UI logic implicitly via LiveData `AuthUiState`.
- Created fragments follow the `features/<feature>/ui/` path format.

## Code Comments Added
- No new complex code that needs excessive explanation was added to `LoginFragment.java` besides standard UI observation patterns, but existing placeholder comments for Google auth, forgot password, etc., were retained for context.
- Added a comment before navigation indicating that it happens only after the successful backend sync.

## Validation / Testing
- Manually checked the logic flow: input validation -> ViewModel login -> Firebase response -> Backend Sync -> UI State changes.
- The `action_loginFragment_to_homeFragment` action was correctly configured in `nav_graph.xml` to point to the newly created `HomeFragment` destination.

## Risks / Notes
- Toasts are used for initial feedback. They might be later migrated to `Snackbar` for a better user experience but are perfectly acceptable for the MVP scope to satisfy the 'actionable error states' requirement.
- Backend API (`/api/v1/auth/sync`) and Firebase must be correctly configured in the environment for the app to successfully transition to `HomeFragment`.
