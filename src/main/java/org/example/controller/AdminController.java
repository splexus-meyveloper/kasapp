package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.AdminCreateUserRequest;
import org.example.dto.request.SetPermissionsRequest;
import org.example.dto.request.UpdateUserCompanyRequest;
import org.example.entity.Company;
import org.example.entity.User;
import org.example.exception.ErrorType;
import org.example.exception.KasappException;
import org.example.repository.CompanyRepository;
import org.example.security.CustomUserDetails;
import org.example.service.AdminService;
import org.example.websocket.PresenceTracker;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final CompanyRepository companyRepository;
    private final PresenceTracker presenceTracker;

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
    public List<User> getAllProfiles(@AuthenticationPrincipal CustomUserDetails user) {
        return adminService.getAllProfiles(user.getCompanyId());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/users/{id}")
    public void deactivate(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {
        adminService.deactivateUser(id, user.getId(), user.getCompanyId());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/users/{id}/role")
    public void updateUserRole(
            @PathVariable Long id,
            @RequestParam String role,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        adminService.updateUserRole(id, role, currentUser.getId(), currentUser.getCompanyId());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/users/{id}/company")
    public void updateUserCompany(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserCompanyRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        adminService.updateUserCompany(id, request.companyId(), currentUser.getId(), currentUser.getCompanyId());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users")
    public ResponseEntity<Void> createSubUser(
            @Valid @RequestBody AdminCreateUserRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        adminService.createSubUser(request, currentUser);
        return ResponseEntity.ok().build();
    }

    /** Tüm şubeleri listele — kullanıcı oluşturma formunda şube seçimi için */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/companies")
    public List<Company> getCompanies() {
        return companyRepository.findAll();
    }

    /** Şu an WebSocket bağlantısı açık (oturumu aktif) kullanıcılar */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/active-users")
    public List<Map<String, Object>> getActiveUsers() {
        Map<Long, String> companyNames = companyRepository.findAll().stream()
                .collect(Collectors.toMap(Company::getId, Company::getName, (a, b) -> a));

        return presenceTracker.getActiveUsers().stream()
                .map(u -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("userId",      u.userId());
                    m.put("username",    u.username());
                    m.put("role",        u.role());
                    m.put("companyId",   u.companyId());
                    m.put("companyName", companyNames.get(u.companyId()));
                    m.put("connectedAt", u.connectedAt());
                    return m;
                })
                .collect(Collectors.toList());
    }
}
