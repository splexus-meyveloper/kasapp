package org.example.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.example.dto.request.LoginRequest;
import org.example.dto.request.RegisterRequest;
import org.example.dto.response.LoginResponse;
import org.example.dto.response.RegisterResponse;
import org.example.security.LoginRateLimiter;
import org.example.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final LoginRateLimiter rateLimiter;

    public AuthController(AuthService authService, LoginRateLimiter rateLimiter) {
        this.authService = authService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request,
                                   HttpServletRequest httpRequest) {
        String clientIp = resolveClientIp(httpRequest);

        if (rateLimiter.isLoginBlocked(clientIp)) {
            long wait = rateLimiter.getLoginSecondsUntilUnblock(clientIp);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Çok fazla başarısız giriş. " + wait + " saniye sonra tekrar deneyin.");
        }

        try {
            LoginResponse response = authService.login(request);
            rateLimiter.recordLoginSuccess(clientIp);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            rateLimiter.recordLoginFailure(clientIp);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @PostMapping("/register-company")
    public ResponseEntity<?> registerCompany(@Valid @RequestBody RegisterRequest request,
                                             HttpServletRequest httpRequest) {
        String clientIp = resolveClientIp(httpRequest);

        // Register için daha sıkı limit: 3 deneme → 1 saat blok
        if (rateLimiter.isRegisterBlocked(clientIp)) {
            long wait = rateLimiter.getRegisterSecondsUntilUnblock(clientIp);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Çok fazla kayıt denemesi. " + wait + " saniye sonra tekrar deneyin.");
        }

        rateLimiter.recordRegisterAttempt(clientIp);
        return ResponseEntity.ok(authService.registerCompany(request));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}