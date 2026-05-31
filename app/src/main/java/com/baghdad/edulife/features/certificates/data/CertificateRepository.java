package com.baghdad.edulife.features.certificates.data;

import com.baghdad.edulife.core.network.ApiClient;
import com.baghdad.edulife.core.network.ApiService;
import com.baghdad.edulife.features.certificates.model.CertificateSummary;

import java.util.List;

import retrofit2.Callback;

public class CertificateRepository {

    private final ApiService apiService =
            ApiClient.getClient().create(ApiService.class);

    public void getCertificates(Callback<List<CertificateSummary>> callback) {
        apiService.getMyCertificates().enqueue(callback);
    }
}
