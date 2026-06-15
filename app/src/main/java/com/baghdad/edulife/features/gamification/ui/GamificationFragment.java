package com.baghdad.edulife.features.gamification.ui;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.gamification.model.Badge;
import com.baghdad.edulife.features.gamification.model.GamificationUiState;
import com.baghdad.edulife.features.gamification.viewmodel.GamificationViewModel;

public class GamificationFragment extends Fragment {

    private static final String[] LEVEL_TITLES = {
            "Novice", "Curious", "Explorer", "Seeker", "Thinker",
            "Achiever", "Scholar", "Expert", "Sage", "Master"
    };
    private static final int[] LEVEL_XP = {
            0, 250, 600, 1100, 1800, 2700, 3900, 5500, 7500, 10000
    };

    private GamificationViewModel viewModel;
    private BadgeAdapter badgeAdapter;

    private TextView levelNumberText;
    private TextView levelTitleText;
    private TextView totalXpText;
    private TextView xpProgressLabel;
    private ProgressBar xpProgressBar;

    private TextView streakCountText;
    private TextView streakDescription;

    private TextView statLessonsCount;
    private TextView statCoursesCount;
    private TextView statCertsCount;
    private TextView badgesSummaryText;

    private RecyclerView badgesRecyclerView;
    private LinearLayout levelListContainer;

