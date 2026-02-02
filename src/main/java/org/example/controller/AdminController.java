package org.example.controller;

import lombok.RequiredArgsConstructor;
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

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin
public class AdminController {

    private final AdminService adminService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{userId}/permissions")
    public List<String> getUserPermissions(@PathVariable("userId") Long userId) {
        return adminService.getUserPermissions(userId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/users/{userId}/permissions")
    public ResponseEntity<Void> setUserPermissions(
            @PathVariable Long userId,
            @RequestBody SetPermissionsRequest req) {

        if (req == null || req.permissions() == null) {
            throw new KasappException(ErrorType.PERMISSION_LIST_CANNOT_BE_EMPTY);
        }

        adminService.replaceUserPermissions(userId, req.permissions());
        return ResponseEntity.ok().build();
    }



    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/profiles")
    public List<User> getAllProfiles(
            @AuthenticationPrincipal CustomUserDetails user){

        return adminService
                .getAllProfiles(user.getCompanyId());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/users/{id}")
    public void deactivate(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user){

        adminService.deactivateUser(id, user.getId());
    }
}



