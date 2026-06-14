package com.edulife.analytics.repository;

import com.edulife.enrollments.entity.Enrollment;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Read-only cohort/progress aggregates (Phase C). All queries are native Postgres aggregates over
 * existing MVP tables — no new tables, no entities exposed (results are interface projections).
 *
 * <p>Bound to {@link Enrollment} only to satisfy Spring Data's domain-type requirement; the
 * queries reference tables directly. Scope is supplied by the service as explicit id collections,
 * which is how RBAC is enforced: the service derives the ids from the resolved user / owned
 * courses / owned group, never from client input.</p>
 *
 * <p>Funnel queries use {@code count(*) FILTER (WHERE ...)} over a derived table of per-enrollment
 * EXISTS flags so all five stage counts come back in one pass instead of five round trips.</p>
 */
public interface CohortAnalyticsRepository extends Repository<Enrollment, UUID> {

    // ── Funnels ────────────────────────────────────────────────────────────────

    /** Course-scoped funnel (teacher: owned courseIds). Caller must pass a non-empty collection. */
    @Query(value = """
            SELECT
                count(*)                          AS enrolled,
                count(*) FILTER (WHERE started)   AS started,
                count(*) FILTER (WHERE completed) AS completed,
                count(*) FILTER (WHERE passed)    AS passed,
                count(*) FILTER (WHERE certified) AS certified
            FROM (
                SELECT
                    EXISTS (SELECT 1 FROM lesson_progress lp
                            WHERE lp.user_id = e.user_id AND lp.course_id = e.course_id) AS started,
                    EXISTS (SELECT 1 FROM course_progress cp
                            WHERE cp.user_id = e.user_id AND cp.course_id = e.course_id
                              AND cp.total_lessons > 0 AND cp.completed_lessons >= cp.total_lessons) AS completed,
                    EXISTS (SELECT 1 FROM exam_attempts ea JOIN exams x ON x.id = ea.exam_id
                            WHERE x.course_id = e.course_id AND ea.user_id = e.user_id AND ea.passed = true) AS passed,
                    EXISTS (SELECT 1 FROM certificates c
                            WHERE c.user_id = e.user_id AND c.course_id = e.course_id) AS certified
                FROM enrollments e
                WHERE e.status = 'ACTIVE' AND e.course_id IN (:courseIds)
            ) t
            """, nativeQuery = true)
    FunnelProjection funnelByCourseIds(@Param("courseIds") Collection<UUID> courseIds);

    /** Group-scoped funnel: enrollments where both the course and the learner belong to the group. */
    @Query(value = """
            SELECT
                count(*)                          AS enrolled,
                count(*) FILTER (WHERE started)   AS started,
                count(*) FILTER (WHERE completed) AS completed,
                count(*) FILTER (WHERE passed)    AS passed,
                count(*) FILTER (WHERE certified) AS certified
            FROM (
                SELECT
                    EXISTS (SELECT 1 FROM lesson_progress lp
                            WHERE lp.user_id = e.user_id AND lp.course_id = e.course_id) AS started,
                    EXISTS (SELECT 1 FROM course_progress cp
                            WHERE cp.user_id = e.user_id AND cp.course_id = e.course_id
                              AND cp.total_lessons > 0 AND cp.completed_lessons >= cp.total_lessons) AS completed,
                    EXISTS (SELECT 1 FROM exam_attempts ea JOIN exams x ON x.id = ea.exam_id
                            WHERE x.course_id = e.course_id AND ea.user_id = e.user_id AND ea.passed = true) AS passed,
                    EXISTS (SELECT 1 FROM certificates c
                            WHERE c.user_id = e.user_id AND c.course_id = e.course_id) AS certified
                FROM enrollments e
                WHERE e.status = 'ACTIVE'
                  AND e.course_id IN (:courseIds)
                  AND e.user_id  IN (:userIds)
            ) t
            """, nativeQuery = true)
    FunnelProjection funnelByGroup(
            @Param("courseIds") Collection<UUID> courseIds,
            @Param("userIds") Collection<UUID> userIds);

    /** Global funnel (platform admin). No scope filter. */
    @Query(value = """
            SELECT
                count(*)                          AS enrolled,
                count(*) FILTER (WHERE started)   AS started,
                count(*) FILTER (WHERE completed) AS completed,
                count(*) FILTER (WHERE passed)    AS passed,
                count(*) FILTER (WHERE certified) AS certified
            FROM (
                SELECT
                    EXISTS (SELECT 1 FROM lesson_progress lp
                            WHERE lp.user_id = e.user_id AND lp.course_id = e.course_id) AS started,
                    EXISTS (SELECT 1 FROM course_progress cp
                            WHERE cp.user_id = e.user_id AND cp.course_id = e.course_id
                              AND cp.total_lessons > 0 AND cp.completed_lessons >= cp.total_lessons) AS completed,
                    EXISTS (SELECT 1 FROM exam_attempts ea JOIN exams x ON x.id = ea.exam_id
                            WHERE x.course_id = e.course_id AND ea.user_id = e.user_id AND ea.passed = true) AS passed,
                    EXISTS (SELECT 1 FROM certificates c
                            WHERE c.user_id = e.user_id AND c.course_id = e.course_id) AS certified
                FROM enrollments e
                WHERE e.status = 'ACTIVE'
            ) t
            """, nativeQuery = true)
    FunnelProjection funnelGlobal();

    // ── Monthly enrollment cohorts ───────────────────────────────────────────────

    /** Enrollment-month cohorts for a set of courses (teacher). */
    @Query(value = """
            SELECT to_char(date_trunc('month', e.enrolled_at), 'YYYY-MM') AS month,
                   count(*) AS total
            FROM enrollments e
            WHERE e.status = 'ACTIVE' AND e.course_id IN (:courseIds)
            GROUP BY 1
            ORDER BY 1
            """, nativeQuery = true)
    List<MonthCountProjection> enrollmentCohortsByCourseIds(@Param("courseIds") Collection<UUID> courseIds);

    /** Enrollment-month cohorts platform-wide. */
    @Query(value = """
            SELECT to_char(date_trunc('month', e.enrolled_at), 'YYYY-MM') AS month,
                   count(*) AS total
            FROM enrollments e
            WHERE e.status = 'ACTIVE'
            GROUP BY 1
            ORDER BY 1
            """, nativeQuery = true)
    List<MonthCountProjection> enrollmentCohortsGlobal();

    // ── Trends ───────────────────────────────────────────────────────────────────

    /** Certificate issuance trend by month, platform-wide. */
    @Query(value = """
            SELECT to_char(date_trunc('month', c.issued_at), 'YYYY-MM') AS month,
                   count(*) AS total
            FROM certificates c
            GROUP BY 1
            ORDER BY 1
            """, nativeQuery = true)
    List<MonthCountProjection> certificateTrendGlobal();

    /** A single student's own lessons-completed trend by month. Scoped to the resolved user id. */
    @Query(value = """
            SELECT to_char(date_trunc('month', lp.completed_at), 'YYYY-MM') AS month,
                   count(*) AS total
            FROM lesson_progress lp
            WHERE lp.user_id = :userId
            GROUP BY 1
            ORDER BY 1
            """, nativeQuery = true)
    List<MonthCountProjection> lessonTrendByUser(@Param("userId") UUID userId);
}
