package com.baghdad.edulife.core.network;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static final String BASE_URL = "http://10.0.2.2:8080/api/v1/";
    // 10.0.2.2 = localhost for Android emulator

    private static Retrofit retrofit;

    private ApiClient() {
        // Prevent instantiation
    }

    public static Retrofit getClient() {
        if (retrofit == null) {

            // Logging interceptor (for debugging)
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            // OkHttp client with Firebase interceptor
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(new FirebaseAuthInterceptor()) // attaches Bearer token
                    .addInterceptor(loggingInterceptor)            // optional (debug only)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
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