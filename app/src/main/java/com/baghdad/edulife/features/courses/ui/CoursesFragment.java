package com.baghdad.edulife.features.courses.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.courses.model.CourseProgressSummary;
import com.baghdad.edulife.features.courses.model.EnrolledCourse;
import com.baghdad.edulife.features.courses.model.UnenrollUiState;
import com.baghdad.edulife.features.courses.viewmodel.EnrollmentViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CoursesFragment extends Fragment {

    private EnrollmentViewModel enrollmentViewModel;
    private EnrolledCourseAdapter adapter;
    private String activeFilter = "ALL";

    private TextView filterAll, filterBeginner, filterIntermediate, filterAdvanced;
    private TextView courseCountText;
    private TextView emptyText;
    private Button browseButton;
    private RecyclerView recycler;
    private List<EnrolledCourse> allEnrolled = new ArrayList<>();
    private Map<String, CourseProgressSummary> courseProgressMap = new HashMap<>();
    private Set<String> courseProgressFailedIds = new HashSet<>();
    // When the last fetch failed, the empty-state text + browse button are repurposed as the
    // retry surface. applyFilter() must skip its normal rendering so the error UI stays put.
    private boolean inErrorState;

    public CoursesFragment() {
        super(R.layout.fragment_courses);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        enrollmentViewModel = new ViewModelProvider(this).get(EnrollmentViewModel.class);

        View coursesHeader = view.findViewById(R.id.coursesHeaderLayout);
        final int origCoursesHeaderTop = coursesHeader.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            coursesHeader.setPadding(coursesHeader.getPaddingLeft(), origCoursesHeaderTop + top,
                    coursesHeader.getPaddingRight(), coursesHeader.getPaddingBottom());
            return WindowInsetsCompat.CONSUMED;
        });

        emptyText = view.findViewById(R.id.coursesEmptyText);
        courseCountText = view.findViewById(R.id.courseCountText);
        browseButton = view.findViewById(R.id.coursesBrowseButton);
        browseButton.setOnClickListener(v -> navigateToHome());

        adapter = new EnrolledCourseAdapter(this::handleOpenCourse, this::handleUnenroll);
        recycler = view.findViewById(R.id.coursesRecycler);
        int spanCount = getResources().getInteger(R.integer.course_grid_span);
        if (spanCount > 1) {
            recycler.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(requireContext(), spanCount));
        }
        recycler.setAdapter(adapter);

        filterAll = view.findViewById(R.id.filterAll);
        filterBeginner = view.findViewById(R.id.filterBeginner);
        filterIntermediate = view.findViewById(R.id.filterIntermediate);
        filterAdvanced = view.findViewById(R.id.filterAdvanced);

        filterAll.setOnClickListener(v -> applyFilter("ALL"));
        filterBeginner.setOnClickListener(v -> applyFilter("BEGINNER"));
        filterIntermediate.setOnClickListener(v -> applyFilter("INTERMEDIATE"));
        filterAdvanced.setOnClickListener(v -> applyFilter("ADVANCED"));

        enrollmentViewModel.getMyEnrollments().observe(getViewLifecycleOwner(), courses -> {
            allEnrolled = courses != null ? courses : new ArrayList<>();
            // A fresh enrollment list always supersedes any prior error UI even when the list
            // happens to be empty, so the user can see "no courses" instead of stale "retry".
            if (inErrorState && courses != null) {
                inErrorState = false;
            }
            applyFilter(activeFilter);
        });

        enrollmentViewModel.getMyCourseProgress().observe(getViewLifecycleOwner(), progressMap -> {
            courseProgressMap = progressMap != null ? progressMap : Collections.emptyMap();
            adapter.setProgressState(courseProgressMap, courseProgressFailedIds);
        });

        enrollmentViewModel.getMyCourseProgressFailedIds().observe(getViewLifecycleOwner(), failedIds -> {
            courseProgressFailedIds = failedIds != null ? failedIds : Collections.emptySet();
            adapter.setProgressState(courseProgressMap, courseProgressFailedIds);
        });

        enrollmentViewModel.getMyEnrollmentsError().observe(getViewLifecycleOwner(), error -> {
            if (error == null || error.isBlank()) {
                // ViewModel posts null at the start of every fetch; treat as "no error visible".
                if (inErrorState) {
                    inErrorState = false;
                    applyFilter(activeFilter);
                }
                return;
            }
            // Distinct error state — repurposes the empty TextView + browse button as the retry
            // surface so a failed fetch is never indistinguishable from "you have no courses".
            inErrorState = true;
            showError(getString(R.string.courses_load_error));
        });

        enrollmentViewModel.getMyEnrollmentsLoading().observe(getViewLifecycleOwner(), loading -> {
            if (Boolean.TRUE.equals(loading) && allEnrolled.isEmpty()) {
                showEmpty(getString(R.string.courses_loading), false);
            }
        });

        enrollmentViewModel.getUnenrollState().observe(getViewLifecycleOwner(), this::handleUnenrollState);

        showEmpty(getString(R.string.courses_loading), false);
    }

    @Override
    public void onResume() {
        super.onResume();
        enrollmentViewModel.loadMyEnrollments();
    }

    private void applyFilter(String level) {
        activeFilter = level;
        updateFilterChipStyles();

        // Error UI owns the empty-state real estate; let the dedicated error renderer manage
        // it so a filter tap during an error does not overwrite "Retry" with "Browse courses".
        if (inErrorState) return;

        List<EnrolledCourse> filtered = new ArrayList<>();
        for (EnrolledCourse c : allEnrolled) {
            if (level.equals("ALL") || level.equalsIgnoreCase(c.level)) {
                filtered.add(c);
            }
        }

        adapter.setItems(filtered);
        if (recycler != null) recycler.setVisibility(View.VISIBLE);

        if (filtered.isEmpty()) {
            if (allEnrolled.isEmpty()) {
                showEmpty(getString(R.string.courses_empty_unenrolled), true);
            } else {
                showEmpty(getString(R.string.courses_empty_filtered,
                        level.toLowerCase(Locale.ROOT)), false);
            }
        } else {
            emptyText.setVisibility(View.GONE);
            browseButton.setVisibility(View.GONE);
        }

        courseCountText.setText(filtered.size() + " course" + (filtered.size() == 1 ? "" : "s") + " enrolled");
    }

    private void showEmpty(String message, boolean showBrowseButton) {
        emptyText.setText(message);
        emptyText.setVisibility(View.VISIBLE);
        browseButton.setVisibility(showBrowseButton ? View.VISIBLE : View.GONE);
        // Restore the normal "Browse" semantics in case the button is being recycled from a
        // prior error-state retry binding.
        browseButton.setText(R.string.courses_browse_cta);
        browseButton.setOnClickListener(v -> navigateToHome());
    }

    private void showError(String message) {
        if (recycler != null) recycler.setVisibility(View.GONE);
        emptyText.setText(message);
        emptyText.setVisibility(View.VISIBLE);
        browseButton.setText(R.string.courses_retry);
        browseButton.setVisibility(View.VISIBLE);
        browseButton.setOnClickListener(v -> enrollmentViewModel.loadMyEnrollments());
    }

    private void handleOpenCourse(EnrolledCourse course) {
        if (course.courseId == null || course.courseId.isBlank()) return;
        Bundle args = new Bundle();
        args.putString("courseId", course.courseId);
        args.putBoolean("isEnrolled", true);
        Navigation.findNavController(requireView())
                .navigate(R.id.action_coursesFragment_to_courseDetailFragment, args);
    }

    private void handleUnenrollState(UnenrollUiState state) {
        if (state == null) return;
        if (state.errorMessage != null) {
            // Errors no longer overwrite the empty-state TextView (which serves "loading" /
            // "no courses") because that conflated two unrelated UI states.
            Toast.makeText(requireContext(),
                    getString(R.string.courses_unenroll_error), Toast.LENGTH_SHORT).show();
            enrollmentViewModel.clearUnenrollState();
            return;
        }
        if (state.unenrolled) {
            Toast.makeText(requireContext(),
                    getString(R.string.courses_unenroll_success), Toast.LENGTH_SHORT).show();
            enrollmentViewModel.clearUnenrollState();
        }
    }

    private void handleUnenroll(EnrolledCourse course) {
        if (course.enrollmentId == null || course.enrollmentId.isBlank()) return;
        String title = course.title != null ? course.title : "this course";
        new AlertDialog.Builder(requireContext())
                .setTitle("Unenroll from course?")
                .setMessage("You will lose access to \"" + title + "\". This cannot be undone.")
                .setPositiveButton("Unenroll", (dialog, which) ->
                        enrollmentViewModel.unenroll(course.enrollmentId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateFilterChipStyles() {
        setChipActive(filterAll, "ALL".equals(activeFilter));
        setChipActive(filterBeginner, "BEGINNER".equals(activeFilter));
        setChipActive(filterIntermediate, "INTERMEDIATE".equals(activeFilter));
        setChipActive(filterAdvanced, "ADVANCED".equals(activeFilter));
    }

    private void navigateToHome() {
        NavOptions options = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setPopUpTo(R.id.homeFragment, false, true)
                .build();
        // Re-enrollment already works from Home. This CTA makes that path explicit instead of
        // forcing the learner to infer which tab contains the enrollment entry point.
        Navigation.findNavController(requireView())
                .navigate(R.id.homeFragment, null, options);
    }

    private void setChipActive(TextView chip, boolean active) {
        chip.setBackgroundResource(active
                ? R.drawable.bg_category_chip_active
                : R.drawable.bg_category_chip);
        chip.setTextColor(active
                ? requireContext().getColor(android.R.color.white)
                : requireContext().getColor(R.color.brand_primary));
        chip.setTypeface(null, active
                ? android.graphics.Typeface.BOLD
                : android.graphics.Typeface.NORMAL);
    }

    // ── Enrolled Course Adapter ───────────────────────────────────────────────

    interface OpenCourseAction {
        void onOpen(EnrolledCourse course);
    }

    interface UnenrollAction {
        void onUnenroll(EnrolledCourse course);
    }

    static class EnrolledCourseAdapter extends RecyclerView.Adapter<EnrolledCourseAdapter.VH> {

        private List<EnrolledCourse> items = new ArrayList<>();
        private Map<String, CourseProgressSummary> progressByCourseId = Collections.emptyMap();
        private Set<String> failedProgressCourseIds = Collections.emptySet();
        private final OpenCourseAction openAction;
        private final UnenrollAction unenrollAction;

        EnrolledCourseAdapter(OpenCourseAction openAction, UnenrollAction unenrollAction) {
            this.openAction = openAction;
            this.unenrollAction = unenrollAction;
        }

        void setItems(List<EnrolledCourse> list) {
            items = list;
            notifyDataSetChanged();
        }

        void setProgressState(Map<String, CourseProgressSummary> progressByCourseId,
                              Set<String> failedProgressCourseIds) {
            this.progressByCourseId = progressByCourseId != null
                    ? progressByCourseId
                    : Collections.emptyMap();
            this.failedProgressCourseIds = failedProgressCourseIds != null
                    ? failedProgressCourseIds
                    : Collections.emptySet();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_enrolled_course, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            EnrolledCourse course = items.get(position);
            holder.title.setText(course.title != null ? course.title : "");
            holder.desc.setText(course.shortDescription != null ? course.shortDescription : "");
            holder.language.setText(normalizeLabel(course.languageCode));
            holder.level.setText(course.level != null ? course.level : "");
            holder.progress.setText(resolveProgressLabel(holder, course));
            holder.continueBtn.setOnClickListener(v -> openAction.onOpen(course));
            holder.unenrollBtn.setOnClickListener(v -> unenrollAction.onUnenroll(course));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            final TextView title, desc, language, level, progress, unenrollBtn;
            final Button continueBtn;

            VH(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.enrolledCourseTitle);
                desc = itemView.findViewById(R.id.enrolledCourseDesc);
                language = itemView.findViewById(R.id.enrolledCourseLanguage);
                level = itemView.findViewById(R.id.enrolledLevelBadge);
                progress = itemView.findViewById(R.id.enrolledCourseProgress);
                continueBtn = itemView.findViewById(R.id.continueLearningButton);
                unenrollBtn = itemView.findViewById(R.id.unenrollButton);
            }
        }

        private String resolveProgressLabel(VH holder, EnrolledCourse course) {
            if (course.courseId == null || course.courseId.isBlank()) {
                return holder.itemView.getContext().getString(R.string.courses_progress_unavailable);
            }

            if (failedProgressCourseIds.contains(course.courseId)) {
                return holder.itemView.getContext().getString(R.string.courses_progress_unavailable);
            }

            CourseProgressSummary progress = progressByCourseId.get(course.courseId);
            if (progress == null) {
                return holder.itemView.getContext().getString(R.string.courses_progress_loading);
            }

            int percent = (int) Math.round(progress.percentComplete);
            return holder.itemView.getContext().getString(
                    R.string.courses_progress_format,
                    progress.completedLessons,
                    progress.totalLessons,
                    percent
            );
        }

        private static String normalizeLabel(String raw) {
            if (raw == null || raw.isBlank()) return "";
            String s = raw.replace('_', ' ').toLowerCase(Locale.ROOT);
            return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
        }
    }
}
