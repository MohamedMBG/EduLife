package com.edulife.advisor.service;

import com.edulife.advisor.dto.CourseContextDto;
import com.edulife.advisor.service.IntentExtractor.IntentResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class DeterministicRanker {

    private static final int TITLE_MATCH_WEIGHT = 15;
    private static final int TAG_MATCH_WEIGHT = 12;
    private static final int DESCRIPTION_MATCH_WEIGHT = 6;
    private static final int LESSON_TITLE_MATCH_WEIGHT = 6;
    private static final int LANGUAGE_MATCH_WEIGHT = 4;
    private static final int LEVEL_MATCH_WEIGHT = 3;

    private static final int MAX_SHORTLIST = 5;

    public List<ScoredCourse> rank(List<CourseContextDto> catalog, IntentResult intent) {
        List<ScoredCourse> scored = new ArrayList<>();

        for (CourseContextDto course : catalog) {
            ScoredCourse result = scoreCourse(course, intent);
            if (result.score > 0) {
                scored.add(result);
            }
        }

        scored.sort(Comparator.comparingInt(ScoredCourse::score).reversed());
        return scored;
    }

    public List<ScoredCourse> shortlist(List<CourseContextDto> catalog, IntentResult intent) {
        List<ScoredCourse> ranked = rank(catalog, intent);
        if (ranked.isEmpty()) {
            return catalog.stream()
                    .map(c -> new ScoredCourse(c, 1, Set.of()))
                    .limit(MAX_SHORTLIST)
                    .toList();
        }
        return ranked.stream().limit(MAX_SHORTLIST).toList();
    }

    private ScoredCourse scoreCourse(CourseContextDto course, IntentResult intent) {
        Set<String> expandedKeywords = intent.expandedKeywords();
        Set<String> directKeywords = intent.keywords();
        Set<String> matched = new LinkedHashSet<>();
        int score = 0;

        String titleNorm = IntentExtractor.normalize(safeStr(course.title()));
        for (String keyword : expandedKeywords) {
            if (titleNorm.contains(keyword)) {
                score += TITLE_MATCH_WEIGHT;
                matched.add(keyword);
            }
        }

        if (course.tags() != null) {
            for (String tag : course.tags()) {
                String tagNorm = IntentExtractor.normalize(tag);
                for (String keyword : expandedKeywords) {
                    if (tagNorm.contains(keyword)) {
                        score += TAG_MATCH_WEIGHT;
                        matched.add(keyword);
                    }
                }
            }
        }

        String descNorm = IntentExtractor.normalize(
                safeStr(course.shortDescription()) + " " + safeStr(course.description()));
        for (String keyword : expandedKeywords) {
            if (descNorm.contains(keyword)) {
                score += DESCRIPTION_MATCH_WEIGHT;
                matched.add(keyword);
            }
        }

        if (course.lessonTitles() != null) {
            String lessonText = IntentExtractor.normalize(String.join(" ", course.lessonTitles()));
            for (String keyword : expandedKeywords) {
                if (lessonText.contains(keyword)) {
                    score += LESSON_TITLE_MATCH_WEIGHT;
                    matched.add(keyword);
                }
            }
        }

        if (course.languageCode() != null) {
            String lang = course.languageCode().toLowerCase();
            String detectedLang = intent.detectedLanguage();
            if (lang.equals(detectedLang)
                    || (lang.equals("en") && detectedLang.equals("en"))
                    || (lang.equals("fr") && detectedLang.equals("fr"))) {
                score += LANGUAGE_MATCH_WEIGHT;
            }
        }

        if (course.level() != null && "BEGINNER".equalsIgnoreCase(course.level())) {
            for (String kw : directKeywords) {
                if (kw.equals("start") || kw.equals("beginner") || kw.equals("new")
                        || kw.equals("first") || kw.equals("begin") || kw.equals("intro")) {
                    score += LEVEL_MATCH_WEIGHT;
                    break;
                }
            }
        }

        return new ScoredCourse(course, score, matched);
    }

    private String safeStr(String s) {
        return s == null ? "" : s;
    }

    public record ScoredCourse(CourseContextDto course, int score, Set<String> matchedKeywords) {}
}
