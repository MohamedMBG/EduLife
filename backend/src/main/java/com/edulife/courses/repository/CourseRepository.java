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

public interface CourseRepository extends JpaRepository<Course, UUID> {

    // Discovery always excludes drafts and archived rows at the query level so the service
    // never has to filter unsafe catalog data in memory after fetching it.
    Page<Course> findAllByStatus(CourseStatus status, Pageable pageable);

    // Level filtering stays in the repository query so pagination counts remain correct.
    Page<Course> findAllByStatusAndLevel(CourseStatus status, String level, Pageable pageable);

    // Detail screens must never expose draft or archived courses to learners, so the
    // published-state check belongs in the repository lookup instead of after fetch.
    Optional<Course> findByIdAndStatus(UUID id, CourseStatus status);

    // CMS queries: teachers see only their own courses; admin sees all.
    List<Course> findAllByCreatedByUserId(UUID createdByUserId);

    // Full-text search via the search_vector tsvector column added in V13__course_fts.sql.
    // plainto_tsquery converts a raw user query into a tsquery without requiring special
    // syntax from the caller (unlike to_tsquery which needs explicit AND/OR operators).
    // The separate countQuery is required by Spring Data JPA for native paginated queries.
    long countByStatus(CourseStatus status);

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
