package com.baghdad.edulife;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.fragment.NavHostFragment;

import com.baghdad.edulife.core.storage.SessionStorage;
import com.baghdad.edulife.features.onboarding.data.OnboardingPreferences;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Avoid resetting the user's current screen during configuration changes.
        if (savedInstanceState == null) {
            configureNavigationStartDestination();
        }
    }

    private void configureNavigationStartDestination() {
        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.main);

        if (navHostFragment == null) {
            return;
        }

        NavController navController = navHostFragment.getNavController();
        NavGraph navGraph = navController.getNavInflater().inflate(R.navigation.nav_graph);
        OnboardingPreferences onboardingPreferences = new OnboardingPreferences(this);
        SessionStorage sessionStorage = new SessionStorage(this);
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        // Route to home if there is an active Firebase user AND a persisted backend session.
        // This avoids forcing a re-login on every app relaunch when the user is still authenticated.
        // HomeFragment will repeat the same guard check and redirect to login if the session is stale.
        if (firebaseUser != null && sessionStorage.hasSession()) {
            navGraph.setStartDestination(R.id.homeFragment);
        } else if (onboardingPreferences.hasSeenOnboarding()) {
            // Onboarding was completed but no active session: go directly to login.
            navGraph.setStartDestination(R.id.loginFragment);
        } else {
            // First-time launch: show onboarding.
            navGraph.setStartDestination(R.id.onboardingFragment);
        }

        navController.setGraph(navGraph);
    }
}
