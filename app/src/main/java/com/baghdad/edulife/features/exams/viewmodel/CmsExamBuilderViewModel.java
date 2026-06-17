package com.baghdad.edulife.features.exams.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.baghdad.edulife.features.exams.data.CmsExamRepository;
import com.baghdad.edulife.features.exams.model.CmsExamChoice;
import com.baghdad.edulife.features.exams.model.CmsExamChoiceRequest;
import com.baghdad.edulife.features.exams.model.CmsExamQuestion;
import com.baghdad.edulife.features.exams.model.CmsExamQuestionRequest;
import com.baghdad.edulife.features.exams.model.CmsExamRequest;
import com.baghdad.edulife.features.exams.model.CmsExamResponse;
import com.baghdad.edulife.features.exams.validation.CmsExamValidator;

import java.util.ArrayList;
import java.util.List;

public class CmsExamBuilderViewModel extends ViewModel {

    private final CmsExamRepository repository = new CmsExamRepository();

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> saving = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> deleting = new MutableLiveData<>(false);
    private final MutableLiveData<CmsExamResponse> existingExam = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showBuilder = new MutableLiveData<>(false);
    private final MutableLiveData<String> loadError = new MutableLiveData<>();
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isEditMode = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> examDeleted = new MutableLiveData<>(false);

    private String examTitle = "Final Exam";
    private int passScore = 80;
    private Integer timeLimitMinutes = 30;
    private final List<QuestionDraft> drafts = new ArrayList<>();
    private boolean accessDenied = false;
    private boolean loaded = false;
    private boolean isExistingExam = false;

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<Boolean> getSaving() { return saving; }
    public LiveData<Boolean> getDeleting() { return deleting; }
    public LiveData<CmsExamResponse> getExistingExam() { return existingExam; }
    public LiveData<Boolean> getShowBuilder() { return showBuilder; }
    public LiveData<String> getLoadError() { return loadError; }
    public LiveData<String> getToastMessage() { return toastMessage; }
    public LiveData<Boolean> getIsEditMode() { return isEditMode; }
    public LiveData<Boolean> getExamDeleted() { return examDeleted; }
    public List<QuestionDraft> getDrafts() { return drafts; }
    public boolean isAccessDenied() { return accessDenied; }

    public String getExamTitle() { return examTitle; }
    public void setExamTitle(String title) { this.examTitle = title; }
    public int getPassScore() { return passScore; }
    public void setPassScore(int score) { this.passScore = score; }
    public Integer getTimeLimitMinutes() { return timeLimitMinutes; }
    public void setTimeLimitMinutes(Integer minutes) { this.timeLimitMinutes = minutes; }

    public void loadExam(String courseId) {
        if (loaded) return;
        loading.setValue(true);
        loadError.setValue(null);
        accessDenied = false;

        repository.getCourseExam(courseId, new CmsExamRepository.ExamCallback() {
            @Override
            public void onSuccess(CmsExamResponse exam) {
                loaded = true;
                loading.setValue(false);
                isExistingExam = true;
                existingExam.setValue(exam);
            }

            @Override
            public void onNotFound() {
                loaded = true;
                loading.setValue(false);
                isExistingExam = false;
                if (drafts.isEmpty()) {
                    drafts.add(new QuestionDraft());
                }
                showBuilder.setValue(true);
            }

            @Override
            public void onAccessDenied(String message) {
                loaded = true;
                loading.setValue(false);
                accessDenied = true;
                loadError.setValue(message);
            }

            @Override
            public void onError(String message) {
                loading.setValue(false);
                loadError.setValue(message);
            }
        });
    }

    public void retryLoad(String courseId) {
        loaded = false;
        loadExam(courseId);
    }

    public void enterEditMode() {
        CmsExamResponse exam = existingExam.getValue();
        if (exam == null) return;

        examTitle = exam.title;
        passScore = exam.passScore;
        timeLimitMinutes = exam.timeLimitMinutes;

        drafts.clear();
        if (exam.questions != null) {
            for (CmsExamQuestion q : exam.questions) {
                QuestionDraft draft = new QuestionDraft();
                draft.questionText = q.questionText;
                draft.choices.clear();
                if (q.choices != null) {
                    for (CmsExamChoice c : q.choices) {
                        ChoiceDraft cd = new ChoiceDraft();
                        cd.choiceText = c.choiceText;
                        cd.correct = c.correct;
                        draft.choices.add(cd);
                    }
                }
                drafts.add(draft);
            }
        }

        isEditMode.setValue(true);
        showBuilder.setValue(true);
    }

    public void cancelEditMode() {
        isEditMode.setValue(false);
        showBuilder.setValue(false);
    }

    public void addQuestion() {
        drafts.add(new QuestionDraft());
    }

    public void removeQuestion(int index) {
        if (index >= 0 && index < drafts.size()) {
            drafts.remove(index);
        }
    }

    public void addChoice(int questionIndex) {
        if (questionIndex >= 0 && questionIndex < drafts.size()) {
            drafts.get(questionIndex).choices.add(new ChoiceDraft());
        }
    }

