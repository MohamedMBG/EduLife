package com.edulife.users.repository;

import com.edulife.users.entity.User;
import com.edulife.users.model.UserRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByFirebaseUid(String firebaseUid);

    Optional<User> findByEmail(String email);

    boolean existsByFirebaseUid(String firebaseUid);

    boolean existsByEmail(String email);

    // Admin user list — role filter stays in the repository so paginated counts remain correct.
    Page<User> findAllByRole(UserRole role, Pageable pageable);
}