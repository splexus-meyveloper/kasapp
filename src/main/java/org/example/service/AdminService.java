package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dto.request.GetProfileRequest;
import org.example.dto.request.SetPermissionsRequest;
import org.example.dto.response.GetProfileResponse;
import org.example.entity.Permission;
import org.example.entity.User;
import org.example.entity.UserPermission;
import org.example.exception.ErrorType;
import org.example.exception.KasappException;
import org.example.repository.PermissionRepository;
import org.example.repository.UserPermissionRepository;
import org.example.repository.UserRepository;
import org.example.security.JwtAuthFilter;
import org.example.skills.AuthUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
    public List<User> getAllProfiles() {

        Long companyId = AuthUtil.getCompanyId();

        return userRepository.findAllByCompanyIdAndActiveTrue(companyId);
    }

    @Transactional
    public void deactivateUser(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new KasappException(ErrorType.USER_NOT_FOUND));

        Long currentUserId = AuthUtil.getUserId();

        if (user.getId().equals(currentUserId)) {
            throw new KasappException(ErrorType.INVALID_TRANSACTION);
        }

        if (!user.isActive()) {
            throw new KasappException(ErrorType.USER_INACTIVE);
        }

        user.setActive(false);
    }

}

