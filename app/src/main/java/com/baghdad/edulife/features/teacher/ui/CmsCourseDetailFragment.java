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
import com.baghdad.edulife.features.teacher.model.CmsCourseDetailUiState;
import com.baghdad.edulife.features.teacher.model.CmsSection;
import com.baghdad.edulife.features.teacher.viewmodel.CmsCourseDetailViewModel;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.Collections;
import java.util.List;

public class CmsCourseDetailFragment extends Fragment {

    private CmsCourseDetailViewModel viewModel;
    private CmsSectionAdapter sectionAdapter;

    private String courseId;
    private String courseTitle;

    private View stateCard;
    private CircularProgressIndicator loadingIndicator;
    private TextView stateText;
    private TextView retryButton;
    private RecyclerView sectionsRecycler;
    private View addSectionButton;
    private View examButton;
    private TextView toolbarTitle;

    public CmsCourseDetailFragment() {
        super(R.layout.fragment_cms_course_detail);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(CmsCourseDetailViewModel.class);

        // Read nav args via Bundle — no SafeArgs
        Bundle args = getArguments();
        courseId = args != null ? args.getString("courseId", "") : "";
        courseTitle = args != null ? args.getString("courseTitle", "") : "";

        toolbarTitle = view.findViewById(R.id.cmsDetailCourseTitle);
        if (toolbarTitle != null) {
            toolbarTitle.setText(TextUtils.isEmpty(courseTitle) ? "Course Detail" : courseTitle);
        }

        view.findViewById(R.id.cmsDetailBackButton).setOnClickListener(v ->
                Navigation.findNavController(view).navigateUp());

        stateCard = view.findViewById(R.id.cmsDetailStateCard);
        loadingIndicator = view.findViewById(R.id.cmsDetailLoadingIndicator);
        stateText = view.findViewById(R.id.cmsDetailStateText);
        retryButton = view.findViewById(R.id.cmsDetailRetryButton);
        sectionsRecycler = view.findViewById(R.id.cmsSectionsRecycler);
        addSectionButton = view.findViewById(R.id.cmsAddSectionButton);

        sectionAdapter = new CmsSectionAdapter((section) -> {
            if (section.id != null && courseId != null) {
                showDeleteSectionDialog(section);
            }
        });

        sectionsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        sectionsRecycler.setAdapter(sectionAdapter);

        examButton = view.findViewById(R.id.cmsExamButton);

        retryButton.setOnClickListener(v -> viewModel.loadSections(courseId));
        addSectionButton.setOnClickListener(v -> showAddSectionDialog());
        examButton.setOnClickListener(v -> {
            Bundle navArgs = new Bundle();
            navArgs.putString("courseId", courseId);
            navArgs.putString("courseTitle", courseTitle);
            Navigation.findNavController(view).navigate(
                    R.id.action_cmsCourseDetailFragment_to_cmsExamBuilderFragment, navArgs);
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);

        if (!TextUtils.isEmpty(courseId)) {
            viewModel.loadSections(courseId);
        }
    }

    private void render(@Nullable CmsCourseDetailUiState state) {
        if (state == null) return;

        if (state.actionMessage != null) {
            Toast.makeText(requireContext(), state.actionMessage, Toast.LENGTH_SHORT).show();
        }

        if (state.loading) {
            stateCard.setVisibility(View.VISIBLE);
            loadingIndicator.setVisibility(View.VISIBLE);
            stateText.setText(R.string.teacher_loading);
            retryButton.setVisibility(View.GONE);
            sectionsRecycler.setVisibility(View.GONE);
            addSectionButton.setVisibility(View.GONE);
            return;
        }

        if (state.error != null) {
            stateCard.setVisibility(View.VISIBLE);
            loadingIndicator.setVisibility(View.GONE);
            stateText.setText(state.error);
            retryButton.setVisibility(View.VISIBLE);
            sectionsRecycler.setVisibility(View.GONE);
            addSectionButton.setVisibility(View.GONE);
            return;
        }

        stateCard.setVisibility(View.GONE);
        sectionsRecycler.setVisibility(View.VISIBLE);
        addSectionButton.setVisibility(View.VISIBLE);
        examButton.setVisibility(View.VISIBLE);

        List<CmsSection> sections = state.sections != null ? state.sections : Collections.emptyList();
        sectionAdapter.submitList(sections);
    }

    private void showAddSectionDialog() {
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, 0);

        EditText titleInput = new EditText(requireContext());
        titleInput.setHint(getString(R.string.teacher_section_title_hint));
        container.addView(titleInput);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.teacher_add_section)
                .setView(container)
                .setPositiveButton(R.string.teacher_create_button, (dialog, which) -> {
                    String title = titleInput.getText().toString().trim();
                    if (TextUtils.isEmpty(title)) {
                        Toast.makeText(requireContext(),
                                R.string.teacher_section_title_hint, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Derive order from current list size + 1
                    int order = sectionAdapter.getItemCount() + 1;
                    viewModel.createSection(courseId, title, order);
                })
                .setNegativeButton(R.string.teacher_cancel_button, null)
                .show();
    }

    private void showDeleteSectionDialog(CmsSection section) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete section?")
                .setMessage("Delete \"" + section.title + "\"? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) ->
                        viewModel.deleteSection(courseId, section.id))
                .setNegativeButton(R.string.teacher_cancel_button, null)
                .show();
    }
}
