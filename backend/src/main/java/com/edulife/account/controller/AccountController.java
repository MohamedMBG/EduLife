package com.edulife.account.controller;

import com.edulife.account.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/account")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Play Store mandate: an authenticated user must be able to delete their own account.
     * Returns 204 so the client knows the local + Firebase state is gone before signing out.
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteAccount() {
        accountService.deleteCurrentAccount();
        return ResponseEntity.noContent().build();
    }
}
