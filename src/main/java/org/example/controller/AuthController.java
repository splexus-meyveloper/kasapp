package org.example.controller;

import org.example.dto.request.LoginRequest;
import org.example.dto.request.RegisterRequest;
import org.example.dto.response.LoginResponse;
import org.example.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }
    @PostMapping("/register")
        public void register(@RequestBody RegisterRequest request){
        authService.register(request);
    }
}