    public GamificationFragment() {
        super(R.layout.fragment_gamification);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(GamificationViewModel.class);

        View header = view.findViewById(R.id.gamificationHeaderLayout);
        final int origTop = header.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            header.setPadding(header.getPaddingLeft(), origTop + top,
                    header.getPaddingRight(), header.getPaddingBottom());
            return WindowInsetsCompat.CONSUMED;
        });

        levelNumberText = view.findViewById(R.id.levelNumberText);
        levelTitleText = view.findViewById(R.id.levelTitleText);
        totalXpText = view.findViewById(R.id.totalXpText);
        xpProgressLabel = view.findViewById(R.id.xpProgressLabel);
        xpProgressBar = view.findViewById(R.id.xpProgressBar);

        streakCountText = view.findViewById(R.id.streakCountText);
        streakDescription = view.findViewById(R.id.streakDescription);

        statLessonsCount = view.findViewById(R.id.statLessonsCount);
        statCoursesCount = view.findViewById(R.id.statCoursesCount);
        statCertsCount = view.findViewById(R.id.statCertsCount);
        badgesSummaryText = view.findViewById(R.id.badgesSummaryText);

        badgesRecyclerView = view.findViewById(R.id.badgesRecyclerView);
        levelListContainer = view.findViewById(R.id.levelListContainer);

        badgeAdapter = new BadgeAdapter();
        badgesRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 4));
        badgesRecyclerView.setAdapter(badgeAdapter);
        badgeAdapter.setOnBadgeClickListener(this::showBadgeDetailDialog);

        renderLevelList();

        viewModel.uiState.observe(getViewLifecycleOwner(), this::render);
        viewModel.refreshState();
    }

    private boolean isFirstRender = true;

    private void render(GamificationUiState state) {
        if (state == null || state.levelInfo == null) return;

        levelNumberText.setText(String.valueOf(state.levelInfo.level));
        levelTitleText.setText(state.levelInfo.title);

        if (isFirstRender) {
            isFirstRender = false;
            animateXpText(totalXpText, state.totalXp);
            animateProgressBar(xpProgressBar, Math.max(0, Math.min(100, state.levelInfo.progressPercent)));
            animateNumber(statLessonsCount, state.lessonsCompleted);
            animateNumber(statCoursesCount, state.coursesEnrolled);
            animateNumber(statCertsCount, state.certificatesEarned);

            View levelRing = getView() != null ? getView().findViewById(R.id.levelRingContainer) : null;
            if (levelRing != null) {
                animateViewScale(levelRing);
            }
            animateViewPop(streakCountText);
        } else {
            totalXpText.setText(getString(R.string.gamification_total_xp, state.totalXp));
            xpProgressBar.setProgress(Math.max(0, Math.min(100, state.levelInfo.progressPercent)));
            statLessonsCount.setText(String.valueOf(state.lessonsCompleted));
            statCoursesCount.setText(String.valueOf(state.coursesEnrolled));
            statCertsCount.setText(String.valueOf(state.certificatesEarned));
        }

        if (state.levelInfo.xpForNextLevel == Integer.MAX_VALUE) {
            xpProgressLabel.setText(R.string.gamification_xp_max_level);
        } else {
            int toGo = Math.max(0, state.levelInfo.xpForNextLevel - state.totalXp);
            xpProgressLabel.setText(getString(R.string.gamification_xp_to_next, toGo));
        }

        streakCountText.setText(String.valueOf(state.streak));
        if (state.streak <= 0) {
            streakDescription.setText(R.string.gamification_streak_empty);
        } else if (state.streak == 1) {
            streakDescription.setText(R.string.gamification_streak_one);
        } else {
            streakDescription.setText(getString(R.string.gamification_streak_active, state.streak));
        }

        int earned = 0;
        for (Badge b : state.badges) {
            if (b.earned) earned++;
        }
        badgesSummaryText.setText(getString(R.string.gamification_badges_summary, earned, state.badges.size()));
        badgeAdapter.submitList(state.badges);

        highlightCurrentLevelRow(state.levelInfo.level);
    }

    private void renderLevelList() {
        if (levelListContainer == null) return;
        levelListContainer.removeAllViews();
        int rowVerticalPad = dp(12);
        for (int i = 0; i < LEVEL_TITLES.length; i++) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, rowVerticalPad, 0, rowVerticalPad);
            row.setTag("levelRow_" + (i + 1));

            TextView left = new TextView(requireContext());
            left.setText(getString(R.string.gamification_level_row, i + 1, LEVEL_TITLES[i]));
            left.setTextColor(requireContext().getColor(R.color.brand_text_primary));
            left.setTextSize(14);
            LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            left.setLayoutParams(leftParams);

            TextView right = new TextView(requireContext());
            right.setText(getString(R.string.gamification_level_row_xp, LEVEL_XP[i]));
            right.setTextColor(requireContext().getColor(R.color.brand_text_secondary));
            right.setTextSize(13);

            row.addView(left);
            row.addView(right);
            levelListContainer.addView(row);

            if (i < LEVEL_TITLES.length - 1) {
                View divider = new View(requireContext());
                LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 1);
                divider.setLayoutParams(dp);
                divider.setBackgroundColor(requireContext().getColor(R.color.brand_border));
                levelListContainer.addView(divider);
            }
        }
    }

    private void highlightCurrentLevelRow(int currentLevel) {
        if (levelListContainer == null) return;
        for (int i = 0; i < levelListContainer.getChildCount(); i++) {
            View child = levelListContainer.getChildAt(i);
            Object tag = child.getTag();
            if (!(tag instanceof String)) continue;
            String t = (String) tag;
            if (!t.startsWith("levelRow_")) continue;
            int rowLevel;
            try {
                rowLevel = Integer.parseInt(t.substring("levelRow_".length()));
            } catch (NumberFormatException e) {
                continue;
            }
            boolean isCurrent = rowLevel == currentLevel;
            if (child instanceof LinearLayout) {
                LinearLayout row = (LinearLayout) child;
                for (int j = 0; j < row.getChildCount(); j++) {
                    View tv = row.getChildAt(j);
                    if (tv instanceof TextView) {
                        ((TextView) tv).setTextColor(requireContext().getColor(
                                isCurrent ? R.color.brand_primary : (j == 0
                                        ? R.color.brand_text_primary
                                        : R.color.brand_text_secondary)));
                        ((TextView) tv).setTypeface(null, isCurrent
                                ? android.graphics.Typeface.BOLD
                                : android.graphics.Typeface.NORMAL);
                    }
                }
            }
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void animateNumber(TextView textView, int endValue) {
        if (endValue <= 0) {
            textView.setText("0");
            return;
        }
        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofInt(0, endValue);
        animator.setDuration(1200);
        animator.addUpdateListener(animation -> textView.setText(animation.getAnimatedValue().toString()));
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        animator.start();
    }

    private void animateXpText(TextView textView, int endXp) {
        if (endXp <= 0) {
            textView.setText(getString(R.string.gamification_total_xp, 0));
            return;
        }
        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofInt(0, endXp);
        animator.setDuration(1200);
        animator.addUpdateListener(animation -> {
            int currentXp = (int) animation.getAnimatedValue();
            textView.setText(getString(R.string.gamification_total_xp, currentXp));
        });
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        animator.start();
    }

    private void animateProgressBar(ProgressBar progressBar, int progress) {
        android.animation.ObjectAnimator animator = android.animation.ObjectAnimator.ofInt(progressBar, "progress", 0, progress);
        animator.setDuration(1200);
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        animator.start();
    }

    private void animateViewScale(View view) {
        view.setScaleX(0f);
        view.setScaleY(0f);
        view.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(800)
                .setStartDelay(200)
                .setInterpolator(new android.view.animation.OvershootInterpolator(1.3f));
    }

    private void animateViewPop(View view) {
        view.setScaleX(0.5f);
        view.setScaleY(0.5f);
        view.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(800)
                .setInterpolator(new android.view.animation.OvershootInterpolator(2.0f));
    }

    private void showBadgeDetailDialog(com.baghdad.edulife.features.gamification.model.Badge badge) {
        if (getContext() == null) return;

        android.app.Dialog dialog = new android.app.Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_badge_detail);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        android.widget.ImageView dialogBadgeIcon = dialog.findViewById(R.id.dialogBadgeIcon);
        TextView dialogBadgeName = dialog.findViewById(R.id.dialogBadgeName);
        TextView dialogBadgeRarity = dialog.findViewById(R.id.dialogBadgeRarity);
        TextView dialogBadgeDesc = dialog.findViewById(R.id.dialogBadgeDesc);
        TextView dialogBadgeStatus = dialog.findViewById(R.id.dialogBadgeStatus);
        View dialogCloseButton = dialog.findViewById(R.id.dialogCloseButton);

        dialogBadgeIcon.setImageResource(badge.iconResId);
        dialogBadgeName.setText(badge.name);
        dialogBadgeDesc.setText(badge.description);

        String rarityLabel = badge.rarity.name().substring(0, 1) + badge.rarity.name().substring(1).toLowerCase();
        dialogBadgeRarity.setText(rarityLabel);

        int rarityTextColor;
        int rarityBgColor;
        switch (badge.rarity) {
            case COMMON:
                rarityTextColor = requireContext().getColor(R.color.brand_primary);
                rarityBgColor = requireContext().getColor(R.color.brand_primary_surface);
                break;
            case RARE:
                rarityTextColor = requireContext().getColor(R.color.admin_accent);
                rarityBgColor = requireContext().getColor(R.color.admin_accent_surface);
                break;
            case EPIC:
                rarityTextColor = 0xFF9B51E0; // Purple
                rarityBgColor = 0xFFF2E8FC;
                break;
            case LEGENDARY:
                rarityTextColor = requireContext().getColor(R.color.gamification_xp_gold);
                rarityBgColor = 0xFFFFF8E7;
                break;
            default:
                rarityTextColor = requireContext().getColor(R.color.brand_text_secondary);
                rarityBgColor = requireContext().getColor(R.color.brand_surface_muted);
        }
        dialogBadgeRarity.setTextColor(rarityTextColor);
        dialogBadgeRarity.setBackgroundTintList(android.content.res.ColorStateList.valueOf(rarityBgColor));

        if (badge.earned) {
            dialogBadgeStatus.setText("Earned 🎉");
            dialogBadgeStatus.setTextColor(requireContext().getColor(R.color.brand_primary));
            dialogBadgeStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(requireContext().getColor(R.color.brand_primary_surface)));
            dialogBadgeIcon.setAlpha(1.0f);
        } else {
            dialogBadgeStatus.setText("Locked 🔒");
            dialogBadgeStatus.setTextColor(requireContext().getColor(R.color.brand_text_secondary));
            dialogBadgeStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(requireContext().getColor(R.color.brand_surface_muted)));
            dialogBadgeIcon.setAlpha(0.35f);
        }

        dialogCloseButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}
