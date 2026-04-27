package com.baghdad.edulife;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;

/**
 * Application entry point for EduLife.
 *
 * Firebase auto-initializes via FirebaseInitProvider before onCreate, so no explicit
 * FirebaseApp.initializeApp() call is required. This class exists as a stable hook
 * for future Sprint 0 wiring: ApiClient base URL injection, shared preferences init,
 * and any other app-scoped singletons.
 */
public class EduLifeApp extends Application {

    private static final String TAG = "EduLifeApp";

    @Override
    public void onCreate() {
        super.onCreate();

        // Confirm Firebase initialized correctly so crashes at auth call-sites are
        // caught at startup rather than silently failing on first auth attempt.
        if (FirebaseApp.getApps(this).isEmpty()) {
            Log.e(TAG, "Firebase did not initialize. Verify google-services.json is present at app/google-services.json.");
        } else {
            Log.d(TAG, "Firebase initialized successfully.");
        }
    }
}
