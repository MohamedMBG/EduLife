package com.baghdad.edulife.core.network;

import com.baghdad.edulife.BuildConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static final String BASE_URL = BuildConfig.API_BASE_URL;

    private static OkHttpClient okHttpClient;
    private static Retrofit retrofit;

    private ApiClient() {
        // Prevent instantiation
    }

    public static synchronized Retrofit getClient() {
        ensureInitialized();
        return retrofit;
    }

    /**
     * Returns the same authenticated OkHttp client Retrofit uses. Exposed so feature code
     * (certificate / lesson resource downloads) can stream large response bodies through the
     * exact same pipeline — including FirebaseAuthInterceptor's Bearer header and
     * FirebaseTokenAuthenticator's one-shot 401 refresh.
     *
     * Important: this client unconditionally attaches the learner's Firebase ID token. Callers
     * MUST only hand it URLs on the EduLife backend host. Third-party URLs should go through a
     * separate, un-authenticated OkHttp instance to avoid leaking the token.
     */
    public static synchronized OkHttpClient authenticatedClient() {
        ensureInitialized();
        return okHttpClient;
    }

    private static void ensureInitialized() {
        if (retrofit != null) return;

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .addInterceptor(new FirebaseAuthInterceptor()) // attaches Bearer token
                // Fail fast when the backend is unreachable so the login screen can recover
                // instead of leaving the learner stuck in loading during /auth/sync. The
                // call timeout caps total round-trip time, including token refresh.
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .callTimeout(25, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .authenticator(new FirebaseTokenAuthenticator());

        if (BuildConfig.DEBUG) {
            // Debug logs help wire the backend during MVP work, but release builds attach no
            // network logger so endpoint metadata and learner identifiers cannot leak to logcat.
            // HEADERS (not BODY) so even debug logcat never carries response bodies with PII —
            // emails, names, internal userIds, exam payloads (audit 2026-06-19 P3-4). Authorization
            // is additionally redacted in case the level is ever raised back to BODY for debugging.
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.redactHeader("Authorization");
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.HEADERS);
            clientBuilder.addInterceptor(loggingInterceptor);
        }

        okHttpClient = clientBuilder.build();

        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }
}
