package com.baghdad.edulife.features.exams.ui;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.exams.viewmodel.CmsExamBuilderViewModel.ChoiceDraft;
import com.baghdad.edulife.features.exams.viewmodel.CmsExamBuilderViewModel.QuestionDraft;

import java.util.List;

public class CmsExamQuestionAdapter
        extends RecyclerView.Adapter<CmsExamQuestionAdapter.QuestionViewHolder> {

    public interface QuestionListener {
        void onQuestionTextChanged(int position, String text);
        void onChoiceTextChanged(int questionPosition, int choicePosition, String text);
        void onCorrectChoiceSelected(int questionPosition, int choicePosition);
        void onAddChoice(int questionPosition);
        void onRemoveChoice(int questionPosition, int choicePosition);
        void onRemoveQuestion(int position);
    }

    private final List<QuestionDraft> questions;
    private final QuestionListener listener;

    public CmsExamQuestionAdapter(List<QuestionDraft> questions, QuestionListener listener) {
        this.questions = questions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public QuestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cms_exam_question, parent, false);
        return new QuestionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuestionViewHolder holder, int position) {
        holder.bind(questions.get(position), position);
    }

    @Override
    public int getItemCount() {
        return questions.size();
    }

    class QuestionViewHolder extends RecyclerView.ViewHolder {

        private final TextView questionNumberLabel;
        private final EditText questionTextInput;
        private final TextView removeQuestionButton;
        private final LinearLayout choicesContainer;
        private final TextView addChoiceButton;

        private TextWatcher questionWatcher;

        QuestionViewHolder(@NonNull View itemView) {
            super(itemView);
            questionNumberLabel = itemView.findViewById(R.id.questionNumberLabel);
            questionTextInput = itemView.findViewById(R.id.questionTextInput);
            removeQuestionButton = itemView.findViewById(R.id.removeQuestionButton);
            choicesContainer = itemView.findViewById(R.id.choicesContainer);
            addChoiceButton = itemView.findViewById(R.id.addChoiceButton);
        }

        void bind(QuestionDraft draft, int position) {
            questionNumberLabel.setText(
                    itemView.getContext().getString(R.string.cms_exam_question_number, position + 1));

            if (questionWatcher != null) {
                questionTextInput.removeTextChangedListener(questionWatcher);
            }
            questionTextInput.setText(draft.questionText);
            questionWatcher = new SimpleTextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    int pos = getBindingAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        listener.onQuestionTextChanged(pos, s.toString());
                    }
                }
            };
            questionTextInput.addTextChangedListener(questionWatcher);

            removeQuestionButton.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onRemoveQuestion(pos);
                }
            });

            addChoiceButton.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onAddChoice(pos);
                }
            });

            buildChoiceViews(draft, position);
        }

        private void buildChoiceViews(QuestionDraft draft, int questionPosition) {
            choicesContainer.removeAllViews();

            for (int i = 0; i < draft.choices.size(); i++) {
                ChoiceDraft choice = draft.choices.get(i);
                final int choiceIndex = i;

                LinearLayout row = new LinearLayout(itemView.getContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                rowParams.topMargin = dpToPx(4);
                row.setLayoutParams(rowParams);

                RadioButton radio = new RadioButton(itemView.getContext());
                radio.setChecked(choice.correct);
                LinearLayout.LayoutParams radioParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                radio.setLayoutParams(radioParams);
                radio.setOnClickListener(v -> {
                    int qPos = getBindingAdapterPosition();
                    if (qPos != RecyclerView.NO_POSITION) {
                        listener.onCorrectChoiceSelected(qPos, choiceIndex);
                        updateRadioStates(draft);
                    }
                });

                EditText choiceInput = new EditText(itemView.getContext());
                choiceInput.setHint(R.string.cms_exam_choice_hint);
                choiceInput.setText(choice.choiceText);
                choiceInput.setTextSize(13);
                choiceInput.setSingleLine(true);
                LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                choiceInput.setLayoutParams(inputParams);
                choiceInput.addTextChangedListener(new SimpleTextWatcher() {
                    @Override
                    public void afterTextChanged(Editable s) {
                        int qPos = getBindingAdapterPosition();
                        if (qPos != RecyclerView.NO_POSITION) {
                            listener.onChoiceTextChanged(qPos, choiceIndex, s.toString());
                        }
                    }
                });

                TextView removeBtn = new TextView(itemView.getContext());
                removeBtn.setText("✕");
                removeBtn.setTextColor(itemView.getContext().getColor(R.color.brand_error));
                removeBtn.setTextSize(14);
                removeBtn.setPadding(dpToPx(8), dpToPx(4), dpToPx(4), dpToPx(4));
                removeBtn.setOnClickListener(v -> {
                    int qPos = getBindingAdapterPosition();
                    if (qPos != RecyclerView.NO_POSITION) {
                        listener.onRemoveChoice(qPos, choiceIndex);
                    }
                });

                row.addView(radio);
                row.addView(choiceInput);
                row.addView(removeBtn);
                choicesContainer.addView(row);
            }
        }

        private void updateRadioStates(QuestionDraft draft) {
            for (int i = 0; i < choicesContainer.getChildCount() && i < draft.choices.size(); i++) {
                LinearLayout row = (LinearLayout) choicesContainer.getChildAt(i);
                RadioButton radio = (RadioButton) row.getChildAt(0);
                radio.setChecked(draft.choices.get(i).correct);
            }
        }

        private int dpToPx(int dp) {
            return (int) (dp * itemView.getContext().getResources().getDisplayMetrics().density);
        }
    }

    private static abstract class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }
}
