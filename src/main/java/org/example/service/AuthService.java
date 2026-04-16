package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.config.PasswordConfig;
import org.example.dto.request.LoginRequest;
import org.example.dto.request.RegisterRequest;
import org.example.dto.response.LoginResponse;
import org.example.dto.response.RegisterResponse;
import org.example.entity.Company;
import org.example.entity.Permission;
import org.example.entity.User;
import org.example.entity.UserPermission;
import org.example.exception.ErrorType;
import org.example.exception.KasappException;
import org.example.repository.CompanyRepository;
import org.example.repository.PermissionRepository;
import org.example.repository.UserPermissionRepository;
import org.example.repository.UserRepository;
import org.example.security.CustomUserDetails;
import org.example.skills.Jwt.JwtService;
import org.example.skills.enums.ERole;
import org.example.util.CompanyCodeGenerator;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final PermissionRepository permissionRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final CompanyCodeGenerator companyCodeGenerator;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PasswordConfig passwordConfig;

    // 🔐 LOGIN (companyCode + username)
    public LoginResponse login(LoginRequest request) {

        // 1️⃣ Company bul
        Company company = companyRepository.findByCode(request.companyCode())
                .orElseThrow(() -> new KasappException(ErrorType.COMPANY_NOT_FOUND));

        // 2️⃣ Kullanıcıyı bul
        User user = userRepository
                .findByCompanyIdAndUsername(company.getId(), request.username())
                .orElseThrow(() -> new KasappException(ErrorType.USER_NOT_FOUND));

        if (!user.isActive()){
            throw new KasappException(ErrorType.USER_INACTIVE);
        }

        // 3️⃣ Şifre kontrolü
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new KasappException(ErrorType.INVALID_USERNAME_OR_PASSWORD);
        }

        // 4️⃣ Yetkileri çek
        List<String> permissions = getUserPermissions(user);

        // 5️⃣ JWT üret
        String token = jwtService.generateToken(user, permissions);

        // 6️⃣ Response
        return new LoginResponse(
                token,
                user.getUsername(),
                user.getRole(),
                permissions
        );
    }

    // 🔑 PERMISSIONS
    private List<String> getUserPermissions(User user) {

        // 🔥 ADMIN ise tüm permissionları ver
        if (user.getRole() == ERole.ADMIN) {
            return permissionRepository.findAll()
                    .stream()
                    .map(Permission::getCode)
                    .toList();
        }

        List<UserPermission> perms = userPermissionRepository.findByUserId(user.getId());

        List<Long> menuIds = perms.stream()
                .map(UserPermission::getPermissionId)
                .toList();

        if (menuIds.isEmpty()) {
            return List.of();
        }

        return permissionRepository.findAllById(menuIds)
                .stream()
                .map(Permission::getCode)
                .toList();
    }

    // 🏢 İLK KAYIT → COMPANY + ADMIN
    public RegisterResponse registerCompany(RegisterRequest request) {

        // Email kontrol
        if (userRepository.existsByEmail(request.email())) {
            throw new KasappException(ErrorType.EMAIL_ALREADY_EXISTS);
        }

        // Company code üret
        String code;
        do {
            code = companyCodeGenerator.generateCode();
        } while (companyRepository.existsByCode(code));

        // Company oluştur
        Company company = Company.builder()
                .name(request.companyName())
                .code(code)
                .createdAt(LocalDateTime.now())
                .build();

        companyRepository.save(company);

        // Admin user oluştur
        User user = new User();
        user.setCompanyId(company.getId());
        user.setUsername(request.username());
        user.setName(request.name());
        user.setSurname(request.surname());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setRole(ERole.ADMIN);
        user.setActive(true);
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        userRepository.save(user);

        return new RegisterResponse(
                "Kayıt başarılı",
                code,
                request.username()
        );
    }
}