    public void removeChoice(int questionIndex, int choiceIndex) {
        if (questionIndex >= 0 && questionIndex < drafts.size()) {
            QuestionDraft q = drafts.get(questionIndex);
            if (choiceIndex >= 0 && choiceIndex < q.choices.size()) {
                q.choices.remove(choiceIndex);
            }
        }
    }

    public void updateQuestionText(int index, String text) {
        if (index >= 0 && index < drafts.size()) {
            drafts.get(index).questionText = text;
        }
    }

    public void updateChoiceText(int questionIndex, int choiceIndex, String text) {
        if (questionIndex >= 0 && questionIndex < drafts.size()) {
            QuestionDraft q = drafts.get(questionIndex);
            if (choiceIndex >= 0 && choiceIndex < q.choices.size()) {
                q.choices.get(choiceIndex).choiceText = text;
            }
        }
    }

    public void setCorrectChoice(int questionIndex, int choiceIndex) {
        if (questionIndex >= 0 && questionIndex < drafts.size()) {
            QuestionDraft q = drafts.get(questionIndex);
            for (int i = 0; i < q.choices.size(); i++) {
                q.choices.get(i).correct = (i == choiceIndex);
            }
        }
    }

    public String validateExam() {
        return CmsExamValidator.validate(examTitle, passScore, timeLimitMinutes, drafts);
    }

    public void saveExam(String courseId) {
        String validation = validateExam();
        if (validation != null) {
            toastMessage.setValue(validation);
            return;
        }

        saving.setValue(true);
        CmsExamRequest request = buildRequest();

        repository.createCourseExam(courseId, request, new CmsExamRepository.CreateExamCallback() {
            @Override
            public void onSuccess(CmsExamResponse exam) {
                saving.setValue(false);
                showBuilder.setValue(false);
                isExistingExam = true;
                existingExam.setValue(exam);
                toastMessage.setValue("Final exam created successfully.");
            }

            @Override
            public void onConflict() {
                saving.setValue(false);
                toastMessage.setValue("An exam already exists for this course.");
                loaded = false;
                loadExam(courseId);
            }

            @Override
            public void onAccessDenied(String message) {
                saving.setValue(false);
                toastMessage.setValue(message);
            }

            @Override
            public void onError(String message) {
                saving.setValue(false);
                toastMessage.setValue(message);
            }
        });
    }

    public void saveChanges(String courseId) {
        String validation = validateExam();
        if (validation != null) {
            toastMessage.setValue(validation);
            return;
        }

        saving.setValue(true);
        CmsExamRequest request = buildRequest();

        repository.updateCourseExam(courseId, request, new CmsExamRepository.UpdateExamCallback() {
            @Override
            public void onSuccess(CmsExamResponse exam) {
                saving.setValue(false);
                isEditMode.setValue(false);
                showBuilder.setValue(false);
                existingExam.setValue(exam);
                toastMessage.setValue("Exam updated successfully.");
            }

            @Override
            public void onAccessDenied(String message) {
                saving.setValue(false);
                toastMessage.setValue(message);
            }

            @Override
            public void onError(String message) {
                saving.setValue(false);
                toastMessage.setValue(message);
            }
        });
    }

    public void deleteExam(String courseId) {
        deleting.setValue(true);

        repository.deleteCourseExam(courseId, new CmsExamRepository.DeleteExamCallback() {
            @Override
            public void onSuccess() {
                deleting.setValue(false);
                isExistingExam = false;
                existingExam.setValue(null);
                isEditMode.setValue(false);
                toastMessage.setValue("Exam deleted successfully.");
                examDeleted.setValue(true);
            }

            @Override
            public void onAccessDenied(String message) {
                deleting.setValue(false);
                toastMessage.setValue(message);
            }

            @Override
            public void onError(String message) {
                deleting.setValue(false);
                toastMessage.setValue(message);
            }
        });
    }

    private CmsExamRequest buildRequest() {
        List<CmsExamQuestionRequest> questionRequests = new ArrayList<>();
        for (int i = 0; i < drafts.size(); i++) {
            QuestionDraft q = drafts.get(i);
            List<CmsExamChoiceRequest> choiceRequests = new ArrayList<>();
            for (ChoiceDraft c : q.choices) {
                choiceRequests.add(new CmsExamChoiceRequest(c.choiceText.trim(), c.correct));
            }
            questionRequests.add(new CmsExamQuestionRequest(
                    q.questionText.trim(), i + 1, choiceRequests));
        }
        return new CmsExamRequest(
                examTitle.trim(), passScore, timeLimitMinutes, questionRequests);
    }

    public void clearToast() {
        toastMessage.setValue(null);
    }

    public static class QuestionDraft {
        public String questionText = "";
        public List<ChoiceDraft> choices;

        public QuestionDraft() {
            choices = new ArrayList<>();
            choices.add(new ChoiceDraft());
            choices.add(new ChoiceDraft());
        }
    }

    public static class ChoiceDraft {
        public String choiceText = "";
        public boolean correct = false;
    }
}
