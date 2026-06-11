package com.baghdad.edulife.features.teacher.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.baghdad.edulife.features.teacher.data.TeacherRepository;
import com.baghdad.edulife.features.teacher.model.CmsCourse;
import com.baghdad.edulife.features.teacher.model.CreateCourseRequest;
import com.baghdad.edulife.features.teacher.model.TeacherDashboardUiState;

import java.util.List;

public class TeacherDashboardViewModel extends ViewModel {

    private final TeacherRepository repository;
    private final MutableLiveData<TeacherDashboardUiState> uiState =
            new MutableLiveData<>(TeacherDashboardUiState.loading());

    public TeacherDashboardViewModel() {
        this.repository = new TeacherRepository();
    }

    public LiveData<TeacherDashboardUiState> getUiState() {
        return uiState;
    }

    public void loadCourses() {
        uiState.setValue(TeacherDashboardUiState.loading());
        repository.loadMyCourses(new TeacherRepository.CoursesCallback() {
            @Override
            public void onSuccess(List<CmsCourse> courses) {
                uiState.postValue(TeacherDashboardUiState.success(courses));
            }

            @Override
            public void onError(String message) {
                uiState.postValue(TeacherDashboardUiState.error(message));
            }
        });
    }

    public void createCourse(String title, String shortDescription,
                             String description, String languageCode) {
        CreateCourseRequest request = new CreateCourseRequest(
                title, shortDescription, description, languageCode, null, null);
        uiState.setValue(TeacherDashboardUiState.loading());
        repository.createCourse(request, new TeacherRepository.CourseCallback() {
            @Override
            public void onSuccess(CmsCourse course) {
                // Refresh the list after creation
                loadCourses();
            }

            @Override
            public void onError(String message) {
                uiState.postValue(TeacherDashboardUiState.error(message));
            }
        });
    }
}
