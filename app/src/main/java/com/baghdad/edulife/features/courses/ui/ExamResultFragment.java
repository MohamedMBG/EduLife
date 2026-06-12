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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ExamResultFragment extends Fragment {

    public ExamResultFragment() {
        super(R.layout.fragment_exam_result);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments() != null ? getArguments() : new Bundle();
        int score = args.getInt("score", 0);
        // Fallback only; the real threshold always arrives from the exam result via ExamFragment.
        int passScore = args.getInt("passScore", 70);
        boolean passed = args.getBoolean("passed", false);
        String certificateNumber = args.getString("certificateNumber", "");
        int attemptsUsed = args.getInt("attemptsUsed", 0);
        String cooldownEndsAt = args.getString("cooldownEndsAt", "");

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
            if (cooldownEndsAt != null && !cooldownEndsAt.isBlank()) {
                retryMessage.setText(getString(R.string.exam_result_cooldown_hint,
                        formatCooldownDate(cooldownEndsAt)));
            } else if (attemptsUsed >= 2) {
                retryMessage.setText(R.string.exam_result_cooldown_active);
            } else {
                retryMessage.setText(getString(R.string.exam_result_attempts_hint,
                        attemptsUsed, 2));
            }
        }

        passScoreLabel.setText(getString(R.string.exam_result_required_score, passScore));

        doneButton.setOnClickListener(v -> Navigation.findNavController(view).popBackStack());
    }

    private String formatCooldownDate(String isoInstant) {
        try {
            Instant instant = Instant.parse(isoInstant);
            DateTimeFormatter fmt = DateTimeFormatter
                    .ofPattern("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
                    .withZone(ZoneId.systemDefault());
            return fmt.format(instant);
        } catch (Exception e) {
            return isoInstant;
        }
    }
}
