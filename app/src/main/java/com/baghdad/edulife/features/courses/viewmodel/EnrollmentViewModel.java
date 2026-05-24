package com.baghdad.edulife.features.courses.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.baghdad.edulife.features.courses.data.CourseRepository;
import com.baghdad.edulife.features.courses.model.EnrolledCourse;
import com.baghdad.edulife.features.courses.model.EnrollmentResponse;
import com.baghdad.edulife.features.courses.model.EnrollUiState;

import java.util.List;

public class EnrollmentViewModel extends AndroidViewModel {

    private final CourseRepository courseRepository;

    private final MutableLiveData<EnrollUiState> enrollState =
            new MutableLiveData<>(EnrollUiState.idle());

    private final MutableLiveData<List<EnrolledCourse>> myEnrollments =
            new MutableLiveData<>();

    public EnrollmentViewModel(@NonNull Application application) {
        super(application);
        this.courseRepository = new CourseRepository();
    }

    public LiveData<EnrollUiState> getEnrollState() {
        return enrollState;
    }

    public LiveData<List<EnrolledCourse>> getMyEnrollments() {
        return myEnrollments;
    }

    public void enroll(String courseId) {
        enrollState.setValue(EnrollUiState.loading());

        courseRepository.enrollCourse(courseId, new CourseRepository.EnrollCallback() {
            @Override
            public void onSuccess(EnrollmentResponse response) {
                enrollState.postValue(EnrollUiState.success());
            }

            @Override
            public void onError(String message) {
                enrollState.postValue(EnrollUiState.error(message));
            }
        });
    }

    public void loadMyEnrollments() {
        courseRepository.getMyEnrollments(new CourseRepository.MyEnrollmentsCallback() {
            @Override
            public void onSuccess(List<EnrolledCourse> courses) {
                myEnrollments.postValue(courses);
            }

            @Override
            public void onError(String message) {
                myEnrollments.postValue(null);
            }
        });
    }
}
