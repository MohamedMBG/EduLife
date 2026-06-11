package com.edulife.certificates;

import com.edulife.certificates.controller.CertificateController;
import com.edulife.certificates.dto.CertificateDetailDto;
import com.edulife.certificates.dto.CertificateSummaryDto;
import com.edulife.certificates.dto.CertificateVerificationDto;
import com.edulife.certificates.exception.CertificateNotFoundException;
import com.edulife.certificates.service.CertificateService;
import com.edulife.common.error.ApiErrorWriter;
import com.edulife.common.error.GlobalApiExceptionHandler;
import com.edulife.security.SecurityConfig;
import com.edulife.users.entity.User;
import com.edulife.users.model.UserRole;
import com.edulife.users.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CertificateController.class)
@Import({SecurityConfig.class, ApiErrorWriter.class, GlobalApiExceptionHandler.class})
class CertificateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CertificateService certificateService;

    @MockBean
    private FirebaseAuth firebaseAuth;

    @MockBean
    private UserRepository userRepository;

    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CERT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID COURSE_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Test
    void getMyCertificatesWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/certificates/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        verifyNoInteractions(certificateService);
    }

    @Test
    void getMyCertificatesWithValidTokenReturns200List() throws Exception {
        mockValidFirebaseToken();
        mockUserResolution();

        CertificateSummaryDto summary = new CertificateSummaryDto(
                CERT_ID, COURSE_ID, "EL-2026-ABC123DEF456",
                "Algebra Foundations", Instant.parse("2026-05-01T10:00:00Z"));

        given(certificateService.getMyCertificates(USER_ID)).willReturn(List.of(summary));

        mockMvc.perform(get("/api/v1/certificates/me")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(CERT_ID.toString()))
                .andExpect(jsonPath("$[0].courseTitle").value("Algebra Foundations"))
                .andExpect(jsonPath("$[0].certificateNumber").value("EL-2026-ABC123DEF456"));
    }

    @Test
    void getCertificateByIdWithValidTokenAndOwnedCertReturns200() throws Exception {
        mockValidFirebaseToken();
        mockUserResolution();

        CertificateDetailDto detail = new CertificateDetailDto(
                CERT_ID, COURSE_ID, "EL-2026-ABC123DEF456",
                "Jane Doe", "Algebra Foundations", "Prof. Smith",
                Instant.parse("2026-05-01T10:00:00Z"), "abc123hashvalue",
                "storage/certificates/certificate-" + CERT_ID + ".pdf");

        given(certificateService.getCertificateById(USER_ID, CERT_ID)).willReturn(detail);

        mockMvc.perform(get("/api/v1/certificates/{id}", CERT_ID)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CERT_ID.toString()))
                .andExpect(jsonPath("$.studentName").value("Jane Doe"))
                .andExpect(jsonPath("$.courseTitle").value("Algebra Foundations"));
    }

    @Test
    void getCertificateByIdForUnownedCertReturns404() throws Exception {
        mockValidFirebaseToken();
        mockUserResolution();

        given(certificateService.getCertificateById(USER_ID, CERT_ID))
                .willThrow(new CertificateNotFoundException("Certificate not found"));

        mockMvc.perform(get("/api/v1/certificates/{id}", CERT_ID)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Certificate not found"));
    }

    @Test
    void verifyCertificateWithoutTokenReturns200BecausePublic() throws Exception {
        String hash = "abc123hashvalue";
        CertificateVerificationDto dto = new CertificateVerificationDto(
                "Jane Doe", "Algebra Foundations", "Prof. Smith",
                Instant.parse("2026-05-01T10:00:00Z"), "EL-2026-ABC123DEF456", true);

        given(certificateService.verifyCertificate(hash)).willReturn(dto);

        mockMvc.perform(get("/api/v1/certificates/verify/{hash}", hash))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.studentName").value("Jane Doe"))
                .andExpect(jsonPath("$.certificateNumber").value("EL-2026-ABC123DEF456"));
    }

    @Test
    void verifyCertificateWithBadHashReturns404() throws Exception {
        String badHash = "nonexistent-hash";
        given(certificateService.verifyCertificate(badHash))
                .willThrow(new CertificateNotFoundException("Certificate not found for the given verification hash"));

        mockMvc.perform(get("/api/v1/certificates/verify/{hash}", badHash))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void verifyCertificateRateLimitedAfterBudgetExhausted() throws Exception {
        // Unique forwarded IP isolates this test's bucket from sibling tests in the shared context.
        String testIp = "203.0.113.42";
        String hash = "ratelimit-hash";
        CertificateVerificationDto dto = new CertificateVerificationDto(
                "Jane Doe", "Algebra Foundations", "Prof. Smith",
                Instant.parse("2026-05-01T10:00:00Z"), "EL-2026-ABC123DEF456", true);

        given(certificateService.verifyCertificate(hash)).willReturn(dto);

        // CERT_VERIFY_LIMIT = 30 calls / minute / IP. First 30 must succeed.
        for (int i = 0; i < 30; i++) {
            mockMvc.perform(get("/api/v1/certificates/verify/{hash}", hash)
                            .header("X-Forwarded-For", testIp))
                    .andExpect(status().isOk());
        }

        // 31st request must be rejected by the shared API error contract.
        mockMvc.perform(get("/api/v1/certificates/verify/{hash}", hash)
                        .header("X-Forwarded-For", testIp))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.message").value("Rate limit exceeded"));
    }

    @Test
    void downloadCertificateWithValidTokenReturns200PdfContentType() throws Exception {
        mockValidFirebaseToken();
        mockUserResolution();

        byte[] fakePdf = new byte[]{37, 80, 68, 70};
        given(certificateService.getCertificatePdfForDownload(USER_ID, CERT_ID)).willReturn(fakePdf);

        mockMvc.perform(get("/api/v1/certificates/{id}/download", CERT_ID)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"certificate-" + CERT_ID + ".pdf\""));
    }

    private void mockValidFirebaseToken() throws Exception {
        FirebaseToken decodedToken = mock(FirebaseToken.class);
        given(firebaseAuth.verifyIdToken("valid-token")).willReturn(decodedToken);
        given(decodedToken.getUid()).willReturn("test-uid");
        given(decodedToken.getEmail()).willReturn("test@edulife.test");
        given(decodedToken.isEmailVerified()).willReturn(true);
    }

    private void mockUserResolution() {
        User user = mock(User.class);
        given(user.getId()).willReturn(USER_ID);
        // FirebaseTokenFilter.authoritiesFor() reads the role to build a granted authority,
        // so a null role here surfaces as an NPE inside the security filter chain.
        given(user.getRole()).willReturn(UserRole.LEARNER);
        given(userRepository.findByFirebaseUid("test-uid")).willReturn(Optional.of(user));
    }
}
