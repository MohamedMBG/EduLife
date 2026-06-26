package com.edulife.certificates.controller;

import com.edulife.certificates.dto.CertificateDetailDto;
import com.edulife.certificates.dto.CertificateSummaryDto;
import com.edulife.certificates.dto.CertificateVerificationDto;
import com.edulife.certificates.service.CertificateService;
import com.edulife.security.FirebaseAuthentication;
import com.edulife.users.entity.User;
import com.edulife.users.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** REST controller for certificate listing, detail retrieval, PDF download, and public verification. */
@RestController
@RequestMapping("/api/v1/certificates")
public class CertificateController {

    private final CertificateService certificateService;
    private final UserRepository userRepository;

    public CertificateController(CertificateService certificateService, UserRepository userRepository) {
        this.certificateService = certificateService;
        this.userRepository = userRepository;
    }

    /** Returns all certificates earned by the authenticated learner. */
    @GetMapping("/me")
    public List<CertificateSummaryDto> getMyCertificates(Authentication auth) {
        UUID userId = resolveUserId(auth);
        return certificateService.getMyCertificates(userId);
    }

    /** Returns full details of a specific certificate owned by the authenticated learner. */
    @GetMapping("/{id}")
    public CertificateDetailDto getCertificate(@PathVariable UUID id, Authentication auth) {
        UUID userId = resolveUserId(auth);
        return certificateService.getCertificateById(userId, id);
    }

    /** Downloads the certificate as a PDF; ownership is verified server-side. */
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadCertificate(@PathVariable UUID id, Authentication auth) {
        UUID userId = resolveUserId(auth);
        byte[] pdf = certificateService.getCertificatePdfForDownload(userId, id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"certificate-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    /** Public endpoint to verify a certificate's authenticity by its hash (no auth required). */
    @GetMapping("/verify/{verificationHash}")
    public CertificateVerificationDto verifyCertificate(@PathVariable String verificationHash) {
        return certificateService.verifyCertificate(verificationHash);
    }

    /** Resolves the internal user ID from the Firebase authentication token. */
    private UUID resolveUserId(Authentication auth) {
        if (!(auth instanceof FirebaseAuthentication firebaseAuth)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        User user = userRepository.findByFirebaseUid(firebaseAuth.getFirebaseUid())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "User not found. Call /auth/sync first."));
        return user.getId();
    }
}
