package com.baghdad.edulife.features.courses.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.baghdad.edulife.R;

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

        view.findViewById(R.id.enrollBackButton).setOnClickListener(v ->
                Navigation.findNavController(view).popBackStack());

        view.findViewById(R.id.enrollButton).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Enrollment coming in next sprint!", Toast.LENGTH_SHORT).show());
    }

    private String normalizeLabel(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String s = raw.replace('_', ' ').toLowerCase(Locale.ROOT);
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }
}
