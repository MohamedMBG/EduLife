package com.baghdad.edulife.features.exam.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.baghdad.edulife.R;

public class ExamResultFragment extends Fragment {

    public ExamResultFragment() {
        super(R.layout.fragment_exam_result);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        int score = args != null ? args.getInt("score", 0) : 0;
        int passScore = args != null ? args.getInt("passScore", 60) : 60;
        boolean passed = args != null && args.getBoolean("passed", false);
        String certNumber = args != null ? args.getString("certificateNumber", "") : "";

        TextView scoreText = view.findViewById(R.id.resultScoreText);
        TextView statusText = view.findViewById(R.id.resultStatusText);
        TextView thresholdText = view.findViewById(R.id.resultThresholdText);
        LinearLayout certCard = view.findViewById(R.id.resultCertCard);
        TextView certNumberText = view.findViewById(R.id.resultCertNumber);
        Button backButton = view.findViewById(R.id.resultBackButton);
        Button retryButton = view.findViewById(R.id.resultRetryButton);

        scoreText.setText(getString(R.string.exam_result_score, score));
        thresholdText.setText(getString(R.string.exam_result_pass_threshold, passScore));

        if (passed) {
            statusText.setText(R.string.exam_result_passed);
            statusText.setTextColor(requireContext().getColor(R.color.brand_primary));

            if (certNumber != null && !certNumber.isBlank()) {
                certCard.setVisibility(View.VISIBLE);
                certNumberText.setText(certNumber);
            }

            retryButton.setVisibility(View.GONE);
        } else {
            statusText.setText(R.string.exam_result_failed);
            statusText.setTextColor(requireContext().getColor(R.color.brand_error));
            certCard.setVisibility(View.GONE);
            retryButton.setVisibility(View.VISIBLE);
        }

        backButton.setOnClickListener(v ->
                Navigation.findNavController(view).popBackStack(R.id.courseDetailFragment, false));

        retryButton.setOnClickListener(v ->
                Navigation.findNavController(view).popBackStack(R.id.examFragment, false));
    }
}
