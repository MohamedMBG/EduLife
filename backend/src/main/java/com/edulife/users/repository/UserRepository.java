package com.edulife.users.repository;

import com.edulife.users.entity.User;
import com.edulife.users.model.UserRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data repository for {@link User} entities, with lookups by Firebase UID and email. */
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByFirebaseUid(String firebaseUid);

    Optional<User> findByEmail(String email);

    boolean existsByFirebaseUid(String firebaseUid);

    boolean existsByEmail(String email);

    /** Idempotent upsert used during auth sync; inserts a new user row only if the Firebase UID is absent. */
    @Modifying
    @Query(value = """
            INSERT INTO users (id, firebase_uid, email, role)
            VALUES (:id, :firebaseUid, :email, :role)
            ON CONFLICT (firebase_uid) DO NOTHING
            """, nativeQuery = true)
    int insertForAuthSyncIfAbsent(
            @Param("id") UUID id,
            @Param("firebaseUid") String firebaseUid,
            @Param("email") String email,
            @Param("role") String role
    );

    // Admin user list — role filter stays in the repository so paginated counts remain correct.
    Page<User> findAllByRole(UserRole role, Pageable pageable);

    long countByRole(UserRole role);
}
