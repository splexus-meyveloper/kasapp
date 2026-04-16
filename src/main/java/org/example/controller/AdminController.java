package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.AdminCreateUserRequest;
import org.example.dto.request.SetPermissionsRequest;
import org.example.entity.User;
import org.example.exception.ErrorType;
import org.example.exception.KasappException;
import org.example.security.CustomUserDetails;
import org.example.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.example.dto.request.AdminCreateUserRequest;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{userId}/permissions")
    public List<String> getUserPermissions(
            @PathVariable("userId") Long userId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        return adminService.getUserPermissions(userId, currentUser.getCompanyId());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/users/{userId}/permissions")
    public ResponseEntity<Void> setUserPermissions(
            @PathVariable Long userId,
            @Valid @RequestBody SetPermissionsRequest req,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        if (req == null || req.permissions() == null) {
            throw new KasappException(ErrorType.PERMISSION_LIST_CANNOT_BE_EMPTY);
        }

        adminService.replaceUserPermissions(userId, req.permissions(), currentUser.getCompanyId());
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/profiles")
    public List<User> getAllProfiles(
            @AuthenticationPrincipal CustomUserDetails user){

        return adminService
                .getAllProfiles(user.getCompanyId());
    }

    // 🔥 SİLME / PASİF YAPMA
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/users/{id}")
    public void deactivate(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user){

        adminService.deactivateUser(id, user.getId(), user.getCompanyId());
    }

    // 🔥 ROLE DEĞİŞTİRME
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/users/{id}/role")
    public void updateUserRole(
            @PathVariable Long id,
            @RequestParam String role,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        adminService.updateUserRole(id, role, currentUser.getId(), currentUser.getCompanyId());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users")
    public ResponseEntity<Void> createSubUser(
            @Valid @RequestBody AdminCreateUserRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        adminService.createSubUser(request, currentUser);
        return ResponseEntity.ok().build();
    }

}
