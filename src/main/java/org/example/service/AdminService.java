package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dto.request.AdminCreateUserRequest;
import org.example.entity.Permission;
import org.example.entity.User;
import org.example.entity.UserPermission;
import org.example.exception.ErrorType;
import org.example.exception.KasappException;
import org.example.repository.PermissionRepository;
import org.example.repository.UserPermissionRepository;
import org.example.repository.UserRepository;
import org.example.security.CustomUserDetails;
import org.example.skills.enums.ERole;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<String> getUserPermissions(Long userId, Long companyId) {

        User user = getCompanyScopedUser(userId, companyId);

        List<Long> permIds = userPermissionRepository.findByUserId(user.getId())
                .stream()
                .map(UserPermission::getPermissionId)
                .toList();

        if (permIds.isEmpty()) return List.of();

        return permissionRepository.findAllById(permIds)
                .stream()
                .map(Permission::getCode)
                .toList();
    }

    @Transactional
    public void replaceUserPermissions(Long userId, List<String> permissions, Long companyId) {

        User user = getCompanyScopedUser(userId, companyId);

        userPermissionRepository.deleteByUserId(user.getId());
        userPermissionRepository.flush();

        if (permissions == null || permissions.isEmpty()) {
            return;
        }

        for (String code : permissions) {
            Permission p = permissionRepository.findByCode(code)
                    .orElseThrow(() -> new KasappException(ErrorType.PERMISSION_NOT_FOUND));

            UserPermission up = new UserPermission();
            up.setUserId(user.getId());
            up.setPermissionId(p.getId());

            userPermissionRepository.save(up);
        }
    }

    @Transactional(readOnly = true)
    public List<User> getAllProfiles(Long companyId) {
        return userRepository.findAllByCompanyIdAndActiveTrue(companyId);
    }

    @Transactional
    public void deactivateUser(Long targetUserId, Long currentUserId, Long companyId) {

        User user = getCompanyScopedUser(targetUserId, companyId);

        long adminCount = userRepository.countByCompanyIdAndRole(companyId, ERole.ADMIN);

        if (user.getId().equals(currentUserId)) {
            throw new KasappException(ErrorType.INVALID_TRANSACTION);
        }

        if (!user.isActive()) {
            throw new KasappException(ErrorType.USER_INACTIVE);
        }

        if (user.getRole() == ERole.ADMIN && adminCount <= 1) {
            throw new KasappException(ErrorType.LAST_ADMIN_CANNOT_BE_DELETED);
        }

        user.setActive(false);
    }

    @Transactional
    public void updateUserRole(Long userId, String role, Long currentUserId, Long companyId) {

        User user = getCompanyScopedUser(userId, companyId);

        ERole newRole = ERole.valueOf(role);

        long adminCount = userRepository.countByCompanyIdAndRole(companyId, ERole.ADMIN);

        if (user.getId().equals(currentUserId) && newRole == ERole.USER) {
            throw new KasappException(ErrorType.CANNOT_CHANGE_OWN_ROLE);
        }

        if (user.getRole() == ERole.ADMIN
                && newRole == ERole.USER
                && adminCount <= 1) {

            throw new KasappException(ErrorType.LAST_ADMIN_CANNOT_BE_CHANGED);
        }

        user.setRole(newRole);
        userRepository.save(user);
    }

    @Transactional
    public void createSubUser(AdminCreateUserRequest request, CustomUserDetails currentUser) {

        User adminUser = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new KasappException(ErrorType.ADMIN_NOT_FOUND));

        if (adminUser.getRole() != ERole.ADMIN) {
            throw new KasappException(ErrorType.PERMISSION_NOT_FOUND);
        }

        if (!adminUser.isActive()) {
            throw new KasappException(ErrorType.USER_INACTIVE);
        }

        boolean usernameExists = userRepository
                .findByCompanyIdAndUsername(adminUser.getCompanyId(), request.username())
                .isPresent();

        if (usernameExists) {
            throw new KasappException(ErrorType.USER_ALREADY_EXISTS);
        }

        if (request.email() != null && !request.email().isBlank()) {
            if (userRepository.existsByEmail(request.email())) {
                throw new KasappException(ErrorType.EMAIL_ALREADY_EXISTS);
            }
        }

        User user = new User();
        user.setCompanyId(adminUser.getCompanyId());
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role() != null ? request.role() : ERole.USER);
        user.setActive(true);
        user.setName(request.name());
        user.setSurname(request.surname());
        user.setEmail(request.email());
        user.setPhone(request.phone());

        userRepository.save(user);
    }

    private User getCompanyScopedUser(Long targetUserId, Long companyId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new KasappException(ErrorType.USER_NOT_FOUND));

        if (!user.getCompanyId().equals(companyId)) {
            throw new KasappException(ErrorType.USER_NOT_FOUND);
        }

        return user;
    }
}