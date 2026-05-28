package com.baghdad.edulife.core.network;

import com.baghdad.edulife.BuildConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static final String BASE_URL = BuildConfig.API_BASE_URL;

    private static Retrofit retrofit;

    private ApiClient() {
        // Prevent instantiation
    }

    public static Retrofit getClient() {
        if (retrofit == null) {

            // Body-level logging leaks profile PII and request bodies into logcat, so it must
            // never be enabled in release builds. BASIC keeps the URL + status line so release
            // crash reports still carry useful diagnostic context.
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.redactHeader("Authorization");
            loggingInterceptor.setLevel(BuildConfig.DEBUG
                    ? HttpLoggingInterceptor.Level.BODY
                    : HttpLoggingInterceptor.Level.BASIC);

            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(new FirebaseAuthInterceptor()) // attaches Bearer token
                    .addInterceptor(loggingInterceptor)
                    // Fail fast when the backend is unreachable so the login screen can recover
                    // instead of leaving the learner stuck in loading during /auth/sync. The
                    // call timeout caps total round-trip time, including token refresh.
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .callTimeout(25, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .authenticator(new FirebaseTokenAuthenticator())
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }

        return retrofit;
    }
}
