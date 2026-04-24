package org.example.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {

    // Login limitleri
    private static final int LOGIN_MAX_ATTEMPTS = 5;
    private static final long LOGIN_BLOCK_SECONDS = 300; // 5 dakika

    // Register limitleri (daha sıkı)
    private static final int REGISTER_MAX_ATTEMPTS = 3;
    private static final long REGISTER_BLOCK_SECONDS = 3600; // 1 saat

    private record AttemptInfo(int count, Instant blockedUntil) {}

    private final Map<String, AttemptInfo> loginAttempts    = new ConcurrentHashMap<>();
    private final Map<String, AttemptInfo> registerAttempts = new ConcurrentHashMap<>();

    // ── LOGIN ─────────────────────────────────────────────────────────

    public boolean isLoginBlocked(String ip) {
        return isBlocked(ip, loginAttempts);
    }

    public void recordLoginFailure(String ip) {
        recordFailure(ip, loginAttempts, LOGIN_MAX_ATTEMPTS, LOGIN_BLOCK_SECONDS);
    }

    public void recordLoginSuccess(String ip) {
        loginAttempts.remove(ip);
    }

    public long getLoginSecondsUntilUnblock(String ip) {
        return getSecondsUntilUnblock(ip, loginAttempts);
    }

    // ── REGISTER ──────────────────────────────────────────────────────

    public boolean isRegisterBlocked(String ip) {
        return isBlocked(ip, registerAttempts);
    }

    public void recordRegisterAttempt(String ip) {
        recordFailure(ip, registerAttempts, REGISTER_MAX_ATTEMPTS, REGISTER_BLOCK_SECONDS);
    }

    public long getRegisterSecondsUntilUnblock(String ip) {
        return getSecondsUntilUnblock(ip, registerAttempts);
    }

    // ── Ortak yardımcılar ─────────────────────────────────────────────

    private boolean isBlocked(String ip, Map<String, AttemptInfo> map) {
        AttemptInfo info = map.get(ip);
        if (info == null) return false;

        if (info.blockedUntil() != null && Instant.now().isBefore(info.blockedUntil())) {
            return true;
        }

        if (info.blockedUntil() != null && Instant.now().isAfter(info.blockedUntil())) {
            map.remove(ip);
        }

        return false;
    }

    private void recordFailure(String ip, Map<String, AttemptInfo> map,
                               int maxAttempts, long blockSeconds) {
        AttemptInfo current = map.getOrDefault(ip, new AttemptInfo(0, null));
        int newCount = current.count() + 1;

        if (newCount >= maxAttempts) {
            map.put(ip, new AttemptInfo(newCount,
                    Instant.now().plusSeconds(blockSeconds)));
        } else {
            map.put(ip, new AttemptInfo(newCount, null));
        }
    }

    private long getSecondsUntilUnblock(String ip, Map<String, AttemptInfo> map) {
        AttemptInfo info = map.get(ip);
        if (info == null || info.blockedUntil() == null) return 0;
        long remaining = info.blockedUntil().getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(remaining, 0);
    }
}