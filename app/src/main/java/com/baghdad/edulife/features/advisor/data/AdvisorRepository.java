package com.baghdad.edulife.features.advisor.data;

import androidx.annotation.NonNull;

import com.baghdad.edulife.core.network.ApiClient;
import com.baghdad.edulife.core.network.ApiService;
import com.baghdad.edulife.features.advisor.model.AdvisorRequest;
import com.baghdad.edulife.features.advisor.model.AdvisorResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdvisorRepository {

    public interface AdvisorCallback {
        void onSuccess(AdvisorResponse response);
        void onRateLimit();
        void onError(String message);
    }

    private final ApiService apiService;

    public AdvisorRepository() {
        this.apiService = ApiClient.getClient().create(ApiService.class);
    }

    public void recommend(String goal, AdvisorCallback callback) {
        apiService.requestAdvisorRecommendation(new AdvisorRequest(goal))
                .enqueue(new Callback<AdvisorResponse>() {
                    @Override
                    public void onResponse(
                            @NonNull Call<AdvisorResponse> call,
                            @NonNull Response<AdvisorResponse> response
                    ) {
                        if (response.code() == 429) {
                            callback.onRateLimit();
                            return;
                        }
                        if (!response.isSuccessful() || response.body() == null) {
                            callback.onError("Advisor unavailable. Status: " + response.code());
                            return;
                        }
                        callback.onSuccess(response.body());
                    }

                    @Override
                    public void onFailure(@NonNull Call<AdvisorResponse> call, @NonNull Throwable t) {
                        callback.onError("Network error. Check your connection and try again.");
                    }
                });
    }
}
