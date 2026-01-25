package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dto.request.SetPermissionsRequest;
import org.example.entity.Permission;
import org.example.entity.User;
import org.example.entity.UserPermission;
import org.example.repository.PermissionRepository;
import org.example.repository.UserPermissionRepository;
import org.example.repository.UserRepository;
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
    public void setUserPermissions(Long userId, SetPermissionsRequest req) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User bulunamadı"));

        // replace mantığı: önce sil sonra ekle
        userPermissionRepository.deleteByUserId(userId);

        if (req == null || req.permissions() == null) return;

        for (String code : req.permissions()) {
            Permission p = permissionRepository.findByCode(code)
                    .orElseThrow(() -> new RuntimeException("Permission yok: " + code));

            UserPermission up = new UserPermission();
            up.setUserId(user.getId());
            up.setPermissionId(p.getId());
            userPermissionRepository.save(up);
        }
    }
}
