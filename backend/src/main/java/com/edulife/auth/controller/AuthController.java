package com.edulife.auth.controller;

import com.edulife.auth.dto.AuthSyncRequest;
import com.edulife.auth.dto.AuthSyncResponse;
import com.edulife.auth.service.AuthSyncService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for Firebase authentication synchronization with the internal user store. */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthSyncService authSyncService;

    public AuthController(AuthSyncService authSyncService) {
        this.authSyncService = authSyncService;
    }

    /** Synchronizes the authenticated Firebase user with the internal user table. */
    @PostMapping("/sync")
    public AuthSyncResponse sync(
            @RequestBody(required = false) AuthSyncRequest request
    ) {
        // Identity comes only from the verified Firebase token in SecurityContext.
        // intendedRole in the body is only honoured on first sync (new user creation).
        return authSyncService.syncCurrentUser(request);
    }
}
