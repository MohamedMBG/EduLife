package com.edulife.auth.controller;

import com.edulife.auth.dto.AuthSyncResponse;
import com.edulife.auth.service.AuthSyncService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthSyncService authSyncService;

    public AuthController(AuthSyncService authSyncService) {
        this.authSyncService = authSyncService;
    }

    @PostMapping("/sync")
    public AuthSyncResponse sync() {
        // The user identity comes only from the verified Firebase token in SecurityContext, never from request body fields.
        return authSyncService.syncCurrentUser();
    }
}
