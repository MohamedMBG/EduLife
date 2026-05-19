package com.baghdad.edulife.features.courses.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.baghdad.edulife.R;
import com.baghdad.edulife.core.storage.SessionStorage;
import com.baghdad.edulife.features.courses.model.CourseSummary;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SessionStorage session = new SessionStorage(requireContext());

        if (!isSessionValid(session)) {
            redirectToLogin(view);
            return;
        }

        bindGreeting(view);
        setupFeaturedCourses(view);
        setupPopularCourses(view);

        view.findViewById(R.id.seeAllFeatured).setOnClickListener(v -> switchToCoursesTab());
        view.findViewById(R.id.seeAllPopular).setOnClickListener(v -> switchToCoursesTab());
    }

    private boolean isSessionValid(SessionStorage session) {
        return FirebaseAuth.getInstance().getCurrentUser() != null && session.hasSession();
    }

    private void bindGreeting(View view) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        TextView greetingName = view.findViewById(R.id.greetingName);

        if (user != null && user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
            greetingName.setText(user.getDisplayName().split(" ")[0]);
        } else if (user != null && user.getEmail() != null) {
            String email = user.getEmail();
            int at = email.indexOf('@');
            String name = at > 0 ? email.substring(0, at) : email;
            greetingName.setText(name.substring(0, 1).toUpperCase() + name.substring(1));
        } else {
            greetingName.setText("Learner");
        }
    }

    private void setupFeaturedCourses(View view) {
        List<CourseSummary> featured = new ArrayList<>();
        featured.add(makeCourse("1", "Android Development Fundamentals",
                "Build your first Android app from scratch.", "BEGINNER", "en"));
        featured.add(makeCourse("3", "Machine Learning with Python",
                "Hands-on ML projects with scikit-learn and TensorFlow.", "INTERMEDIATE", "en"));
        featured.add(makeCourse("5", "Advanced Kotlin Coroutines",
                "Master async programming with Flow and coroutines.", "ADVANCED", "en"));

        CourseAdapter adapter = new CourseAdapter(true);
        adapter.setCourses(featured);
        adapter.setOnCourseClickListener(course -> navigateToDetail(view, course));

        RecyclerView recycler = view.findViewById(R.id.featuredCoursesRecycler);
        recycler.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        recycler.setAdapter(adapter);
    }

    private void setupPopularCourses(View view) {
        List<CourseSummary> popular = new ArrayList<>();
        popular.add(makeCourse("2", "UI/UX Design Principles",
                "Typography, color, layout, and user flows.", "BEGINNER", "en"));
        popular.add(makeCourse("4", "Backend APIs with Node.js",
                "RESTful APIs, auth systems, and database integrations.", "INTERMEDIATE", "en"));
        popular.add(makeCourse("7", "Web Design with CSS Grid",
                "Responsive layouts using CSS Grid and Flexbox.", "BEGINNER", "en"));

        CourseAdapter adapter = new CourseAdapter(false);
        adapter.setCourses(popular);
        adapter.setOnCourseClickListener(course -> navigateToDetail(view, course));

        RecyclerView recycler = view.findViewById(R.id.popularCoursesRecycler);
        recycler.setAdapter(adapter);
    }

    private void navigateToDetail(View view, CourseSummary course) {
        Bundle args = new Bundle();
        args.putString("courseId", course.id);
        args.putString("courseTitle", course.title);
        args.putString("courseLevel", course.level);
        args.putString("courseLanguage", course.languageCode);
        args.putString("courseDesc", course.shortDescription);
        Navigation.findNavController(view)
                .navigate(R.id.action_homeFragment_to_courseDetailFragment, args);
    }

    private void switchToCoursesTab() {
        BottomNavigationView nav = requireActivity().findViewById(R.id.bottomNavView);
        if (nav != null) nav.setSelectedItemId(R.id.coursesFragment);
    }

    private void redirectToLogin(View view) {
        Navigation.findNavController(view)
                .navigate(R.id.action_homeFragment_to_loginFragment);
    }

    private CourseSummary makeCourse(String id, String title, String desc, String level, String lang) {
        CourseSummary c = new CourseSummary();
        c.id = id; c.title = title; c.shortDescription = desc;
        c.level = level; c.languageCode = lang;
        return c;
    }
}
