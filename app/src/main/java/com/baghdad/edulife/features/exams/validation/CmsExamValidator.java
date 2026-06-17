package com.baghdad.edulife.features.exams.validation;

import com.baghdad.edulife.features.exams.viewmodel.CmsExamBuilderViewModel.ChoiceDraft;
import com.baghdad.edulife.features.exams.viewmodel.CmsExamBuilderViewModel.QuestionDraft;

import java.util.List;

public final class CmsExamValidator {

    private CmsExamValidator() {}

    public static String validate(String title, int passScore,
                                  Integer timeLimitMinutes, List<QuestionDraft> drafts) {
        if (title == null || title.trim().isEmpty()) {
            return "Exam title is required.";
        }
        if (title.trim().length() > 200) {
            return "Exam title must be 200 characters or fewer.";
        }
        if (passScore < 1 || passScore > 100) {
            return "Pass score must be between 1 and 100.";
        }
        if (timeLimitMinutes != null && timeLimitMinutes < 1) {
            return "Time limit must be at least 1 minute.";
        }
        if (drafts == null || drafts.isEmpty()) {
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
}
