package com.edulife.certificates;

import com.edulife.certificates.config.CertificateStorageProperties;
import com.edulife.certificates.dto.CertificateDetailDto;
import com.edulife.certificates.entity.Certificate;
import com.edulife.certificates.exception.CertificateAccessDeniedException;
import com.edulife.certificates.exception.CertificateNotDownloadableException;
import com.edulife.certificates.exception.CertificateNotFoundException;
import com.edulife.certificates.repository.CertificateRepository;
import com.edulife.certificates.service.CertificatePdfService;
import com.edulife.certificates.service.CertificateService;
import com.edulife.courses.entity.Course;
import com.edulife.courses.repository.CourseRepository;
import com.edulife.profiles.entity.Profile;
import com.edulife.profiles.repository.ProfileRepository;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CertificateServiceTest {

    private static final UUID LEARNER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID OTHER_LEARNER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID TEACHER_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID COURSE_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID ATTEMPT_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    private static final UUID CERT_ID = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

    @Mock private CertificateRepository certificateRepository;
    @Mock private ProfileRepository profileRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private CertificatePdfService pdfService;

    @TempDir
    Path tempDir;

    @Test
    void generateCertificateStoresDynamicSnapshotsFromDatabaseRelationships() throws Exception {
        CertificateService service = service();
        Course course = new Course(
                COURSE_ID,
                "algebra-foundations",
                "Algebra Foundations",
                "Short",
                "Full description",
                "en",
                "BEGINNER",
                null,
                TEACHER_ID
        );
        Profile learnerProfile = new Profile(LEARNER_ID);
        learnerProfile.update("Jane Learner", null);
        Profile teacherProfile = new Profile(TEACHER_ID);
        teacherProfile.update("Prof. Teacher", null);

        given(certificateRepository.existsByUserIdAndCourseId(LEARNER_ID, COURSE_ID)).willReturn(false);
        given(profileRepository.findByUserId(LEARNER_ID)).willReturn(Optional.of(learnerProfile));
        given(courseRepository.findById(COURSE_ID)).willReturn(Optional.of(course));
        given(profileRepository.findByUserId(TEACHER_ID)).willReturn(Optional.of(teacherProfile));
        given(pdfService.generatePdf(any(Certificate.class))).willReturn(new byte[]{37, 80, 68, 70});
        given(certificateRepository.save(any(Certificate.class))).willAnswer(invocation -> savedCertificate(invocation.getArgument(0)));

        CertificateDetailDto dto = service.generateCertificateAfterExamPass(LEARNER_ID, COURSE_ID, ATTEMPT_ID);

        assertThat(dto.learnerName()).isEqualTo("Jane Learner");
        assertThat(dto.teacherName()).isEqualTo("Prof. Teacher");
        assertThat(dto.courseTitle()).isEqualTo("Algebra Foundations");
        assertThat(dto.courseLevel()).isEqualTo("BEGINNER");
        assertThat(dto.verificationHash()).isNotBlank();

        ArgumentCaptor<Certificate> saved = ArgumentCaptor.forClass(Certificate.class);
        verify(certificateRepository, atLeastOnce()).save(saved.capture());
        Certificate cert = saved.getAllValues().get(0);
        assertThat(cert.getLearnerNameSnapshot()).isEqualTo("Jane Learner");
        assertThat(cert.getTeacherNameSnapshot()).isEqualTo("Prof. Teacher");
        assertThat(cert.getCourseTitleSnapshot()).isEqualTo("Algebra Foundations");
        assertThat(cert.getCourseLevelSnapshot()).isEqualTo("BEGINNER");
    }

    @Test
    void getCertificateByIdScopesLookupToCurrentLearner() {
        CertificateService service = service();
        given(certificateRepository.findByIdAndUserId(CERT_ID, OTHER_LEARNER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCertificateById(OTHER_LEARNER_ID, CERT_ID))
                .isInstanceOf(CertificateNotFoundException.class)
                .hasMessage("Certificate not found");

        // Ownership is enforced by querying on both certificate id and current learner id.
        verify(certificateRepository).findByIdAndUserId(CERT_ID, OTHER_LEARNER_ID);
    }

    @Test
    void downloadRegeneratesPdfFromSnapshotForOwner() throws Exception {
        CertificateService service = service();
        byte[] pdf = new byte[]{37, 80, 68, 70, 1, 2, 3};
        given(certificateRepository.findById(CERT_ID)).willReturn(Optional.of(ownedCertificate(LEARNER_ID)));
        given(pdfService.generatePdf(any(Certificate.class))).willReturn(pdf);

        byte[] result = service.getCertificatePdfForDownload(LEARNER_ID, CERT_ID);

        assertThat(result).isEqualTo(pdf);
    }

    @Test
    void downloadByAnotherLearnerIsForbidden() {
        CertificateService service = service();
        given(certificateRepository.findById(CERT_ID)).willReturn(Optional.of(ownedCertificate(LEARNER_ID)));

        assertThatThrownBy(() -> service.getCertificatePdfForDownload(OTHER_LEARNER_ID, CERT_ID))
                .isInstanceOf(CertificateAccessDeniedException.class);
    }

    @Test
    void downloadMissingCertificateIsNotFound() {
        CertificateService service = service();
        given(certificateRepository.findById(CERT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCertificatePdfForDownload(LEARNER_ID, CERT_ID))
                .isInstanceOf(CertificateNotFoundException.class);
    }

    @Test
    void downloadCertificateWithoutHashIsConflict() {
        CertificateService service = service();
        Certificate cert = new Certificate(LEARNER_ID, COURSE_ID, ATTEMPT_ID, "EL-2026-NOHASH",
                "Jane Learner", "Prof. Teacher", "Algebra Foundations", "BEGINNER", null, null);
        given(certificateRepository.findById(CERT_ID)).willReturn(Optional.of(cert));

        assertThatThrownBy(() -> service.getCertificatePdfForDownload(LEARNER_ID, CERT_ID))
                .isInstanceOf(CertificateNotDownloadableException.class);
    }

    private CertificateService service() {
        CertificateStorageProperties properties = new CertificateStorageProperties();
        properties.setStorageDir(tempDir.toString());
        properties.setPublicBaseUrl("http://localhost:8080/api/v1/certificates");
        return new CertificateService(
                certificateRepository,
                profileRepository,
                courseRepository,
                pdfService,
                properties
        );
    }

    private Certificate ownedCertificate(UUID ownerId) {
        Certificate cert = new Certificate(ownerId, COURSE_ID, ATTEMPT_ID, "EL-2026-ABC123DEF456",
                "Jane Learner", "Prof. Teacher", "Algebra Foundations", "BEGINNER",
                "abc123hashvalueabc123hashvalue", null);
        ReflectionTestUtils.setField(cert, "id", CERT_ID);
        ReflectionTestUtils.setField(cert, "issuedAt", Instant.parse("2026-06-16T10:00:00Z"));
        return cert;
    }

    private Certificate savedCertificate(Certificate certificate) {
        if (certificate.getId() == null) {
            ReflectionTestUtils.setField(certificate, "id", CERT_ID);
        }
        if (certificate.getIssuedAt() == null) {
            ReflectionTestUtils.setField(certificate, "issuedAt", Instant.parse("2026-06-16T10:00:00Z"));
            ReflectionTestUtils.setField(certificate, "createdAt", Instant.parse("2026-06-16T10:00:00Z"));
        }
        return certificate;
    }
}
