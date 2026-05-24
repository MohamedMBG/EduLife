package com.baghdad.edulife.features.courses.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.baghdad.edulife.R;
import com.baghdad.edulife.core.storage.SessionStorage;
import com.baghdad.edulife.features.auth.viewmodel.AuthViewModel;
import com.baghdad.edulife.features.courses.model.CourseCatalogUiState;
import com.baghdad.edulife.features.courses.model.CourseSummary;
import com.baghdad.edulife.features.courses.viewmodel.CourseCatalogViewModel;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Collections;

public class HomeFragment extends Fragment {

    private AuthViewModel authViewModel;
    private CourseCatalogViewModel courseCatalogViewModel;
    private SessionStorage sessionStorage;
    private CourseCatalogAdapter courseCatalogAdapter;

    private View loadingIndicator;
    private TextView statusText;
    private TextView retryButton;
    private TextView allFilterButton;
    private TextView beginnerFilterButton;
    private TextView intermediateFilterButton;

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        courseCatalogViewModel = new ViewModelProvider(this).get(CourseCatalogViewModel.class);
        sessionStorage = new SessionStorage(requireContext());

        if (!isSessionValid()) {
            redirectToLogin(view);
            return;
        }

        bindSessionData(view);
        setupRecyclerView(view);
        setupFilterButtons(view);

        loadingIndicator = view.findViewById(R.id.loadingIndicator);
        statusText = view.findViewById(R.id.statusText);
        retryButton = view.findViewById(R.id.retryButton);

        retryButton.setOnClickListener(v -> reloadCurrentFilter());
        view.findViewById(R.id.logoutButton).setOnClickListener(v -> handleLogout(view));

        courseCatalogViewModel.getUiState().observe(getViewLifecycleOwner(), this::renderCatalogState);

        CourseCatalogUiState currentState = courseCatalogViewModel.getUiState().getValue();
        if (currentState == null
                || (currentState.courses.isEmpty() && currentState.errorMessage == null && !currentState.loading)) {
            courseCatalogViewModel.loadCourses(null);
        } else {
            updateFilterButtons(currentState.selectedCategory);
        }
    }

    private void setupRecyclerView(@NonNull View view) {
        RecyclerView courseRecyclerView = view.findViewById(R.id.courseRecyclerView);
        courseCatalogAdapter = new CourseCatalogAdapter(this::openCourseDetail);
        courseRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        courseRecyclerView.setAdapter(courseCatalogAdapter);
    }

    private void setupFilterButtons(@NonNull View view) {
        allFilterButton = view.findViewById(R.id.allFilterButton);
        beginnerFilterButton = view.findViewById(R.id.beginnerFilterButton);
        intermediateFilterButton = view.findViewById(R.id.intermediateFilterButton);

        // The backend currently maps the category query to the seeded level bucket.
        allFilterButton.setOnClickListener(v -> courseCatalogViewModel.loadCourses(null));
        beginnerFilterButton.setOnClickListener(v -> courseCatalogViewModel.loadCourses("BEGINNER"));
        intermediateFilterButton.setOnClickListener(v -> courseCatalogViewModel.loadCourses("INTERMEDIATE"));
    }

    private void renderCatalogState(CourseCatalogUiState state) {
        if (state == null) {
            return;
        }

        updateFilterButtons(state.selectedCategory);
        loadingIndicator.setVisibility(state.loading ? View.VISIBLE : View.GONE);

        if (state.loading) {
            statusText.setVisibility(View.VISIBLE);
            statusText.setText(R.string.catalog_loading);
            retryButton.setVisibility(View.GONE);
            courseCatalogAdapter.submitList(Collections.emptyList());
            return;
        }

        if (state.errorMessage != null && !state.errorMessage.isBlank()) {
            statusText.setVisibility(View.VISIBLE);
            statusText.setText(state.errorMessage);
            retryButton.setVisibility(View.VISIBLE);
            courseCatalogAdapter.submitList(Collections.emptyList());
            return;
        }

        retryButton.setVisibility(View.GONE);

        if (state.courses.isEmpty()) {
            statusText.setVisibility(View.VISIBLE);
            statusText.setText(R.string.catalog_empty);
        } else {
            statusText.setVisibility(View.GONE);
        }

        courseCatalogAdapter.submitList(state.courses);
    }

    private void updateFilterButtons(String category) {
        styleFilterButton(allFilterButton, category == null);
        styleFilterButton(beginnerFilterButton, "BEGINNER".equals(category));
        styleFilterButton(intermediateFilterButton, "INTERMEDIATE".equals(category));
    }

    private void styleFilterButton(TextView button, boolean selected) {
        if (button == null) {
            return;
        }

        button.setBackgroundResource(selected
                ? R.drawable.bg_catalog_filter_button_active
                : R.drawable.bg_catalog_filter_button);
        button.setTextColor(requireContext().getColor(selected
                ? android.R.color.white
                : R.color.brand_primary));
    }

    private void reloadCurrentFilter() {
        CourseCatalogUiState state = courseCatalogViewModel.getUiState().getValue();
        courseCatalogViewModel.loadCourses(state != null ? state.selectedCategory : null);
    }

    private boolean isSessionValid() {
        return FirebaseAuth.getInstance().getCurrentUser() != null;
    }

    private void bindSessionData(@NonNull View view) {
        String role = sessionStorage.getRole();
        String userId = sessionStorage.getUserId();

        // Fall back to Firebase identity when backend sync has not populated SessionStorage yet,
        // so the header never shows "Unknown" while the user is clearly logged in.
        com.google.firebase.auth.FirebaseUser firebaseUser =
                FirebaseAuth.getInstance().getCurrentUser();
        if ((role == null || role.isBlank()) && firebaseUser != null) {
            String email = firebaseUser.getEmail();
            String displayName = firebaseUser.getDisplayName();
            if (displayName != null && !displayName.isBlank()) {
                role = displayName;
            } else if (email != null && !email.isBlank()) {
                role = email;
            } else {
                role = "Student";
            }
        }
        if ((userId == null || userId.isBlank()) && firebaseUser != null) {
            userId = firebaseUser.getUid();
        }

        TextView roleText = view.findViewById(R.id.roleText);
        TextView userIdText = view.findViewById(R.id.userIdText);

        roleText.setText(getString(R.string.catalog_signed_in_as, role != null ? role : "Student"));
        userIdText.setText(getString(R.string.catalog_internal_id, userId != null ? userId : "Unavailable"));
    }

    private void openCourseDetail(CourseSummary courseSummary) {
        Bundle args = new Bundle();
        args.putString("courseId", courseSummary.id);
        Navigation.findNavController(requireView())
                .navigate(R.id.action_homeFragment_to_courseDetailFragment, args);
    }

    private void handleLogout(@NonNull View view) {
        authViewModel.signOut();
        redirectToLogin(view);
    }

    private void redirectToLogin(@NonNull View view) {
        NavOptions navOptions = new NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true)
                .build();

        Navigation.findNavController(view)
                .navigate(R.id.action_homeFragment_to_loginFragment, null, navOptions);
    }
}
