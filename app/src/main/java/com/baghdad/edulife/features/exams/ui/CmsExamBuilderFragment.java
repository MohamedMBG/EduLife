package com.baghdad.edulife.features.exams.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.exams.model.CmsExamChoice;
import com.baghdad.edulife.features.exams.model.CmsExamQuestion;
import com.baghdad.edulife.features.exams.model.CmsExamResponse;
import com.baghdad.edulife.features.exams.viewmodel.CmsExamBuilderViewModel;
import com.google.android.material.progressindicator.CircularProgressIndicator;

public class CmsExamBuilderFragment extends Fragment {

    private CmsExamBuilderViewModel viewModel;
    private CmsExamQuestionAdapter adapter;

    private String courseId;

    private View stateCard;
    private CircularProgressIndicator loadingIndicator;
    private TextView stateText;
    private TextView retryButton;
    private View scrollView;
    private View existingExamSection;
    private TextView existingExamTitle;
    private TextView existingExamPassScore;
    private TextView existingExamTimeLimit;
    private LinearLayout existingExamQuestionsContainer;
    private View existingExamActions;
    private TextView examEditButton;
    private TextView examDeleteButton;
    private View builderSection;
    private RecyclerView questionsRecycler;
    private EditText titleInput;
    private EditText passScoreInput;
    private EditText timeLimitInput;
    private View bottomActions;
    private TextView saveButton;
    private TextView cancelButton;

    public CmsExamBuilderFragment() {
        super(R.layout.fragment_cms_exam_builder);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(CmsExamBuilderViewModel.class);

        Bundle args = getArguments();
        courseId = args != null ? args.getString("courseId", "") : "";

        bindViews(view);
        setupAdapter();
        observeViewModel();

        viewModel.loadExam(courseId);
    }

    private void bindViews(View view) {
        view.findViewById(R.id.examBuilderBackButton).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        stateCard = view.findViewById(R.id.examBuilderStateCard);
        loadingIndicator = view.findViewById(R.id.examBuilderLoadingIndicator);
        stateText = view.findViewById(R.id.examBuilderStateText);
        retryButton = view.findViewById(R.id.examBuilderRetryButton);
        retryButton.setOnClickListener(v -> viewModel.retryLoad(courseId));

        scrollView = view.findViewById(R.id.examBuilderScrollView);
        existingExamSection = view.findViewById(R.id.existingExamSection);
        existingExamTitle = view.findViewById(R.id.existingExamTitle);
        existingExamPassScore = view.findViewById(R.id.existingExamPassScore);
        existingExamTimeLimit = view.findViewById(R.id.existingExamTimeLimit);
        existingExamQuestionsContainer = view.findViewById(R.id.existingExamQuestionsContainer);
        existingExamActions = view.findViewById(R.id.existingExamActions);
        examEditButton = view.findViewById(R.id.examEditButton);
        examDeleteButton = view.findViewById(R.id.examDeleteButton);
        builderSection = view.findViewById(R.id.builderSection);
        questionsRecycler = view.findViewById(R.id.examQuestionsRecycler);
        titleInput = view.findViewById(R.id.examTitleInput);
        passScoreInput = view.findViewById(R.id.examPassScoreInput);
        timeLimitInput = view.findViewById(R.id.examTimeLimitInput);
        bottomActions = view.findViewById(R.id.examBottomActions);
        saveButton = view.findViewById(R.id.examSaveButton);
        cancelButton = view.findViewById(R.id.examCancelButton);

        saveButton.setOnClickListener(v -> {
            Boolean editing = viewModel.getIsEditMode().getValue();
            if (editing != null && editing) {
                viewModel.saveChanges(courseId);
            } else {
                viewModel.saveExam(courseId);
            }
        });

        cancelButton.setOnClickListener(v -> viewModel.cancelEditMode());

        examEditButton.setOnClickListener(v -> viewModel.enterEditMode());

        examDeleteButton.setOnClickListener(v -> showDeleteConfirmation());

        view.findViewById(R.id.examAddQuestionButton).setOnClickListener(v -> {
            viewModel.addQuestion();
            int count = viewModel.getDrafts().size();
            adapter.notifyItemInserted(count - 1);
            questionsRecycler.post(() -> questionsRecycler.smoothScrollToPosition(count - 1));
        });

        titleInput.addTextChangedListener(simpleWatcher(s -> viewModel.setExamTitle(s)));
        passScoreInput.addTextChangedListener(simpleWatcher(s -> {
            try {
                viewModel.setPassScore(Integer.parseInt(s));
            } catch (NumberFormatException ignored) {}
        }));
        timeLimitInput.addTextChangedListener(simpleWatcher(s -> {
            try {
                viewModel.setTimeLimitMinutes(s.isEmpty() ? null : Integer.parseInt(s));
            } catch (NumberFormatException ignored) {}
        }));
    }

