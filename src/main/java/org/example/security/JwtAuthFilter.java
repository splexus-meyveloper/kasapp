package org.example.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.skills.Jwt.JwtService;
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

            String username = jwt.getSubject();
            Long userId = jwt.getClaim("userId").asLong();
            Long companyId = jwt.getClaim("companyId").asLong();
            String role = jwt.getClaim("role").asString();
            List<String> permissions =
                    jwt.getClaim("permissions").asList(String.class);

            // Authorities oluştur
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();

            if (role != null) {
                authorities.add(
                        new SimpleGrantedAuthority("ROLE_" + role)
                );
            }

            if (permissions != null) {
                permissions.forEach(p ->
                        authorities.add(new SimpleGrantedAuthority(p))
                );
            }

            // CustomUserDetails oluştur
            CustomUserDetails userDetails =
                    new CustomUserDetails(
                            userId,
                            companyId,
                            username,
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
