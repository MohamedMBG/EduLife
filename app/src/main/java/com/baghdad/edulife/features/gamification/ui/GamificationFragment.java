package com.baghdad.edulife.features.gamification.ui;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.baghdad.edulife.R;
import com.baghdad.edulife.core.storage.SessionStorage;
import com.baghdad.edulife.features.courses.model.CourseProgressSummary;
import com.baghdad.edulife.features.courses.model.EnrolledCourse;
import com.baghdad.edulife.features.courses.viewmodel.EnrollmentViewModel;
import com.baghdad.edulife.features.gamification.model.Badge;
import com.baghdad.edulife.features.gamification.model.CourseProgressItem;
import com.baghdad.edulife.features.gamification.model.GamificationUiState;
import com.baghdad.edulife.features.gamification.model.LeaderboardEntryResponse;
import com.baghdad.edulife.features.gamification.model.LevelInfo;
import com.baghdad.edulife.features.gamification.model.LevelMilestone;
import com.baghdad.edulife.features.gamification.viewmodel.GamificationViewModel;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GamificationFragment extends Fragment {

    private static final String[] LEVEL_TITLES = {
            "Novice", "Curious", "Explorer", "Seeker", "Thinker",
            "Achiever", "Scholar", "Expert", "Sage", "Master"
    };
    private static final int[] LEVEL_XP = {
            0, 250, 600, 1100, 1800, 2700, 3900, 5500, 7500, 10000
    };

    private GamificationViewModel viewModel;
    private EnrollmentViewModel enrollmentViewModel;

    private BadgeAdapter badgeAdapter;
    private LevelMilestoneAdapter levelMilestoneAdapter;
    private CourseProgressAdapter courseProgressAdapter;

    // Hero header
    private TextView levelNumberText;
    private TextView levelTitleText;
    private TextView totalXpText;
    private TextView xpProgressLabel;
    private ProgressBar xpProgressBar;
    private TextView motivationText;

    // Stat cards
    private TextView streakCountText;
    private TextView rankText;

    // Streak section
    private TextView streakDescription;
    private TextView longestStreakText;
    private LinearLayout weekStripContainer;

    // Stats
    private TextView statLessonsCount;
    private TextView statCoursesCount;
    private TextView statCertsCount;

    // Courses
    private RecyclerView coursesRecyclerView;
    private View coursesEmptyView;
    private TextView coursesErrorView;

    // Badges
    private TextView badgesSummaryText;
    private RecyclerView badgesRecyclerView;

    // States
    private View loadingState;
    private View errorState;

    // Latest course data
    private List<EnrolledCourse> latestEnrollments = new ArrayList<>();
    private Map<String, CourseProgressSummary> latestProgress;
    private Set<String> latestFailedProgress;
    private String latestEnrollmentsError;

    private boolean isFirstRender = true;
    private boolean hasGamificationState = false;
    private boolean levelPathScrolled = false;
    private boolean skipNextResumeRefresh = false;
    private boolean enrollmentsLoaded = false;
    private String currentUserId;

    public GamificationFragment() {
        super(R.layout.fragment_gamification);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(GamificationViewModel.class);
        enrollmentViewModel = new ViewModelProvider(this).get(EnrollmentViewModel.class);

        SessionStorage session = new SessionStorage(requireContext());
        currentUserId = session.getUserId();

        View header = view.findViewById(R.id.gamificationHeaderLayout);
        final int origTop = header.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            header.setPadding(header.getPaddingLeft(), origTop + top,
                    header.getPaddingRight(), header.getPaddingBottom());
            return WindowInsetsCompat.CONSUMED;
        });

        bindViews(view);
        setupRecyclers();
        setupClickListeners(view);

        observeGamification();
        observeCourses();
        observeLeaderboard();

        showLoading();
        viewModel.refreshState();
        viewModel.loadLeaderboard(20);
        enrollmentViewModel.loadMyEnrollments();
        skipNextResumeRefresh = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (skipNextResumeRefresh) {
            skipNextResumeRefresh = false;
            return;
        }
        if (viewModel != null) {
            viewModel.refreshState();
            viewModel.loadLeaderboard(20);
        }
        if (enrollmentViewModel != null) {
            enrollmentViewModel.loadMyEnrollments();
        }
    }

    private void bindViews(@NonNull View view) {
        levelNumberText = view.findViewById(R.id.levelNumberText);
        levelTitleText = view.findViewById(R.id.levelTitleText);
        totalXpText = view.findViewById(R.id.totalXpText);
        xpProgressLabel = view.findViewById(R.id.xpProgressLabel);
        xpProgressBar = view.findViewById(R.id.xpProgressBar);
        motivationText = view.findViewById(R.id.motivationText);

        streakCountText = view.findViewById(R.id.streakCountText);
        rankText = view.findViewById(R.id.rankText);

        streakDescription = view.findViewById(R.id.streakDescription);
        longestStreakText = view.findViewById(R.id.longestStreakText);
        weekStripContainer = view.findViewById(R.id.weekStripContainer);

        statLessonsCount = view.findViewById(R.id.statLessonsCount);
        statCoursesCount = view.findViewById(R.id.statCoursesCount);
        statCertsCount = view.findViewById(R.id.statCertsCount);

        coursesRecyclerView = view.findViewById(R.id.coursesRecyclerView);
        coursesEmptyView = view.findViewById(R.id.coursesEmptyView);
        coursesErrorView = view.findViewById(R.id.coursesErrorView);

        badgesSummaryText = view.findViewById(R.id.badgesSummaryText);
        badgesRecyclerView = view.findViewById(R.id.badgesRecyclerView);

        loadingState = view.findViewById(R.id.loadingState);
        errorState = view.findViewById(R.id.errorState);
    }

    private void setupRecyclers() {
        badgeAdapter = new BadgeAdapter();
        badgesRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 4));
        badgesRecyclerView.setAdapter(badgeAdapter);
        badgeAdapter.setOnBadgeClickListener(this::showBadgeDetailDialog);

        levelMilestoneAdapter = new LevelMilestoneAdapter();
        levelPathRecycler().setLayoutManager(
                new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
        levelPathRecycler().setAdapter(levelMilestoneAdapter);

        courseProgressAdapter = new CourseProgressAdapter();
        coursesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        coursesRecyclerView.setAdapter(courseProgressAdapter);
        courseProgressAdapter.setOnCourseClickListener(this::openCourse);
    }

    private void setupClickListeners(@NonNull View view) {
        view.findViewById(R.id.levelInfoButton).setOnClickListener(v -> showLevelInfoDialog());
        view.findViewById(R.id.retryButton).setOnClickListener(v -> {
            showLoading();
            viewModel.refreshState();
            viewModel.loadLeaderboard(20);
            enrollmentViewModel.loadMyEnrollments();
        });

        view.findViewById(R.id.rankCard).setOnClickListener(v -> navigateToLeaderboard());
        view.findViewById(R.id.actionViewRanking).setOnClickListener(v -> navigateToLeaderboard());

        view.findViewById(R.id.actionContinueLearning).setOnClickListener(v -> {
            Navigation.findNavController(requireView()).navigate(R.id.homeFragment);
        });

        view.findViewById(R.id.actionViewBadges).setOnClickListener(v -> {
            if (badgesRecyclerView != null) {
                badgesRecyclerView.getParent().requestChildFocus(badgesRecyclerView, badgesRecyclerView);
            }
        });
    }

    private RecyclerView levelPathRecycler() {
        return requireView().findViewById(R.id.levelPathRecyclerView);
    }

    // ── Observers ──────────────────────────────────────────────────────────

    private void observeGamification() {
        viewModel.uiState.observe(getViewLifecycleOwner(), state -> {
            if (state == null || state.levelInfo == null) return;
            hasGamificationState = true;
            showContent();
            render(state);
        });

        viewModel.isLoading.observe(getViewLifecycleOwner(), loading -> {
            if (Boolean.TRUE.equals(loading) && !hasGamificationState) {
                showLoading();
            }
        });

        viewModel.errorMessage.observe(getViewLifecycleOwner(), message -> {
            if (message != null && !hasGamificationState) {
                showError();
            }
        });
    }

    private void observeCourses() {
        enrollmentViewModel.getMyEnrollments().observe(getViewLifecycleOwner(), courses -> {
            latestEnrollments = courses != null ? courses : new ArrayList<>();
            enrollmentsLoaded = true;
            renderCourses();
        });
        enrollmentViewModel.getMyCourseProgress().observe(getViewLifecycleOwner(), progress -> {
            latestProgress = progress;
            renderCourses();
        });
        enrollmentViewModel.getMyCourseProgressFailedIds().observe(getViewLifecycleOwner(), failed -> {
            latestFailedProgress = failed;
            renderCourses();
        });
        enrollmentViewModel.getMyEnrollmentsError().observe(getViewLifecycleOwner(), error -> {
            latestEnrollmentsError = error;
            renderCourses();
        });
    }

    private void observeLeaderboard() {
        viewModel.leaderboard.observe(getViewLifecycleOwner(), entries -> {
            if (entries == null || currentUserId == null) {
                rankText.setText("—");
                return;
            }
            int myRank = -1;
            for (LeaderboardEntryResponse entry : entries) {
                if (currentUserId.equals(entry.userId)) {
                    myRank = entry.rank;
                    break;
                }
            }
            if (myRank > 0) {
                rankText.setText(getString(R.string.gamification_v2_rank_value, myRank));
            } else {
                rankText.setText("—");
            }
        });
    }

    // ── Whole-screen state toggles ───────────────────────────────────────────

    private void showLoading() {
        loadingState.setVisibility(View.VISIBLE);
        errorState.setVisibility(View.GONE);
    }

    private void showError() {
        loadingState.setVisibility(View.GONE);
        errorState.setVisibility(View.VISIBLE);
    }

    private void showContent() {
        loadingState.setVisibility(View.GONE);
        errorState.setVisibility(View.GONE);
    }

    // ── Rendering ────────────────────────────────────────────────────────────

    private void render(GamificationUiState state) {
        LevelInfo level = state.levelInfo;

        levelNumberText.setText(String.valueOf(level.level));
        levelTitleText.setText(level.title);
        View ring = requireView().findViewById(R.id.levelRingContainer);
        ring.setContentDescription(getString(R.string.gamification_level_ring_desc, level.level));

        int progress = Math.max(0, Math.min(100, level.progressPercent));
        if (isFirstRender) {
            isFirstRender = false;
            animateXpNumber(totalXpText, state.totalXp);
            animateProgressBar(xpProgressBar, progress);
            animateNumber(statLessonsCount, state.lessonsCompleted);
            animateNumber(statCoursesCount, state.coursesEnrolled);
            animateNumber(statCertsCount, state.certificatesEarned);
            animateViewScale(ring);
            animateViewPop(streakCountText);
        } else {
            totalXpText.setText(String.valueOf(state.totalXp));
            xpProgressBar.setProgress(progress);
            statLessonsCount.setText(String.valueOf(state.lessonsCompleted));
            statCoursesCount.setText(String.valueOf(state.coursesEnrolled));
            statCertsCount.setText(String.valueOf(state.certificatesEarned));
        }

        if (level.xpForNextLevel == Integer.MAX_VALUE) {
            xpProgressLabel.setText(R.string.gamification_xp_max_level);
        } else {
            int toGo = Math.max(0, level.xpForNextLevel - state.totalXp);
            xpProgressLabel.setText(getString(R.string.gamification_xp_to_next, toGo));
        }
        motivationText.setText(motivationFor(state));

        renderStreak(state);
        renderWeekStrip(state.lastActivityDate, state.streak);
        renderBadges(state.badges);
        renderLevelPath(level.level);
    }

    private void renderStreak(GamificationUiState state) {
        streakCountText.setText(String.valueOf(state.streak));
        if (state.streak <= 0) {
            streakDescription.setText(R.string.gamification_streak_empty);
        } else if (state.streak == 1) {
            streakDescription.setText(R.string.gamification_streak_one);
        } else {
            streakDescription.setText(getString(R.string.gamification_streak_active, state.streak));
        }

        if (state.longestStreak <= 0) {
            longestStreakText.setText(R.string.gamification_longest_streak_none);
        } else if (state.longestStreak == 1) {
            longestStreakText.setText(R.string.gamification_longest_streak_one);
        } else {
            longestStreakText.setText(getString(R.string.gamification_longest_streak, state.longestStreak));
        }
    }

    private void renderBadges(List<Badge> badges) {
        int earned = 0;
        for (Badge b : badges) {
            if (b.earned) earned++;
        }
        badgesSummaryText.setText(getString(R.string.gamification_badges_summary, earned, badges.size()));
        badgeAdapter.submitList(badges);
    }

    private void renderLevelPath(int currentLevel) {
        List<LevelMilestone> milestones = new ArrayList<>(LEVEL_TITLES.length);
        for (int i = 0; i < LEVEL_TITLES.length; i++) {
            int lvl = i + 1;
            LevelMilestone.State state = lvl < currentLevel
                    ? LevelMilestone.State.DONE
                    : (lvl == currentLevel ? LevelMilestone.State.CURRENT : LevelMilestone.State.LOCKED);
            milestones.add(new LevelMilestone(lvl, LEVEL_TITLES[i], LEVEL_XP[i], state));
        }
        levelMilestoneAdapter.submitList(milestones);

        if (!levelPathScrolled) {
            levelPathScrolled = true;
            int target = Math.max(0, currentLevel - 2);
            levelPathRecycler().post(() -> levelPathRecycler().scrollToPosition(target));
        }
    }

    private void renderWeekStrip(String lastActivityDate, int currentStreak) {
        weekStripContainer.removeAllViews();
        String[] initials = getResources().getStringArray(R.array.gamification_week_day_initials);
        String[] names = getResources().getStringArray(R.array.gamification_week_day_names);

        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate lastActive = parseDate(lastActivityDate);
        LocalDate windowStart = (lastActive != null && currentStreak > 0)
                ? lastActive.minusDays(currentStreak - 1L)
                : null;

        for (int i = 0; i < 7; i++) {
            LocalDate day = monday.plusDays(i);
            boolean inWindow = windowStart != null
                    && !day.isBefore(windowStart)
                    && !day.isAfter(lastActive);
            boolean active = inWindow && !day.isAfter(today);
            addWeekDayCell(initials[i], names[i], active);
        }
    }

    private void addWeekDayCell(String initial, String fullName, boolean active) {
        LinearLayout cell = new LinearLayout(requireContext());
        LinearLayout.LayoutParams cellParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        cell.setLayoutParams(cellParams);
        cell.setGravity(Gravity.CENTER);

        TextView dot = new TextView(requireContext());
        int size = dp(32);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(size, size);
        dot.setLayoutParams(dotParams);
        dot.setGravity(Gravity.CENTER);
        dot.setText(initial);
        dot.setTextSize(11);
        dot.setTypeface(null, android.graphics.Typeface.BOLD);
        if (active) {
            dot.setBackgroundResource(R.drawable.bg_week_day_active);
            dot.setTextColor(requireContext().getColor(R.color.white));
            dot.setContentDescription(getString(R.string.gamification_week_day_active_desc, fullName));
        } else {
            dot.setBackgroundResource(R.drawable.bg_week_day_inactive);
            dot.setTextColor(requireContext().getColor(R.color.brand_text_muted));
            dot.setContentDescription(getString(R.string.gamification_week_day_inactive_desc, fullName));
        }

        cell.addView(dot);
        weekStripContainer.addView(cell);
    }

    private void renderCourses() {
        if (coursesRecyclerView == null) return;

        if (!enrollmentsLoaded && latestEnrollmentsError == null) {
            coursesRecyclerView.setVisibility(View.GONE);
            coursesEmptyView.setVisibility(View.GONE);
            coursesErrorView.setVisibility(View.GONE);
            return;
        }

        boolean hasEnrollments = latestEnrollments != null && !latestEnrollments.isEmpty();

        if (latestEnrollmentsError != null && !hasEnrollments) {
            coursesErrorView.setVisibility(View.VISIBLE);
            coursesEmptyView.setVisibility(View.GONE);
            coursesRecyclerView.setVisibility(View.GONE);
            return;
        }
        if (!hasEnrollments) {
            coursesEmptyView.setVisibility(View.VISIBLE);
            coursesErrorView.setVisibility(View.GONE);
            coursesRecyclerView.setVisibility(View.GONE);
            return;
        }

        coursesErrorView.setVisibility(View.GONE);
        coursesEmptyView.setVisibility(View.GONE);
        coursesRecyclerView.setVisibility(View.VISIBLE);

        List<CourseProgressItem> items = new ArrayList<>(latestEnrollments.size());
        for (EnrolledCourse course : latestEnrollments) {
            if (course == null) continue;
            CourseProgressSummary progress = (latestProgress != null && course.courseId != null)
                    ? latestProgress.get(course.courseId) : null;
            boolean failed = latestFailedProgress != null && course.courseId != null
                    && latestFailedProgress.contains(course.courseId);
            items.add(new CourseProgressItem(course, progress, failed));
        }
        courseProgressAdapter.submitList(items);
    }

    private String motivationFor(GamificationUiState state) {
        LevelInfo level = state.levelInfo;
        if (level.xpForNextLevel == Integer.MAX_VALUE) {
            return getString(R.string.gamification_motivation_max);
        }
        if (state.totalXp <= 0) {
            return getString(R.string.gamification_motivation_start);
        }
        int toGo = Math.max(0, level.xpForNextLevel - state.totalXp);
        int nextLevel = level.level + 1;
        if (toGo > 0 && toGo <= 100) {
            return getString(R.string.gamification_motivation_close, toGo, nextLevel);
        }
        if (state.streak >= 2) {
            return getString(R.string.gamification_motivation_streak, state.streak);
        }
        return getString(R.string.gamification_motivation_next, toGo, nextLevel);
    }

    // ── Navigation ─────────────────────────────────────────────────────────

    private void openCourse(EnrolledCourse course) {
        if (course == null || course.courseId == null || course.courseId.isBlank()) return;
        Bundle args = new Bundle();
        args.putString("courseId", course.courseId);
        args.putBoolean("isEnrolled", true);
        Navigation.findNavController(requireView())
                .navigate(R.id.action_gamificationFragment_to_courseDetailFragment, args);
    }

    private void navigateToLeaderboard() {
        Navigation.findNavController(requireView())
                .navigate(R.id.action_gamificationFragment_to_leaderboardFragment);
    }

    // ── Dialogs ──────────────────────────────────────────────────────────────

    private void showLevelInfoDialog() {
        if (getContext() == null) return;
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.gamification_level_info_title)
                .setMessage(R.string.gamification_level_info_body)
                .setPositiveButton(R.string.gamification_level_info_got_it, null)
                .show();
    }

    private void showBadgeDetailDialog(Badge badge) {
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

        String rarityLabel = badge.rarity.name().substring(0, 1)
                + badge.rarity.name().substring(1).toLowerCase();
        dialogBadgeRarity.setText(rarityLabel);

        int rarityTextColor;
        int rarityBgColor;
        switch (badge.rarity) {
            case COMMON:
                rarityTextColor = requireContext().getColor(R.color.gamification_green);
                rarityBgColor = requireContext().getColor(R.color.gamification_green_light);
                break;
            case RARE:
                rarityTextColor = requireContext().getColor(R.color.gamification_blue);
                rarityBgColor = requireContext().getColor(R.color.gamification_blue_light);
                break;
            case EPIC:
                rarityTextColor = requireContext().getColor(R.color.gamification_purple);
                rarityBgColor = requireContext().getColor(R.color.gamification_purple_light);
                break;
            case LEGENDARY:
                rarityTextColor = requireContext().getColor(R.color.gamification_orange);
                rarityBgColor = requireContext().getColor(R.color.gamification_orange_light);
                break;
            default:
                rarityTextColor = requireContext().getColor(R.color.brand_text_secondary);
                rarityBgColor = requireContext().getColor(R.color.brand_surface_muted);
        }
        dialogBadgeRarity.setTextColor(rarityTextColor);
        dialogBadgeRarity.setBackgroundTintList(android.content.res.ColorStateList.valueOf(rarityBgColor));

        if (badge.earned) {
            dialogBadgeStatus.setText("Earned");
            dialogBadgeStatus.setTextColor(requireContext().getColor(R.color.gamification_green));
            dialogBadgeStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    requireContext().getColor(R.color.gamification_green_light)));
            dialogBadgeIcon.setAlpha(1.0f);
        } else {
            dialogBadgeStatus.setText("Locked");
            dialogBadgeStatus.setTextColor(requireContext().getColor(R.color.brand_text_secondary));
            dialogBadgeStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    requireContext().getColor(R.color.brand_surface_muted)));
            dialogBadgeIcon.setAlpha(0.35f);
        }

        dialogCloseButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    @Nullable
    private static LocalDate parseDate(@Nullable String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value);
        } catch (Exception ignored) {
            if (value.length() >= 10) {
                try {
                    return LocalDate.parse(value.substring(0, 10));
                } catch (Exception ignored2) {
                    return null;
                }
            }
            return null;
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
        ValueAnimator animator = ValueAnimator.ofInt(0, endValue);
        animator.setDuration(1200);
        animator.addUpdateListener(animation -> textView.setText(animation.getAnimatedValue().toString()));
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();
    }

    private void animateXpNumber(TextView textView, int endXp) {
        if (endXp <= 0) {
            textView.setText("0");
            return;
        }
        ValueAnimator animator = ValueAnimator.ofInt(0, endXp);
        animator.setDuration(1200);
        animator.addUpdateListener(animation ->
                textView.setText(animation.getAnimatedValue().toString()));
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();
    }

    private void animateProgressBar(ProgressBar progressBar, int progress) {
        ObjectAnimator animator = ObjectAnimator.ofInt(progressBar, "progress", 0, progress);
        animator.setDuration(1200);
        animator.setInterpolator(new DecelerateInterpolator());
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
                .setInterpolator(new OvershootInterpolator(1.3f));
    }

    private void animateViewPop(View view) {
        view.setScaleX(0.5f);
        view.setScaleY(0.5f);
        view.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(800)
                .setInterpolator(new OvershootInterpolator(2.0f));
    }
}
