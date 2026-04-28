package com.baghdad.edulife.core.network;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class FirebaseAuthInterceptor implements Interceptor {

    private final FirebaseAuth firebaseAuth;

    public FirebaseAuthInterceptor() {
        this.firebaseAuth = FirebaseAuth.getInstance();
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            return chain.proceed(originalRequest);
        }

        try {
            String token = Tasks.await(
                    currentUser.getIdToken(false),
                    10,
                    TimeUnit.SECONDS
            ).getToken();

            if (token == null || token.isBlank()) {
                return chain.proceed(originalRequest);
            }

            Request authenticatedRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer " + token)
                    .build();

            return chain.proceed(authenticatedRequest);

        } catch (Exception e) {
            return chain.proceed(originalRequest);
        }
    }
}