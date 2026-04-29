package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.security.CustomUserDetails;
import org.example.service.MyActivityService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/my-activities")
@RequiredArgsConstructor
public class MyActivityController {

    private final MyActivityService myActivityService;

    @GetMapping
    public ResponseEntity<?> getMyActivities(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "100") int size) {

        return ResponseEntity.ok(
                myActivityService.getMyActivities(
                        userDetails.getId(),
                        userDetails.getCompanyId(),
                        action, startDate, endDate,
                        page, size
                )
        );
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        byte[] pdf = myActivityService.exportPdf(
                userDetails.getId(),
                userDetails.getCompanyId(),
                userDetails.getUsername(),
                action, startDate, endDate
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"islemlerim.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}