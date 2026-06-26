package com.edulife.admin.service;

import com.edulife.admin.dto.ChangeRoleRequest;
import com.edulife.admin.dto.UserSummaryDto;
import com.edulife.users.entity.User;
import com.edulife.users.model.UserRole;
import com.edulife.users.repository.UserRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Platform-level user management. All methods require ADMIN authority enforced by
 * AdminUserController's class-level @PreAuthorize.
 */
@Service
public class AdminUserService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final UserRepository userRepository;

    public AdminUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Lists users with optional role filter, paginated and sorted by creation date.
     *
     * @param roleFilter optional role to filter by; {@code null} returns all users
     * @param pageable   page request (capped at {@value MAX_PAGE_SIZE} per page)
     */
    @Transactional(readOnly = true)
    public Page<UserSummaryDto> listUsers(UserRole roleFilter, Pageable pageable) {
        Pageable sanitized = sanitize(pageable);

        Page<User> page = (roleFilter != null)
                ? userRepository.findAllByRole(roleFilter, sanitized)
                : userRepository.findAll(sanitized);

        return page.map(this::toDto);
    }

    /**
     * Changes the role of an existing user, blocking demotion of the last ADMIN.
     *
     * @throws ResponseStatusException 404 if user not found, 409 if demoting the last admin
     */
    @Transactional
    public UserSummaryDto changeRole(UUID userId, ChangeRoleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Block demoting the last remaining ADMIN — otherwise the platform could be left with no
        // account able to manage users, roles, or content, with no in-app way to recover.
        if (user.getRole() == UserRole.ADMIN
                && request.role() != UserRole.ADMIN
                && userRepository.countByRole(UserRole.ADMIN) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot remove the last administrator");
        }

        user.setRole(request.role());
        // JPA dirty-checking persists the role change on transaction commit; no explicit save needed.
        return toDto(user);
    }

    private UserSummaryDto toDto(User user) {
        return new UserSummaryDto(user.getId(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }

    private Pageable sanitize(Pageable pageable) {
        int page = pageable == null ? 0 : Math.max(pageable.getPageNumber(), 0);
        int size = pageable == null ? DEFAULT_PAGE_SIZE : pageable.getPageSize();
        int safeSize = (size <= 0) ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        // Stable sort by creation date keeps the list predictable for admin pagination.
        return PageRequest.of(page, safeSize, Sort.by("createdAt").ascending());
    }
}
