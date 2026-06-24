package org.example.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.entity.User;
import org.example.entity.UserPermission;
import org.example.repository.PermissionRepository;
import org.example.repository.UserPermissionRepository;
import org.example.repository.UserRepository;
import org.example.skills.Jwt.JwtService;
import org.example.skills.enums.ERole;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final PermissionRepository permissionRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Token yoksa geç
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {

            // JWT doğrula
            DecodedJWT jwt = jwtService.verify(token);

            Long userId = jwt.getClaim("userId").asLong();

            // Kullanıcıyı DB'den çek — aktif değilse reddet
            User dbUser = userRepository.findById(userId)
                    .filter(User::isActive)
                    .orElseThrow(() -> new RuntimeException("Inactive or missing user"));

            String username = dbUser.getUsername();
            Long companyId = dbUser.getCompanyId();
            String role = dbUser.getRole().name();

            // Yetkileri her istekte DB'den oku — JWT'deki eski yetki kullanılmaz
            List<String> permissions;
            if (dbUser.getRole() == ERole.ADMIN) {
                permissions = permissionRepository.findAll()
                        .stream()
                        .map(p -> p.getCode())
                        .toList();
            } else {
                List<Long> permIds = userPermissionRepository.findByUserId(userId)
                        .stream()
                        .map(UserPermission::getPermissionId)
                        .toList();
                permissions = permIds.isEmpty()
                        ? List.of()
                        : permissionRepository.findAllById(permIds)
                                .stream()
                                .map(p -> p.getCode())
                                .toList();
            }

            // Authorities oluştur
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();

            if (role != null) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }

            permissions.forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));

            // CustomUserDetails oluştur
            CustomUserDetails userDetails =
                    new CustomUserDetails(
                            userId,
                            companyId,
                            username,
                            role,
                            authorities
                    );

            // Authentication oluştur
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            authorities
                    );

            // SecurityContext’e koy
            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (Exception ex) {

            SecurityContextHolder.clearContext();

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter()
                    .write("{\"error\":\"Invalid or expired token\"}");
        }
    }
}
