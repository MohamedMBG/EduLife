package com.edulife.certificates.controller;

import com.edulife.certificates.dto.CertificateDto;
import com.edulife.certificates.service.CertificateService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/certificates")
public class CertificateController {

    private final CertificateService certificateService;

    public CertificateController(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @GetMapping
    public List<CertificateDto> getMyCertificates() {
        return certificateService.getMyCertificates();
    }
}
