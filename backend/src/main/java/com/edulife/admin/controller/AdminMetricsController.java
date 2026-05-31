package com.edulife.admin.controller;

import com.edulife.admin.dto.AdminMetricsDto;
import com.edulife.admin.service.AdminMetricsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/metrics")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMetricsController {

    private final AdminMetricsService metricsService;

    public AdminMetricsController(AdminMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping
    public AdminMetricsDto getMetrics() {
        return metricsService.getMetrics();
    }
}
