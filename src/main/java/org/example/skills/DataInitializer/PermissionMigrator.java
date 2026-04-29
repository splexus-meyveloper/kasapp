package org.example.skills.DataInitializer;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.example.entity.Permission;
import org.example.repository.PermissionRepository;
import org.example.skills.enums.EPermission;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Tüm ortamlarda (dev + prod) çalışır.
 * EPermission enum'undaki tüm değerlerin DB'de var olmasını garantiler.
 * Yeni permission eklendiğinde otomatik INSERT eder, mevcutlara dokunmaz.
 */
@Component
@RequiredArgsConstructor
public class PermissionMigrator {

    private final PermissionRepository permissionRepository;

    @PostConstruct
    public void ensurePermissionsExist() {
        Arrays.stream(EPermission.values()).forEach(p ->
                permissionRepository.findByCode(p.name()).orElseGet(() -> {
                    Permission perm = new Permission();
                    perm.setCode(p.name());
                    Permission saved = permissionRepository.save(perm);
                    System.out.println("✅ Yeni permission eklendi: " + p.name());
                    return saved;
                })
        );
    }
}