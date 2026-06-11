package com.baghdad.edulife.features.teacher.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.baghdad.edulife.features.teacher.data.TeacherRepository;
import com.baghdad.edulife.features.teacher.model.CmsCourse;
import com.baghdad.edulife.features.teacher.model.CmsCourseDetailUiState;
import com.baghdad.edulife.features.teacher.model.CmsLesson;
import com.baghdad.edulife.features.teacher.model.CmsSection;
import com.baghdad.edulife.features.teacher.model.CreateLessonRequest;
import com.baghdad.edulife.features.teacher.model.CreateSectionRequest;

import java.util.List;

public class CmsCourseDetailViewModel extends ViewModel {

    private final TeacherRepository repository;
    private final MutableLiveData<CmsCourseDetailUiState> uiState =
            new MutableLiveData<>(CmsCourseDetailUiState.loading());

    // Track current courseId for refreshes
    private String currentCourseId;

    public CmsCourseDetailViewModel() {
        this.repository = new TeacherRepository();
    }

    public LiveData<CmsCourseDetailUiState> getUiState() {
        return uiState;
    }

    public void loadSections(String courseId) {
        this.currentCourseId = courseId;
        uiState.setValue(CmsCourseDetailUiState.loading());
        repository.loadSections(courseId, new TeacherRepository.SectionsCallback() {
            @Override
            public void onSuccess(List<CmsSection> sections) {
                uiState.postValue(CmsCourseDetailUiState.success(null, sections));
            }

            @Override
            public void onError(String message) {
                uiState.postValue(CmsCourseDetailUiState.error(message));
            }
        });
    }

    public void createSection(String courseId, String title, int order) {
        CreateSectionRequest request = new CreateSectionRequest(title, null, order);
        repository.createSection(courseId, request, new TeacherRepository.SectionCallback() {
            @Override
            public void onSuccess(CmsSection section) {
                // Reload sections after creation
                loadSections(courseId);
            }

            @Override
            public void onError(String message) {
                CmsCourseDetailUiState current = uiState.getValue();
                if (current != null) {
                    uiState.postValue(current.withActionMessage("Error: " + message));
                } else {
                    uiState.postValue(CmsCourseDetailUiState.error(message));
                }
            }
        });
    }

    public void deleteSection(String courseId, String sectionId) {
        repository.deleteSection(courseId, sectionId, new TeacherRepository.VoidCallback() {
            @Override
            public void onSuccess() {
                loadSections(courseId);
            }

            @Override
            public void onError(String message) {
                CmsCourseDetailUiState current = uiState.getValue();
                if (current != null) {
                    uiState.postValue(current.withActionMessage("Error: " + message));
                } else {
                    uiState.postValue(CmsCourseDetailUiState.error(message));
                }
            }
        });
    }

    public void createLesson(String sectionId, String title, String type,
                             int order, String contentUrl) {
        CreateLessonRequest request = new CreateLessonRequest(
                title, null, type, null, order, false, contentUrl, null);
        repository.createLesson(sectionId, request, new TeacherRepository.LessonCallback() {
            @Override
            public void onSuccess(CmsLesson lesson) {
                // Reload sections to reflect new lesson count (if backend supports it)
                if (currentCourseId != null) {
                    loadSections(currentCourseId);
                }
            }

            @Override
            public void onError(String message) {
                CmsCourseDetailUiState current = uiState.getValue();
                if (current != null) {
                    uiState.postValue(current.withActionMessage("Error: " + message));
                } else {
                    uiState.postValue(CmsCourseDetailUiState.error(message));
                }
            }
        });
    }

    public void deleteLesson(String sectionId, String lessonId) {
        repository.deleteLesson(sectionId, lessonId, new TeacherRepository.VoidCallback() {
            @Override
            public void onSuccess() {
                if (currentCourseId != null) {
                    loadSections(currentCourseId);
                }
            }

            @Override
            public void onError(String message) {
                CmsCourseDetailUiState current = uiState.getValue();
                if (current != null) {
                    uiState.postValue(current.withActionMessage("Error: " + message));
                } else {
                    uiState.postValue(CmsCourseDetailUiState.error(message));
                }
            }
        });
    }
}
