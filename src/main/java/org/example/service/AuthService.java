package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.config.PasswordConfig;
import org.example.dto.request.LoginRequest;
import org.example.dto.request.RegisterRequest;
import org.example.dto.response.LoginResponse;
import org.example.entity.Permission;
import org.example.entity.User;
import org.example.entity.UserPermission;
import org.example.exception.ErrorType;
import org.example.exception.KasappException;
import org.example.mapper.UserMapper;
import org.example.repository.PermissionRepository;
import org.example.repository.UserPermissionRepository;
import org.example.repository.UserRepository;
import org.example.skills.AuthUtil;
import org.example.skills.Jwt.JwtService;
import org.example.skills.enums.ERole;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final PermissionRepository permissionRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PasswordConfig passwordConfig;

    public LoginResponse login(LoginRequest request) {

        // 1️⃣ Kullanıcıyı bul
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new KasappException(ErrorType.USER_NOT_FOUND));

        if (!user.isActive()){
            throw new KasappException(ErrorType.USER_INACTIVE);
        }

        // 2️⃣ Şifre kontrolü
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new KasappException(ErrorType.INVALID_USERNAME_OR_PASSWORD);
        }

        // 3️⃣ Kullanıcının yetkilerini DB'den çek
        List<String> permissions = getUserPermissions(user);

        // 4️⃣ JWT üret (ARTIK permission + companyId + userId içeriyor)
        String token = jwtService.generateToken(user, permissions);

        // 5️⃣ Response dön
        return new LoginResponse(
                token,
                user.getUsername(),
                user.getRole(),
                permissions
        );
    }

    private List<String> getUserPermissions(User user) {

        // ADMIN bile olsa DB’den alıyoruz (gerçek sistem)
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

    public void register(RegisterRequest request){

        // 🔐 Sisteme giriş yapan kullanıcıyı JWT'den al
        Long adminUserId = AuthUtil.getUserId();

        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new KasappException(ErrorType.ADMIN_NOT_FOUND));

        // 🔴 YETKİ KONTROLÜ
        if (adminUser.getRole() != ERole.ADMIN) {
            throw new KasappException(ErrorType.PERMISSION_NOT_FOUND);
        }

        // ✅ Yeni kullanıcı oluştur
        User user = new User();
        user.setUsername(request.username());
        user.setCompanyId(adminUser.getCompanyId());   // 🔥 Aynı firmaya ekler
        user.setRole(ERole.USER);                       // 🔥 ZORUNLU
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        userRepository.save(user);
    }

}
