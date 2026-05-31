package com.baghdad.edulife.core.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.baghdad.edulife.core.session.SessionEventBus;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.TimeUnit;

import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

public class FirebaseTokenAuthenticator implements Authenticator {

    private static final int MAX_AUTH_RETRY_COUNT = 1;
    // Forced token refresh is a real network round-trip; allow more headroom than the cached
    // path in FirebaseAuthInterceptor but still keep it bounded so OkHttp does not stall.
    private static final long TOKEN_REFRESH_TIMEOUT_SECONDS = 8;
    private static final Object TOKEN_REFRESH_LOCK = new Object();

    private final FirebaseAuth firebaseAuth;

    public FirebaseTokenAuthenticator() {
        this.firebaseAuth = FirebaseAuth.getInstance();
    }

    @Nullable
    @Override
    public Request authenticate(@Nullable Route route, @NonNull Response response) {
        if (responseCount(response) > MAX_AUTH_RETRY_COUNT) {
            return null; // prevents infinite 401 retry loop
        }

        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user == null) {
            return null; // unauthenticated state — 401 surfaces to the caller
        }

        // Serialize forced refreshes so a burst of parallel 401s does not trigger N concurrent
        // Firebase round-trips for the same user.
        synchronized (TOKEN_REFRESH_LOCK) {
            String refreshedToken;
            try {
                refreshedToken = Tasks.await(
                        user.getIdToken(true),
                        TOKEN_REFRESH_TIMEOUT_SECONDS,
                        TimeUnit.SECONDS
                ).getToken();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            } catch (Exception e) {
                SessionEventBus.postSessionExpired();
                return null;
            }

            if (refreshedToken == null || refreshedToken.isBlank()) {
                // Sign-out is a UI concern — post to the bus so the host activity drives
                // teardown from the main thread instead of the OkHttp worker thread.
                SessionEventBus.postSessionExpired();
                return null;
            }

            return response.request()
                    .newBuilder()
                    .header("Authorization", "Bearer " + refreshedToken)
                    .build();
        }
    }

    private int responseCount(Response response) {
        int count = 1;

        while ((response = response.priorResponse()) != null) {
            count++;
        }

        return count;
    }
}
