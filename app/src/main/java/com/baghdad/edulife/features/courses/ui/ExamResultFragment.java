package com.baghdad.edulife.features.courses.ui;

import android.content.res.ColorStateList;
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

        Bundle args = getArguments() != null ? getArguments() : new Bundle();
        int score = args.getInt("score", 0);
        int passScore = args.getInt("passScore", 80);
        boolean passed = args.getBoolean("passed", false);
        String certificateNumber = args.getString("certificateNumber", "");

        TextView scoreCircle = view.findViewById(R.id.examResultScoreCircle);
        TextView statusLabel = view.findViewById(R.id.examResultStatusLabel);
        TextView passScoreLabel = view.findViewById(R.id.examResultPassScoreLabel);
        LinearLayout certCard = view.findViewById(R.id.examResultCertCard);
        TextView certNumber = view.findViewById(R.id.examResultCertNumber);
        TextView retryMessage = view.findViewById(R.id.examResultRetryMessage);
        Button doneButton = view.findViewById(R.id.examResultDoneButton);

        scoreCircle.setText(score + "%");

        if (passed) {
            scoreCircle.setBackgroundTintList(
                    ColorStateList.valueOf(requireContext().getColor(R.color.exam_pass_green)));
            statusLabel.setText(R.string.exam_result_passed);
            statusLabel.setTextColor(requireContext().getColor(R.color.exam_pass_green));
            certCard.setVisibility(View.VISIBLE);
            retryMessage.setVisibility(View.GONE);
            if (certificateNumber != null && !certificateNumber.isBlank()) {
                certNumber.setText(getString(R.string.exam_result_cert_number, certificateNumber));
            }
        } else {
            scoreCircle.setBackgroundTintList(
                    ColorStateList.valueOf(requireContext().getColor(R.color.exam_fail_red)));
            statusLabel.setText(R.string.exam_result_failed);
            statusLabel.setTextColor(requireContext().getColor(R.color.exam_fail_red));
            certCard.setVisibility(View.GONE);
            retryMessage.setVisibility(View.VISIBLE);
            retryMessage.setText(R.string.exam_result_retry_hint);
        }

        passScoreLabel.setText(getString(R.string.exam_result_required_score, passScore));

        doneButton.setOnClickListener(v -> Navigation.findNavController(view).popBackStack());
    }
}
