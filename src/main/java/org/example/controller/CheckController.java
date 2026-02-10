package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.CheckEntryRequest;
import org.example.dto.request.CheckExitRequest;
import org.example.dto.response.CheckListResponse;
import org.example.entity.Check;
import org.example.security.CustomUserDetails;
import org.example.service.CheckService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/checks")
@RequiredArgsConstructor
public class CheckController {

    private final CheckService service;

    @PostMapping("/in")
    public ResponseEntity<?> checkIn(
            @Valid @RequestBody CheckEntryRequest req,
            @AuthenticationPrincipal CustomUserDetails user
    ) {

        service.checkIn(
                req,
                user.getId(),
                user.getCompanyId()
        );

        return ResponseEntity.ok("Çek giriş yapıldı");
    }

    @PostMapping("/out")
    public ResponseEntity<?> checkOut(
            @Valid @RequestBody CheckExitRequest req,
            @AuthenticationPrincipal CustomUserDetails user
    ) {

        service.checkOut(
                req,
                user.getId(),
                user.getCompanyId()
        );

        return ResponseEntity.ok("Çek çıkışı yapıldı");
    }

    @GetMapping("/portfolio")
    public List<CheckListResponse> portfolio(
            @AuthenticationPrincipal CustomUserDetails user
    ){
        return service.getPortfolioChecks(
                user.getCompanyId()
        );
    }
}
