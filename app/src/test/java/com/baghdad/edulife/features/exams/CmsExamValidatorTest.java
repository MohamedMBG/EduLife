package com.baghdad.edulife.features.exams;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.baghdad.edulife.features.exams.validation.CmsExamValidator;
import com.baghdad.edulife.features.exams.viewmodel.CmsExamBuilderViewModel.ChoiceDraft;
import com.baghdad.edulife.features.exams.viewmodel.CmsExamBuilderViewModel.QuestionDraft;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class CmsExamValidatorTest {

    @Test
    public void validExamPasses() {
        assertNull(CmsExamValidator.validate("Final Exam", 80, 30, List.of(validQuestion())));
    }

    @Test
    public void missingTitleFails() {
        String result = CmsExamValidator.validate("", 80, 30, List.of(validQuestion()));
        assertNotNull(result);
        assertEquals("Exam title is required.", result);
    }

    @Test
    public void nullTitleFails() {
        String result = CmsExamValidator.validate(null, 80, 30, List.of(validQuestion()));
        assertNotNull(result);
        assertEquals("Exam title is required.", result);
    }

    @Test
    public void titleOver200CharsFails() {
        String longTitle = "A".repeat(201);
        String result = CmsExamValidator.validate(longTitle, 80, 30, List.of(validQuestion()));
        assertNotNull(result);
        assertEquals("Exam title must be 200 characters or fewer.", result);
    }

    @Test
    public void passScoreBelowRangeFails() {
        String result = CmsExamValidator.validate("Exam", 0, 30, List.of(validQuestion()));
        assertNotNull(result);
        assertEquals("Pass score must be between 1 and 100.", result);
    }

    @Test
    public void passScoreAboveRangeFails() {
        String result = CmsExamValidator.validate("Exam", 101, 30, List.of(validQuestion()));
        assertNotNull(result);
        assertEquals("Pass score must be between 1 and 100.", result);
    }

    @Test
    public void timeLimitZeroFails() {
        String result = CmsExamValidator.validate("Exam", 80, 0, List.of(validQuestion()));
        assertNotNull(result);
        assertEquals("Time limit must be at least 1 minute.", result);
    }

    @Test
    public void timeLimitNegativeFails() {
        String result = CmsExamValidator.validate("Exam", 80, -5, List.of(validQuestion()));
        assertNotNull(result);
        assertEquals("Time limit must be at least 1 minute.", result);
    }

    @Test
    public void nullTimeLimitPasses() {
        assertNull(CmsExamValidator.validate("Exam", 80, null, List.of(validQuestion())));
    }

    @Test
    public void noQuestionsFails() {
        String result = CmsExamValidator.validate("Exam", 80, 30, new ArrayList<>());
        assertNotNull(result);
        assertEquals("At least one question is required.", result);
    }

    @Test
    public void nullQuestionListFails() {
        String result = CmsExamValidator.validate("Exam", 80, 30, null);
        assertNotNull(result);
        assertEquals("At least one question is required.", result);
    }

    @Test
    public void questionWithEmptyTextFails() {
        QuestionDraft q = validQuestion();
        q.questionText = "";
        String result = CmsExamValidator.validate("Exam", 80, 30, List.of(q));
        assertNotNull(result);
        assertEquals("Question 1 is missing text.", result);
    }

    @Test
    public void questionWithFewerThan2ChoicesFails() {
        QuestionDraft q = new QuestionDraft();
        q.questionText = "What?";
        q.choices.clear();
        ChoiceDraft c = new ChoiceDraft();
        c.choiceText = "Only one";
        c.correct = true;
        q.choices.add(c);

        String result = CmsExamValidator.validate("Exam", 80, 30, List.of(q));
        assertNotNull(result);
        assertEquals("Question 1 must have at least 2 choices.", result);
    }

    @Test
    public void emptyChoiceTextFails() {
        QuestionDraft q = validQuestion();
        q.choices.get(0).choiceText = "";
        String result = CmsExamValidator.validate("Exam", 80, 30, List.of(q));
        assertNotNull(result);
        assertEquals("Question 1, choice 1 is missing text.", result);
    }

    @Test
    public void zeroCorrectChoicesFails() {
        QuestionDraft q = validQuestion();
        q.choices.get(0).correct = false;
        q.choices.get(1).correct = false;
        String result = CmsExamValidator.validate("Exam", 80, 30, List.of(q));
        assertNotNull(result);
        assertEquals("Question 1 must have exactly one correct answer.", result);
    }

    @Test
    public void multipleCorrectChoicesFails() {
        QuestionDraft q = validQuestion();
        q.choices.get(0).correct = true;
        q.choices.get(1).correct = true;
        String result = CmsExamValidator.validate("Exam", 80, 30, List.of(q));
        assertNotNull(result);
        assertEquals("Question 1 has multiple correct answers. Select exactly one.", result);
    }

    @Test
    public void exactlyOneCorrectChoicePasses() {
        assertNull(CmsExamValidator.validate("Exam", 80, 30, List.of(validQuestion())));
    }

    @Test
    public void boundaryPassScore1Passes() {
        assertNull(CmsExamValidator.validate("Exam", 1, 30, List.of(validQuestion())));
    }

    @Test
    public void boundaryPassScore100Passes() {
        assertNull(CmsExamValidator.validate("Exam", 100, 30, List.of(validQuestion())));
    }

    @Test
    public void titleExactly200CharsPasses() {
        String title200 = "A".repeat(200);
        assertNull(CmsExamValidator.validate(title200, 80, 30, List.of(validQuestion())));
    }

    private QuestionDraft validQuestion() {
        QuestionDraft q = new QuestionDraft();
        q.questionText = "What is 2 + 2?";
        q.choices.clear();

        ChoiceDraft correct = new ChoiceDraft();
        correct.choiceText = "4";
        correct.correct = true;
        q.choices.add(correct);

        ChoiceDraft wrong = new ChoiceDraft();
        wrong.choiceText = "5";
        wrong.correct = false;
        q.choices.add(wrong);

        return q;
    }
}
