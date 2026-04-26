package com.baghdad.edulife.features.onboarding.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.onboarding.model.OnboardingItem;

import java.util.Arrays;
import java.util.List;

public class OnboardingViewModel extends AndroidViewModel {

    private final List<OnboardingItem> items;
    private final MutableLiveData<Integer> currentPage = new MutableLiveData<>(0);

    public OnboardingViewModel(@NonNull Application application) {
        super(application);
        items = buildItems(application);
    }

    private List<OnboardingItem> buildItems(Application app) {
        return Arrays.asList(
                new OnboardingItem(
                        app.getString(R.string.onboarding_title),
                        app.getString(R.string.onboarding_subtitle),
                        app.getString(R.string.onboarding_accent_purpose),
                        R.drawable.ic_onboarding_discover,
                        R.color.onboarding_page1_bg),
                new OnboardingItem(
                        app.getString(R.string.onboarding_structure_title),
                        app.getString(R.string.onboarding_structure_subtitle),
                        app.getString(R.string.onboarding_accent_journey),
                        R.drawable.ic_onboarding_journey,
                        R.color.onboarding_page2_bg),
                new OnboardingItem(
                        app.getString(R.string.onboarding_certificate_title),
                        app.getString(R.string.onboarding_certificate_subtitle),
                        app.getString(R.string.onboarding_accent_certificate),
                        R.drawable.ic_onboarding_certificate,
                        R.color.onboarding_page3_bg)
        );
    }

    public List<OnboardingItem> getItems() { return items; }
    public LiveData<Integer> getCurrentPage() { return currentPage; }

    public void setCurrentPage(int page) { currentPage.setValue(page); }

    public int getPageCount() { return items.size(); }

    public boolean isLastPage() {
        Integer page = currentPage.getValue();
        return page != null && page == items.size() - 1;
    }
}
