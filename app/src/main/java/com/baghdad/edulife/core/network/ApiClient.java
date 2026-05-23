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

            // Logging interceptor (for debugging)
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            // Firebase ID tokens must never be printed to logcat because log files are often shared during debugging.
            loggingInterceptor.redactHeader("Authorization");
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            // OkHttp client with Firebase interceptor
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(new FirebaseAuthInterceptor()) // attaches Bearer token
                    .addInterceptor(loggingInterceptor)            // optional (debug only)
                    // Fail fast when the backend IP is unreachable so the login screen can recover instead of
                    // leaving the learner stuck in loading during /auth/sync.
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .callTimeout(35, TimeUnit.SECONDS)
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
