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
import com.baghdad.edulife.features.auth.viewmodel.AuthViewModel;
import com.baghdad.edulife.features.groupadmin.model.GroupAdminUiState;
import com.baghdad.edulife.features.groupadmin.model.GroupSummary;
import com.baghdad.edulife.features.groupadmin.viewmodel.GroupAdminDashboardViewModel;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.Collections;
import java.util.List;

/**
 * Group admin home: the list of cohorts this admin owns, with create + approvals entry points.
 * Mirrors the web "Group admin portal" (My Groups + Course Approvals).
 */
public class GroupAdminDashboardFragment extends Fragment {

    private GroupAdminDashboardViewModel viewModel;
    private AuthViewModel authViewModel;
    private GroupSummaryAdapter adapter;

    private View stateCard;
    private CircularProgressIndicator loadingIndicator;
    private TextView stateText;
    private TextView retryButton;
    private RecyclerView recyclerView;
    private View emptyView;

    public GroupAdminDashboardFragment() {
        super(R.layout.fragment_group_admin_dashboard);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(GroupAdminDashboardViewModel.class);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        stateCard = view.findViewById(R.id.groupStateCard);
        loadingIndicator = view.findViewById(R.id.groupLoadingIndicator);
        stateText = view.findViewById(R.id.groupStateText);
        retryButton = view.findViewById(R.id.groupRetryButton);
        recyclerView = view.findViewById(R.id.groupsRecycler);
        emptyView = view.findViewById(R.id.groupsEmptyView);

        adapter = new GroupSummaryAdapter(this::openGroup);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        retryButton.setOnClickListener(v -> viewModel.loadGroups());
        view.findViewById(R.id.groupFab).setOnClickListener(v -> showCreateGroupDialog());

        view.findViewById(R.id.groupApprovalsCta).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(
                        R.id.action_groupAdminDashboardFragment_to_courseApprovalsFragment));

        view.findViewById(R.id.groupLogoutButton).setOnClickListener(v -> {
            authViewModel.signOut();
            Navigation.findNavController(view).navigate(
                    R.id.action_groupAdminDashboardFragment_to_loginFragment);
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);
        viewModel.getMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg == null) return;
            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
            viewModel.clearMessage();
        });

        if (viewModel.getUiState().getValue() == null
                || viewModel.getUiState().getValue().loading) {
            viewModel.loadGroups();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Counts change when a group is edited on the detail screen; refresh on return.
        viewModel.loadGroups();
    }

    private void openGroup(GroupSummary group) {
        Bundle args = new Bundle();
        args.putString("groupId", group.id);
        args.putString("groupName", group.name != null ? group.name : "");
        Navigation.findNavController(requireView())
                .navigate(R.id.action_groupAdminDashboardFragment_to_groupDetailFragment, args);
    }

    private void render(@Nullable GroupAdminUiState state) {
        if (state == null) return;

        if (state.loading) {
            stateCard.setVisibility(View.VISIBLE);
            loadingIndicator.setVisibility(View.VISIBLE);
            stateText.setText(R.string.group_admin_loading);
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
        List<GroupSummary> groups = state.groups != null ? state.groups : Collections.emptyList();
        if (groups.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            adapter.submitList(groups);
        }
    }

    private void showCreateGroupDialog() {
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, 0);

        EditText nameInput = new EditText(requireContext());
        nameInput.setHint(getString(R.string.group_admin_name_hint));
        container.addView(nameInput);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.group_admin_create_title)
                .setView(container)
                .setPositiveButton(R.string.group_admin_create_button, (dialog, which) -> {
                    String name = nameInput.getText().toString().trim();
                    if (TextUtils.isEmpty(name)) {
                        Toast.makeText(requireContext(),
                                R.string.group_admin_name_hint, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    viewModel.createGroup(name);
                })
                .setNegativeButton(R.string.group_admin_cancel_button, null)
                .show();
    }
}
