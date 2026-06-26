package com.edulife.groups.repository;

import com.edulife.groups.entity.Group;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link Group} entities. */
public interface GroupRepository extends JpaRepository<Group, UUID> {

    List<Group> findAllByCreatedBy(UUID createdBy);

    List<Group> findAllByIdIn(Collection<UUID> ids);
}
