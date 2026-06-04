package com.baghdad.edulife.features.courses.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.baghdad.edulife.features.courses.data.ExamRepository;
import com.baghdad.edulife.features.courses.model.ExamResultResponse;
import com.baghdad.edulife.features.courses.model.ExamStatusUiState;
import com.baghdad.edulife.features.courses.model.ExamSubmitUiState;
import com.baghdad.edulife.features.courses.model.ExamUiState;
import com.baghdad.edulife.features.courses.model.SubmitExamRequest;

import java.util.List;

public class ExamViewModel extends AndroidViewModel {

    private final ExamRepository examRepository;
    private final MutableLiveData<ExamStatusUiState> examStatusState =
            new MutableLiveData<>(ExamStatusUiState.idle());
    private final MutableLiveData<ExamUiState> examState = new MutableLiveData<>(ExamUiState.idle());
    private final MutableLiveData<ExamSubmitUiState> submitState = new MutableLiveData<>(ExamSubmitUiState.idle());

    public ExamViewModel(@NonNull Application application) {
        super(application);
        this.examRepository = new ExamRepository();
    }

    public LiveData<ExamStatusUiState> getExamStatusState() {
        return examStatusState;
    }

    public LiveData<ExamUiState> getExamState() {
        return examState;
    }

    public LiveData<ExamSubmitUiState> getSubmitState() {
        return submitState;
    }

    public void loadExamStatus(String courseId) {
        examStatusState.setValue(ExamStatusUiState.loading());
        examRepository.getExamStatus(courseId, new ExamRepository.ExamStatusCallback() {
            @Override
            public void onSuccess(com.baghdad.edulife.features.courses.model.ExamStatusResponse status) {
                // Status gates the entire exam screen, so it has its own LiveData instead of being
                // merged into question loading state.
                examStatusState.postValue(ExamStatusUiState.success(status));
            }

            @Override
            public void onError(String message) {
                examStatusState.postValue(ExamStatusUiState.error(message));
            }
        });
    }

    public void loadExam(String courseId) {
        examState.setValue(ExamUiState.loading());
        // The status preflight keeps the learner flow aligned with backend attempt rules
        // before the client renders any questions that should already be locked.
        examRepository.getExamStatus(courseId, new ExamRepository.ExamStatusCallback() {
            @Override
            public void onSuccess(com.baghdad.edulife.features.courses.model.ExamStatusResponse status) {
                if (status.passed) {
                    examState.postValue(ExamUiState.alreadyPassed());
                    return;
                }
                if (status.inCooldown) {
                    examState.postValue(ExamUiState.cooldown(status.cooldownEndsAt));
                    return;
                }

                examRepository.getExam(courseId, new ExamRepository.ExamCallback() {
                    @Override
                    public void onSuccess(com.baghdad.edulife.features.courses.model.ExamResponse exam) {
                        examState.postValue(ExamUiState.success(exam));
                    }

                    @Override
                    public void onError(String message) {
                        examState.postValue(ExamUiState.error(message));
                    }
                });
            }

            @Override
            public void onError(String message) {
                examState.postValue(ExamUiState.error(message));
            }
        });
    }

    public void submitExam(String courseId, List<SubmitExamRequest.AnswerItem> answers) {
        submitState.setValue(ExamSubmitUiState.loading());
        SubmitExamRequest request = new SubmitExamRequest(answers);
        examRepository.submitExam(courseId, request, new ExamRepository.SubmitCallback() {
            @Override
            public void onSuccess(ExamResultResponse result) {
                submitState.postValue(ExamSubmitUiState.success(result));
            }

            @Override
            public void onAlreadyPassed() {
                submitState.postValue(ExamSubmitUiState.alreadyPassed());
            }

            @Override
            public void onCooldown(String cooldownEndsAt) {
                submitState.postValue(ExamSubmitUiState.cooldown(cooldownEndsAt));
            }

            @Override
            public void onError(String message) {
                submitState.postValue(ExamSubmitUiState.error(message));
            }
        });
    }
}
