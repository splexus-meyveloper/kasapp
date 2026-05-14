package org.example.skills.DataInitializer;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.example.entity.Company;
import org.example.entity.Permission;
import org.example.entity.User;
import org.example.entity.UserPermission;
import org.example.repository.CompanyRepository;
import org.example.repository.PermissionRepository;
import org.example.repository.UserPermissionRepository;
import org.example.repository.UserRepository;
import org.example.skills.enums.BranchType;
import org.example.skills.enums.ERole;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Objects;

@Component
@Profile("dev")
@DependsOn("permissionMigrator")
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PermissionRepository permissionRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void init() {
        Company merkez = ensureCompany(
                "BURSA",
                "Bursa Merkez",
                BranchType.MERKEZ,
                null
        );

        Company sube = ensureCompany(
                "ADAPAZARI",
                "Adapazari Sube",
                BranchType.SUBE,
                merkez.getId()
        );

        ensureAdminUser("admin", "1234", merkez.getId(), "Merkez admin olusturuldu: BURSA / admin / 1234");
        ensureAdminUser("admin2", "1234", sube.getId(), "Sube admin olusturuldu: ADAPAZARI / admin2 / 1234");
    }

    private Company ensureCompany(String code, String name, BranchType branchType, Long parentCompanyId) {
        Company company = companyRepository.findByCode(code)
                .orElseGet(() -> companyRepository.save(Company.builder()
                        .code(code)
                        .name(name)
                        .branchType(branchType)
                        .parentCompanyId(parentCompanyId)
                        .createdAt(LocalDateTime.now())
                        .build()));

        boolean changed = false;
        if (company.getBranchType() != branchType) {
            company.setBranchType(branchType);
            changed = true;
        }
        if (!Objects.equals(company.getParentCompanyId(), parentCompanyId)) {
            company.setParentCompanyId(parentCompanyId);
            changed = true;
        }
        if (company.getName() == null || company.getName().isBlank()) {
            company.setName(name);
            changed = true;
        }

        return changed ? companyRepository.save(company) : company;
    }

    private void ensureAdminUser(String username, String password, Long companyId, String logMessage) {
        if (userRepository.findByUsername(username).isPresent()) {
            return;
        }

        User admin = new User();
        admin.setUsername(username);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole(ERole.ADMIN);
        admin.setCompanyId(companyId);
        admin.setActive(true);
        userRepository.save(admin);

        grantAllPermissions(admin);
        System.out.println(logMessage);
    }

    private void grantAllPermissions(User user) {
        for (Permission perm : permissionRepository.findAll()) {
            UserPermission up = new UserPermission();
            up.setUserId(user.getId());
            up.setPermissionId(perm.getId());
            userPermissionRepository.save(up);
        }
    }
}
