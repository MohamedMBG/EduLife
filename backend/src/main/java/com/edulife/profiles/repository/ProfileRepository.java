package com.edulife.profiles.repository;

import com.edulife.profiles.entity.Profile;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link Profile} entities, with lookups by user ID. */
public interface ProfileRepository extends JpaRepository<Profile, UUID> {

    Optional<Profile> findByUserId(UUID userId);

    List<Profile> findAllByUserIdIn(Collection<UUID> userIds);
}
