package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.response.DashboardResponse;
import org.example.security.CustomUserDetails;
import org.example.service.DashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService service;

    @GetMapping
    public DashboardResponse dashboard(@AuthenticationPrincipal CustomUserDetails user) {
        return service.getDashboard(user.getCompanyId());
    }
}
