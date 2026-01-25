package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.request.SetPermissionsRequest;
import org.example.service.AdminService;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PostMapping("/users/{userId}/permissions")
    public void setUserPermissions(@PathVariable("userId") Long userId,
                                   @RequestBody SetPermissionsRequest req) {
        adminService.setUserPermissions(userId, req);
    }
}


