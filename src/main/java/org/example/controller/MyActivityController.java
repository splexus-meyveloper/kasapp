package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.security.CustomUserDetails;
import org.example.service.MyActivityService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/my-activities")
@RequiredArgsConstructor
public class MyActivityController {

    private final MyActivityService myActivityService;

    @GetMapping
    public ResponseEntity<?> getMyActivities(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.ok(
                myActivityService.getMyActivities(
                        userDetails.getId(),
                        userDetails.getCompanyId()
                )
        );
    }
}