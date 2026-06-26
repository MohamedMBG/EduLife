package com.edulife.advisor.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Extracts search intent from a learner's goal string by tokenizing, removing stop words,
 * expanding keywords via synonym groups, and detecting the input language.
 */
@Component
public class IntentExtractor {

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "are", "be", "become", "but", "can", "could",
            "do", "does", "for", "from", "get", "have", "how", "i", "in",
            "into", "is", "it", "its", "just", "learn", "like", "me", "more",
            "my", "need", "of", "on", "or", "please", "should", "so", "some",
            "the", "to", "very", "want", "was", "what", "when", "where",
            "which", "will", "with", "would"
    );

    private static final Map<String, List<String>> SYNONYM_GROUPS = Map.ofEntries(
            // Android / Mobile
            Map.entry("android", List.of("android", "mobile", "mobile app", "application mobile",
                    "développement mobile", "kotlin", "java android", "android studio", "apk")),
            Map.entry("mobile", List.of("android", "mobile", "mobile app", "application mobile",
                    "développement mobile", "kotlin", "android studio")),
            Map.entry("kotlin", List.of("android", "mobile", "kotlin", "android studio")),

            // Web
            Map.entry("web", List.of("web", "website", "frontend", "backend", "react",
                    "html", "css", "javascript", "spring boot", "fullstack", "full stack")),
            Map.entry("website", List.of("web", "website", "frontend", "html", "css", "javascript")),
            Map.entry("frontend", List.of("web", "frontend", "react", "html", "css", "javascript", "ui")),
            Map.entry("backend", List.of("web", "backend", "spring boot", "api", "server")),
            Map.entry("react", List.of("web", "frontend", "react", "javascript")),
            Map.entry("javascript", List.of("web", "frontend", "javascript", "react")),
            Map.entry("html", List.of("web", "frontend", "html", "css")),
            Map.entry("css", List.of("web", "frontend", "html", "css")),

            // AI / Data
            Map.entry("ai", List.of("ai", "artificial intelligence", "machine learning", "data",
                    "python", "deep learning", "neural")),
            Map.entry("artificial", List.of("ai", "artificial intelligence", "machine learning")),
            Map.entry("intelligence", List.of("ai", "artificial intelligence", "machine learning")),
            Map.entry("machine", List.of("ai", "machine learning", "data", "python")),
            Map.entry("learning", List.of("machine learning", "deep learning")),
            Map.entry("data", List.of("data", "data analysis", "python", "ai", "statistics")),
            Map.entry("python", List.of("python", "data", "ai", "machine learning", "programming")),

            // Bac / Math
            Map.entry("bac", List.of("bac", "baccalauréat", "math", "algebra", "physics",
                    "science math", "lycée", "examen")),
            Map.entry("math", List.of("math", "algebra", "calculus", "geometry", "statistics", "bac")),
            Map.entry("algebra", List.of("math", "algebra", "bac")),
            Map.entry("physics", List.of("physics", "mechanics", "forces", "motion", "bac")),
            Map.entry("science", List.of("science", "math", "physics", "bac")),

            // Engineering
            Map.entry("engineer", List.of("engineering", "math", "physics", "science")),
            Map.entry("engineering", List.of("engineering", "math", "physics", "science")),
            Map.entry("robotics", List.of("robotics", "physics", "mechanics", "digital", "programming")),

            // Languages
            Map.entry("english", List.of("english", "communication", "reading", "listening", "language")),
            Map.entry("french", List.of("french", "français", "expression", "writing", "language")),
            Map.entry("francais", List.of("french", "français", "expression", "writing")),

            // Design
            Map.entry("design", List.of("design", "ui", "ux", "interface", "graphic")),
            Map.entry("designer", List.of("design", "ui", "ux", "interface", "graphic")),

            // Business
            Map.entry("business", List.of("business", "productivity", "communication", "planning",
                    "entrepreneurship", "management")),
            Map.entry("entrepreneur", List.of("business", "entrepreneurship", "planning", "startup")),

            // Darija / Moroccan
            Map.entry("bghit", List.of()),
            Map.entry("ndir", List.of()),
            Map.entry("app", List.of("android", "mobile", "mobile app", "application", "web")),
            Map.entry("application", List.of("android", "mobile", "application", "web", "software")),
            Map.entry("applications", List.of("android", "mobile", "application", "web", "software")),
            Map.entry("développement", List.of("development", "programming", "software")),
            Map.entry("créer", List.of("create", "build", "development")),
            Map.entry("programmer", List.of("programming", "software", "development", "code")),
            Map.entry("programming", List.of("programming", "software", "development", "code")),
            Map.entry("software", List.of("software", "programming", "development", "digital")),
            Map.entry("development", List.of("software", "programming", "development", "code")),
            Map.entry("developer", List.of("software", "programming", "development", "web", "mobile")),
            Map.entry("build", List.of("development", "create", "programming")),
            Map.entry("create", List.of("development", "create", "build"))
    );

    /** Parses the goal into direct keywords, synonym-expanded keywords, and the detected language. */
    public IntentResult extract(String goal) {
        String normalized = normalize(goal);
        Set<String> tokens = tokenize(normalized);
        Set<String> expanded = expand(tokens);
        String detectedLanguage = detectLanguage(goal);
        return new IntentResult(tokens, expanded, detectedLanguage);
    }

    private Set<String> tokenize(String text) {
        Set<String> tokens = new LinkedHashSet<>();
        for (String part : text.split("[^a-zA-Zéèêàçôûîïü0-9]+")) {
            String lower = part.toLowerCase();
            if (lower.length() >= 2 && !STOP_WORDS.contains(lower)) {
                tokens.add(lower);
            }
        }
        return tokens;
    }

    /** Expands the token set by adding synonyms from {@code SYNONYM_GROUPS}. */
    private Set<String> expand(Set<String> tokens) {
        Set<String> expanded = new LinkedHashSet<>(tokens);
        for (String token : tokens) {
            List<String> synonyms = SYNONYM_GROUPS.get(token);
            if (synonyms != null) {
                expanded.addAll(synonyms);
            }
        }
        return expanded;
    }

    /** Detects the goal language as Darija, French, or English based on marker words. */
    private String detectLanguage(String goal) {
        String lower = goal.toLowerCase();
        if (lower.contains("bghit") || lower.contains("ndir") || lower.contains("dyal")
                || lower.contains("khdam") || lower.contains("hadi")) {
            return "darija";
        }
        if (lower.contains("je veux") || lower.contains("j'aimerais") || lower.contains("créer")
                || lower.contains("apprendre") || lower.contains("développer")
                || lower.contains("application") || lower.contains("cours")) {
            return "fr";
        }
        return "en";
    }

    /** Lowercases text and strips French diacritics for consistent keyword matching. */
    static String normalize(String text) {
        return text.toLowerCase()
                .replace("é", "e").replace("è", "e").replace("ê", "e")
                .replace("à", "a").replace("ç", "c").replace("ô", "o")
                .replace("û", "u").replace("î", "i").replace("ï", "i");
    }

    /** Result of intent extraction: original keywords, expanded keywords, and detected language. */
    public record IntentResult(
            Set<String> keywords,
            Set<String> expandedKeywords,
            String detectedLanguage
    ) {}
}
