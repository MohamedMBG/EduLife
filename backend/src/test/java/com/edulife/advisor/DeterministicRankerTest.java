package com.edulife.advisor;

import com.edulife.advisor.dto.CourseContextDto;
import com.edulife.advisor.service.DeterministicRanker;
import com.edulife.advisor.service.DeterministicRanker.ScoredCourse;
import com.edulife.advisor.service.IntentExtractor;
import com.edulife.advisor.service.IntentExtractor.IntentResult;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeterministicRankerTest {

    private static final UUID ANDROID_COURSE = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MATH_COURSE = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID WEB_COURSE = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private IntentExtractor intentExtractor;
    private DeterministicRanker ranker;

    @BeforeEach
    void setUp() {
        intentExtractor = new IntentExtractor();
        ranker = new DeterministicRanker();
    }

    @Test
    void androidIntentRanksAndroidCourseFirst() {
        List<CourseContextDto> catalog = testCatalog();
        IntentResult intent = intentExtractor.extract("I want to make Android apps");

        List<ScoredCourse> ranked = ranker.rank(catalog, intent);

        assertThat(ranked).isNotEmpty();
        assertThat(ranked.get(0).course().id()).isEqualTo(ANDROID_COURSE);
    }

    @Test
    void mobileAppsIntentRanksAndroidCourseFirst() {
        List<CourseContextDto> catalog = testCatalog();
        IntentResult intent = intentExtractor.extract("I wanna build mobile apps");

        List<ScoredCourse> ranked = ranker.rank(catalog, intent);

        assertThat(ranked).isNotEmpty();
        assertThat(ranked.get(0).course().id()).isEqualTo(ANDROID_COURSE);
    }

    @Test
    void frenchAndroidIntentRanksAndroidCourseFirst() {
        List<CourseContextDto> catalog = testCatalog();
        IntentResult intent = intentExtractor.extract("je veux créer des applications android");

        List<ScoredCourse> ranked = ranker.rank(catalog, intent);

        assertThat(ranked).isNotEmpty();
        assertThat(ranked.get(0).course().id()).isEqualTo(ANDROID_COURSE);
    }

    @Test
    void darijaAndroidIntentRanksAndroidCourseFirst() {
        List<CourseContextDto> catalog = testCatalog();
        IntentResult intent = intentExtractor.extract("bghit ndir app android");

        List<ScoredCourse> ranked = ranker.rank(catalog, intent);

        assertThat(ranked).isNotEmpty();
        assertThat(ranked.get(0).course().id()).isEqualTo(ANDROID_COURSE);
    }

    @Test
    void bacMathIntentRanksMathCourseFirst() {
        List<CourseContextDto> catalog = testCatalog();
        IntentResult intent = intentExtractor.extract("I need help with Bac math algebra");

        List<ScoredCourse> ranked = ranker.rank(catalog, intent);

        assertThat(ranked).isNotEmpty();
        assertThat(ranked.get(0).course().id()).isEqualTo(MATH_COURSE);
    }

    @Test
    void webDeveloperIntentRanksWebCourseFirst() {
        List<CourseContextDto> catalog = testCatalog();
        IntentResult intent = intentExtractor.extract("I want to become a web developer");

        List<ScoredCourse> ranked = ranker.rank(catalog, intent);

        assertThat(ranked).isNotEmpty();
        assertThat(ranked.get(0).course().id()).isEqualTo(WEB_COURSE);
    }

    @Test
    void androidCourseScoresMuchHigherThanMathForAndroidIntent() {
        List<CourseContextDto> catalog = testCatalog();
        IntentResult intent = intentExtractor.extract("I want to make Android apps");

        List<ScoredCourse> ranked = ranker.rank(catalog, intent);

        ScoredCourse androidResult = ranked.stream()
                .filter(s -> s.course().id().equals(ANDROID_COURSE))
                .findFirst().orElseThrow();
        ScoredCourse mathResult = ranked.stream()
                .filter(s -> s.course().id().equals(MATH_COURSE))
                .findFirst().orElse(null);

        if (mathResult != null) {
            assertThat(androidResult.score()).isGreaterThan(mathResult.score() * 2);
        }
    }

    @Test
    void shortlistReturnsMaxFiveCourses() {
        List<CourseContextDto> catalog = testCatalog();
        IntentResult intent = intentExtractor.extract("learn something");

        List<ScoredCourse> shortlist = ranker.shortlist(catalog, intent);

        assertThat(shortlist).hasSizeLessThanOrEqualTo(5);
    }

    @Test
    void shortlistFallbackWhenNoMatchesStillReturnsCourses() {
        List<CourseContextDto> catalog = testCatalog();
        IntentResult intent = intentExtractor.extract("xyz qqq zzz");

        List<ScoredCourse> shortlist = ranker.shortlist(catalog, intent);

        assertThat(shortlist).isNotEmpty();
    }

    private List<CourseContextDto> testCatalog() {
        return List.of(
                new CourseContextDto(
                        ANDROID_COURSE,
                        "Android App Development",
                        "Learn to build Android mobile applications using Java and Kotlin",
                        "Complete course on building Android applications from scratch. "
                                + "Covers layouts, activities, fragments, networking, and publishing to Play Store.",
                        "BEGINNER",
                        "en",
                        List.of("android", "mobile", "java", "kotlin"),
                        List.of("Setting Up Android Studio", "Your First Android App",
                                "Layouts and Views", "Activities and Intents",
                                "RecyclerView and Adapters", "Networking with Retrofit")
                ),
                new CourseContextDto(
                        MATH_COURSE,
                        "Science Math Bac",
                        "Prepare for the Baccalaureate math exam with algebra and analysis",
                        "Full preparation for the Science Math Baccalaureate exam. "
                                + "Covers algebra, analysis, geometry, and probability.",
                        "INTERMEDIATE",
                        "fr",
                        List.of("math", "bac", "algebra", "science"),
                        List.of("Algèbre linéaire", "Suites numériques",
                                "Fonctions et limites", "Probabilités")
                ),
                new CourseContextDto(
                        WEB_COURSE,
                        "Web Development with React",
                        "Build modern web applications using React and JavaScript",
                        "Learn to create responsive web apps using React, HTML, CSS, and JavaScript. "
                                + "Covers components, state management, and API integration.",
                        "BEGINNER",
                        "en",
                        List.of("web", "react", "javascript", "frontend"),
                        List.of("HTML & CSS Basics", "JavaScript Fundamentals",
                                "Introduction to React", "State and Props",
                                "API Integration", "Deployment")
                )
        );
    }
}
