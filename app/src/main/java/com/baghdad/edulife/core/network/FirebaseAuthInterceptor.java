package com.baghdad.edulife.core.network;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * OkHttp interceptor that attaches the current Firebase ID token as a Bearer header.
 *
 * <p>Uses the cached token when still valid; skips injection if the request already
 * carries an Authorization header (e.g. /auth/sync passes its own fresh token).
 * Token fetch failures are surfaced as {@link java.io.IOException} so callers see
 * a network error rather than a misleading 401.
 */
public class FirebaseAuthInterceptor implements Interceptor {

    // getIdToken(false) returns the cached token instantly when it is still valid; the timeout
    // only matters when Firebase needs a network round-trip. Keep it short so a stalled fetch
    // fails fast instead of holding an OkHttp dispatcher thread for 10s.
    private static final long TOKEN_TIMEOUT_SECONDS = 5;

    private final FirebaseAuth firebaseAuth;

    public FirebaseAuthInterceptor() {
        this.firebaseAuth = FirebaseAuth.getInstance();
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();

        if (originalRequest.header("Authorization") != null) {
            // /auth/sync can supply the freshly fetched Firebase token directly from the login
            // flow. Respect that header so the interceptor does not trigger a second token fetch
            // and accidentally turn a Firebase latency issue into a fake backend outage.
            return chain.proceed(originalRequest);
        }

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            // No signed-in user; let the request proceed without a Bearer header so the
            // server returns a clean 401 rather than the client masking the state.
            return chain.proceed(originalRequest);
        }

        String token;
        try {
            token = Tasks.await(
                    currentUser.getIdToken(false),
                    TOKEN_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
            ).getToken();
        } catch (TimeoutException e) {
            // Hard failures must surface as IOException so onFailure paths show a real network
            // error instead of a misleading 401 from an unauthenticated retry.
            throw new IOException("Firebase token fetch timed out", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Firebase token fetch interrupted", e);
        } catch (Exception e) {
            throw new IOException("Firebase token fetch failed", e);
        }

        if (token == null || token.isBlank()) {
            // Rare race: signed-in user but no token. Proceed without the header so the server
            // returns 401 and FirebaseTokenAuthenticator drives a forced refresh exactly once.
            return chain.proceed(originalRequest);
        }

        Request authenticatedRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer " + token)
                .build();

        return chain.proceed(authenticatedRequest);
    }
}
