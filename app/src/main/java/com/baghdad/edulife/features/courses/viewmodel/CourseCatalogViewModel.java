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
        // Show the seeded fallback catalog immediately so the home screen is never blank
        // while OkHttp waits up to 35s for an unreachable backend.
        uiState.setValue(CourseCatalogUiState.success(
                CourseRepository.fallbackCourses(category), category));

        // The first Android catalog slice intentionally keeps paging fixed to page 0 so
        // the UI can prove the backend contract before adding infinite scroll complexity.
        courseRepository.loadCourses(category, 0, new CourseRepository.CourseCatalogCallback() {
            @Override
            public void onSuccess(List<CourseSummary> courses) {
                uiState.postValue(CourseCatalogUiState.success(courses, category));
            }

            @Override
            public void onError(String message) {
                // Repository already falls back internally, but keep the catalog populated
                // in case a future error path bypasses that fallback.
                uiState.postValue(CourseCatalogUiState.success(
                        CourseRepository.fallbackCourses(category), category));
            }
        });
    }
}
