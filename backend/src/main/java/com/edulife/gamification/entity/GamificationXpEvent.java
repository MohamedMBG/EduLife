package com.edulife.gamification.entity;

import com.edulife.gamification.model.XpEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "gamification_xp_events",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_gamification_xp_events_dedup",
                columnNames = "dedup_key"
        )
)
public class GamificationXpEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40, updatable = false)
    private XpEventType eventType;

    @Column(name = "xp", nullable = false, updatable = false)
    private int xp;

    @Column(name = "source_ref", length = 255, updatable = false)
    private String sourceRef;

    @Column(name = "dedup_key", nullable = false, length = 255, updatable = false)
    private String dedupKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected GamificationXpEvent() {
    }

    public GamificationXpEvent(UUID userId, XpEventType eventType, int xp, String sourceRef, String dedupKey) {
        this.userId = userId;
        this.eventType = eventType;
        this.xp = xp;
        this.sourceRef = sourceRef;
        this.dedupKey = dedupKey;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public XpEventType getEventType() {
        return eventType;
    }

    public int getXp() {
        return xp;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public String getDedupKey() {
        return dedupKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
