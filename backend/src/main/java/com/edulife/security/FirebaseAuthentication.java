package com.edulife.security;

import java.util.Collection;
import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class FirebaseAuthentication extends AbstractAuthenticationToken {

    private final String firebaseUid;
    private final String email;
    private final String displayName;

    public FirebaseAuthentication(String firebaseUid, String email, String displayName) {
        this(firebaseUid, email, displayName, List.of());
    }

    public FirebaseAuthentication(
            String firebaseUid,
            String email,
            String displayName,
            Collection<? extends GrantedAuthority> authorities
    ) {
        super(authorities);
        this.firebaseUid = firebaseUid;
        this.email = email;
        this.displayName = displayName;
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

    public String getDisplayName() {
        return displayName;
    }
}
