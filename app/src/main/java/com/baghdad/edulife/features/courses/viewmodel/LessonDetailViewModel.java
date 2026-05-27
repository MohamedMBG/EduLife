package com.baghdad.edulife.features.courses.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.baghdad.edulife.features.courses.data.CourseRepository;
import com.baghdad.edulife.features.courses.model.LessonDetail;
import com.baghdad.edulife.features.courses.model.LessonDetailUiState;

public class LessonDetailViewModel extends AndroidViewModel {

    private final CourseRepository courseRepository;
    private final MutableLiveData<LessonDetailUiState> uiState =
            new MutableLiveData<>(LessonDetailUiState.idle());

    public LessonDetailViewModel(@NonNull Application application) {
        super(application);
        this.courseRepository = new CourseRepository();
    }

    public LiveData<LessonDetailUiState> getUiState() {
        return uiState;
    }

    public void loadLessonDetail(String courseId, String lessonId) {
        uiState.setValue(LessonDetailUiState.loading());

        courseRepository.loadLessonDetail(courseId, lessonId, new CourseRepository.LessonDetailCallback() {
            @Override
            public void onSuccess(LessonDetail lessonDetail) {
                uiState.postValue(LessonDetailUiState.success(lessonDetail));
            }

            @Override
            public void onError(String message) {
                uiState.postValue(LessonDetailUiState.error(message));
            }
        });
    }

    public void markLessonComplete(String courseId, String lessonId, CompletionCallback callback) {
        courseRepository.markLessonComplete(courseId, lessonId, new CourseRepository.MarkLessonCompleteCallback() {
            @Override
            public void onSuccess() {
                callback.onSuccess();
            }

            @Override
            public void onForbidden() {
                callback.onForbidden();
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public interface CompletionCallback {
        void onSuccess();
        void onForbidden();
        void onError(String message);
    }
}
