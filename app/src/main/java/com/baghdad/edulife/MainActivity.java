package com.baghdad.edulife;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.baghdad.edulife.core.session.SessionEventBus;
import com.baghdad.edulife.core.storage.SessionStorage;
import com.baghdad.edulife.features.onboarding.data.OnboardingPreferences;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import kotlin.Unit;
import me.ibrahimsn.lib.SmoothBottomBar;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        SmoothBottomBar bottomNav = findViewById(R.id.bottomNavView);
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

        int[] tabDestinations = {R.id.homeFragment, R.id.coursesFragment, R.id.profileFragment};
        NavOptions tabOptions = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setPopUpTo(R.id.homeFragment, false, true)
                .build();

        bottomNav.setOnItemSelected((kotlin.jvm.functions.Function1<Integer, Unit>) position -> {
            if (position >= 0 && position < tabDestinations.length) {
                navController.navigate(tabDestinations[position], null, tabOptions);
            }
            return Unit.INSTANCE;
        });

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int id = destination.getId();
            boolean isMainTab = id == R.id.homeFragment
                    || id == R.id.coursesFragment
                    || id == R.id.profileFragment;
            bottomNav.setVisibility(isMainTab ? View.VISIBLE : View.GONE);
            if (id == R.id.homeFragment)       bottomNav.setItemActiveIndex(0);
            else if (id == R.id.coursesFragment) bottomNav.setItemActiveIndex(1);
            else if (id == R.id.profileFragment) bottomNav.setItemActiveIndex(2);
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
            navGraph.setStartDestination(R.id.homeFragment);
        } else if (onboardingPreferences.hasSeenOnboarding()) {
            navGraph.setStartDestination(R.id.loginFragment);
        } else {
            navGraph.setStartDestination(R.id.onboardingFragment);
        }

        navController.setGraph(navGraph);
    }
}
