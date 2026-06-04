package com.baghdad.edulife.features.certificates.data;

import androidx.annotation.NonNull;

import com.baghdad.edulife.core.network.ApiClient;
import com.baghdad.edulife.core.network.ApiService;
import com.baghdad.edulife.features.certificates.model.CertificateDetail;
import com.baghdad.edulife.features.certificates.model.CertificateSummary;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CertificateRepository {

    private final ApiService apiService =
            ApiClient.getClient().create(ApiService.class);

    public void getCertificates(Callback<List<CertificateSummary>> callback) {
        apiService.getMyCertificates().enqueue(callback);
    }

    public interface CertificateDetailCallback {
        void onSuccess(CertificateDetail detail);
        void onError(String message);
    }

    public void getCertificateById(String id, CertificateDetailCallback callback) {
        apiService.getCertificateById(id).enqueue(new Callback<CertificateDetail>() {
            @Override
            public void onResponse(@NonNull Call<CertificateDetail> call,
                                   @NonNull Response<CertificateDetail> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Could not load certificate. Status: " + response.code());
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<CertificateDetail> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }
}
