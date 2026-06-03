package com.baghdad.edulife.features.courses.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.baghdad.edulife.features.courses.data.CourseRepository;
import com.baghdad.edulife.features.courses.model.CourseDetail;
import com.baghdad.edulife.features.courses.model.CourseDetailUiState;
import com.baghdad.edulife.features.courses.model.CourseProgressResponse;

public class CourseDetailViewModel extends AndroidViewModel {

    private final CourseRepository courseRepository;
    private final MutableLiveData<CourseDetailUiState> uiState =
            new MutableLiveData<>(CourseDetailUiState.idle());
    private final MutableLiveData<CourseProgressResponse> progressLiveData = new MutableLiveData<>();

    public CourseDetailViewModel(@NonNull Application application) {
        super(application);
        this.courseRepository = new CourseRepository();
    }

    public LiveData<CourseDetailUiState> getUiState() {
        return uiState;
    }

    public LiveData<CourseProgressResponse> getProgress() {
        return progressLiveData;
    }

    public void loadProgress(String courseId) {
        if (progressLiveData.getValue() != null) return;
        courseRepository.getCourseProgress(courseId, new CourseRepository.CourseProgressCallback() {
            @Override
            public void onSuccess(CourseProgressResponse response) {
                progressLiveData.postValue(response);
            }

            @Override
            public void onError(String message) {
                // Silent — progress is additive, not blocking
            }
        });
    }

    public void loadCourseDetail(String courseId) {
        uiState.setValue(CourseDetailUiState.loading());

        courseRepository.loadCourseDetail(courseId, new CourseRepository.CourseDetailCallback() {
            @Override
            public void onSuccess(CourseDetail courseDetail) {
                uiState.postValue(CourseDetailUiState.success(courseDetail));
            }

            @Override
            public void onError(String message) {
                uiState.postValue(CourseDetailUiState.error(message));
            }
        });
    }
}
