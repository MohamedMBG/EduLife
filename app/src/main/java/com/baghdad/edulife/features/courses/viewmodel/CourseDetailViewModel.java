package com.baghdad.edulife.features.courses.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.baghdad.edulife.features.courses.data.CourseRepository;
import com.baghdad.edulife.features.courses.model.CourseDetail;
import com.baghdad.edulife.features.courses.model.CourseDetailUiState;
import com.baghdad.edulife.features.courses.model.CourseProgressSummary;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CourseDetailViewModel extends AndroidViewModel {

    private final CourseRepository courseRepository;
    private final MutableLiveData<CourseDetailUiState> uiState =
            new MutableLiveData<>(CourseDetailUiState.idle());
    private final MutableLiveData<Map<String, Boolean>> lessonCompletionState =
            new MutableLiveData<>(new LinkedHashMap<>());
    // Aggregate completed/total/percent for the course-learning progress card. Kept separate
    // from the flattened per-lesson map so the header can render even before sections bind.
    private final MutableLiveData<CourseProgressSummary> progressSummary =
            new MutableLiveData<>();
    // True when the last progress fetch failed (vs. simply not loaded), so the card can show a
    // retry affordance instead of silently vanishing.
    private final MutableLiveData<Boolean> progressError = new MutableLiveData<>(false);

    public CourseDetailViewModel(@NonNull Application application) {
        super(application);
        this.courseRepository = new CourseRepository();
    }

    public LiveData<CourseDetailUiState> getUiState() {
        return uiState;
    }

    public LiveData<Map<String, Boolean>> getLessonCompletionState() {
        return lessonCompletionState;
    }

    public LiveData<CourseProgressSummary> getProgressSummary() {
        return progressSummary;
    }

    public LiveData<Boolean> getProgressError() {
        return progressError;
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

    public void loadLessonCompletion(String courseId) {
        courseRepository.getCourseProgress(courseId, new CourseRepository.CourseProgressCallback() {
            @Override
            public void onSuccess(CourseProgressSummary progress) {
                // Course detail needs only the per-lesson completed flag, so the fragment receives
                // a normalized lessonId -> completed map instead of parsing nested section DTOs.
                lessonCompletionState.postValue(flattenCompletionMap(progress));
                progressSummary.postValue(progress);
                progressError.postValue(false);
            }

            @Override
            public void onError(String message) {
                // Completion indicators are additive UX. A failure here must not block the
                // underlying course detail screen or hide otherwise accessible lessons.
                lessonCompletionState.postValue(new LinkedHashMap<>());
                progressSummary.postValue(null);
                progressError.postValue(true);
            }
        });
    }

    private Map<String, Boolean> flattenCompletionMap(CourseProgressSummary progress) {
        Map<String, Boolean> completionMap = new LinkedHashMap<>();
        if (progress == null || progress.sections == null) {
            return completionMap;
        }

        for (CourseProgressSummary.SectionProgressSummary section : progress.sections) {
            List<CourseProgressSummary.LessonProgressSummary> lessons =
                    section != null ? section.lessons : null;
            if (lessons == null) {
                continue;
            }
            for (CourseProgressSummary.LessonProgressSummary lesson : lessons) {
                if (lesson != null && lesson.lessonId != null && !lesson.lessonId.isBlank()) {
                    completionMap.put(lesson.lessonId, lesson.completed);
                }
            }
        }
        return completionMap;
    }
}
