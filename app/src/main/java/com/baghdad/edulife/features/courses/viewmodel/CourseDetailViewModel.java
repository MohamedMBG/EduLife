package com.baghdad.edulife.features.courses.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.baghdad.edulife.features.courses.data.CourseRepository;
import com.baghdad.edulife.features.courses.model.CourseDetail;
import com.baghdad.edulife.features.courses.model.CourseDetailUiState;

public class CourseDetailViewModel extends AndroidViewModel {

    private final CourseRepository courseRepository;
    private final MutableLiveData<CourseDetailUiState> uiState =
            new MutableLiveData<>(CourseDetailUiState.idle());

    public CourseDetailViewModel(@NonNull Application application) {
        super(application);
        this.courseRepository = new CourseRepository();
    }

    public LiveData<CourseDetailUiState> getUiState() {
        return uiState;
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
