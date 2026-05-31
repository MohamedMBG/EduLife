package com.baghdad.edulife.features.courses.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.baghdad.edulife.features.courses.data.CourseRepository;
import com.baghdad.edulife.features.courses.model.EnrolledCourse;
import com.baghdad.edulife.features.courses.model.CourseProgressSummary;
import com.baghdad.edulife.features.courses.model.EnrollmentResponse;
import com.baghdad.edulife.features.courses.model.EnrollUiState;
import com.baghdad.edulife.features.courses.model.UnenrollUiState;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EnrollmentViewModel extends AndroidViewModel {

    private final CourseRepository courseRepository;

    private final MutableLiveData<EnrollUiState> enrollState =
            new MutableLiveData<>(EnrollUiState.idle());

    private final MutableLiveData<UnenrollUiState> unenrollState =
            new MutableLiveData<>(UnenrollUiState.idle());

    private final MutableLiveData<List<EnrolledCourse>> myEnrollments =
            new MutableLiveData<>();

    /**
     * Separate signal so a failed enrollment fetch doesn't masquerade as "you have no
     * courses". CoursesFragment renders the empty list and this error independently.
     */
    private final MutableLiveData<String> myEnrollmentsError = new MutableLiveData<>();

    private final MutableLiveData<Boolean> myEnrollmentsLoading = new MutableLiveData<>(false);

    private final MutableLiveData<Map<String, CourseProgressSummary>> myCourseProgress =
            new MutableLiveData<>(new LinkedHashMap<>());

    private final MutableLiveData<Set<String>> myCourseProgressFailedIds =
            new MutableLiveData<>(new LinkedHashSet<>());

    public EnrollmentViewModel(@NonNull Application application) {
        super(application);
        this.courseRepository = new CourseRepository();
    }

    public LiveData<EnrollUiState> getEnrollState() {
        return enrollState;
    }

    public LiveData<UnenrollUiState> getUnenrollState() {
        return unenrollState;
    }

    public LiveData<List<EnrolledCourse>> getMyEnrollments() {
        return myEnrollments;
    }

    public LiveData<String> getMyEnrollmentsError() {
        return myEnrollmentsError;
    }

    public LiveData<Boolean> getMyEnrollmentsLoading() {
        return myEnrollmentsLoading;
    }

    public LiveData<Map<String, CourseProgressSummary>> getMyCourseProgress() {
        return myCourseProgress;
    }

    public LiveData<Set<String>> getMyCourseProgressFailedIds() {
        return myCourseProgressFailedIds;
    }

    public void enroll(String courseId) {
        enrollState.setValue(EnrollUiState.loading());

        courseRepository.enrollCourse(courseId, new CourseRepository.EnrollCallback() {
            @Override
            public void onSuccess(EnrollmentResponse response) {
                enrollState.postValue(EnrollUiState.success());
            }

            @Override
            public void onAlreadyEnrolled(EnrollmentResponse response) {
                // 409 from the backend still means the learner can access the course; surface
                // it so the UI shows "already enrolled" rather than a misleading success toast.
                enrollState.postValue(EnrollUiState.alreadyEnrolled());
            }

            @Override
            public void onError(String message) {
                enrollState.postValue(EnrollUiState.error(message));
            }
        });
    }

    /**
     * Consumed by the fragment after navigation so re-entering the enroll screen does not
     * re-trigger the "enrolled" branch and immediately bounce the learner away again.
     */
    public void clearEnrollState() {
        enrollState.setValue(EnrollUiState.idle());
    }

    public void unenroll(String enrollmentId) {
        unenrollState.setValue(UnenrollUiState.loading());

        courseRepository.unenroll(enrollmentId, new CourseRepository.UnenrollCallback() {
            @Override
            public void onSuccess() {
                unenrollState.postValue(UnenrollUiState.success());
                loadMyEnrollments();
            }

            @Override
            public void onError(String message) {
                unenrollState.postValue(UnenrollUiState.error(message));
            }
        });
    }

    public void clearUnenrollState() {
        unenrollState.setValue(UnenrollUiState.idle());
    }

    public void loadMyEnrollments() {
        myEnrollmentsLoading.postValue(true);
        // Clear any stale error before the new fetch so a transient failure does not linger
        // on screen once a follow-up refresh succeeds.
        myEnrollmentsError.postValue(null);

        courseRepository.getMyEnrollments(new CourseRepository.MyEnrollmentsCallback() {
            @Override
            public void onSuccess(List<EnrolledCourse> courses) {
                myEnrollmentsLoading.postValue(false);
                myEnrollments.postValue(courses);
                loadCourseProgressFor(courses);
            }

            @Override
            public void onError(String message) {
                myEnrollmentsLoading.postValue(false);
                // Never post null — observers should always receive a list and a separate error
                // signal so the empty-list view stays distinguishable from a failed fetch.
                if (myEnrollments.getValue() == null) {
                    myEnrollments.postValue(Collections.emptyList());
                }
                myCourseProgress.postValue(new LinkedHashMap<>());
                myCourseProgressFailedIds.postValue(new LinkedHashSet<>());
                myEnrollmentsError.postValue(message);
            }
        });
    }

    private void loadCourseProgressFor(List<EnrolledCourse> courses) {
        myCourseProgress.setValue(new LinkedHashMap<>());
        myCourseProgressFailedIds.setValue(new LinkedHashSet<>());

        if (courses == null || courses.isEmpty()) {
            return;
        }

        for (EnrolledCourse course : courses) {
            if (course == null || course.courseId == null || course.courseId.isBlank()) {
                continue;
            }

            // Each card loads progress independently so one failing course-progress request does
            // not block the rest of the learner's enrolled catalog from rendering normally.
            courseRepository.getCourseProgress(course.courseId, new CourseRepository.CourseProgressCallback() {
                @Override
                public void onSuccess(CourseProgressSummary progress) {
                    Map<String, CourseProgressSummary> updated =
                            new LinkedHashMap<>(safeProgressMap());
                    updated.put(course.courseId, progress);
                    myCourseProgress.postValue(updated);
                }

                @Override
                public void onError(String message) {
                    Set<String> failedIds = new LinkedHashSet<>(safeFailedProgressIds());
                    failedIds.add(course.courseId);
                    myCourseProgressFailedIds.postValue(failedIds);
                }
            });
        }
    }

    private Map<String, CourseProgressSummary> safeProgressMap() {
        Map<String, CourseProgressSummary> current = myCourseProgress.getValue();
        return current != null ? current : new LinkedHashMap<>();
    }

    private Set<String> safeFailedProgressIds() {
        Set<String> current = myCourseProgressFailedIds.getValue();
        return current != null ? current : new LinkedHashSet<>();
    }
}
