package com.baghdad.edulife.features.courses.ui;

import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.courses.model.CareerAdvisorUiState;
import com.baghdad.edulife.features.courses.model.CareerCourseRecommendation;
import com.baghdad.edulife.features.courses.viewmodel.CareerAdvisorViewModel;

public class CareerAdvisorFragment extends Fragment {

    private CareerAdvisorViewModel viewModel;
    private CareerRecommendationAdapter adapter;

    private EditText goalInput;
    private Button analyzeButton;
    private View loadingIndicator;
    private TextView assistantBubble;
    private TextView goalEcho;
    private TextView errorText;
    private TextView resultsTitle;
    private RecyclerView recommendationRecycler;

    // View references to the card wrapper for AI recommendations to style it dynamically.
    private View cardContainer;
    private TextView cardTitle;
    private android.widget.ImageView cardIcon;

    public CareerAdvisorFragment() {
        super(R.layout.fragment_career_advisor);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(CareerAdvisorViewModel.class);

        View header = view.findViewById(R.id.careerAdvisorHeaderLayout);
        final int originalHeaderTop = header.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            header.setPadding(header.getPaddingLeft(), originalHeaderTop + top,
                    header.getPaddingRight(), header.getPaddingBottom());
            return WindowInsetsCompat.CONSUMED;
        });

        goalInput = view.findViewById(R.id.careerGoalInput);
        analyzeButton = view.findViewById(R.id.careerAnalyzeButton);
        loadingIndicator = view.findViewById(R.id.careerAdvisorLoading);
        assistantBubble = view.findViewById(R.id.careerAssistantBubble);
        goalEcho = view.findViewById(R.id.careerGoalEcho);
        errorText = view.findViewById(R.id.careerAdvisorErrorText);
        resultsTitle = view.findViewById(R.id.careerAdvisorResultsTitle);
        recommendationRecycler = view.findViewById(R.id.careerRecommendationRecycler);

        // Bind the card container views to dynamically adjust aesthetics based on response state.
        cardContainer = view.findViewById(R.id.careerAdvisorCardContainer);
        cardTitle = view.findViewById(R.id.careerAdvisorCardTitle);
        cardIcon = view.findViewById(R.id.careerAdvisorCardIcon);

        adapter = new CareerRecommendationAdapter(this::openRecommendedCourse);
        recommendationRecycler.setAdapter(adapter);

        view.findViewById(R.id.careerAdvisorBackButton)
                .setOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        analyzeButton.setOnClickListener(v -> {
            hideKeyboard(v);
            // The learner's text is sent only to the local ViewModel matcher; no free-form goal
            // is persisted or trusted by the backend for permissions or enrollment.
            viewModel.analyzeGoal(goalInput.getText().toString());
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), this::renderState);
    }

    private void renderState(CareerAdvisorUiState state) {
        if (state == null) {
            return;
        }

        analyzeButton.setEnabled(!state.loading);
        loadingIndicator.setVisibility(state.loading ? View.VISIBLE : View.GONE);
        assistantBubble.setText(state.assistantMessage);

        if (state.learnerGoal != null && !state.learnerGoal.isBlank()) {
            goalEcho.setText(getString(R.string.career_advisor_goal_echo, state.learnerGoal));
            goalEcho.setVisibility(View.VISIBLE);
        } else {
            goalEcho.setVisibility(View.GONE);
        }

        if (state.errorMessage != null && !state.errorMessage.isBlank()) {
            errorText.setText(state.errorMessage);
            errorText.setVisibility(View.VISIBLE);
        } else {
            errorText.setVisibility(View.GONE);
        }

        adapter.submitList(state.recommendations);
        boolean hasRecommendations = state.recommendations != null && !state.recommendations.isEmpty();

        // Update the advisor card container styling depending on whether recommendations were found.
        if (cardContainer != null && cardTitle != null && cardIcon != null) {
            if (hasRecommendations) {
                // Recommendations found: apply premium mint style with brand primary highlights.
                cardContainer.setBackgroundResource(R.drawable.bg_advisor_response_card);
                cardTitle.setText("Advisor Analysis");
                cardIcon.setImageTintList(android.content.res.ColorStateList.valueOf(
                        androidx.core.content.ContextCompat.getColor(requireContext(), R.color.brand_primary)));
            } else {
                // Initial or empty state: use the neutral guidance background and secondary accents.
                cardContainer.setBackgroundResource(R.drawable.bg_advisor_guidance_card);
                cardTitle.setText("AI Career Guide");
                cardIcon.setImageTintList(android.content.res.ColorStateList.valueOf(
                        androidx.core.content.ContextCompat.getColor(requireContext(), R.color.brand_text_secondary)));
            }
        }

        resultsTitle.setVisibility(hasRecommendations ? View.VISIBLE : View.GONE);
        recommendationRecycler.setVisibility(hasRecommendations ? View.VISIBLE : View.GONE);
    }

    private void openRecommendedCourse(CareerCourseRecommendation recommendation) {
        if (recommendation == null
                || recommendation.course == null
                || recommendation.course.id == null
                || recommendation.course.id.isBlank()) {
            return;
        }

        Bundle args = new Bundle();
        args.putString("courseId", recommendation.course.id);
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
