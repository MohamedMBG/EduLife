package com.edulife.gamification.model;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Canonical catalog of all 12 badges. Badge definitions are shared between Android and Web
 * via the backend API; clients must never duplicate these constants.
 */
public final class BadgeCatalog {

    public static final String FIRST_FLAME = "first_flame";
    public static final String BOOKWORM = "bookworm";
    public static final String SPEED_RUN = "speed_run";
    public static final String SHARP_MIND = "sharp_mind";
    public static final String GRADUATE = "graduate";
    public static final String ON_A_ROLL = "on_a_roll";
    public static final String DEDICATED = "dedicated";
    public static final String STAR_LEARNER = "star_learner";
    public static final String SCHOLAR = "scholar";
    public static final String MASTER = "master";
    public static final String TROPHY_HUNTER = "trophy_hunter";
    public static final String INFERNO = "inferno";

    private static final List<BadgeDefinition> ALL = List.of(
            new BadgeDefinition(FIRST_FLAME, "First Flame", BadgeRarity.COMMON, "Complete your first lesson."),
            new BadgeDefinition(BOOKWORM, "Bookworm", BadgeRarity.RARE, "Complete 10 lessons."),
            new BadgeDefinition(SPEED_RUN, "Speed Run", BadgeRarity.RARE, "Complete 3 lessons in a single day."),
            new BadgeDefinition(SHARP_MIND, "Sharp Mind", BadgeRarity.EPIC, "Pass your first exam."),
            new BadgeDefinition(GRADUATE, "Graduate", BadgeRarity.EPIC, "Earn your first certificate."),
            new BadgeDefinition(ON_A_ROLL, "On A Roll", BadgeRarity.COMMON, "Complete 5 lessons in 7 days."),
            new BadgeDefinition(DEDICATED, "Dedicated", BadgeRarity.EPIC, "Maintain a 14-day streak."),
            new BadgeDefinition(STAR_LEARNER, "Star Learner", BadgeRarity.LEGENDARY, "Maintain a 30-day streak."),
            new BadgeDefinition(SCHOLAR, "Scholar", BadgeRarity.EPIC, "Reach level 7."),
            new BadgeDefinition(MASTER, "Master", BadgeRarity.LEGENDARY, "Reach level 10."),
            new BadgeDefinition(TROPHY_HUNTER, "Trophy Hunter", BadgeRarity.LEGENDARY, "Earn 3 certificates."),
            new BadgeDefinition(INFERNO, "Inferno", BadgeRarity.LEGENDARY, "Maintain a 60-day streak.")
    );

    private static final Map<String, BadgeDefinition> BY_ID =
            ALL.stream().collect(Collectors.toUnmodifiableMap(BadgeDefinition::id, b -> b));

    private BadgeCatalog() {
    }

    /** Returns all badge definitions in display order. */
    public static List<BadgeDefinition> all() {
        return ALL;
    }

    /** Looks up a badge by its unique string identifier. */
    public static Optional<BadgeDefinition> byId(String id) {
        return Optional.ofNullable(BY_ID.get(id));
    }
}
