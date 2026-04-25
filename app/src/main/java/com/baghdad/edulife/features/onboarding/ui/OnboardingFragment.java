package com.baghdad.edulife.features.onboarding.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.baghdad.edulife.R;

public class OnboardingFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_onboarding, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tintTitleKeyword(view.findViewById(R.id.txtTitle));
        styleTermsText(view.findViewById(R.id.txtTerms));

        // Onboarding only introduces the learning journey; account creation remains in the auth feature.
        view.findViewById(R.id.btnGetStarted).setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_onboardingFragment_to_registerFragment));

        // Skip and Login both lead to authentication so users can enter the MVP learning loop quickly.
        View.OnClickListener goToLogin = v -> NavHostFragment.findNavController(this)
                .navigate(R.id.action_onboardingFragment_to_loginFragment);
        view.findViewById(R.id.txtSkip).setOnClickListener(goToLogin);
        view.findViewById(R.id.btnLogin).setOnClickListener(goToLogin);
    }

    private void tintTitleKeyword(TextView titleView) {
        String title = getString(R.string.onboarding_title);
        SpannableString styledTitle = new SpannableString(title);
        int keywordStart = title.indexOf("purpose");

        if (keywordStart >= 0) {
            styledTitle.setSpan(
                    new ForegroundColorSpan(requireContext().getColor(R.color.onboarding_green)),
                    keywordStart,
                    keywordStart + "purpose".length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }

        titleView.setText(styledTitle);
    }

    private void styleTermsText(TextView termsView) {
        String terms = getString(R.string.onboarding_terms);
        SpannableString styledTerms = new SpannableString(terms);

        applyGreenLink(styledTerms, terms, "Terms of Service");
        applyGreenLink(styledTerms, terms, "Privacy Policy");

        // Terms are visually highlighted now; legal destination screens can be attached when they exist.
        termsView.setText(styledTerms);
        termsView.setHighlightColor(Color.TRANSPARENT);
        termsView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void applyGreenLink(SpannableString spannable, String source, String target) {
        int start = source.indexOf(target);
        if (start < 0) {
            return;
        }

        spannable.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                // Legal pages are outside the current MVP screen scope, so this is intentionally a no-op.
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(requireContext().getColor(R.color.onboarding_green));
                ds.setUnderlineText(false);
                ds.setFakeBoldText(true);
            }
        }, start, start + target.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
}
