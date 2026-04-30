package com.edulife.security;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
class TestSecurityController {

    @GetMapping("/api/v1/public/ping")
    ResponseEntity<Map<String, String>> publicPing() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping("/api/v1/secure/profile")
    ResponseEntity<Map<String, String>> protectedProfile(Authentication authentication) {
        FirebaseAuthentication firebaseAuthentication = (FirebaseAuthentication) authentication;

        // The protected endpoint reads identity only from the authenticated security context populated by the Firebase filter.
        return ResponseEntity.ok(Map.of(
                "firebaseUid", firebaseAuthentication.getFirebaseUid(),
                "email", firebaseAuthentication.getEmail()
        ));
    }

    @GetMapping("/api/v1/secure/bad-request")
    ResponseEntity<Map<String, String>> badRequest() {
        // Test-only endpoint proving controller exceptions use the global API error contract.
        throw new IllegalArgumentException("Invalid test request");
    }

    @GetMapping("/api/v1/secure/server-error")
    ResponseEntity<Map<String, String>> serverError() {
        // Test-only endpoint proving unexpected exceptions are hidden behind a safe public message.
        throw new IllegalStateException("Sensitive internal detail");
    }
}
