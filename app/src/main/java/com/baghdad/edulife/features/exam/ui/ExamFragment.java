package com.baghdad.edulife.features.exam.ui;

import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.exam.model.ExamDto;
import com.baghdad.edulife.features.exam.model.ExamResultDto;
import com.baghdad.edulife.features.exam.model.ExamResultUiState;
import com.baghdad.edulife.features.exam.model.ExamUiState;
import com.baghdad.edulife.features.exam.model.SubmitExamRequest;
import com.baghdad.edulife.features.exam.viewmodel.ExamViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExamFragment extends Fragment {

    private ExamViewModel viewModel;
    private final Map<String, String> answers = new HashMap<>();

    private ProgressBar loadingIndicator;
    private TextView statusText;
    private ScrollView scrollView;
    private LinearLayout questionsContainer;
    private LinearLayout submitFooter;
    private Button submitButton;
    private TextView validationText;
    private TextView examTitleText;
    private TextView examPassScoreText;
    private TextView examQuestionCountText;

    private String courseId;

    public ExamFragment() {
        super(R.layout.fragment_exam);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ExamViewModel.class);

        loadingIndicator = view.findViewById(R.id.examLoadingIndicator);
        statusText = view.findViewById(R.id.examStatusText);
        scrollView = view.findViewById(R.id.examScrollView);
        questionsContainer = view.findViewById(R.id.examQuestionsContainer);
        submitFooter = view.findViewById(R.id.examSubmitFooter);
        submitButton = view.findViewById(R.id.examSubmitButton);
        validationText = view.findViewById(R.id.examValidationText);
        examTitleText = view.findViewById(R.id.examTitleText);
        examPassScoreText = view.findViewById(R.id.examPassScoreText);
        examQuestionCountText = view.findViewById(R.id.examQuestionCountText);

        courseId = getArguments() != null ? getArguments().getString("courseId", "") : "";
        String examTitle = getArguments() != null ? getArguments().getString("examTitle", "Exam") : "Exam";
        examTitleText.setText(examTitle);

        view.findViewById(R.id.examBackButton).setOnClickListener(v ->
                Navigation.findNavController(view).popBackStack());

        viewModel.getExamState().observe(getViewLifecycleOwner(), this::renderExamState);
        viewModel.getResultState().observe(getViewLifecycleOwner(), this::renderResultState);

        if (viewModel.getExamState().getValue() == null
                || viewModel.getExamState().getValue().exam == null) {
            viewModel.loadExam(courseId);
        }

        submitButton.setOnClickListener(v -> onSubmitClicked());
    }

    private void renderExamState(ExamUiState state) {
        if (state == null) return;

        loadingIndicator.setVisibility(state.loading ? View.VISIBLE : View.GONE);

        if (state.loading) {
            scrollView.setVisibility(View.GONE);
            submitFooter.setVisibility(View.GONE);
            statusText.setVisibility(View.VISIBLE);
            statusText.setText(R.string.exam_loading);
            return;
        }

        if (state.errorMessage != null) {
            scrollView.setVisibility(View.GONE);
            submitFooter.setVisibility(View.GONE);
            statusText.setVisibility(View.VISIBLE);
            statusText.setText(state.errorMessage);
            return;
        }

        if (state.exam != null) {
            bindExam(state.exam);
            submitButton.setEnabled(!state.submitting);
            submitButton.setText(state.submitting ? R.string.exam_submitting : R.string.exam_submit);
        }
    }

    private void renderResultState(ExamResultUiState state) {
        if (state == null || (state.result == null && state.errorMessage == null)) return;

        if (state.errorMessage != null) {
            validationText.setVisibility(View.VISIBLE);
            validationText.setText(state.errorMessage);
            submitButton.setEnabled(true);
            submitButton.setText(R.string.exam_submit);
            return;
        }

        if (state.result != null) {
            navigateToResult(state.result);
        }
    }

    private void bindExam(ExamDto exam) {
        examPassScoreText.setText(getString(R.string.exam_pass_score, exam.passScore));
        int count = exam.questions != null ? exam.questions.size() : 0;
        examQuestionCountText.setText(getString(R.string.exam_question_count, count));

        questionsContainer.removeAllViews();
        answers.clear();

        if (exam.questions != null) {
            for (int i = 0; i < exam.questions.size(); i++) {
                questionsContainer.addView(createQuestionCard(i + 1, exam.questions.get(i)));
            }
        }

        statusText.setVisibility(View.GONE);
        scrollView.setVisibility(View.VISIBLE);
        submitFooter.setVisibility(View.VISIBLE);
    }

    private View createQuestionCard(int number, ExamDto.QuestionDto question) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_course_card);
        card.setPadding(dp(20), dp(20), dp(20), dp(20));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(16);
        card.setLayoutParams(params);

        TextView questionText = new TextView(requireContext());
        questionText.setText(number + ". " + question.questionText);
        questionText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        questionText.setTextColor(requireContext().getColor(R.color.brand_text_primary));
        questionText.setTypeface(questionText.getTypeface(), Typeface.BOLD);
        questionText.setPadding(0, 0, 0, dp(14));
        card.addView(questionText);

        RadioGroup radioGroup = new RadioGroup(requireContext());
        radioGroup.setOrientation(RadioGroup.VERTICAL);

        if (question.choices != null) {
            for (ExamDto.ChoiceDto choice : question.choices) {
                RadioButton rb = new RadioButton(requireContext());
                rb.setText(choice.choiceText);
                rb.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                rb.setTextColor(requireContext().getColor(R.color.brand_text_body));
                rb.setPadding(dp(4), dp(10), dp(4), dp(10));
                rb.setTag(choice.choiceId);
                radioGroup.addView(rb);
            }
        }

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton selected = group.findViewById(checkedId);
            if (selected != null && selected.getTag() != null) {
                answers.put(question.questionId, (String) selected.getTag());
                validationText.setVisibility(View.GONE);
            }
        });

        card.addView(radioGroup);
        return card;
    }

    private void onSubmitClicked() {
        ExamUiState state = viewModel.getExamState().getValue();
        if (state == null || state.exam == null || state.exam.questions == null) return;

        // Validate all questions answered
        for (ExamDto.QuestionDto q : state.exam.questions) {
            if (!answers.containsKey(q.questionId)) {
                validationText.setText(R.string.exam_answer_all);
                validationText.setVisibility(View.VISIBLE);
                scrollView.smoothScrollTo(0, 0);
                return;
            }
        }

        validationText.setVisibility(View.GONE);

        List<SubmitExamRequest.AnswerDto> answerList = new ArrayList<>();
        for (Map.Entry<String, String> entry : answers.entrySet()) {
            answerList.add(new SubmitExamRequest.AnswerDto(entry.getKey(), entry.getValue()));
        }

        viewModel.submitExam(courseId, answerList);
    }

    private void navigateToResult(ExamResultDto result) {
        Bundle args = new Bundle();
        args.putInt("score", result.score);
        args.putInt("passScore", result.passScore);
        args.putBoolean("passed", result.passed);
        args.putString("certificateNumber", result.certificateNumber != null ? result.certificateNumber : "");
        args.putString("courseId", courseId);
        Navigation.findNavController(requireView())
                .navigate(R.id.action_examFragment_to_examResultFragment, args);
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value,
                requireContext().getResources().getDisplayMetrics());
    }
}
