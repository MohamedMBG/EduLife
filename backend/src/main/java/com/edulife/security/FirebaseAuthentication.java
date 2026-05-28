package com.edulife.security;

import java.util.Collection;
import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class FirebaseAuthentication extends AbstractAuthenticationToken {

    private final String firebaseUid;
    private final String email;

    public FirebaseAuthentication(String firebaseUid, String email) {
        // Authorities default to empty for paths that do not need role gating; the token filter
        // promotes this to a role-aware authentication once the user is resolved from the DB.
        this(firebaseUid, email, List.of());
    }

    public FirebaseAuthentication(
            String firebaseUid,
            String email,
            Collection<? extends GrantedAuthority> authorities
    ) {
        super(authorities);
        this.firebaseUid = firebaseUid;
        this.email = email;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return firebaseUid;
    }

    public String getFirebaseUid() {
        return firebaseUid;
    }

    public String getEmail() {
        return email;
    }
}
