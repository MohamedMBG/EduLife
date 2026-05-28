package com.baghdad.edulife.features.courses.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.baghdad.edulife.features.courses.data.CourseRepository;
import com.baghdad.edulife.features.courses.model.CourseProgress;
import com.baghdad.edulife.features.courses.model.CourseProgressUiState;

public class CourseProgressViewModel extends AndroidViewModel {

    private final CourseRepository courseRepository;
    private final MutableLiveData<CourseProgressUiState> uiState =
            new MutableLiveData<>(CourseProgressUiState.idle());

    public CourseProgressViewModel(@NonNull Application application) {
        super(application);
        this.courseRepository = new CourseRepository();
    }

    public LiveData<CourseProgressUiState> getUiState() {
        return uiState;
    }

    public void loadCourseProgress(String courseId) {
        uiState.setValue(CourseProgressUiState.loading());

        courseRepository.loadCourseProgress(courseId, new CourseRepository.CourseProgressCallback() {
            @Override
            public void onSuccess(CourseProgress progress) {
                uiState.postValue(CourseProgressUiState.success(progress));
            }

            @Override
            public void onError(String message) {
                uiState.postValue(CourseProgressUiState.error(message));
            }
        });
    }
}
