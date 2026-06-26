package com.edulife.gamification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity recording that a learner has unlocked a specific badge. Uses a composite
 * primary key of (user_id, badge_id) to prevent duplicate unlocks.
 */
@Entity
@Table(name = "user_badges")
public class UserBadge {

    @EmbeddedId
    private Key id;

    @Column(name = "unlocked_at", nullable = false, updatable = false)
    private Instant unlockedAt;

    protected UserBadge() {
    }

    public UserBadge(UUID userId, String badgeId) {
        this.id = new Key(userId, badgeId);
    }

    @PrePersist
    void onCreate() {
        if (unlockedAt == null) {
            unlockedAt = Instant.now();
        }
    }

    public UUID getUserId() {
        return id.userId;
    }

    public String getBadgeId() {
        return id.badgeId;
    }

    public Instant getUnlockedAt() {
        return unlockedAt;
    }

    /** Composite primary key pairing a user with a badge identifier. */
    @Embeddable
    public static class Key implements Serializable {

        @Column(name = "user_id", nullable = false, updatable = false)
        private UUID userId;

        @Column(name = "badge_id", nullable = false, length = 40, updatable = false)
        private String badgeId;

        protected Key() {
        }

        public Key(UUID userId, String badgeId) {
            this.userId = userId;
            this.badgeId = badgeId;
        }

        public UUID getUserId() {
            return userId;
        }

        public String getBadgeId() {
            return badgeId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key key)) return false;
            return Objects.equals(userId, key.userId) && Objects.equals(badgeId, key.badgeId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, badgeId);
        }
    }
}
