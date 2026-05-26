package com.baghdad.edulife.features.exam.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.baghdad.edulife.features.exam.data.ExamRepository;
import com.baghdad.edulife.features.exam.model.ExamDto;
import com.baghdad.edulife.features.exam.model.ExamResultDto;
import com.baghdad.edulife.features.exam.model.ExamResultUiState;
import com.baghdad.edulife.features.exam.model.ExamUiState;
import com.baghdad.edulife.features.exam.model.SubmitExamRequest;

import java.util.List;

public class ExamViewModel extends AndroidViewModel {

    private final ExamRepository repository;

    private final MutableLiveData<ExamUiState> examState =
            new MutableLiveData<>(ExamUiState.idle());
    private final MutableLiveData<ExamResultUiState> resultState =
            new MutableLiveData<>(ExamResultUiState.idle());

    public ExamViewModel(@NonNull Application application) {
        super(application);
        repository = new ExamRepository();
    }

    public LiveData<ExamUiState> getExamState() { return examState; }
    public LiveData<ExamResultUiState> getResultState() { return resultState; }

    public void loadExam(String courseId) {
        examState.setValue(ExamUiState.loading());
        repository.loadExam(courseId, new ExamRepository.ExamCallback() {
            @Override
            public void onSuccess(ExamDto exam) {
                examState.postValue(ExamUiState.success(exam));
            }

            @Override
            public void onError(String message) {
                examState.postValue(ExamUiState.error(message));
            }
        });
    }

    public void submitExam(String courseId, List<SubmitExamRequest.AnswerDto> answers) {
        ExamDto currentExam = examState.getValue() != null ? examState.getValue().exam : null;
        examState.setValue(ExamUiState.submitting(currentExam));
        resultState.setValue(ExamResultUiState.loading());

        repository.submitExam(courseId, new SubmitExamRequest(answers), new ExamRepository.ExamResultCallback() {
            @Override
            public void onSuccess(ExamResultDto result) {
                resultState.postValue(ExamResultUiState.success(result));
            }

            @Override
            public void onError(String message) {
                resultState.postValue(ExamResultUiState.error(message));
                examState.postValue(ExamUiState.success(currentExam));
            }
        });
    }
}
