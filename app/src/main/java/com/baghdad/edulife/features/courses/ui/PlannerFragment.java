package com.baghdad.edulife.features.courses.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.courses.model.EnrolledCourse;
import com.baghdad.edulife.features.courses.model.PlannerTask;
import com.baghdad.edulife.features.courses.viewmodel.EnrollmentViewModel;
import com.baghdad.edulife.features.courses.viewmodel.PlannerViewModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fragment serving as the UI controller for the Study Planner.
 */
public class PlannerFragment extends Fragment {

    private PlannerViewModel viewModel;
    private EnrollmentViewModel enrollmentViewModel;

    private TextView progressHoursText;
    private TextView progressPercentageChip;
    private ProgressBar progressBar;
    private EditText goalInput;
    private TextView targetHoursText;
    private LinearLayout focusCoursesContainer;
    private TextView emptyTasksText;
    private RecyclerView tasksRecyclerView;
    private EditText taskInput;

    private PlannerTaskAdapter taskAdapter;

    private final List<EnrolledCourse> enrolledCourses = new ArrayList<>();
    private final Set<String> focusCourseIds = new HashSet<>();

    public PlannerFragment() {
        super(R.layout.fragment_planner);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(PlannerViewModel.class);
        // Reuse EnrollmentViewModel to bind the active enrolled courses to focus selection
        enrollmentViewModel = new ViewModelProvider(this).get(EnrollmentViewModel.class);

        // Standard status bar top-padding adjustments
        View header = view.findViewById(R.id.plannerHeaderLayout);
        final int originalHeaderTop = header.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            header.setPadding(header.getPaddingLeft(), originalHeaderTop + top,
                    header.getPaddingRight(), header.getPaddingBottom());
            return WindowInsetsCompat.CONSUMED;
        });

        // Bind progress tracker views
        progressHoursText = view.findViewById(R.id.progressHoursText);
        progressPercentageChip = view.findViewById(R.id.progressPercentageChip);
        progressBar = view.findViewById(R.id.plannerProgressBar);
        View log30mButton = view.findViewById(R.id.log30mButton);
        View log1hButton = view.findViewById(R.id.log1hButton);

        log30mButton.setOnClickListener(v -> viewModel.addCompletedHours(0.5f));
        log1hButton.setOnClickListener(v -> viewModel.addCompletedHours(1.0f));

        // Bind weekly goal view
        goalInput = view.findViewById(R.id.plannerGoalInput);
        goalInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                viewModel.setWeeklyGoal(v.getText().toString());
                hideKeyboard(v);
                return true;
            }
            return false;
        });

        // Bind target hour views
        targetHoursText = view.findViewById(R.id.targetHoursText);
        ImageButton decrementBtn = view.findViewById(R.id.decrementHoursBtn);
        ImageButton incrementBtn = view.findViewById(R.id.incrementHoursBtn);

        decrementBtn.setOnClickListener(v -> viewModel.decrementTargetHours());
        incrementBtn.setOnClickListener(v -> viewModel.incrementTargetHours());

        // Bind day circles
        TextView[] dayViews = new TextView[]{
                view.findViewById(R.id.dayMon),
                view.findViewById(R.id.dayTue),
                view.findViewById(R.id.dayWed),
                view.findViewById(R.id.dayThu),
                view.findViewById(R.id.dayFri),
                view.findViewById(R.id.daySat),
                view.findViewById(R.id.daySun)
        };
        String[] dayNames = new String[]{
                "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
        };

        for (int i = 0; i < dayViews.length; i++) {
            final String dayName = dayNames[i];
            dayViews[i].setOnClickListener(v -> viewModel.toggleStudyDay(dayName));
        }

        // Bind course focus views
        focusCoursesContainer = view.findViewById(R.id.focusCoursesContainer);

        // Bind checklist views
        emptyTasksText = view.findViewById(R.id.emptyTasksText);
        tasksRecyclerView = view.findViewById(R.id.tasksRecyclerView);
        taskInput = view.findViewById(R.id.taskInput);
        Button addTaskButton = view.findViewById(R.id.addTaskButton);

        tasksRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        taskAdapter = new PlannerTaskAdapter(new PlannerTaskAdapter.PlannerTaskListener() {
            @Override
            public void onTaskToggled(String taskId) {
                viewModel.toggleTask(taskId);
            }

            @Override
            public void onTaskDeleted(String taskId) {
                viewModel.deleteTask(taskId);
            }
        });
        tasksRecyclerView.setAdapter(taskAdapter);

        addTaskButton.setOnClickListener(v -> {
            String text = taskInput.getText().toString().trim();
            if (!text.isEmpty()) {
                List<PlannerTask> currentTasks = viewModel.getTasks().getValue();
                if (currentTasks != null && currentTasks.size() >= 10) {
                    Toast.makeText(requireContext(), "Checklist limited to 10 active tasks to keep plan focused", Toast.LENGTH_SHORT).show();
                    return;
                }
                viewModel.addTask(text);
                taskInput.setText("");
                hideKeyboard(v);
            }
        });

        taskInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String text = v.getText().toString().trim();
                if (!text.isEmpty()) {
                    List<PlannerTask> currentTasks = viewModel.getTasks().getValue();
                    if (currentTasks != null && currentTasks.size() >= 10) {
                        Toast.makeText(requireContext(), "Checklist limited to 10 active tasks to keep plan focused", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    viewModel.addTask(text);
                    v.setText("");
                    hideKeyboard(v);
                }
                return true;
            }
            return false;
        });

        // Reset Week Dialog
        view.findViewById(R.id.resetWeekButton).setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.planner_reset_title)
                    .setMessage(R.string.planner_reset_message)
                    .setPositiveButton(R.string.planner_reset_btn, (dialog, which) -> {
                        viewModel.startNewWeek();
                        Toast.makeText(requireContext(), "New study week started!", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(R.string.planner_reset_cancel, null)
                    .show();
        });

        // ── Observe ViewState LiveDatas ──

        viewModel.getWeeklyGoal().observe(getViewLifecycleOwner(), goal -> {
            if (goal != null && !goal.equals(goalInput.getText().toString()) && !goalInput.hasFocus()) {
                goalInput.setText(goal);
            }
        });

        viewModel.getTargetHours().observe(getViewLifecycleOwner(), target -> {
            updateProgressAndHours();
        });

        viewModel.getCompletedHours().observe(getViewLifecycleOwner(), completed -> {
            updateProgressAndHours();
        });

        viewModel.getStudyDays().observe(getViewLifecycleOwner(), selectedDays -> {
            for (int i = 0; i < dayViews.length; i++) {
                String dayName = dayNames[i];
                boolean selected = selectedDays != null && selectedDays.contains(dayName);
                dayViews[i].setSelected(selected);
                dayViews[i].setTextColor(requireContext().getColor(
                        selected ? android.R.color.white : R.color.brand_text_secondary));
            }
        });

        viewModel.getFocusCourses().observe(getViewLifecycleOwner(), selectedIds -> {
            focusCourseIds.clear();
            if (selectedIds != null) {
                focusCourseIds.addAll(selectedIds);
            }
            rebuildCourseCheckboxes();
        });

        viewModel.getTasks().observe(getViewLifecycleOwner(), tasksList -> {
            taskAdapter.submitList(tasksList);
            boolean empty = tasksList == null || tasksList.isEmpty();
            emptyTasksText.setVisibility(empty ? View.VISIBLE : View.GONE);
            tasksRecyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        });

        // Load active enrolled courses to select focus courses from
        enrollmentViewModel.getMyEnrollments().observe(getViewLifecycleOwner(), courses -> {
            enrolledCourses.clear();
            if (courses != null) {
                enrolledCourses.addAll(courses);
            }
            rebuildCourseCheckboxes();
        });

        enrollmentViewModel.loadMyEnrollments();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Autosave goal on pause (when navigating away or minimizing app)
        if (viewModel != null && goalInput != null) {
            viewModel.setWeeklyGoal(goalInput.getText().toString());
        }
    }

    private void updateProgressAndHours() {
        int target = viewModel.getTargetHours().getValue() != null ? viewModel.getTargetHours().getValue() : 10;
        float completed = viewModel.getCompletedHours().getValue() != null ? viewModel.getCompletedHours().getValue() : 0.0f;

        targetHoursText.setText(getString(R.string.planner_hours_unit, target));
        progressHoursText.setText(getString(R.string.planner_logged_hours, completed, target));

        int percentage = Math.round((completed / (float) target) * 100f);
        int clamped = Math.max(0, Math.min(percentage, 100));
        progressBar.setProgress(clamped);
        if (progressPercentageChip != null) {
            progressPercentageChip.setText(clamped + "%");
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    /**
     * Dynamically builds course selection checkboxes based on active enrollments.
     */
    private void rebuildCourseCheckboxes() {
        if (focusCoursesContainer == null) return;
        focusCoursesContainer.removeAllViews();

        if (enrolledCourses.isEmpty()) {
            TextView noneText = new TextView(requireContext());
            noneText.setText("No active enrollments. Enroll in courses from the Home tab to add focus here.");
            noneText.setTextColor(requireContext().getColor(R.color.brand_text_muted));
            noneText.setTextSize(13);
            noneText.setLineSpacing(0f, 1.35f);
            noneText.setPadding(0, dp(8), 0, dp(8));
            focusCoursesContainer.addView(noneText);
            return;
        }

        for (EnrolledCourse course : enrolledCourses) {
            CheckBox checkBox = new CheckBox(requireContext());
            checkBox.setText(course.title);
            checkBox.setButtonTintList(android.content.res.ColorStateList.valueOf(
                    requireContext().getColor(R.color.brand_primary)));
            checkBox.setTextColor(requireContext().getColor(R.color.brand_text_primary));
            checkBox.setChecked(focusCourseIds.contains(course.courseId));
            checkBox.setPadding(dp(8), dp(10), dp(8), dp(10));
            checkBox.setTextSize(14);
            LinearLayout.LayoutParams cbParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            cbParams.setMargins(0, dp(2), 0, dp(2));
            checkBox.setLayoutParams(cbParams);

            // Prevent recursion when rendering
            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(focusCourseIds.contains(course.courseId));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                viewModel.toggleFocusCourse(course.courseId);
            });

            focusCoursesContainer.addView(checkBox);
        }
    }

    private void hideKeyboard(View view) {
        InputMethodManager manager =
                (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null) {
            manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
