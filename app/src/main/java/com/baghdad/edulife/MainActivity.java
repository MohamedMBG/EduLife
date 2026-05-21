package com.baghdad.edulife;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

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

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavView);
        View mainContainer = findViewById(R.id.mainContainer);
        View navHostView = findViewById(R.id.main);

        ViewCompat.setOnApplyWindowInsetsListener(mainContainer, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            navHostView.setPadding(bars.left, bars.top, bars.right, 0);
            bottomNav.setPadding(bars.left, 0, bars.right, bars.bottom);
            return insets;
        });

        if (savedInstanceState == null) {
            configureNavigationStartDestination();
        }

        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.main);
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(bottomNav, navController);

            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int id = destination.getId();
                boolean isMainTab = id == R.id.homeFragment
                        || id == R.id.coursesFragment
                        || id == R.id.profileFragment;
                bottomNav.setVisibility(isMainTab ? View.VISIBLE : View.GONE);
            });
        }
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

        // Start inside the authenticated shell only when Firebase and backend sync state both exist.
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
