package com.edulife.gamification;

import com.edulife.gamification.model.LevelTable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LevelTableTest {

    @Test
    void levelForExactThresholdsMatchesIndex() {
        assertThat(LevelTable.levelFor(0)).isEqualTo(1);
        assertThat(LevelTable.levelFor(250)).isEqualTo(2);
        assertThat(LevelTable.levelFor(600)).isEqualTo(3);
        assertThat(LevelTable.levelFor(1100)).isEqualTo(4);
        assertThat(LevelTable.levelFor(1800)).isEqualTo(5);
        assertThat(LevelTable.levelFor(2700)).isEqualTo(6);
        assertThat(LevelTable.levelFor(3900)).isEqualTo(7);
        assertThat(LevelTable.levelFor(5500)).isEqualTo(8);
        assertThat(LevelTable.levelFor(7500)).isEqualTo(9);
        assertThat(LevelTable.levelFor(10000)).isEqualTo(10);
    }

    @Test
    void levelForBelowFirstThresholdIsLevelOne() {
        assertThat(LevelTable.levelFor(-50)).isEqualTo(1);
        assertThat(LevelTable.levelFor(0)).isEqualTo(1);
        assertThat(LevelTable.levelFor(249)).isEqualTo(1);
    }

    @Test
    void levelForBetweenThresholdsResolvesToLowerLevel() {
        assertThat(LevelTable.levelFor(251)).isEqualTo(2);
        assertThat(LevelTable.levelFor(599)).isEqualTo(2);
        assertThat(LevelTable.levelFor(601)).isEqualTo(3);
    }

    @Test
    void levelForAboveMaxClampsToTen() {
        assertThat(LevelTable.levelFor(20000)).isEqualTo(10);
        assertThat(LevelTable.levelFor(Integer.MAX_VALUE)).isEqualTo(10);
    }

    @Test
    void namesAndThresholdsAreImmutable() {
        assertThat(LevelTable.thresholds()).hasSize(10);
        assertThat(LevelTable.names())
                .containsExactly("Novice", "Curious", "Explorer", "Seeker", "Thinker",
                        "Achiever", "Scholar", "Expert", "Sage", "Master");
    }

    @Test
    void nameForLevelClampsToValidRange() {
        assertThat(LevelTable.nameForLevel(0)).isEqualTo("Novice");
        assertThat(LevelTable.nameForLevel(11)).isEqualTo("Master");
        assertThat(LevelTable.nameForLevel(7)).isEqualTo("Scholar");
    }
}
