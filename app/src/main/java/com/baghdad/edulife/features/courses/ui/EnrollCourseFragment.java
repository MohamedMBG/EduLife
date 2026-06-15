package com.baghdad.edulife.features.courses.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.courses.model.EnrollUiState;
import com.baghdad.edulife.features.courses.viewmodel.EnrollmentViewModel;
import com.baghdad.edulife.features.gamification.data.GamificationRepository;
import com.baghdad.edulife.features.gamification.model.GamificationUiState;

import java.util.Locale;

public class EnrollCourseFragment extends Fragment {

    public EnrollCourseFragment() {
        super(R.layout.fragment_enroll_course);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        String courseId       = args != null ? args.getString("courseId", "")       : "";
        String courseTitle    = args != null ? args.getString("courseTitle", "")    : "";
        String courseLevel    = args != null ? args.getString("courseLevel", "")    : "";
        String courseLanguage = args != null ? args.getString("courseLanguage", "") : "";
        String courseDesc     = args != null ? args.getString("courseDesc", "")     : "";
        int    sectionCount   = args != null ? args.getInt("sectionCount", 0)       : 0;
        int    lessonCount    = args != null ? args.getInt("lessonCount", 0)        : 0;

        ((TextView) view.findViewById(R.id.enrollCourseTitle)).setText(courseTitle);
        ((TextView) view.findViewById(R.id.enrollLevelBadge)).setText(normalizeLabel(courseLevel));
        ((TextView) view.findViewById(R.id.enrollLanguageBadge)).setText(normalizeLabel(courseLanguage));
        ((TextView) view.findViewById(R.id.enrollSectionCount))
                .setText(getString(R.string.course_detail_section_count, sectionCount));
        ((TextView) view.findViewById(R.id.enrollDescription)).setText(
                courseDesc.isBlank() ? getString(R.string.catalog_subtitle) : courseDesc);
        ((TextView) view.findViewById(R.id.enrollPerkSections))
                .setText(getString(R.string.course_detail_section_count, sectionCount));
        ((TextView) view.findViewById(R.id.enrollPerkLessons))
                .setText(lessonCount + " lessons");
        ((TextView) view.findViewById(R.id.enrollInstructorName))
                .setText("EduLife Team");

        Button enrollButton = view.findViewById(R.id.enrollButton);

        EnrollmentViewModel vm = new ViewModelProvider(this).get(EnrollmentViewModel.class);

        vm.getEnrollState().observe(getViewLifecycleOwner(), state -> {
            applyState(view, enrollButton, state);

            if (state.enrolled) {
                // Distinguish a fresh enrol from a 409 "already enrolled" so the learner is not
                // told they just enrolled when they were already in the course.
                Toast.makeText(requireContext(),
                        state.alreadyEnrolled
                                ? R.string.enroll_already_enrolled
                                : R.string.enroll_success,
                        Toast.LENGTH_SHORT).show();

                if (!state.alreadyEnrolled) {
                    // Backend awards the XP inside EnrollmentService; the client only
                    // refetches the authoritative state so the home / gamification cards
                    // reflect the new total on the next render.
                    new GamificationRepository().loadMyState(new GamificationRepository.StateCallback() {
                        @Override public void onSuccess(GamificationUiState ignored) {}
                        @Override public void onError(String ignored) {}
                    });
                }

                NavController nav = Navigation.findNavController(view);
                NavOptions opts = new NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .setPopUpTo(R.id.homeFragment, false, true)
                        .build();
                nav.navigate(R.id.coursesFragment, null, opts);

                // Reset so re-entering this fragment via back nav does not immediately re-fire
                // the success branch and bounce the learner away again.
                vm.clearEnrollState();
            }
        });

        view.findViewById(R.id.enrollBackButton).setOnClickListener(v ->
                Navigation.findNavController(view).popBackStack());

        enrollButton.setOnClickListener(v -> {
            if (!courseId.isBlank()) {
                vm.enroll(courseId);
            }
        });
    }

    private void applyState(View root, Button enrollButton, EnrollUiState state) {
        enrollButton.setEnabled(!state.loading);

        if (state.loading) {
            enrollButton.setText(R.string.enrolling);
            return;
        }

        enrollButton.setText(R.string.enroll_cta);

        if (state.errorMessage != null) {
            TextView errorView = root.findViewById(R.id.enrollErrorText);
            if (errorView != null) {
                errorView.setVisibility(View.VISIBLE);
                errorView.setText(state.errorMessage);
            }
        } else {
            TextView errorView = root.findViewById(R.id.enrollErrorText);
            if (errorView != null) errorView.setVisibility(View.GONE);
        }
    }

    private String normalizeLabel(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String s = raw.replace('_', ' ').toLowerCase(Locale.ROOT);
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }
}
