package com.baghdad.edulife.features.courses.ui;

import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.courses.model.ExamChoice;
import com.baghdad.edulife.features.courses.model.ExamQuestion;
import com.baghdad.edulife.features.courses.model.ExamResponse;
import com.baghdad.edulife.features.courses.model.ExamStatusResponse;
import com.baghdad.edulife.features.courses.model.ExamStatusUiState;
import com.baghdad.edulife.features.courses.model.ExamSubmitUiState;
import com.baghdad.edulife.features.courses.model.ExamUiState;
import com.baghdad.edulife.features.courses.model.SubmitExamRequest;
import com.baghdad.edulife.features.courses.viewmodel.ExamViewModel;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExamFragment extends Fragment {

    private ExamViewModel examViewModel;
    private ProgressBar loadingIndicator;
    private TextView statusText;
    private FrameLayout examGateContainer;
    private TextView examGateEyebrow;
    private TextView examGateTitle;
    private TextView examGateBody;
    private TextView examGateMeta;
    private Button examGateButton;
    private ScrollView scrollView;
    private LinearLayout questionsContainer;
    private LinearLayout submitFooter;
    private TextView progressText;
    private Button submitButton;

    private final Map<String, String> selectedChoices = new HashMap<>();
    private ExamResponse currentExam;
    private String courseId;

    public ExamFragment() {
        super(R.layout.fragment_exam);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        examViewModel = new ViewModelProvider(this).get(ExamViewModel.class);

        loadingIndicator = view.findViewById(R.id.examLoadingIndicator);
        statusText = view.findViewById(R.id.examStatusText);
        examGateContainer = view.findViewById(R.id.examGateContainer);
        examGateEyebrow = view.findViewById(R.id.examGateEyebrow);
        examGateTitle = view.findViewById(R.id.examGateTitle);
        examGateBody = view.findViewById(R.id.examGateBody);
        examGateMeta = view.findViewById(R.id.examGateMeta);
        examGateButton = view.findViewById(R.id.examGateButton);
        scrollView = view.findViewById(R.id.examScrollView);
        questionsContainer = view.findViewById(R.id.examQuestionsContainer);
        submitFooter = view.findViewById(R.id.examSubmitFooter);
        progressText = view.findViewById(R.id.examProgressText);
        submitButton = view.findViewById(R.id.examSubmitButton);

        view.findViewById(R.id.examBackButton).setOnClickListener(v ->
                Navigation.findNavController(view).popBackStack());
        examGateButton.setOnClickListener(v ->
                Navigation.findNavController(view).popBackStack());

        courseId = getArguments() != null ? getArguments().getString("courseId", "") : "";

        examViewModel.getExamStatusState().observe(getViewLifecycleOwner(), this::renderExamStatusState);
        examViewModel.getExamState().observe(getViewLifecycleOwner(), this::renderExamState);
        examViewModel.getSubmitState().observe(getViewLifecycleOwner(), this::renderSubmitState);

        submitButton.setOnClickListener(v -> {
            submitButton.setEnabled(false);
            List<SubmitExamRequest.AnswerItem> answers = new ArrayList<>();
            for (Map.Entry<String, String> entry : selectedChoices.entrySet()) {
                answers.add(new SubmitExamRequest.AnswerItem(entry.getKey(), entry.getValue()));
            }
            examViewModel.submitExam(courseId, answers);
        });

        ExamStatusUiState currentStatusState = examViewModel.getExamStatusState().getValue();
        if (currentStatusState == null || currentStatusState.status == null) {
            // Access is checked first so users in cooldown never see answer inputs they cannot submit.
            examViewModel.loadExamStatus(courseId);
        }
    }

    private void renderExamStatusState(ExamStatusUiState state) {
        if (state == null) return;

        if (state.loading) {
            showInlineStatus(getString(R.string.exam_status_loading), true);
            return;
        }

        if (state.errorMessage != null && !state.errorMessage.isBlank()) {
            showInlineStatus(state.errorMessage, false);
            return;
        }

        if (state.status == null) return;

        ExamStatusResponse status = state.status;
        if (status.passed) {
            // The learner loop awards a certificate after passing, so the exam should lock here
            // instead of letting the user re-enter the question flow.
            showGateState(
                    getString(R.string.exam_gate_passed_eyebrow),
                    getString(R.string.exam_gate_passed_title),
                    getString(R.string.exam_gate_passed_body),
                    getString(R.string.exam_gate_passed_meta)
            );
            return;
        }

        if (status.inCooldown) {
            showGateState(
                    getString(R.string.exam_gate_cooldown_eyebrow),
                    getString(R.string.exam_gate_cooldown_title),
                    getString(R.string.exam_gate_cooldown_body, formatInstant(status.cooldownEndsAt)),
                    getString(
                            R.string.exam_gate_cooldown_meta,
                            status.failedAttempts,
                            status.maxAttemptsBeforeCooldown
                    )
            );
            return;
        }

        ExamUiState currentExamState = examViewModel.getExamState().getValue();
        if (currentExamState == null || currentExamState.exam == null) {
            examViewModel.loadExam(courseId);
        }
    }

    private void renderExamState(ExamUiState state) {
        if (state == null) return;

        loadingIndicator.setVisibility(state.loading ? View.VISIBLE : View.GONE);

        if (state.loading) {
            showInlineStatus(getString(R.string.exam_loading), true);
            return;
        }

        if (state.errorMessage != null && !state.errorMessage.isBlank()) {
            showInlineStatus(state.errorMessage, false);
            return;
        }

        if (state.exam != null) {
            currentExam = state.exam;
            bindExam(state.exam);
        }
    }

    private void renderSubmitState(ExamSubmitUiState state) {
        if (state == null) return;

        if (state.loading) {
            submitButton.setEnabled(false);
            return;
        }

        if (state.alreadyPassed) {
            submitButton.setEnabled(false);
            showGateState(
                    getString(R.string.exam_gate_passed_eyebrow),
                    getString(R.string.exam_gate_passed_title),
                    getString(R.string.exam_gate_passed_body),
                    getString(R.string.exam_gate_passed_meta)
            );
            return;
        }

        if (state.cooldownEndsAt != null) {
            submitButton.setEnabled(false);
            showGateState(
                    getString(R.string.exam_gate_cooldown_eyebrow),
                    getString(R.string.exam_gate_cooldown_title),
                    getString(R.string.exam_gate_cooldown_body, formatInstant(state.cooldownEndsAt)),
                    getString(R.string.exam_gate_cooldown_meta_generic)
            );
            return;
        }

        if (state.errorMessage != null && !state.errorMessage.isBlank()) {
            submitButton.setEnabled(true);
            Toast.makeText(requireContext(), state.errorMessage, Toast.LENGTH_LONG).show();
            return;
        }

        if (state.result != null) {
            Bundle args = new Bundle();
            args.putString("courseId", courseId);
            args.putString("examId", state.result.examId != null ? state.result.examId : "");
            args.putInt("score", state.result.score);
            args.putInt("passScore", state.result.passScore);
            args.putBoolean("passed", state.result.passed);
            args.putString("certificateNumber", state.result.certificateNumber != null ? state.result.certificateNumber : "");
            args.putInt("attemptsUsed", state.result.attemptsUsed);
            args.putString("cooldownEndsAt", state.result.cooldownEndsAt != null ? state.result.cooldownEndsAt : "");
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_examFragment_to_examResultFragment, args);
        }
    }

    private void bindExam(ExamResponse exam) {
        TextView titleText = requireView().findViewById(R.id.examTitleText);
        TextView passScoreText = requireView().findViewById(R.id.examPassScoreText);
        TextView questionCountText = requireView().findViewById(R.id.examQuestionCountText);

        titleText.setText(exam.title);
        passScoreText.setText(getString(R.string.exam_pass_score_label, exam.passScore));
        int count = exam.questions != null ? exam.questions.size() : 0;
        questionCountText.setText(getString(R.string.exam_question_count, count));

        renderQuestions(exam);

        examGateContainer.setVisibility(View.GONE);
        statusText.setVisibility(View.GONE);
        scrollView.setVisibility(View.VISIBLE);
        submitFooter.setVisibility(View.VISIBLE);
        updateProgress(count);
    }

    private void renderQuestions(ExamResponse exam) {
        questionsContainer.removeAllViews();
        if (exam.questions == null) return;
        for (int i = 0; i < exam.questions.size(); i++) {
            ExamQuestion q = exam.questions.get(i);
            questionsContainer.addView(createQuestionView(i + 1, q, exam.questions.size()));
        }
    }

    private View createQuestionView(int number, ExamQuestion question, int totalQuestions) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(requireContext().getColor(R.color.brand_surface));
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = dp(12);
        card.setLayoutParams(params);

        TextView questionNumber = new TextView(requireContext());
        questionNumber.setText(getString(R.string.exam_question_label, number));
        questionNumber.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        questionNumber.setTextColor(requireContext().getColor(R.color.brand_text_secondary));
        LinearLayout.LayoutParams numParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        numParams.bottomMargin = dp(6);
        questionNumber.setLayoutParams(numParams);

        TextView questionText = new TextView(requireContext());
        questionText.setText(question.questionText);
        questionText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        questionText.setTypeface(questionText.getTypeface(), Typeface.BOLD);
        questionText.setTextColor(requireContext().getColor(R.color.brand_text_primary));
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        textParams.bottomMargin = dp(12);
        questionText.setLayoutParams(textParams);

        RadioGroup radioGroup = new RadioGroup(requireContext());
        radioGroup.setOrientation(RadioGroup.VERTICAL);

        if (question.choices != null) {
            for (ExamChoice choice : question.choices) {
                RadioButton rb = new RadioButton(requireContext());
                rb.setText(choice.choiceText);
                rb.setTag(choice.choiceId);
                rb.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                rb.setPadding(dp(4), dp(8), dp(4), dp(8));
                rb.setTextColor(requireContext().getColor(R.color.brand_text_primary));
                radioGroup.addView(rb);
            }
        }

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton selected = group.findViewById(checkedId);
            if (selected != null) {
                selectedChoices.put(question.questionId, (String) selected.getTag());
                updateProgress(totalQuestions);
            }
        });

        card.addView(questionNumber);
        card.addView(questionText);
        card.addView(radioGroup);
        return card;
    }

    private void updateProgress(int totalQuestions) {
        int answered = selectedChoices.size();
        progressText.setText(getString(R.string.exam_progress, answered, totalQuestions));
        submitButton.setEnabled(answered == totalQuestions && totalQuestions > 0);
    }

    private void showInlineStatus(String message, boolean showLoading) {
        loadingIndicator.setVisibility(showLoading ? View.VISIBLE : View.GONE);
        examGateContainer.setVisibility(View.GONE);
        scrollView.setVisibility(View.GONE);
        submitFooter.setVisibility(View.GONE);
        statusText.setVisibility(View.VISIBLE);
        statusText.setText(message);
    }

    private void showGateState(String eyebrow, String title, String body, String meta) {
        loadingIndicator.setVisibility(View.GONE);
        statusText.setVisibility(View.GONE);
        scrollView.setVisibility(View.GONE);
        submitFooter.setVisibility(View.GONE);
        examGateContainer.setVisibility(View.VISIBLE);

        examGateEyebrow.setText(eyebrow);
        examGateTitle.setText(title);
        examGateBody.setText(body);

        if (meta == null || meta.isBlank()) {
            examGateMeta.setVisibility(View.GONE);
        } else {
            examGateMeta.setVisibility(View.VISIBLE);
            examGateMeta.setText(meta);
        }
    }

    private String formatInstant(String isoInstant) {
        if (isoInstant == null || isoInstant.isBlank()) {
            return getString(R.string.exam_status_unknown_time);
        }
        try {
            Instant instant = Instant.parse(isoInstant);
            DateTimeFormatter formatter = DateTimeFormatter
                    .ofPattern("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
                    .withZone(ZoneId.systemDefault());
            return formatter.format(instant);
        } catch (Exception e) {
            return isoInstant;
        }
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                requireContext().getResources().getDisplayMetrics()
        );
    }
}
