package com.edulife.admin.service;

import com.edulife.admin.dto.AdminMetricsDto;
import com.edulife.certificates.repository.CertificateRepository;
import com.edulife.courses.model.CourseStatus;
import com.edulife.courses.repository.CourseRepository;
import com.edulife.enrollments.model.EnrollmentStatus;
import com.edulife.enrollments.repository.EnrollmentRepository;
import com.edulife.users.model.UserRole;
import com.edulife.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminMetricsService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CertificateRepository certificateRepository;

    public AdminMetricsService(
            UserRepository userRepository,
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            CertificateRepository certificateRepository
    ) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.certificateRepository = certificateRepository;
    }

    @Transactional(readOnly = true)
    public AdminMetricsDto getMetrics() {
        return new AdminMetricsDto(
                userRepository.countByRole(UserRole.LEARNER),
                userRepository.countByRole(UserRole.TEACHER),
                userRepository.countByRole(UserRole.GROUP_ADMIN),
                courseRepository.countByStatus(CourseStatus.DRAFT),
                courseRepository.countByStatus(CourseStatus.PUBLISHED),
                courseRepository.countByStatus(CourseStatus.ARCHIVED),
                enrollmentRepository.countByStatus(EnrollmentStatus.ACTIVE),
                certificateRepository.count()
        );
    }
}
