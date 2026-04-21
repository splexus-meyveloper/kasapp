package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.security.CustomUserDetails;
import org.example.service.MyActivityService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/my-activities")
@RequiredArgsConstructor
public class MyActivityController {

    private final MyActivityService myActivityService;

    @GetMapping
    public ResponseEntity<?> getMyActivities() {

        CustomUserDetails userDetails =
                (CustomUserDetails) SecurityContextHolder.getContext()
                        .getAuthentication()
                        .getPrincipal();

        Long userId = userDetails.getId();
        Long companyId = userDetails.getCompanyId();

        return ResponseEntity.ok(
                myActivityService.getMyActivities(userId, companyId)
        );
    }
}