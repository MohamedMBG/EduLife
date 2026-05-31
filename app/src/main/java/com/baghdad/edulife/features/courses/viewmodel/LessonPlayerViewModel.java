package com.baghdad.edulife.features.courses.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.baghdad.edulife.features.courses.data.CourseRepository;
import com.baghdad.edulife.features.courses.model.LessonDetail;

/**
 * Owns the lesson player's network calls so the fragment never talks to ApiClient directly.
 * Single instance per fragment; survives configuration changes so the in-flight detail fetch
 * and the cached payload do not get wasted on rotation.
 */
public class LessonPlayerViewModel extends ViewModel {

    private final CourseRepository repository = new CourseRepository();

    private final MutableLiveData<LessonDetail> _detail = new MutableLiveData<>();
    public final LiveData<LessonDetail> detail = _detail;

    private final MutableLiveData<Boolean> _detailLoading = new MutableLiveData<>(false);
    public final LiveData<Boolean> detailLoading = _detailLoading;

    private final MutableLiveData<String> _detailError = new MutableLiveData<>();
    public final LiveData<String> detailError = _detailError;

    private final MutableLiveData<Boolean> _completed = new MutableLiveData<>(false);
    public final LiveData<Boolean> completed = _completed;

    private final MutableLiveData<CourseRepository.MarkCompleteFailure> _completionError =
            new MutableLiveData<>();
    public final LiveData<CourseRepository.MarkCompleteFailure> completionError = _completionError;

    private boolean fetchInFlight;
    private boolean markInFlight;

    public void loadLessonDetail(String courseId, String lessonId) {
        if (fetchInFlight) return;
        if (_detail.getValue() != null) return;
        fetchInFlight = true;
        _detailLoading.postValue(true);
        _detailError.postValue(null);

        repository.loadLessonDetail(courseId, lessonId, new CourseRepository.LessonDetailCallback() {
            @Override
            public void onSuccess(LessonDetail detail) {
                fetchInFlight = false;
                _detailLoading.postValue(false);
                _detail.postValue(detail);
            }

            @Override
            public void onError(String message) {
                fetchInFlight = false;
                _detailLoading.postValue(false);
                _detailError.postValue(message);
            }
        });
    }

    /**
     * Idempotent at the call level: a second mark-complete request is swallowed once the first
     * succeeds or while one is already in flight, so the lesson cannot be marked twice from a
     * single screen even if the user taps the button and closes the viewer in the same beat.
     */
    public void markComplete(String courseId, String lessonId) {
        if (Boolean.TRUE.equals(_completed.getValue())) return;
        if (markInFlight) return;
        markInFlight = true;

        repository.markLessonComplete(courseId, lessonId, new CourseRepository.MarkCompleteCallback() {
            @Override
            public void onSuccess() {
                markInFlight = false;
                _completed.postValue(true);
            }

            @Override
            public void onError(CourseRepository.MarkCompleteFailure reason) {
                markInFlight = false;
                _completionError.postValue(reason);
            }
        });
    }

    public void clearCompletionError() {
        _completionError.setValue(null);
    }
}
