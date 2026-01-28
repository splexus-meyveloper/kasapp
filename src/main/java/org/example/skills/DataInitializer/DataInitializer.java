package org.example.skills.DataInitializer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.example.entity.Permission;
import org.example.entity.User;
import org.example.entity.UserPermission;
import org.example.repository.PermissionRepository;
import org.example.repository.UserPermissionRepository;
import org.example.repository.UserRepository;
import org.example.skills.enums.EPermission;
import org.example.skills.enums.ERole;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void init() {

        // 1) Permission'ları oluştur
        Arrays.stream(EPermission.values()).forEach(p -> {
            permissionRepository.findByCode(p.name()).orElseGet(() -> {
                Permission perm = new Permission();
                perm.setCode(p.name());
                return permissionRepository.save(perm);
            });
        });

        // 2) Admin yoksa oluştur
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPasswordHash(passwordEncoder.encode("1234"));
            admin.setRole(ERole.ADMIN);
            admin.setCompanyId(1L); // ✅ BUNU EKLE
            admin.setActive(true);
            userRepository.save(admin);


            // 3) Admin'e tüm permissionları ata (DB üzerinden)
            for (Permission perm : permissionRepository.findAll()) {
                UserPermission up = new UserPermission();
                up.setUserId(admin.getId());
                up.setPermissionId(perm.getId());
                userPermissionRepository.save(up);
            }

            System.out.println("✅ Admin oluşturuldu: admin / 1234");
        }
    }
}
