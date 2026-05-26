package com.baghdad.edulife.features.certificates.data;

import androidx.annotation.NonNull;

import com.baghdad.edulife.core.network.ApiClient;
import com.baghdad.edulife.core.network.ApiService;
import com.baghdad.edulife.features.certificates.model.CertificateDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CertificateRepository {

    private final ApiService apiService;

    public CertificateRepository() {
        this.apiService = ApiClient.getClient().create(ApiService.class);
    }

    public interface CertificatesCallback {
        void onSuccess(List<CertificateDto> certificates);
        void onError(String message);
    }

    public void loadMyCertificates(CertificatesCallback callback) {
        apiService.getMyCertificates().enqueue(new Callback<List<CertificateDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<CertificateDto>> call, @NonNull Response<List<CertificateDto>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Failed to load certificates. Status: " + response.code());
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<List<CertificateDto>> call, @NonNull Throwable t) {
                callback.onError("Network error: " + safeMessage(t));
            }
        });
    }

    private String safeMessage(Throwable t) {
        return t.getMessage() != null && !t.getMessage().isBlank() ? t.getMessage() : "Unknown error";
    }
}
