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
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.onboarding.data.OnboardingPreferences;
import com.baghdad.edulife.features.onboarding.model.OnboardingItem;

import java.util.ArrayList;
import java.util.List;

public class OnboardingFragment extends Fragment {

    private final List<OnboardingItem> onboardingItems = new ArrayList<>();

    private OnboardingPreferences onboardingPreferences;
    private TextView titleView;
    private TextView subtitleView;
    private TextView ctaButton;
    private LinearLayout dotRow;
    private int currentStep = 0;

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

        onboardingPreferences = new OnboardingPreferences(requireContext());
        titleView = view.findViewById(R.id.txtTitle);
        subtitleView = view.findViewById(R.id.txtSubtitle);
        ctaButton = view.findViewById(R.id.btnGetStarted);
        dotRow = view.findViewById(R.id.dotRow);

        buildOnboardingItems();
        styleTermsText(view.findViewById(R.id.txtTerms));
        renderCurrentStep();

        ctaButton.setOnClickListener(v -> handlePrimaryAction());

        // Skip and Login both finish onboarding because the user chose to enter the auth flow directly.
        View.OnClickListener goToLogin = v ->
                completeOnboardingAndNavigate(R.id.action_onboardingFragment_to_loginFragment);
        view.findViewById(R.id.txtSkip).setOnClickListener(goToLogin);
        view.findViewById(R.id.btnLogin).setOnClickListener(goToLogin);
    }

    private void buildOnboardingItems() {
        onboardingItems.clear();
        onboardingItems.add(new OnboardingItem(
                getString(R.string.onboarding_title),
                getString(R.string.onboarding_subtitle),
                getString(R.string.onboarding_accent_purpose)
        ));
        onboardingItems.add(new OnboardingItem(
                getString(R.string.onboarding_structure_title),
                getString(R.string.onboarding_structure_subtitle),
                getString(R.string.onboarding_accent_journey)
        ));
        onboardingItems.add(new OnboardingItem(
                getString(R.string.onboarding_certificate_title),
                getString(R.string.onboarding_certificate_subtitle),
                getString(R.string.onboarding_accent_certificate)
        ));
    }

    private void handlePrimaryAction() {
        if (currentStep < onboardingItems.size() - 1) {
            currentStep++;
            renderCurrentStep();
            return;
        }

        // Account creation starts the learning loop, while Firebase implementation stays in the auth feature.
        completeOnboardingAndNavigate(R.id.action_onboardingFragment_to_registerFragment);
    }

    private void renderCurrentStep() {
        OnboardingItem currentItem = onboardingItems.get(currentStep);

        titleView.setText(buildAccentedTitle(currentItem));
        subtitleView.setText(currentItem.getSubtitle());
        ctaButton.setText(currentStep == onboardingItems.size() - 1
                ? R.string.onboarding_get_started
                : R.string.onboarding_next);

        for (int index = 0; index < dotRow.getChildCount(); index++) {
            View dot = dotRow.getChildAt(index);
            dot.setBackgroundResource(index == currentStep
                    ? R.drawable.bg_dot_active
                    : R.drawable.bg_dot_inactive);
        }
    }

    private SpannableString buildAccentedTitle(OnboardingItem item) {
        String title = item.getTitle();
        String accentText = item.getAccentText();
        SpannableString styledTitle = new SpannableString(title);
        int accentStart = title.indexOf(accentText);

        if (accentStart >= 0) {
            styledTitle.setSpan(
                    new ForegroundColorSpan(requireContext().getColor(R.color.onboarding_green)),
                    accentStart,
                    accentStart + accentText.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }

        return styledTitle;
    }

    private void completeOnboardingAndNavigate(int actionId) {
        onboardingPreferences.markOnboardingSeen();

        NavController navController = NavHostFragment.findNavController(this);
        if (navController.getCurrentDestination() != null
                && navController.getCurrentDestination().getId() == R.id.onboardingFragment) {
            navController.navigate(actionId);
        }
    }

    private void styleTermsText(TextView termsView) {
        String terms = getString(R.string.onboarding_terms);
        SpannableString styledTerms = new SpannableString(terms);

        applyGreenLink(styledTerms, terms, getString(R.string.onboarding_terms_label));
        applyGreenLink(styledTerms, terms, getString(R.string.onboarding_privacy_label));

        // Legal pages are not part of the current MVP screen set, so links are visual until those routes exist.
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
                // Intentionally empty until Terms and Privacy screens are added to the navigation graph.
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
