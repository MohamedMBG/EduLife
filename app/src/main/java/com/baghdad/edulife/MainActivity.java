package com.baghdad.edulife;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.baghdad.edulife.core.session.SessionEventBus;
import com.baghdad.edulife.core.storage.SessionStorage;
import com.baghdad.edulife.features.onboarding.data.OnboardingPreferences;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        insetsController.hide(WindowInsetsCompat.Type.systemBars());
        insetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavView);
        View mainContainer = findViewById(R.id.mainContainer);
        View navHostView = findViewById(R.id.main);

        ViewCompat.setOnApplyWindowInsetsListener(mainContainer, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // no top padding — fragments handle status bar insets themselves
            navHostView.setPadding(bars.left, 0, bars.right, 0);
            bottomNav.setPadding(bars.left, 0, bars.right, bars.bottom);
            return insets; // propagate so fitsSystemWindows fragments receive insets
        });

        if (savedInstanceState == null) {
            configureNavigationStartDestination();
        }

        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.main);
        if (navHostFragment == null) return;

        NavController navController = navHostFragment.getNavController();

        // The bottom-nav menu item ids ARE the nav-graph destination ids (see menu/bottom_nav_menu.xml),
        // so a selected item maps straight onto navigate() with no index table.
        NavOptions tabOptions = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setPopUpTo(R.id.homeFragment, false, true)
                .build();

        bottomNav.setOnItemSelectedListener(item -> {
            // Guard against re-navigating to the tab we're already on. This also makes the
            // setSelectedItemId() sync below a no-op instead of a feedback loop, because by the
            // time onDestinationChanged fires the current destination already equals the item id.
            if (navController.getCurrentDestination() != null
                    && navController.getCurrentDestination().getId() == item.getItemId()) {
                return true;
            }
            navController.navigate(item.getItemId(), null, tabOptions);
            return true;
        });

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int id = destination.getId();
            boolean isMainTab = id == R.id.homeFragment
                    || id == R.id.coursesFragment
                    || id == R.id.plannerFragment
                    || id == R.id.gamificationFragment
                    || id == R.id.profileFragment;
            bottomNav.setVisibility(isMainTab ? View.VISIBLE : View.GONE);
            if (isMainTab && bottomNav.getSelectedItemId() != id) {
                bottomNav.setSelectedItemId(id);
            }
        });

        observeSessionExpiry(navController);
    }

    /**
     * Drains the SessionEventBus and runs sign-out + nav from a single point so transient
     * network failures inside OkHttp no longer log the learner out from multiple call sites.
     */
    private void observeSessionExpiry(NavController navController) {
        SessionEventBus.sessionExpired().observe(this, expired -> {
            if (!Boolean.TRUE.equals(expired)) return;

            FirebaseAuth.getInstance().signOut();
            new SessionStorage(this).clearSession();

            Toast.makeText(this,
                    R.string.session_expired_message, Toast.LENGTH_LONG).show();

            NavOptions toLogin = new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph, true)
                    .build();
            navController.navigate(R.id.loginFragment, null, toLogin);

            // Consume the signal so a re-attached observer (e.g. after rotation) cannot
            // re-fire sign-out + nav.
            SessionEventBus.clear();
        });
    }

    private void configureNavigationStartDestination() {
        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.main);

        if (navHostFragment == null) return;

        NavController navController = navHostFragment.getNavController();
        NavGraph navGraph = navController.getNavInflater().inflate(R.navigation.nav_graph);
        OnboardingPreferences onboardingPreferences = new OnboardingPreferences(this);
        SessionStorage sessionStorage = new SessionStorage(this);
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if (firebaseUser != null && sessionStorage.hasSession()) {
            // Route relaunches by role so teachers/admins/group-admins are not dropped into the
            // learner home (LoginFragment routes the same way for the first sign-in of a session).
            navGraph.setStartDestination(startDestinationForRole(sessionStorage.getRole()));
        } else if (onboardingPreferences.hasSeenOnboarding()) {
            navGraph.setStartDestination(R.id.loginFragment);
        } else {
            navGraph.setStartDestination(R.id.onboardingFragment);
        }

        navController.setGraph(navGraph);
    }

    /** Maps the stored role to its home destination; unknown roles fall back to the learner flow. */
    private int startDestinationForRole(String role) {
        if ("ADMIN".equals(role)) return R.id.adminDashboardFragment;
        if ("TEACHER".equals(role)) return R.id.teacherDashboardFragment;
        if ("GROUP_ADMIN".equals(role)) return R.id.groupAdminDashboardFragment;
        return R.id.homeFragment;
    }
}
