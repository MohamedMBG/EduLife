package com.edulife.users.entity;

import com.edulife.users.model.UserRole;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_firebase_uid", columnNames = "firebase_uid"),
                @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    // V12 relaxes NOT NULL on these columns so a self-deleted user can have PII stripped while
    // the row stays alive for audit-bound references (certificates, exam attempts).
    @Column(name = "firebase_uid", unique = true, length = 128)
    private String firebaseUid;

    @Column(name = "email", unique = true, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role = UserRole.LEARNER;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected User() {
    }

    public User(String firebaseUid, String email) {
        this.firebaseUid = firebaseUid;
        this.email = email;
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

    public String getFirebaseUid() {
        return firebaseUid;
    }

    public String getEmail() {
        return email;
    }

    public UserRole getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Admin role assignment. LEARNER is the only safe default so promotion must be explicit. */
    public void setRole(UserRole role) {
        this.role = role;
    }

    /**
     * Re-links a verified email row to the current Firebase account. This is needed when a
     * Firebase user is recreated and receives a new uid while the backend still has the same
     * trusted email row.
     */
    public void relinkFirebaseUid(String firebaseUid) {
        this.firebaseUid = firebaseUid;
    }

    /**
     * Strips identifying fields for a self-deleted account. The row stays alive so audit-bound
     * references (certificates, exam attempts) keep a stable user_id; only PII is cleared.
     */
    public void anonymize() {
        this.email = null;
        this.firebaseUid = null;
    }
}
