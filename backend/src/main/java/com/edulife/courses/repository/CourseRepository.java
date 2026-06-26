package com.edulife.courses.repository;

import com.edulife.courses.entity.Course;
import com.edulife.courses.model.CourseStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data JPA repository for {@link Course} entities with discovery, CMS, and full-text search queries. */
public interface CourseRepository extends JpaRepository<Course, UUID> {

    // Discovery always excludes drafts and archived rows at the query level so the service
    // never has to filter unsafe catalog data in memory after fetching it.
    Page<Course> findAllByStatus(CourseStatus status, Pageable pageable);

    List<Course> findAllByStatus(CourseStatus status);

    // Level filtering stays in the repository query so pagination counts remain correct.
    Page<Course> findAllByStatusAndLevel(CourseStatus status, String level, Pageable pageable);

    // Detail screens must never expose draft or archived courses to learners, so the
    // published-state check belongs in the repository lookup instead of after fetch.
    Optional<Course> findByIdAndStatus(UUID id, CourseStatus status);

    // CMS queries: teachers see only their own courses; admin sees all.
    List<Course> findAllByCreatedByUserId(UUID createdByUserId);

    // Group admins review courses authored by the teachers inside their groups.
    List<Course> findAllByCreatedByUserIdIn(java.util.Collection<UUID> createdByUserIds);

    long countByStatus(CourseStatus status);

    /**
     * Full-text search against the {@code search_vector} tsvector column.
     * Uses {@code plainto_tsquery} so callers pass plain text without special syntax.
     */
    @Query(
        value = "SELECT * FROM courses WHERE status = 'PUBLISHED' " +
                "AND search_vector @@ plainto_tsquery('simple', :query) " +
                "ORDER BY ts_rank(search_vector, plainto_tsquery('simple', :query)) DESC, published_at DESC",
        countQuery = "SELECT count(*) FROM courses WHERE status = 'PUBLISHED' " +
                     "AND search_vector @@ plainto_tsquery('simple', :query)",
        nativeQuery = true
    )
    Page<Course> searchPublished(@Param("query") String query, Pageable pageable);
}
