package com.baghdad.edulife.features.courses.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.baghdad.edulife.R;
import com.baghdad.edulife.features.courses.model.CourseSummary;

import java.util.ArrayList;
import java.util.List;

public class CoursesFragment extends Fragment {

    private CourseAdapter adapter;
    private List<CourseSummary> allCourses;
    private String activeFilter = "ALL";

    private TextView filterAll, filterBeginner, filterIntermediate, filterAdvanced;
    private TextView courseCountText;

    public CoursesFragment() {
        super(R.layout.fragment_courses);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        allCourses = buildMockCourses();

        adapter = new CourseAdapter(false);
        adapter.setOnCourseClickListener(course -> {
            Bundle args = new Bundle();
            args.putString("courseId", course.id);
            args.putString("courseTitle", course.title);
            args.putString("courseLevel", course.level);
            args.putString("courseLanguage", course.languageCode);
            args.putString("courseDesc", course.shortDescription);
            Navigation.findNavController(view)
                    .navigate(R.id.action_coursesFragment_to_courseDetailFragment, args);
        });

        RecyclerView recycler = view.findViewById(R.id.coursesRecycler);
        recycler.setAdapter(adapter);

        courseCountText = view.findViewById(R.id.courseCountText);
        filterAll = view.findViewById(R.id.filterAll);
        filterBeginner = view.findViewById(R.id.filterBeginner);
        filterIntermediate = view.findViewById(R.id.filterIntermediate);
        filterAdvanced = view.findViewById(R.id.filterAdvanced);

        applyFilter("ALL");

        filterAll.setOnClickListener(v -> applyFilter("ALL"));
        filterBeginner.setOnClickListener(v -> applyFilter("BEGINNER"));
        filterIntermediate.setOnClickListener(v -> applyFilter("INTERMEDIATE"));
        filterAdvanced.setOnClickListener(v -> applyFilter("ADVANCED"));
    }

    private void applyFilter(String level) {
        activeFilter = level;
        updateFilterChipStyles();

        List<CourseSummary> filtered = new ArrayList<>();
        for (CourseSummary c : allCourses) {
            if (level.equals("ALL") || level.equalsIgnoreCase(c.level)) {
                filtered.add(c);
            }
        }
        adapter.setCourses(filtered);
        courseCountText.setText(filtered.size() + " course" + (filtered.size() == 1 ? "" : "s") + " available");
    }

    private void updateFilterChipStyles() {
        setChipActive(filterAll, "ALL".equals(activeFilter));
        setChipActive(filterBeginner, "BEGINNER".equals(activeFilter));
        setChipActive(filterIntermediate, "INTERMEDIATE".equals(activeFilter));
        setChipActive(filterAdvanced, "ADVANCED".equals(activeFilter));
    }

    private void setChipActive(TextView chip, boolean active) {
        chip.setBackgroundResource(active
                ? R.drawable.bg_category_chip_active
                : R.drawable.bg_category_chip);
        chip.setTextColor(active ? 0xFFFFFFFF : 0xFF0F8A68);
        chip.setTypeface(null, active
                ? android.graphics.Typeface.BOLD
                : android.graphics.Typeface.NORMAL);
    }

    private List<CourseSummary> buildMockCourses() {
        List<CourseSummary> list = new ArrayList<>();

        list.add(makeCourse("1", "Android Development Fundamentals",
                "Build your first Android app from scratch with Java and Material Design.", "BEGINNER", "en"));
        list.add(makeCourse("2", "UI/UX Design Principles",
                "Learn the foundations of great design — typography, color, layout, and user flows.", "BEGINNER", "en"));
        list.add(makeCourse("3", "Machine Learning with Python",
                "From linear regression to neural networks. Hands-on projects with scikit-learn and TensorFlow.", "INTERMEDIATE", "en"));
        list.add(makeCourse("4", "Backend APIs with Node.js",
                "Design and build RESTful APIs, authentication systems, and database integrations.", "INTERMEDIATE", "en"));
        list.add(makeCourse("5", "Advanced Kotlin Coroutines",
                "Master asynchronous programming patterns, Flow, and structured concurrency in Kotlin.", "ADVANCED", "en"));
        list.add(makeCourse("6", "Data Structures & Algorithms",
                "Deepen your problem-solving skills with trees, graphs, dynamic programming, and more.", "ADVANCED", "en"));
        list.add(makeCourse("7", "Web Design with CSS Grid",
                "Create responsive, modern web layouts using CSS Grid and Flexbox.", "BEGINNER", "en"));
        list.add(makeCourse("8", "React Native Mobile Apps",
                "Cross-platform mobile development with React Native, hooks, and native APIs.", "INTERMEDIATE", "en"));

        return list;
    }

    private CourseSummary makeCourse(String id, String title, String desc, String level, String lang) {
        CourseSummary c = new CourseSummary();
        c.id = id;
        c.title = title;
        c.shortDescription = desc;
        c.level = level;
        c.languageCode = lang;
        return c;
    }
}
