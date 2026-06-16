package com.baghdad.edulife.features.advisor.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.advisor.model.AdvisorRecommendation;
import com.baghdad.edulife.features.advisor.model.AdvisorUiState;
import com.baghdad.edulife.features.advisor.viewmodel.AdvisorViewModel;

public class AdvisorFragment extends Fragment {

    private AdvisorViewModel viewModel;
    private AdvisorRecommendationAdapter adapter;

    private EditText goalInput;
    private Button analyzeButton;
    private View loadingCard;
    private TextView goalEcho;
    private View responseCard;
    private TextView cardTitle;
    private ImageView cardIcon;
    private TextView assistantMessage;
    private TextView emptyState;
    private View errorCard;
    private TextView errorText;
    private Button retryButton;
    private TextView resultsTitle;
    private RecyclerView recycler;

    public AdvisorFragment() {
        super(R.layout.fragment_advisor);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(AdvisorViewModel.class);

        View header = view.findViewById(R.id.advisorHeaderLayout);
        final int originalHeaderTop = header.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            header.setPadding(header.getPaddingLeft(), originalHeaderTop + top,
                    header.getPaddingRight(), header.getPaddingBottom());
            return WindowInsetsCompat.CONSUMED;
        });

        goalInput = view.findViewById(R.id.advisorGoalInput);
        analyzeButton = view.findViewById(R.id.advisorAnalyzeButton);
        loadingCard = view.findViewById(R.id.advisorLoadingCard);
        goalEcho = view.findViewById(R.id.advisorGoalEcho);
        responseCard = view.findViewById(R.id.advisorResponseCard);
        cardTitle = view.findViewById(R.id.advisorCardTitle);
        cardIcon = view.findViewById(R.id.advisorCardIcon);
        assistantMessage = view.findViewById(R.id.advisorAssistantMessage);
        emptyState = view.findViewById(R.id.advisorEmptyState);
        errorCard = view.findViewById(R.id.advisorErrorCard);
        errorText = view.findViewById(R.id.advisorErrorText);
        retryButton = view.findViewById(R.id.advisorRetryButton);
        resultsTitle = view.findViewById(R.id.advisorResultsTitle);
        recycler = view.findViewById(R.id.advisorRecycler);

        adapter = new AdvisorRecommendationAdapter(this::openCourse);
        recycler.setAdapter(adapter);

        view.findViewById(R.id.advisorBackButton)
                .setOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        analyzeButton.setOnClickListener(v -> {
            hideKeyboard(v);
            viewModel.recommend(goalInput.getText().toString());
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), this::renderState);
    }

    private void renderState(AdvisorUiState state) {
        if (state == null) return;

        analyzeButton.setEnabled(!state.loading);
        loadingCard.setVisibility(state.loading ? View.VISIBLE : View.GONE);

        if (state.learnerGoal != null && !state.learnerGoal.isBlank()) {
            goalEcho.setText(getString(R.string.career_advisor_goal_echo, state.learnerGoal));
            goalEcho.setVisibility(View.VISIBLE);
        } else {
            goalEcho.setVisibility(View.GONE);
        }

        boolean hasError = state.errorMessage != null && !state.errorMessage.isBlank();
        if (hasError) {
            errorText.setText(state.errorMessage);
            errorCard.setVisibility(View.VISIBLE);
            // Show retry only when there is a non-rate-limit retriable goal and not loading.
            boolean showRetry = !state.isRateLimit && state.retryGoal != null;
            retryButton.setVisibility(showRetry ? View.VISIBLE : View.GONE);
            if (showRetry) {
                String goal = state.retryGoal;
                retryButton.setOnClickListener(v -> viewModel.recommend(goal));
            }
        } else {
            errorCard.setVisibility(View.GONE);
        }

        boolean hasRecs = state.recommendations != null && !state.recommendations.isEmpty();
        boolean isSuccessEmpty = !state.loading && !hasError
                && state.learnerGoal != null && !state.learnerGoal.isBlank()
                && (state.recommendations != null && state.recommendations.isEmpty())
                && (state.assistantMessage == null || state.assistantMessage.isBlank());

        emptyState.setVisibility(isSuccessEmpty ? View.VISIBLE : View.GONE);

        if (!state.assistantMessage.isBlank() || !hasError) {
            assistantMessage.setText(state.assistantMessage);
            responseCard.setVisibility(View.VISIBLE);
            if (hasRecs) {
                responseCard.setBackgroundResource(R.drawable.bg_advisor_response_card);
                cardTitle.setText("Advisor Analysis");
                cardIcon.setImageTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.brand_primary)));
            } else {
                responseCard.setBackgroundResource(R.drawable.bg_advisor_guidance_card);
                cardTitle.setText("AI Career Guide");
                cardIcon.setImageTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.brand_text_secondary)));
            }
        } else {
            responseCard.setVisibility(View.GONE);
        }

        adapter.submitList(state.recommendations);
        resultsTitle.setVisibility(hasRecs ? View.VISIBLE : View.GONE);
        recycler.setVisibility(hasRecs ? View.VISIBLE : View.GONE);
    }

    private void openCourse(AdvisorRecommendation recommendation) {
        if (recommendation == null
                || recommendation.courseId == null
                || recommendation.courseId.isBlank()) {
            return;
        }
        Bundle args = new Bundle();
        args.putString("courseId", recommendation.courseId);
        args.putBoolean("isEnrolled", false);
        Navigation.findNavController(requireView())
                .navigate(R.id.action_careerAdvisorFragment_to_courseDetailFragment, args);
    }

    private void hideKeyboard(View view) {
        InputMethodManager manager =
                (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null) {
            manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
