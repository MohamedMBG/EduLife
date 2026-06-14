package com.baghdad.edulife.features.courses.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.baghdad.edulife.features.courses.data.CourseRepository;
import com.baghdad.edulife.features.courses.model.CareerAdvisorUiState;
import com.baghdad.edulife.features.courses.model.CareerCourseRecommendation;
import com.baghdad.edulife.features.courses.model.CourseSummary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CareerAdvisorViewModel extends AndroidViewModel {

    private static final int MAX_RECOMMENDATIONS = 2;

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "a", "an", "and", "are", "be", "become", "for", "from", "i", "in", "it",
            "learn", "me", "my", "of", "on", "or", "the", "to", "want", "with"
    ));

    private static final Map<String, List<String>> CAREER_SIGNALS = buildCareerSignals();

    private final CourseRepository courseRepository;
    private final MutableLiveData<CareerAdvisorUiState> uiState =
            new MutableLiveData<>(CareerAdvisorUiState.idle());

    public CareerAdvisorViewModel(@NonNull Application application) {
        super(application);
        this.courseRepository = new CourseRepository();
    }

    public LiveData<CareerAdvisorUiState> getUiState() {
        return uiState;
    }

    public void analyzeGoal(String rawGoal) {
        String goal = rawGoal == null ? "" : rawGoal.trim();
        if (goal.length() < 4) {
            uiState.setValue(CareerAdvisorUiState.error(goal, "Write a clearer career goal first."));
            return;
        }

        uiState.setValue(CareerAdvisorUiState.loading(goal));
        courseRepository.loadCoursesForAdvisor(new CourseRepository.CourseCatalogCallback() {
            @Override
            public void onSuccess(List<CourseSummary> courses) {
                List<CourseSummary> source = courses != null ? courses : Collections.emptyList();
                if (source.isEmpty()) {
                    uiState.postValue(CareerAdvisorUiState.error(goal,
                            "There are no published courses to compare right now."));
                    return;
                }

                List<CareerCourseRecommendation> ranked = rankCourses(goal, source);
                String message = buildAdvisorMessage(goal, ranked);
                uiState.postValue(CareerAdvisorUiState.success(goal, message, ranked));
            }

            @Override
            public void onError(String message) {
                uiState.postValue(CareerAdvisorUiState.error(goal, message));
            }
        });
    }

    private List<CareerCourseRecommendation> rankCourses(String goal, List<CourseSummary> courses) {
        Set<String> goalTokens = tokenize(goal);
        Set<String> expandedSignals = expandCareerSignals(goalTokens);
        List<CareerCourseRecommendation> ranked = new ArrayList<>();

        for (CourseSummary course : courses) {
            ScoredCourse scored = scoreCourse(goalTokens, expandedSignals, course);
            if (scored.score > 0) {
                ranked.add(new CareerCourseRecommendation(course, scored.score, scored.reason));
            }
        }

        // If no course shares a direct signal, still show useful next steps instead of leaving
        // the learner at a dead end while the catalog is still small.
        if (ranked.isEmpty()) {
            for (CourseSummary course : courses) {
                int baseScore = "BEGINNER".equalsIgnoreCase(course.level) ? 8 : 4;
                ranked.add(new CareerCourseRecommendation(
                        course,
                        baseScore,
                        "Closest starting point from the current catalog while a more exact career course is added."
                ));
            }
        }

        ranked.sort(Comparator.comparingInt((CareerCourseRecommendation r) -> r.score).reversed());
        return strongestOneOrTwo(ranked);
    }

    private ScoredCourse scoreCourse(
            Set<String> goalTokens,
            Set<String> expandedSignals,
            CourseSummary course
    ) {
        String courseText = normalize(joinCourseText(course));
        Set<String> courseTokens = tokenize(courseText);
        Set<String> matched = new LinkedHashSet<>();
        int score = 0;

        for (String token : goalTokens) {
            if (courseTokens.contains(token)) {
                score += 12;
                matched.add(token);
            }
        }

        for (String signal : expandedSignals) {
            if (courseText.contains(signal)) {
                score += 8;
                matched.add(signal);
            }
        }

        if ("BEGINNER".equalsIgnoreCase(course.level)
                && containsAny(goalTokens, "start", "beginner", "new", "first")) {
            // Beginner fit is a real product rule: learners asking where to start should not
            // be pushed into advanced material just because one keyword overlaps.
            score += 6;
            matched.add("beginner level");
        }

        if ("en".equalsIgnoreCase(course.languageCode)
                && containsAny(goalTokens, "english", "international", "global")) {
            score += 6;
            matched.add("English");
        }

        if ("fr".equalsIgnoreCase(course.languageCode)
                && containsAny(goalTokens, "french", "francais", "morocco", "maroc")) {
            score += 6;
            matched.add("French");
        }

        return new ScoredCourse(score, buildReason(matched, course));
    }

    private String buildReason(Set<String> matched, CourseSummary course) {
        if (matched.isEmpty()) {
            return "Recommended as the closest structured path available now.";
        }

        List<String> signals = new ArrayList<>(matched);
        int limit = Math.min(signals.size(), 3);
        String joined = String.join(", ", signals.subList(0, limit));
        String level = course.level != null && !course.level.isBlank()
                ? course.level.toLowerCase(Locale.ROOT)
                : "current";
        return "I picked this because your goal connects with " + joined + ". This course is a "
                + level + " path, so the next step is realistic: open the outline, check the lessons, then enroll if the level feels right.";
    }

    private String buildAdvisorMessage(String goal, List<CareerCourseRecommendation> ranked) {
        if (ranked == null || ranked.isEmpty()) {
            return "I checked the current catalog, but I could not find a useful course match for: \""
                    + goal + "\". Try writing the career, school subject, or skill more directly.";
        }

        CareerCourseRecommendation best = ranked.get(0);
        StringBuilder message = new StringBuilder();
        message.append("I checked the current EduLife courses against your goal: \"")
                .append(goal)
                .append("\".\n\n");
        message.append("My best recommendation is ")
                .append(best.course.title)
                .append(". ")
                .append(best.reason);

        if (ranked.size() > 1) {
            CareerCourseRecommendation second = ranked.get(1);
            message.append("\n\nA second option is ")
                    .append(second.course.title)
                    .append(" if you want a supporting skill after the first course.");
        } else {
            message.append("\n\nI am showing one course because it is the clearest fit from the current catalog.");
        }

        return message.toString();
    }

    private List<CareerCourseRecommendation> strongestOneOrTwo(List<CareerCourseRecommendation> ranked) {
        if (ranked.isEmpty()) {
            return ranked;
        }

        List<CareerCourseRecommendation> result = new ArrayList<>();
        result.add(ranked.get(0));

        // Keep advice focused: show a second course only when it is close enough to be useful.
        // Otherwise the learner gets one clear next step instead of a noisy recommendation list.
        if (ranked.size() > 1 && ranked.get(1).score >= Math.max(8, ranked.get(0).score / 2)) {
            result.add(ranked.get(1));
        }

        if (result.size() > MAX_RECOMMENDATIONS) {
            return new ArrayList<>(result.subList(0, MAX_RECOMMENDATIONS));
        }
        return result;
    }

    private Set<String> tokenize(String text) {
        Set<String> tokens = new LinkedHashSet<>();
        String[] parts = normalize(text).split("[^a-z0-9]+");
        for (String part : parts) {
            if (part.length() < 3 || STOP_WORDS.contains(part)) {
                continue;
            }
            tokens.add(part);
        }
        return tokens;
    }

    private Set<String> expandCareerSignals(Set<String> goalTokens) {
        Set<String> signals = new LinkedHashSet<>();
        for (String token : goalTokens) {
            List<String> related = CAREER_SIGNALS.get(token);
            if (related != null) {
                signals.addAll(related);
            }
        }
        return signals;
    }

    private boolean containsAny(Set<String> tokens, String... values) {
        for (String value : values) {
            if (tokens.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private String joinCourseText(CourseSummary course) {
        return safe(course.title) + " "
                + safe(course.shortDescription) + " "
                + safe(course.level) + " "
                + safe(course.languageCode);
    }

    private String normalize(String value) {
        return safe(value)
                .toLowerCase(Locale.ROOT)
                .replace('\u00e9', 'e')
                .replace('\u00e8', 'e')
                .replace('\u00ea', 'e')
                .replace('\u00e0', 'a')
                .replace('\u00e7', 'c');
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private static Map<String, List<String>> buildCareerSignals() {
        Map<String, List<String>> signals = new HashMap<>();
        List<String> digital = Arrays.asList("digital", "productivity", "skills", "study");
        signals.put("developer", digital);
        signals.put("programmer", digital);
        signals.put("software", digital);
        signals.put("app", digital);
        signals.put("web", digital);
        signals.put("computer", digital);
        signals.put("technology", digital);
        signals.put("portfolio", digital);

        List<String> english = Arrays.asList("english", "communication", "reading", "listening");
        signals.put("english", english);
        signals.put("communication", english);
        signals.put("international", english);
        signals.put("tourism", english);

        List<String> french = Arrays.asList("french", "expression", "writing", "revision");
        signals.put("french", french);
        signals.put("francais", french);
        signals.put("writing", french);

        List<String> math = Arrays.asList("math", "algebra", "sciences", "bac");
        signals.put("engineer", math);
        signals.put("engineering", math);
        signals.put("data", math);
        signals.put("math", math);

        List<String> physics = Arrays.asList("physics", "motion", "forces", "mechanics");
        signals.put("mechanical", physics);
        signals.put("physics", physics);
        signals.put("robotics", physics);

        List<String> design = Arrays.asList("ui", "design", "interface", "clarity");
        signals.put("designer", design);
        signals.put("design", design);
        signals.put("interface", design);

        List<String> business = Arrays.asList("productivity", "communication", "planning", "career");
        signals.put("business", business);
        signals.put("entrepreneur", business);
        return signals;
    }

    private static class ScoredCourse {
        final int score;
        final String reason;

        ScoredCourse(int score, String reason) {
            this.score = score;
            this.reason = reason;
        }
    }
}
