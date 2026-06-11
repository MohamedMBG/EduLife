package com.baghdad.edulife.features.teacher.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.baghdad.edulife.R;
import com.baghdad.edulife.core.storage.SessionStorage;
import com.baghdad.edulife.features.teacher.model.CmsCourse;
import com.baghdad.edulife.features.teacher.model.TeacherDashboardUiState;
import com.baghdad.edulife.features.teacher.viewmodel.TeacherDashboardViewModel;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.Collections;
import java.util.List;

public class TeacherDashboardFragment extends Fragment {

    private TeacherDashboardViewModel viewModel;
    private TeacherCourseAdapter adapter;

    private View stateCard;
    private CircularProgressIndicator loadingIndicator;
    private TextView stateText;
    private TextView retryButton;
    private RecyclerView recyclerView;
    private View emptyView;
    private View fabButton;

    public TeacherDashboardFragment() {
        super(R.layout.fragment_teacher_dashboard);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(TeacherDashboardViewModel.class);

        // Bind header name from session
        SessionStorage session = new SessionStorage(requireContext());
        TextView welcomeNameText = view.findViewById(R.id.teacherWelcomeName);
        if (welcomeNameText != null) {
            String userId = session.getUserId();
            welcomeNameText.setText(userId != null ? "Teacher" : "Teacher");
        }

        stateCard = view.findViewById(R.id.teacherStateCard);
        loadingIndicator = view.findViewById(R.id.teacherLoadingIndicator);
        stateText = view.findViewById(R.id.teacherStateText);
        retryButton = view.findViewById(R.id.teacherRetryButton);
        recyclerView = view.findViewById(R.id.teacherCoursesRecycler);
        emptyView = view.findViewById(R.id.teacherEmptyView);
        fabButton = view.findViewById(R.id.teacherFab);

        adapter = new TeacherCourseAdapter(course -> {
            Bundle args = new Bundle();
            args.putString("courseId", course.id);
            args.putString("courseTitle", course.title != null ? course.title : "");
            Navigation.findNavController(view)
                    .navigate(R.id.action_teacherDashboardFragment_to_cmsCourseDetailFragment, args);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        retryButton.setOnClickListener(v -> viewModel.loadCourses());

        fabButton.setOnClickListener(v -> showCreateCourseDialog());

        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);

        if (viewModel.getUiState().getValue() == null
                || viewModel.getUiState().getValue().loading) {
            viewModel.loadCourses();
        }
    }

    private void render(@Nullable TeacherDashboardUiState state) {
        if (state == null) return;

        if (state.loading) {
            stateCard.setVisibility(View.VISIBLE);
            loadingIndicator.setVisibility(View.VISIBLE);
            stateText.setText(R.string.teacher_loading);
            retryButton.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);
            return;
        }

        if (state.error != null) {
            stateCard.setVisibility(View.VISIBLE);
            loadingIndicator.setVisibility(View.GONE);
            stateText.setText(state.error);
            retryButton.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);
            return;
        }

        stateCard.setVisibility(View.GONE);

        List<CmsCourse> courses = state.courses != null ? state.courses : Collections.emptyList();
        if (courses.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            adapter.submitList(courses);
        }
    }

    private void showCreateCourseDialog() {
        // Build inline dialog with EditTexts
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, 0);

        EditText titleInput = new EditText(requireContext());
        titleInput.setHint(getString(R.string.teacher_course_title_hint));
        container.addView(titleInput);

        EditText descInput = new EditText(requireContext());
        descInput.setHint(getString(R.string.teacher_course_desc_hint));
        container.addView(descInput);

        EditText langInput = new EditText(requireContext());
        langInput.setHint(getString(R.string.teacher_course_lang_hint));
        container.addView(langInput);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.teacher_create_course_title)
                .setView(container)
                .setPositiveButton(R.string.teacher_create_button, (dialog, which) -> {
                    String title = titleInput.getText().toString().trim();
                    String desc = descInput.getText().toString().trim();
                    String lang = langInput.getText().toString().trim();

                    if (TextUtils.isEmpty(title)) {
                        Toast.makeText(requireContext(),
                                R.string.teacher_course_title_hint, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (TextUtils.isEmpty(lang)) {
                        lang = "en";
                    }

                    viewModel.createCourse(title, desc, desc, lang);
                })
                .setNegativeButton(R.string.teacher_cancel_button, null)
                .show();
    }
}
