package com.edulife.gamification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "user_gamification_state")
public class UserGamificationState {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "total_xp", nullable = false)
    private int totalXp;

    @Column(name = "level", nullable = false)
    private int level;

    @Column(name = "current_streak", nullable = false)
    private int currentStreak;

    @Column(name = "longest_streak", nullable = false)
    private int longestStreak;

    @Column(name = "last_activity_date")
    private LocalDate lastActivityDate;

    @Column(name = "streak_bonus_3_awarded", nullable = false)
    private boolean streakBonus3Awarded;

    @Column(name = "streak_bonus_7_awarded", nullable = false)
    private boolean streakBonus7Awarded;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Optimistic lock guards concurrent XP awards on the same user; the second
    // writer is forced to retry against fresh totals instead of clobbering them.
    @Version
    @Column(name = "version")
    private Long version;

    protected UserGamificationState() {
    }

    public UserGamificationState(UUID userId) {
        this.userId = userId;
        this.totalXp = 0;
        this.level = 1;
        this.currentStreak = 0;
        this.longestStreak = 0;
        this.streakBonus3Awarded = false;
        this.streakBonus7Awarded = false;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }

    public UUID getUserId() {
        return userId;
    }

    public int getTotalXp() {
        return totalXp;
    }

    public int getLevel() {
        return level;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public int getLongestStreak() {
        return longestStreak;
    }

    public LocalDate getLastActivityDate() {
        return lastActivityDate;
    }

    public boolean isStreakBonus3Awarded() {
        return streakBonus3Awarded;
    }

    public boolean isStreakBonus7Awarded() {
        return streakBonus7Awarded;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void addXp(int delta) {
        if (delta < 0) {
            throw new IllegalArgumentException("XP delta must be non-negative");
        }
        this.totalXp += delta;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
        if (currentStreak > this.longestStreak) {
            this.longestStreak = currentStreak;
        }
    }

    public void setLastActivityDate(LocalDate date) {
        this.lastActivityDate = date;
    }

    public void setStreakBonus3Awarded(boolean awarded) {
        this.streakBonus3Awarded = awarded;
    }

    public void setStreakBonus7Awarded(boolean awarded) {
        this.streakBonus7Awarded = awarded;
    }
}
