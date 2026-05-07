package org.example.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class DuplicateSubmissionFilter extends OncePerRequestFilter {

    private static final long DUPLICATE_WINDOW_MILLIS = 2_000;
    private static final long CLEANUP_AFTER_MILLIS = 10_000;

    private final Map<String, Long> recentRequests = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!shouldGuard(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        CachedBodyHttpServletRequest wrapped = new CachedBodyHttpServletRequest(request);
        String key = buildKey(wrapped);
        long now = Instant.now().toEpochMilli();
        cleanup(now);

        Long previous = recentRequests.putIfAbsent(key, now);
        if (previous != null && now - previous < DUPLICATE_WINDOW_MILLIS) {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            response.setContentType("application/json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"message\":\"Ayni istek cok kisa sure icinde tekrar gonderildi.\"}");
            return;
        }

        recentRequests.put(key, now);
        filterChain.doFilter(wrapped, response);
    }

    private boolean shouldGuard(HttpServletRequest request) {
        String method = request.getMethod();
        if (!("POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method))) {
            return false;
        }

        String uri = request.getRequestURI();
        if (uri == null || !uri.startsWith("/api/") || uri.startsWith("/api/auth/")) {
            return false;
        }

        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().contains("application/json");
    }

    private String buildKey(CachedBodyHttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String principal = authentication != null ? authentication.getName() : "anonymous";
        String query = request.getQueryString() == null ? "" : request.getQueryString();
        return principal + "|" + request.getMethod() + "|" + request.getRequestURI() + "|" + query + "|" + sha256(request.getCachedBody());
    }

    private String sha256(byte[] body) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(body));
        } catch (Exception e) {
            return Integer.toHexString(java.util.Arrays.hashCode(body));
        }
    }

    private void cleanup(long now) {
        Iterator<Map.Entry<String, Long>> iterator = recentRequests.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (now - entry.getValue() > CLEANUP_AFTER_MILLIS) {
                iterator.remove();
            }
        }
    }
}
