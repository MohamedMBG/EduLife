package com.baghdad.edulife.features.certificates.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.baghdad.edulife.features.certificates.data.CertificateRepository;
import com.baghdad.edulife.features.certificates.model.CertificateSummary;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CertificateViewModel extends ViewModel {

    private final CertificateRepository repository = new CertificateRepository();

    private final MutableLiveData<List<CertificateSummary>> certificates = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public LiveData<List<CertificateSummary>> getCertificates() { return certificates; }
    public LiveData<Boolean> isLoading() { return loading; }
    public LiveData<String> getError() { return error; }

    public void load() {
        loading.setValue(true);
        error.setValue(null);
        repository.getCertificates(new Callback<>() {
            @Override
            public void onResponse(Call<List<CertificateSummary>> call,
                                   Response<List<CertificateSummary>> response) {
                loading.postValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    certificates.postValue(response.body());
                } else {
                    error.postValue("Could not load certificates.");
                }
            }

            @Override
            public void onFailure(Call<List<CertificateSummary>> call, Throwable t) {
                loading.postValue(false);
                error.postValue("Network error. Check your connection.");
            }
        });
    }
}
