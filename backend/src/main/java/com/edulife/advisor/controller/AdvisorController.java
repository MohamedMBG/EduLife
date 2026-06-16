package com.edulife.advisor.controller;

import com.edulife.advisor.dto.AdvisorRequest;
import com.edulife.advisor.dto.AdvisorResponse;
import com.edulife.advisor.service.AdvisorService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/advisor")
public class AdvisorController {

    private final AdvisorService advisorService;

    public AdvisorController(AdvisorService advisorService) {
        this.advisorService = advisorService;
    }

    @PostMapping("/recommend")
    public AdvisorResponse recommend(@Valid @RequestBody AdvisorRequest request) {
        return advisorService.recommend(request);
    }
}
