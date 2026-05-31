package com.baghdad.edulife.features.courses.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.baghdad.edulife.features.courses.data.CourseRepository;
import com.baghdad.edulife.features.courses.model.CourseCatalogUiState;
import com.baghdad.edulife.features.courses.model.CourseSummary;

import java.util.List;

public class CourseCatalogViewModel extends AndroidViewModel {

    private final CourseRepository courseRepository;
    private final MutableLiveData<CourseCatalogUiState> uiState =
            new MutableLiveData<>(CourseCatalogUiState.idle());

    public CourseCatalogViewModel(@NonNull Application application) {
        super(application);
        this.courseRepository = new CourseRepository();
    }

    public LiveData<CourseCatalogUiState> getUiState() {
        return uiState;
    }

    public void loadCourses(String category) {
        // Emit a true loading state instead of optimistically swapping in the seeded fallback.
        // The previous behaviour made the empty / loaded / errored states visually identical
        // and hid real fetch failures behind a stale fallback list.
        uiState.setValue(CourseCatalogUiState.loading(category));

        // The first Android catalog slice intentionally keeps paging fixed to page 0 so the UI
        // can prove the backend contract before adding infinite scroll complexity.
        courseRepository.loadCourses(category, 0, new CourseRepository.CourseCatalogCallback() {
            @Override
            public void onSuccess(List<CourseSummary> courses) {
                uiState.postValue(CourseCatalogUiState.success(courses, category));
            }

            @Override
            public void onError(String message) {
                // Real errors now surface to the UI. HomeFragment renders the error message and
                // the existing Retry button kicks off a re-fetch. The seeded fallback remains
                // accessible via CourseRepository.fallbackCourses() for the explicit
                // offline-dev path but is no longer auto-applied here.
                uiState.postValue(CourseCatalogUiState.error(message, category));
            }
        });
    }
}
