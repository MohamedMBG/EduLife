package com.edulife.admin.controller;

import com.edulife.admin.dto.ChangeRoleRequest;
import com.edulife.admin.dto.UserSummaryDto;
import com.edulife.admin.service.AdminUserService;
import com.edulife.users.model.UserRole;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
// All admin user-management endpoints require ADMIN to prevent TEACHERs from promoting accounts.
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    /**
     * Lists all users with optional role filter.
     * GET /api/v1/admin/users?role=TEACHER&page=0&size=20
     */
    @GetMapping
    public Page<UserSummaryDto> listUsers(
            @RequestParam(required = false) UserRole role,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return adminUserService.listUsers(role, pageable);
    }

    /**
     * Changes the role of a user.
     * PUT /api/v1/admin/users/{id}/role
     */
    @PutMapping("/{id}/role")
    public ResponseEntity<UserSummaryDto> changeRole(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeRoleRequest request
    ) {
        return ResponseEntity.ok(adminUserService.changeRole(id, request));
    }
}
