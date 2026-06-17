package com.baghdad.edulife.features.exams.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.baghdad.edulife.features.exams.data.CmsExamRepository;
import com.baghdad.edulife.features.exams.model.CmsExamChoiceRequest;
import com.baghdad.edulife.features.exams.model.CmsExamQuestionRequest;
import com.baghdad.edulife.features.exams.model.CmsExamRequest;
import com.baghdad.edulife.features.exams.model.CmsExamResponse;

import java.util.ArrayList;
import java.util.List;

public class CmsExamBuilderViewModel extends ViewModel {

    private final CmsExamRepository repository = new CmsExamRepository();

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> saving = new MutableLiveData<>(false);
    private final MutableLiveData<CmsExamResponse> existingExam = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showBuilder = new MutableLiveData<>(false);
    private final MutableLiveData<String> loadError = new MutableLiveData<>();
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();

    private String examTitle = "Final Exam";
    private int passScore = 80;
    private Integer timeLimitMinutes = 30;
    private final List<QuestionDraft> drafts = new ArrayList<>();
    private boolean accessDenied = false;
    private boolean loaded = false;

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<Boolean> getSaving() { return saving; }
    public LiveData<CmsExamResponse> getExistingExam() { return existingExam; }
    public LiveData<Boolean> getShowBuilder() { return showBuilder; }
    public LiveData<String> getLoadError() { return loadError; }
    public LiveData<String> getToastMessage() { return toastMessage; }
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
                existingExam.setValue(exam);
            }

            @Override
            public void onNotFound() {
                loaded = true;
                loading.setValue(false);
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
        if (examTitle == null || examTitle.trim().isEmpty()) {
            return "Exam title is required.";
        }
        if (examTitle.trim().length() > 200) {
            return "Exam title must be 200 characters or fewer.";
        }
        if (passScore < 1 || passScore > 100) {
            return "Pass score must be between 1 and 100.";
        }
        if (timeLimitMinutes != null && timeLimitMinutes < 1) {
            return "Time limit must be at least 1 minute.";
        }
        if (drafts.isEmpty()) {
            return "At least one question is required.";
        }
        for (int i = 0; i < drafts.size(); i++) {
            QuestionDraft q = drafts.get(i);
            if (q.questionText == null || q.questionText.trim().isEmpty()) {
                return "Question " + (i + 1) + " is missing text.";
            }
            if (q.choices.size() < 2) {
                return "Question " + (i + 1) + " must have at least 2 choices.";
            }
            boolean hasCorrect = false;
            for (int j = 0; j < q.choices.size(); j++) {
                ChoiceDraft c = q.choices.get(j);
                if (c.choiceText == null || c.choiceText.trim().isEmpty()) {
                    return "Question " + (i + 1) + ", choice " + (j + 1) + " is missing text.";
                }
                if (c.correct) {
                    if (hasCorrect) {
                        return "Question " + (i + 1) + " has multiple correct answers. Select exactly one.";
                    }
                    hasCorrect = true;
                }
            }
            if (!hasCorrect) {
                return "Question " + (i + 1) + " must have exactly one correct answer.";
            }
        }
        return null;
    }

    public void saveExam(String courseId) {
        String validation = validateExam();
        if (validation != null) {
            toastMessage.setValue(validation);
            return;
        }

        saving.setValue(true);

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

        CmsExamRequest request = new CmsExamRequest(
                examTitle.trim(), passScore, timeLimitMinutes, questionRequests);

        repository.createCourseExam(courseId, request, new CmsExamRepository.CreateExamCallback() {
            @Override
            public void onSuccess(CmsExamResponse exam) {
                saving.setValue(false);
                showBuilder.setValue(false);
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
