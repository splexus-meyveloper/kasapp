package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.PosLogRequest;
import org.example.dto.response.PosLogResponse;
import org.example.dto.response.PosTerminalGroupResponse;
import org.example.security.CustomUserDetails;
import org.example.service.PosService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pos")
@RequiredArgsConstructor
public class PosController {

    private final PosService service;

    @PreAuthorize("hasAuthority('KASA') or hasRole('ADMIN')")
    @GetMapping("/terminals")
    public List<PosTerminalGroupResponse> getTerminals() {
        return service.getTerminals();
    }

    @PreAuthorize("hasAuthority('KASA') or hasRole('ADMIN')")
    @PostMapping("/log")
    public ResponseEntity<PosLogResponse> logPos(
            @Valid @RequestBody PosLogRequest req,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(
                service.logPos(req, user.getId(), user.getCompanyId()));
    }

    /** Log listesi — sadece admin */
    @PreAuthorize("hasAuthority('KASA') or hasRole('ADMIN')")
    @GetMapping("/logs")
    public List<PosLogResponse> getLogs(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal CustomUserDetails user) {
        return service.getLogs(
                user.getCompanyId(),
                user.getId(),
                "ADMIN".equalsIgnoreCase(user.getRole()),
                page,
                size
        );
    }
}
