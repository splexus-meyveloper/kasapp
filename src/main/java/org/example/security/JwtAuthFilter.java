package org.example.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 🔴🔴🔴 BURASI
        System.out.println(">>> JWT FILTER WORKING <<<");

        String auth = request.getHeader("Authorization");

        if (auth == null || !auth.startsWith("Bearer ")) {
            System.out.println(">>> NO AUTH HEADER <<<");
            filterChain.doFilter(request, response);
            return;
        }

        String token = auth.substring(7);

        try {
            DecodedJWT jwt = jwtService.verify(token);

            String username = jwt.getSubject();
            Long userId = jwt.getClaim("userId").asLong();
            Long companyId = jwt.getClaim("companyId").asLong();
            String role = jwt.getClaim("role").asString();

            List<String> permissions = jwt.getClaim("permissions").asList(String.class);

            System.out.println("JWT USER = " + username);
            System.out.println("JWT ROLE = " + role);
            System.out.println("JWT PERMS = " + permissions);

            List<SimpleGrantedAuthority> authorities = new ArrayList<>();

            // ROLE
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));

            // PERMISSIONS
            if (permissions != null) {
                for (String p : permissions) {
                    System.out.println("ADDING PERMISSION = " + p);
                    authorities.add(new SimpleGrantedAuthority(p));
                }
            }

            System.out.println("FINAL AUTHORITIES = " + authorities);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            authorities
                    );

            request.setAttribute("userId", userId);
            request.setAttribute("companyId", companyId);

            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (Exception ex) {
            ex.printStackTrace();

            SecurityContextHolder.clearContext();
            response.setStatus(401);
            response.setContentType("application/json; charset=utf-8");
            response.getWriter().write("{\"message\":\"Unauthorized\"}");
        }
    }
}
