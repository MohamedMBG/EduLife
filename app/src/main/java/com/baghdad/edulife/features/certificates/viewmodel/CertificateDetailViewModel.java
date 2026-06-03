package com.baghdad.edulife.features.certificates.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.baghdad.edulife.features.certificates.data.CertificateRepository;
import com.baghdad.edulife.features.certificates.model.CertificateDetail;

public class CertificateDetailViewModel extends ViewModel {

    private final CertificateRepository repository = new CertificateRepository();

    private final MutableLiveData<CertificateDetail> detail = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public LiveData<CertificateDetail> getDetail() { return detail; }
    public LiveData<Boolean> isLoading() { return loading; }
    public LiveData<String> getError() { return error; }

    public void load(String certId) {
        if (detail.getValue() != null) return;
        loading.setValue(true);
        error.setValue(null);
        repository.getCertificateById(certId, new CertificateRepository.CertificateDetailCallback() {
            @Override
            public void onSuccess(CertificateDetail cert) {
                loading.postValue(false);
                detail.postValue(cert);
            }

            @Override
            public void onError(String message) {
                loading.postValue(false);
                error.postValue(message);
            }
        });
    }
}
