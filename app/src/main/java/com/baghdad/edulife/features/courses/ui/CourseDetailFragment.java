package com.baghdad.edulife.features.courses.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.baghdad.edulife.R;

public class CourseDetailFragment extends Fragment {

    public CourseDetailFragment() {
        super(R.layout.fragment_course_detail);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        String title = args != null ? args.getString("courseTitle", "Course") : "Course";
        String level = args != null ? args.getString("courseLevel", "BEGINNER") : "BEGINNER";
        String language = args != null ? args.getString("courseLanguage", "en") : "en";
        String desc = args != null ? args.getString("courseDesc", "") : "";

        bindHeader(view, title, level, language, desc);
        buildSections(view, level);

        view.findViewById(R.id.backButton).setOnClickListener(v ->
                Navigation.findNavController(view).popBackStack());

        view.findViewById(R.id.enrollButton).setOnClickListener(v ->
                Toast.makeText(requireContext(),
                        "Enrollment coming in the next sprint!", Toast.LENGTH_SHORT).show());
    }

    private void bindHeader(View view, String title, String level, String language, String desc) {
        TextView titleView = view.findViewById(R.id.detailTitle);
        TextView levelView = view.findViewById(R.id.detailLevelBadge);
        TextView langView = view.findViewById(R.id.detailLanguage);
        TextView descView = view.findViewById(R.id.detailDescription);
        TextView sectionCount = view.findViewById(R.id.sectionCount);

        titleView.setText(title);
        levelView.setText(level.toUpperCase());
        langView.setText("Language: " + (language.equalsIgnoreCase("en") ? "English" : language));
        descView.setText(desc.isEmpty()
                ? "This course covers all the essential concepts and hands-on projects to take your skills to the next level."
                : desc + "\n\nThis course is designed for learners who want to build real-world skills through structured lessons and practical exercises.");
        sectionCount.setText(getSectionCount(level) + " sections");
    }

    private void buildSections(View view, String level) {
        LinearLayout container = view.findViewById(R.id.sectionsContainer);
        String[] sections = getMockSections(level);

        for (int i = 0; i < sections.length; i++) {
            LinearLayout wrapper = new LinearLayout(requireContext());
            wrapper.setOrientation(LinearLayout.HORIZONTAL);
            wrapper.setGravity(android.view.Gravity.CENTER_VERTICAL);
            wrapper.setPadding(dp(16), dp(14), dp(16), dp(14));
            wrapper.setBackground(requireContext().getDrawable(R.drawable.bg_section_item));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, dp(10));
            wrapper.setLayoutParams(lp);

            TextView numberView = new TextView(requireContext());
            numberView.setText(String.format("%02d", i + 1));
            numberView.setTextColor(0xFF0F8A68);
            numberView.setTextSize(13f);
            numberView.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams numLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            numLp.setMargins(0, 0, dp(14), 0);
            numberView.setLayoutParams(numLp);

            TextView sectionTitle = new TextView(requireContext());
            sectionTitle.setText(sections[i]);
            sectionTitle.setTextColor(0xFF0E1A2A);
            sectionTitle.setTextSize(14f);
            sectionTitle.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            wrapper.addView(numberView);
            wrapper.addView(sectionTitle);
            container.addView(wrapper);
        }
    }

    private int getSectionCount(String level) {
        if ("ADVANCED".equalsIgnoreCase(level)) return 6;
        if ("INTERMEDIATE".equalsIgnoreCase(level)) return 5;
        return 4;
    }

    private String[] getMockSections(String level) {
        if ("ADVANCED".equalsIgnoreCase(level)) {
            return new String[]{
                    "Course Introduction & Prerequisites",
                    "Core Concepts Deep Dive",
                    "Advanced Patterns & Techniques",
                    "Performance Optimization",
                    "Real-World Project Walkthrough",
                    "Final Assessment & Certification"
            };
        } else if ("INTERMEDIATE".equalsIgnoreCase(level)) {
            return new String[]{
                    "Getting Started",
                    "Foundational Concepts",
                    "Building Core Features",
                    "Integration & Testing",
                    "Final Project & Review"
            };
        } else {
            return new String[]{
                    "Introduction & Setup",
                    "Core Fundamentals",
                    "Hands-On Project",
                    "Quiz & Assessment"
            };
        }
    }

    private int dp(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