    private void setupAdapter() {
        adapter = new CmsExamQuestionAdapter(viewModel.getDrafts(),
                new CmsExamQuestionAdapter.QuestionListener() {
            @Override
            public void onQuestionTextChanged(int position, String text) {
                viewModel.updateQuestionText(position, text);
            }

            @Override
            public void onChoiceTextChanged(int questionPosition, int choicePosition, String text) {
                viewModel.updateChoiceText(questionPosition, choicePosition, text);
            }

            @Override
            public void onCorrectChoiceSelected(int questionPosition, int choicePosition) {
                viewModel.setCorrectChoice(questionPosition, choicePosition);
            }

            @Override
            public void onAddChoice(int questionPosition) {
                viewModel.addChoice(questionPosition);
                adapter.notifyItemChanged(questionPosition);
            }

            @Override
            public void onRemoveChoice(int questionPosition, int choicePosition) {
                viewModel.removeChoice(questionPosition, choicePosition);
                adapter.notifyItemChanged(questionPosition);
            }

            @Override
            public void onRemoveQuestion(int position) {
                viewModel.removeQuestion(position);
                adapter.notifyItemRemoved(position);
                adapter.notifyItemRangeChanged(position, adapter.getItemCount());
            }
        });

        questionsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        questionsRecycler.setNestedScrollingEnabled(false);
        questionsRecycler.setAdapter(adapter);
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            if (loading != null && loading) {
                stateCard.setVisibility(View.VISIBLE);
                loadingIndicator.setVisibility(View.VISIBLE);
                stateText.setText(R.string.teacher_loading);
                retryButton.setVisibility(View.GONE);
                scrollView.setVisibility(View.GONE);
                bottomActions.setVisibility(View.GONE);
            }
        });

        viewModel.getLoadError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                stateCard.setVisibility(View.VISIBLE);
                loadingIndicator.setVisibility(View.GONE);
                stateText.setText(error);
                retryButton.setVisibility(viewModel.isAccessDenied() ? View.GONE : View.VISIBLE);
                scrollView.setVisibility(View.GONE);
                bottomActions.setVisibility(View.GONE);
            }
        });

        viewModel.getExistingExam().observe(getViewLifecycleOwner(), exam -> {
            if (exam != null) {
                showExistingExam(exam);
            }
        });

        viewModel.getShowBuilder().observe(getViewLifecycleOwner(), show -> {
            if (show != null && show) {
                Boolean editing = viewModel.getIsEditMode().getValue();
                boolean isEdit = editing != null && editing;

                stateCard.setVisibility(View.GONE);
                scrollView.setVisibility(View.VISIBLE);
                existingExamSection.setVisibility(View.GONE);
                builderSection.setVisibility(View.VISIBLE);
                bottomActions.setVisibility(View.VISIBLE);

                if (isEdit) {
                    cancelButton.setVisibility(View.VISIBLE);
                    saveButton.setText(R.string.cms_exam_save_changes);
                    populateBuilderFromDrafts();
                } else {
                    cancelButton.setVisibility(View.GONE);
                    saveButton.setText(R.string.cms_exam_save);
                }
            }
        });

        viewModel.getIsEditMode().observe(getViewLifecycleOwner(), editing -> {
            if (editing != null && !editing) {
                CmsExamResponse exam = viewModel.getExistingExam().getValue();
                if (exam != null) {
                    showExistingExam(exam);
                }
            }
        });

        viewModel.getSaving().observe(getViewLifecycleOwner(), saving -> {
            if (saving != null) {
                saveButton.setEnabled(!saving);
                Boolean editing = viewModel.getIsEditMode().getValue();
                boolean isEdit = editing != null && editing;
                saveButton.setText(saving
                        ? R.string.cms_exam_saving
                        : (isEdit ? R.string.cms_exam_save_changes : R.string.cms_exam_save));
                saveButton.setAlpha(saving ? 0.6f : 1.0f);
            }
        });

        viewModel.getDeleting().observe(getViewLifecycleOwner(), deleting -> {
            if (deleting != null) {
                examDeleteButton.setEnabled(!deleting);
                examDeleteButton.setText(deleting
                        ? R.string.cms_exam_deleting
                        : R.string.cms_exam_delete);
                examDeleteButton.setAlpha(deleting ? 0.6f : 1.0f);
            }
        });

        viewModel.getExamDeleted().observe(getViewLifecycleOwner(), deleted -> {
            if (deleted != null && deleted) {
                if (isAdded()) {
                    Navigation.findNavController(requireView()).navigateUp();
                }
            }
        });

        viewModel.getToastMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                viewModel.clearToast();
            }
        });
    }

    private void showExistingExam(CmsExamResponse exam) {
        stateCard.setVisibility(View.GONE);
        scrollView.setVisibility(View.VISIBLE);
        existingExamSection.setVisibility(View.VISIBLE);
        builderSection.setVisibility(View.GONE);
        bottomActions.setVisibility(View.GONE);
        existingExamActions.setVisibility(View.VISIBLE);

        existingExamTitle.setText(exam.title);
        existingExamPassScore.setText(
                getString(R.string.cms_exam_pass_score_label, exam.passScore));

        if (exam.timeLimitMinutes != null && exam.timeLimitMinutes > 0) {
            existingExamTimeLimit.setText(
                    getString(R.string.cms_exam_time_limit_label, exam.timeLimitMinutes));
        } else {
            existingExamTimeLimit.setText(R.string.cms_exam_no_time_limit);
        }

        existingExamQuestionsContainer.removeAllViews();
        if (exam.questions != null) {
            for (int i = 0; i < exam.questions.size(); i++) {
                CmsExamQuestion q = exam.questions.get(i);
                addExistingQuestionView(q, i + 1);
            }
        }
    }

    private void populateBuilderFromDrafts() {
        titleInput.setText(viewModel.getExamTitle());
        passScoreInput.setText(String.valueOf(viewModel.getPassScore()));
        Integer tl = viewModel.getTimeLimitMinutes();
        timeLimitInput.setText(tl != null ? String.valueOf(tl) : "");
        adapter.notifyDataSetChanged();
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.cms_exam_delete_title)
                .setMessage(R.string.cms_exam_delete_message)
                .setPositiveButton(R.string.cms_exam_delete, (dialog, which) ->
                        viewModel.deleteExam(courseId))
                .setNegativeButton(R.string.cms_exam_cancel, null)
                .show();
    }

    private void addExistingQuestionView(CmsExamQuestion question, int number) {
        int pad = dpToPx(16);
        int smallPad = dpToPx(8);

        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_teacher_course_card);
        card.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.topMargin = smallPad;
        card.setLayoutParams(cardParams);

        TextView qLabel = new TextView(requireContext());
        qLabel.setText(getString(R.string.cms_exam_question_number, number));
        qLabel.setTextColor(getResources().getColor(R.color.teacher_text_primary, null));
        qLabel.setTextSize(13);
        qLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(qLabel);

        TextView qText = new TextView(requireContext());
        qText.setText(question.questionText);
        qText.setTextColor(getResources().getColor(R.color.teacher_text_primary, null));
        qText.setTextSize(14);
        LinearLayout.LayoutParams qTextParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        qTextParams.topMargin = dpToPx(4);
        qText.setLayoutParams(qTextParams);
        card.addView(qText);

        if (question.choices != null) {
            for (CmsExamChoice choice : question.choices) {
                TextView choiceView = new TextView(requireContext());
                String prefix = choice.correct ? "✓  " : "○  ";
                choiceView.setText(prefix + choice.choiceText);
                choiceView.setTextSize(13);
                choiceView.setTextColor(getResources().getColor(
                        choice.correct ? R.color.brand_primary : R.color.teacher_text_secondary,
                        null));
                if (choice.correct) {
                    choiceView.setTypeface(null, android.graphics.Typeface.BOLD);
                }
                LinearLayout.LayoutParams cParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                cParams.topMargin = dpToPx(4);
                cParams.leftMargin = dpToPx(8);
                choiceView.setLayoutParams(cParams);
                card.addView(choiceView);
            }
        }

        existingExamQuestionsContainer.addView(card);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private TextWatcher simpleWatcher(java.util.function.Consumer<String> onChanged) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                onChanged.accept(s.toString());
            }
        };
    }
}
