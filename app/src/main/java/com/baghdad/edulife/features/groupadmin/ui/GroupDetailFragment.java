package com.baghdad.edulife.features.groupadmin.ui;

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
import com.baghdad.edulife.features.courses.model.CourseSummary;
import com.baghdad.edulife.features.groupadmin.data.GroupAdminRepository;
import com.baghdad.edulife.features.groupadmin.model.GroupCourse;
import com.baghdad.edulife.features.groupadmin.model.GroupDetail;
import com.baghdad.edulife.features.groupadmin.model.GroupDetailUiState;
import com.baghdad.edulife.features.groupadmin.model.GroupMember;
import com.baghdad.edulife.features.groupadmin.viewmodel.GroupDetailViewModel;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Single-cohort management: members (add by email / remove) and attached courses (assign). */
public class GroupDetailFragment extends Fragment {

    private GroupDetailViewModel viewModel;
    private GroupMemberAdapter memberAdapter;
    private GroupCourseAdapter courseAdapter;

    private String groupId;

    private View stateCard;
    private CircularProgressIndicator loadingIndicator;
    private TextView stateText;
    private TextView retryButton;
    private View content;
    private TextView membersEmpty;
    private TextView coursesEmpty;
    // Holds the latest attached courses so the assign picker can hide duplicates.
    private List<GroupCourse> attachedCourses = Collections.emptyList();

    public GroupDetailFragment() {
        super(R.layout.fragment_group_detail);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(GroupDetailViewModel.class);

        Bundle args = getArguments();
        groupId = args != null ? args.getString("groupId", "") : "";
        String groupName = args != null ? args.getString("groupName", "") : "";

        TextView titleView = view.findViewById(R.id.groupDetailName);
        titleView.setText(TextUtils.isEmpty(groupName)
                ? getString(R.string.group_admin_portal_subtitle) : groupName);

        view.findViewById(R.id.groupDetailBack).setOnClickListener(v ->
                Navigation.findNavController(view).navigateUp());

        stateCard = view.findViewById(R.id.groupDetailStateCard);
        loadingIndicator = view.findViewById(R.id.groupDetailLoading);
        stateText = view.findViewById(R.id.groupDetailStateText);
        retryButton = view.findViewById(R.id.groupDetailRetry);
        content = view.findViewById(R.id.groupDetailContent);
        membersEmpty = view.findViewById(R.id.membersEmpty);
        coursesEmpty = view.findViewById(R.id.coursesEmpty);

        memberAdapter = new GroupMemberAdapter(this::confirmRemoveMember);
        RecyclerView membersRecycler = view.findViewById(R.id.membersRecycler);
        membersRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        membersRecycler.setAdapter(memberAdapter);

        courseAdapter = new GroupCourseAdapter();
        RecyclerView coursesRecycler = view.findViewById(R.id.coursesRecycler);
        coursesRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        coursesRecycler.setAdapter(courseAdapter);

        retryButton.setOnClickListener(v -> viewModel.loadDetail(groupId));
        view.findViewById(R.id.addMemberButton).setOnClickListener(v -> showAddMemberDialog());
        view.findViewById(R.id.assignCourseButton).setOnClickListener(v -> showAssignCourseDialog());

        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);
        viewModel.getMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg == null) return;
            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
            viewModel.clearMessage();
        });

        viewModel.loadDetail(groupId);
    }

    private void render(@Nullable GroupDetailUiState state) {
        if (state == null) return;

        if (state.loading) {
            stateCard.setVisibility(View.VISIBLE);
            loadingIndicator.setVisibility(View.VISIBLE);
            stateText.setText(R.string.group_admin_loading);
            retryButton.setVisibility(View.GONE);
            content.setVisibility(View.GONE);
            return;
        }

        if (state.error != null) {
            stateCard.setVisibility(View.VISIBLE);
            loadingIndicator.setVisibility(View.GONE);
            stateText.setText(state.error);
            retryButton.setVisibility(View.VISIBLE);
            content.setVisibility(View.GONE);
            return;
        }

        stateCard.setVisibility(View.GONE);
        content.setVisibility(View.VISIBLE);
        bindDetail(state.detail);
    }

    private void bindDetail(@Nullable GroupDetail detail) {
        if (detail == null) return;

        List<GroupMember> members = detail.members != null ? detail.members : Collections.emptyList();
        memberAdapter.submitList(members);
        membersEmpty.setVisibility(members.isEmpty() ? View.VISIBLE : View.GONE);

        attachedCourses = detail.courses != null ? detail.courses : Collections.emptyList();
        courseAdapter.submitList(attachedCourses);
        coursesEmpty.setVisibility(attachedCourses.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void confirmRemoveMember(GroupMember member) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.group_member_remove_title)
                .setMessage(getString(R.string.group_member_remove_body,
                        member.email != null ? member.email : member.userId))
                .setPositiveButton(R.string.group_member_remove_confirm,
                        (d, w) -> viewModel.removeMember(groupId, member.userId))
                .setNegativeButton(R.string.group_admin_cancel_button, null)
                .show();
    }

    private void showAddMemberDialog() {
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, 0);

        EditText emailInput = new EditText(requireContext());
        emailInput.setHint(getString(R.string.group_member_email_hint));
        emailInput.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        container.addView(emailInput);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.group_member_add_title)
                .setView(container)
                .setPositiveButton(R.string.group_member_add_confirm, (dialog, which) -> {
                    String email = emailInput.getText().toString().trim();
                    if (TextUtils.isEmpty(email)) {
                        Toast.makeText(requireContext(),
                                R.string.group_member_email_hint, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    viewModel.addMember(groupId, email);
                })
                .setNegativeButton(R.string.group_admin_cancel_button, null)
                .show();
    }

    private void showAssignCourseDialog() {
        // Pull the published catalog, then drop any course already attached to this group.
        viewModel.loadCatalog(new GroupAdminRepository.CatalogCallback() {
            @Override
            public void onSuccess(List<CourseSummary> courses) {
                if (!isAdded()) return;

                Set<String> attachedIds = new HashSet<>();
                for (GroupCourse gc : attachedCourses) {
                    if (gc.courseId != null) attachedIds.add(gc.courseId);
                }

                final List<CourseSummary> selectable = new ArrayList<>();
                final List<String> titles = new ArrayList<>();
                for (CourseSummary c : courses) {
                    if (c.id != null && !attachedIds.contains(c.id)) {
                        selectable.add(c);
                        titles.add(c.title != null ? c.title : c.slug);
                    }
                }

                if (selectable.isEmpty()) {
                    Toast.makeText(requireContext(),
                            R.string.group_course_none_available, Toast.LENGTH_SHORT).show();
                    return;
                }

                new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.group_course_assign_title)
                        .setItems(titles.toArray(new String[0]), (dialog, which) ->
                                viewModel.attachCourse(groupId, selectable.get(which).id))
                        .setNegativeButton(R.string.group_admin_cancel_button, null)
                        .show();
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
