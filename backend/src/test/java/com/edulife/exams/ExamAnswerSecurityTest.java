package com.edulife.exams;

import com.edulife.common.error.ApiErrorWriter;
import com.edulife.common.error.GlobalApiExceptionHandler;
import com.edulife.exams.controller.ExamController;
import com.edulife.exams.dto.ExamDto;
import com.edulife.exams.service.ExamService;
import com.edulife.security.SecurityConfig;
import com.edulife.users.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security audit: verifies that correct-answer flags are never serialised in the exam
 * response visible to the learner.  ExamDto.ChoiceDto intentionally omits isCorrect;
 * this test pins that contract so a future refactor cannot accidentally leak it.
 */
@WebMvcTest(ExamController.class)
@Import({SecurityConfig.class, ApiErrorWriter.class, GlobalApiExceptionHandler.class})
class ExamAnswerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExamService examService;

    @MockBean
    private FirebaseAuth firebaseAuth;

    @MockBean
    private UserRepository userRepository;

    @Test
    void examResponseDoesNotContainCorrectAnswerFlag() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID correctChoiceId = UUID.randomUUID();
        UUID wrongChoiceId = UUID.randomUUID();

        ExamDto exam = new ExamDto(
                examId,
                courseId,
                "Test exam",
                80,
                null,
                List.of(new ExamDto.QuestionDto(
                        questionId,
                        "What is 2 + 2?",
                        1,
                        List.of(
                                new ExamDto.ChoiceDto(correctChoiceId, "4"),
                                new ExamDto.ChoiceDto(wrongChoiceId, "5")
                        )
                ))
        );

        FirebaseToken token = mock(FirebaseToken.class);
        given(firebaseAuth.verifyIdToken("valid-token")).willReturn(token);
        given(token.getUid()).willReturn("uid-learner");
        given(token.getEmail()).willReturn("learner@edulife.test");
        given(token.isEmailVerified()).willReturn(true);
        given(examService.getExam(any(UUID.class))).willReturn(exam);

        mockMvc.perform(get("/api/v1/courses/{courseId}/exam", courseId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[0].choices[0].choiceId").exists())
                .andExpect(jsonPath("$.questions[0].choices[0].choiceText").exists())
                // These must never appear in the response regardless of JSON serialisation settings.
                .andExpect(jsonPath("$.questions[0].choices[0].isCorrect").doesNotExist())
                .andExpect(jsonPath("$.questions[0].choices[0].correct").doesNotExist())
                .andExpect(jsonPath("$.questions[0].choices[1].isCorrect").doesNotExist())
                .andExpect(jsonPath("$.questions[0].choices[1].correct").doesNotExist());
    }
}
