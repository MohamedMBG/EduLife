package com.baghdad.edulife.features.onboarding.ui;

import android.animation.ValueAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.viewpager2.widget.ViewPager2;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.onboarding.data.OnboardingPreferences;
import com.baghdad.edulife.features.onboarding.viewmodel.OnboardingViewModel;

import java.util.ArrayList;
import java.util.List;

public class OnboardingFragment extends Fragment {

    private ViewPager2 viewPager;
    private LinearLayout dotRow;
    private TextView btnGetStarted;
    private TextView txtSkip;

    private OnboardingViewModel viewModel;
    private OnboardingPreferences onboardingPreferences;
    private final List<View> dots = new ArrayList<>();

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

        viewModel = new ViewModelProvider(this).get(OnboardingViewModel.class);
        onboardingPreferences = new OnboardingPreferences(requireContext());

        viewPager = view.findViewById(R.id.viewPager);
        dotRow = view.findViewById(R.id.dotRow);
        btnGetStarted = view.findViewById(R.id.btnGetStarted);
        txtSkip = view.findViewById(R.id.txtSkip);

        OnboardingPagerAdapter adapter = new OnboardingPagerAdapter(
                requireContext(), viewModel.getItems());
        viewPager.setAdapter(adapter);

        buildDots(viewModel.getPageCount());
        updateIndicators(0);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                viewModel.setCurrentPage(position);
                updateIndicators(position);
                updateCtaLabel(position);
                txtSkip.setVisibility(
                        position == viewModel.getPageCount() - 1 ? View.INVISIBLE : View.VISIBLE);
            }
        });

        btnGetStarted.setOnClickListener(v -> handlePrimaryAction());

        View.OnClickListener goToLogin =
                v -> completeOnboardingAndNavigate(R.id.action_onboardingFragment_to_loginFragment);
        txtSkip.setOnClickListener(goToLogin);
        view.findViewById(R.id.btnLogin).setOnClickListener(goToLogin);

        styleTermsText(view.findViewById(R.id.txtTerms));
    }

    private void buildDots(int count) {
        dotRow.removeAllViews();
        dots.clear();
        int size = dpToPx(10);
        int gap = dpToPx(8);

        for (int i = 0; i < count; i++) {
            View dot = new View(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            if (i > 0) params.leftMargin = gap;
            dot.setLayoutParams(params);
            dot.setBackgroundResource(R.drawable.bg_dot_inactive);
            dotRow.addView(dot);
            dots.add(dot);
        }
    }

    private void updateIndicators(int activePage) {
        int inactiveWidth = dpToPx(10);
        int activeWidth = dpToPx(24);

        for (int i = 0; i < dots.size(); i++) {
            View dot = dots.get(i);
            boolean isActive = i == activePage;
            int targetWidth = isActive ? activeWidth : inactiveWidth;

            dot.setBackgroundResource(
                    isActive ? R.drawable.bg_indicator_active : R.drawable.bg_dot_inactive);

            ViewGroup.LayoutParams params = dot.getLayoutParams();
            if (params.width != targetWidth) {
                animateWidth(dot, params.width > 0 ? params.width : dpToPx(10), targetWidth);
            }
        }
    }

    private void animateWidth(View view, int from, int to) {
        ValueAnimator animator = ValueAnimator.ofInt(from, to);
        animator.addUpdateListener(va -> {
            ViewGroup.LayoutParams p = view.getLayoutParams();
            p.width = (int) va.getAnimatedValue();
            view.setLayoutParams(p);
        });
        animator.setDuration(250);
        animator.setInterpolator(new FastOutSlowInInterpolator());
        animator.start();
    }

    private void updateCtaLabel(int position) {
        boolean isLast = position == viewModel.getPageCount() - 1;
        btnGetStarted.setText(isLast ? R.string.onboarding_get_started : R.string.onboarding_next);
    }

    private void handlePrimaryAction() {
        if (!viewModel.isLastPage()) {
            Integer current = viewModel.getCurrentPage().getValue();
            viewPager.setCurrentItem((current != null ? current : 0) + 1, true);
        } else {
            completeOnboardingAndNavigate(R.id.action_onboardingFragment_to_registerFragment);
        }
    }

    private void completeOnboardingAndNavigate(int actionId) {
        onboardingPreferences.markOnboardingSeen();
        NavController nav = NavHostFragment.findNavController(this);
        if (nav.getCurrentDestination() != null
                && nav.getCurrentDestination().getId() == R.id.onboardingFragment) {
            nav.navigate(actionId);
        }
    }

    private void styleTermsText(TextView termsView) {
        String terms = getString(R.string.onboarding_terms);
        SpannableString styledTerms = new SpannableString(terms);
        applyGreenLink(styledTerms, terms, getString(R.string.onboarding_terms_label));
        applyGreenLink(styledTerms, terms, getString(R.string.onboarding_privacy_label));
        termsView.setText(styledTerms);
        termsView.setHighlightColor(Color.TRANSPARENT);
        termsView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void applyGreenLink(SpannableString spannable, String source, String target) {
        int start = source.indexOf(target);
        if (start < 0) return;
        spannable.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {}

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(ContextCompat.getColor(requireContext(), R.color.onboarding_green));
                ds.setUnderlineText(false);
                ds.setFakeBoldText(true);
            }
        }, start, start + target.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
