package com.edulife.auth.config;

import com.edulife.users.model.UserRole;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Server-trusted mapping of known staff Firebase emails to their role.
 *
 * Consulted on every /auth/sync so the seeded staff accounts (admin / teacher / group admin)
 * always resolve to the correct role regardless of Flyway migration ordering or deploy state.
 * The seed migrations (V18–V20) are UPDATE-only and become no-ops when the user row does not
 * exist yet (fresh DB, or a row created as LEARNER before promotion), which left staff stuck
 * on the learner portal. This allowlist removes that ordering dependency.
 *
 * Assignment is driven only off the verified Firebase email from the token — never off
 * client-supplied input — so it cannot be abused to self-assign a privileged role.
 */
@ConfigurationProperties(prefix = "edulife.staff")
public class StaffRoleProperties {

    private List<Entry> roles = new ArrayList<>();

    public List<Entry> getRoles() {
        return roles;
    }

    public void setRoles(List<Entry> roles) {
        this.roles = roles;
    }

    /** Returns the configured staff role for an email, or null when the email is not staff. */
    public UserRole roleFor(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String normalized = email.trim();
        for (Entry entry : roles) {
            if (entry.getEmail() != null && entry.getEmail().equalsIgnoreCase(normalized)) {
                return entry.getRole();
            }
        }
        return null;
    }

    public static class Entry {
        private String email;
        private UserRole role;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public UserRole getRole() {
            return role;
        }

        public void setRole(UserRole role) {
            this.role = role;
        }
    }
}
