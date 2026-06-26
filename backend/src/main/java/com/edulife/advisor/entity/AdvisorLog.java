package com.edulife.advisor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Audit entity that records each advisor recommendation request, its response, provider, and latency. */
@Entity
@Table(name = "advisor_log")
public class AdvisorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "goal", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String goal;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_json", nullable = false, updatable = false, columnDefinition = "jsonb")
    private String responseJson;

    @Column(name = "provider", nullable = false, updatable = false, length = 32)
    private String provider;

    @Column(name = "model", nullable = false, updatable = false, length = 64)
    private String model;

    @Column(name = "latency_ms", updatable = false)
    private Integer latencyMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AdvisorLog() {}

    public AdvisorLog(UUID userId, String goal, String responseJson, String provider, String model, Integer latencyMs) {
        this.userId = userId;
        this.goal = goal;
        this.responseJson = responseJson;
        this.provider = provider;
        this.model = model;
        this.latencyMs = latencyMs;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getGoal() { return goal; }
    public String getResponseJson() { return responseJson; }
    public String getProvider() { return provider; }
    public String getModel() { return model; }
    public Integer getLatencyMs() { return latencyMs; }
    public Instant getCreatedAt() { return createdAt; }
}
