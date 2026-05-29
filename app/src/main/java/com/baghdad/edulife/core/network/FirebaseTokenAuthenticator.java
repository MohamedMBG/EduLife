package com.baghdad.edulife.core.network;

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
    private static final Object TOKEN_REFRESH_LOCK = new Object();

    private final FirebaseAuth firebaseAuth;

    public FirebaseTokenAuthenticator() {
        this.firebaseAuth = FirebaseAuth.getInstance();
    }

    @Override
    public Request authenticate(Route route, Response response) {
        if (responseCount(response) > MAX_AUTH_RETRY_COUNT) {
            return null; // prevents infinite 401 retry loop
        }

        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user == null) {
            return null; // unauthenticated state
        }

        synchronized (TOKEN_REFRESH_LOCK) {
            try {
                String refreshedToken = Tasks.await(
                        user.getIdToken(true), // force refresh
                        10,
                        TimeUnit.SECONDS
                ).getToken();

                if (refreshedToken == null || refreshedToken.isBlank()) {
                    // Sign-out is a UI concern (it also has to clear SessionStorage and nav
                    // to login). Post to the bus so the host activity drives the teardown
                    // from the main thread instead of having the network layer do it on a
                    // transient blip from an OkHttp worker thread.
                    SessionEventBus.postSessionExpired();
                    return null;
                }

                return response.request()
                        .newBuilder()
                        .header("Authorization", "Bearer " + refreshedToken)
                        .build();

            } catch (Exception e) {
                SessionEventBus.postSessionExpired();
                return null;
            }
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