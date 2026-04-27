package com.edulife.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import java.util.List;

public class FirebaseAuthentication extends AbstractAuthenticationToken {

    private final String firebaseUid;
    private final String email;

    public FirebaseAuthentication(String firebaseUid, String email) {
        // Authorities stay empty here because role resolution must come from trusted backend data, not from request payloads or an assumed default.
        super(List.of());
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
