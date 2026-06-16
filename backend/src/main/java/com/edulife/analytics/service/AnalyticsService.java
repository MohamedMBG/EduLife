package com.edulife.analytics.service;

import com.edulife.analytics.dto.PlatformAnalyticsDto;
import com.edulife.analytics.dto.StudentAnalyticsSummaryDto;
import com.edulife.analytics.dto.TeacherAnalyticsDto;
import com.edulife.analytics.dto.TeacherCourseAnalyticsDto;
import com.edulife.certificates.repository.CertificateRepository;
import com.edulife.courses.entity.Course;
import com.edulife.courses.model.CourseStatus;
import com.edulife.courses.repository.CourseRepository;
import com.edulife.enrollments.model.EnrollmentStatus;
import com.edulife.enrollments.repository.EnrollmentRepository;
import com.edulife.exams.entity.Exam;
import com.edulife.exams.repository.ExamAttemptRepository;
import com.edulife.exams.repository.ExamRepository;
import com.edulife.progress.repository.CourseProgressRepository;
import com.edulife.progress.repository.LessonProgressRepository;
import com.edulife.security.FirebaseAuthentication;
import com.edulife.users.entity.User;
import com.edulife.users.model.UserRole;
import com.edulife.users.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Read-only Phase A analytics. All three views derive aggregates from existing MVP tables and
 * never write data. Scoping is enforced here from the server-resolved user — never from a
 * client-supplied id or role.
 */
@Service
public class AnalyticsService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final CourseProgressRepository courseProgressRepository;
    private final ExamRepository examRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final CertificateRepository certificateRepository;

    public AnalyticsService(
            UserRepository userRepository,
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            LessonProgressRepository lessonProgressRepository,
            CourseProgressRepository courseProgressRepository,
            ExamRepository examRepository,
            ExamAttemptRepository examAttemptRepository,
            CertificateRepository certificateRepository
    ) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.lessonProgressRepository = lessonProgressRepository;
        this.courseProgressRepository = courseProgressRepository;
        this.examRepository = examRepository;
        this.examAttemptRepository = examAttemptRepository;
        this.certificateRepository = certificateRepository;
    }

    /**
     * Student's own summary. The user id comes from the resolved Firebase identity, so the
     * caller can only ever read their own counts — there is no path to pass another user's id.
     */
    @Transactional(readOnly = true)
    public StudentAnalyticsSummaryDto getMyStudentSummary() {
        UUID userId = resolveCurrentUser().getId();

        Double avgScore = examAttemptRepository.averageScoreByUserId(userId);
        Integer bestScore = examAttemptRepository.maxScoreByUserId(userId);

        return new StudentAnalyticsSummaryDto(
                enrollmentRepository.countByUserIdAndStatus(userId, EnrollmentStatus.ACTIVE),
                lessonProgressRepository.countByUserId(userId),
                examAttemptRepository.countByUserId(userId),
                examAttemptRepository.countByUserIdAndPassedTrue(userId),
                certificateRepository.countByUserId(userId),
                avgScore == null ? 0 : (int) Math.round(avgScore),
                bestScore == null ? 0 : bestScore
        );
    }

    /**
     * Analytics for the requesting teacher's own courses only. Ownership is enforced by querying
     * courses authored by the resolved user id; no teacherId is accepted from the client. The
     * controller already restricts this endpoint to TEACHER/ADMIN roles.
     */
    @Transactional(readOnly = true)
    public TeacherAnalyticsDto getMyTeacherAnalytics() {
        UUID teacherId = resolveCurrentUser().getId();

        // Ownership scope: only courses whose author is the resolved teacher.
        List<Course> ownedCourses = courseRepository.findAllByCreatedByUserId(teacherId);

        List<TeacherCourseAnalyticsDto> courseStats = ownedCourses.stream()
                .map(this::toCourseAnalytics)
                .toList();

        return new TeacherAnalyticsDto(courseStats.size(), courseStats);
    }

    /**
     * Global platform counts. The controller restricts this to ADMIN; this method exposes only
     * aggregate counts, never per-user PII.
     */
    @Transactional(readOnly = true)
    public PlatformAnalyticsDto getPlatformAnalytics() {
        return new PlatformAnalyticsDto(
                userRepository.countByRole(UserRole.LEARNER),
                userRepository.countByRole(UserRole.TEACHER),
                userRepository.countByRole(UserRole.GROUP_ADMIN),
                userRepository.countByRole(UserRole.ADMIN),
                courseRepository.countByStatus(CourseStatus.DRAFT),
                courseRepository.countByStatus(CourseStatus.PUBLISHED),
                courseRepository.countByStatus(CourseStatus.ARCHIVED),
                enrollmentRepository.countByStatus(EnrollmentStatus.ACTIVE),
                examAttemptRepository.count(),
                examAttemptRepository.countByPassedTrue(),
                certificateRepository.count()
        );
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private TeacherCourseAnalyticsDto toCourseAnalytics(Course course) {
        UUID courseId = course.getId();

        long activeEnrollments = enrollmentRepository.countByCourseIdAndStatus(courseId, EnrollmentStatus.ACTIVE);
        long learnersWithProgress = courseProgressRepository.countByCourseId(courseId);
        long learnersCompleted = courseProgressRepository.countCompletedByCourseId(courseId);

        // A course may have no exam yet (CMS not finished); treat missing exam as zero attempts
        // rather than failing the whole teacher dashboard.
        Optional<Exam> exam = examRepository.findByCourseId(courseId);
        long examAttempts = exam.map(e -> examAttemptRepository.countByExamId(e.getId())).orElse(0L);
        long examsPassed = exam.map(e -> examAttemptRepository.countByExamIdAndPassedTrue(e.getId())).orElse(0L);

        long certificatesIssued = certificateRepository.countByCourseId(courseId);

        return new TeacherCourseAnalyticsDto(
                courseId,
                course.getTitle(),
                course.getStatus().name(),
                activeEnrollments,
                learnersWithProgress,
                learnersCompleted,
                percent(learnersCompleted, learnersWithProgress),
                examAttempts,
                examsPassed,
                percent(examsPassed, examAttempts),
                certificatesIssued
        );
    }

    /** Percentage rounded to one decimal; zero denominator yields 0.0 to avoid divide-by-zero. */
    private static double percent(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        return Math.round((numerator * 1000.0) / denominator) / 10.0;
    }

    /**
     * Resolves the internal user from the Firebase identity in the security context. Role and
     * user id are taken only from the trusted users table, never from the request body or params.
     */
    private User resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof FirebaseAuthentication firebaseAuth)) {
            throw new IllegalStateException("Firebase authentication is required.");
        }

        String firebaseUid = firebaseAuth.getFirebaseUid();
        return userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found. Call /auth/sync first."));
    }
}
