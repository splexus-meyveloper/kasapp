package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.entity.Permission;
import org.example.entity.User;
import org.example.entity.UserPermission;
import org.example.exception.ErrorType;
import org.example.exception.KasappException;
import org.example.repository.PermissionRepository;
import org.example.repository.UserPermissionRepository;
import org.example.repository.UserRepository;
import org.example.skills.enums.ERole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final UserPermissionRepository userPermissionRepository;

    @Transactional(readOnly = true)
    public List<String> getUserPermissions(Long userId) {

        List<Long> permIds = userPermissionRepository.findByUserId(userId)
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
    public void replaceUserPermissions(Long userId, List<String> permissions) {

        // 🔥 1️⃣ Önce HER ŞEYİ SİL
        userPermissionRepository.deleteByUserId(userId);

        // (opsiyonel ama sağlamcı)
        userPermissionRepository.flush();

        // 2️⃣ Liste boşsa -> kullanıcı yetkisiz kalır
        if (permissions == null || permissions.isEmpty()) {
            return;
        }

        // 3️⃣ Yeniden ekle
        for (String code : permissions) {
            Permission p = permissionRepository.findByCode(code)
                    .orElseThrow(() -> new KasappException(ErrorType.PERMISSION_NOT_FOUND));

            UserPermission up = new UserPermission();
            up.setUserId(userId);
            up.setPermissionId(p.getId());

            userPermissionRepository.save(up);
        }
    }




    @Transactional(readOnly = true)
    public List<User> getAllProfiles(Long companyId) {
        return userRepository
                .findAllByCompanyIdAndActiveTrue(companyId);
    }

    @Transactional
    public void deactivateUser(Long targetUserId,
                               Long currentUserId){

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() ->
                        new KasappException(ErrorType.USER_NOT_FOUND));

        long adminCount = userRepository.countByRole(ERole.ADMIN);

        // ❌ kendini silemezsin
        if (user.getId().equals(currentUserId)) {
            throw new KasappException(ErrorType.INVALID_TRANSACTION);
        }

        // ❌ zaten pasif
        if (!user.isActive()) {
            throw new KasappException(ErrorType.USER_INACTIVE);
        }

        // ❌ son admin silinemez
        if (user.getRole() == ERole.ADMIN && adminCount <= 1) {
            throw new KasappException(ErrorType.LAST_ADMIN_CANNOT_BE_DELETED);
        }

        user.setActive(false);
    }

    public void updateUserRole(Long userId, String role, Long currentUserId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new KasappException(ErrorType.USER_NOT_FOUND));

        ERole newRole = ERole.valueOf(role);

        long adminCount = userRepository.countByRole(ERole.ADMIN);

        // ❌ kendini USER yapamazsın
        if (user.getId().equals(currentUserId) && newRole == ERole.USER) {
            throw new KasappException(ErrorType.CANNOT_CHANGE_OWN_ROLE);
        }

        // ❌ son admin USER yapılamaz
        if (user.getRole() == ERole.ADMIN
                && newRole == ERole.USER
                && adminCount <= 1) {

            throw new KasappException(ErrorType.LAST_ADMIN_CANNOT_BE_CHANGED);
        }

        user.setRole(newRole);

        userRepository.save(user);
    }

}

