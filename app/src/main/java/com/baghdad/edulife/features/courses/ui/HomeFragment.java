package com.baghdad.edulife.features.courses.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.baghdad.edulife.R;
import com.baghdad.edulife.core.storage.SessionStorage;
import com.baghdad.edulife.features.auth.viewmodel.AuthViewModel;
import com.google.firebase.auth.FirebaseAuth;

public class HomeFragment extends Fragment {

    private AuthViewModel authViewModel;
    private SessionStorage sessionStorage;

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // AuthViewModel is scoped to this fragment; it handles both logout and state clearing.
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // SessionStorage gives access to the persisted internal userId and role from /auth/sync.
        sessionStorage = new SessionStorage(requireContext());

        // Auth guard: if Firebase user is gone or local session was never written,
        // the user cannot remain on the authenticated home screen.
        if (!isSessionValid()) {
            redirectToLogin(view);
            return;
        }

        // Display the session identity so the stub confirms what was synced from the backend.
        bindSessionData(view);

        // Logout clears both Firebase and the local session, then redirects to login.
        view.findViewById(R.id.logoutButton).setOnClickListener(v -> handleLogout(view));
    }

    /**
     * Validates that a real Firebase user is signed in AND a local session was persisted.
     * Both must be true: Firebase alone is insufficient without a successful backend sync.
     */
    private boolean isSessionValid() {
        boolean hasFirebaseUser = FirebaseAuth.getInstance().getCurrentUser() != null;
        boolean hasLocalSession = sessionStorage.hasSession();
        return hasFirebaseUser && hasLocalSession;
    }

    /**
     * Populates the UI labels with the userId and role stored by SessionStorage after /auth/sync.
     */
    private void bindSessionData(@NonNull View view) {
        String userId = sessionStorage.getUserId();
        String role = sessionStorage.getRole();

        TextView roleText = view.findViewById(R.id.roleText);
        TextView userIdText = view.findViewById(R.id.userIdText);

        // Show a dash placeholder if the value is somehow null (defensive; should not happen after sync).
        roleText.setText(role != null ? role : "—");
        userIdText.setText(userId != null ? userId : "—");
    }

    /**
     * Signs the user out of Firebase and clears the local session via AuthViewModel,
     * then navigates back to the login screen and clears the home screen from the back stack.
     * The user must not be able to press back to re-enter the authenticated area after logout.
     */
    private void handleLogout(@NonNull View view) {
        // Delegate sign-out to AuthViewModel so both Firebase and SessionStorage are cleared atomically.
        authViewModel.signOut();
        redirectToLogin(view);
    }

    /**
     * Navigates to the login screen and pops the entire authenticated back stack.
     * This is used both for logout and for the auth guard when session is invalid.
     */
    private void redirectToLogin(@NonNull View view) {
        NavOptions navOptions = new NavOptions.Builder()
                // Pop everything up to and including the nav graph root so there is nothing to go back to.
                .setPopUpTo(R.id.nav_graph, true)
                .build();

        Navigation.findNavController(view)
                .navigate(R.id.action_homeFragment_to_loginFragment, null, navOptions);
    }
}
