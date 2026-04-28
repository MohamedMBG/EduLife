package com.baghdad.edulife.core.network;

import com.baghdad.edulife.features.auth.model.AuthSyncResponse;

import retrofit2.Call;
import retrofit2.http.POST;

/**
 * Retrofit interface defining all EduLife backend API endpoints.
 *
 * The Firebase Bearer token is injected automatically by FirebaseAuthInterceptor.
 * Do not add manual Authorization headers here.
 */
public interface ApiService {

    /**
     * Syncs the authenticated Firebase user with the EduLife backend.
     * Returns the internal userId and role for local session storage.
     *
     * Endpoint: POST /api/v1/auth/sync
     * Authorization: Bearer <Firebase ID token>  (added by FirebaseAuthInterceptor)
     */
    @POST("auth/sync")
    Call<AuthSyncResponse> syncUser();
}

