package com.site.meetingandclass.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks failed login attempts per key (email, lower-cased) and locks the
 * account for a configurable window after too many failures.
 *
 * In-memory only — fine for a single-instance student project, but should be
 * replaced with Redis/DB for clustered deployments.
 */
@Service
public class LoginAttemptService {

    private static class Entry {
        int failures;
        Instant lockedUntil; // nullable
    }

    private final Map<String, Entry> attempts = new ConcurrentHashMap<>();

    @Value("${app.security.login.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.security.login.lock-duration-minutes:15}")
    private int lockDurationMinutes;

    private String key(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    public boolean isLocked(String email) {
        Entry e = attempts.get(key(email));
        if (e == null || e.lockedUntil == null) return false;
        if (Instant.now().isAfter(e.lockedUntil)) {
            // lock expired — reset
            attempts.remove(key(email));
            return false;
        }
        return true;
    }

    public synchronized void loginFailed(String email) {
        Entry e = attempts.computeIfAbsent(key(email), k -> new Entry());
        e.failures++;
        if (e.failures >= maxAttempts) {
            e.lockedUntil = Instant.now().plus(Duration.ofMinutes(lockDurationMinutes));
        }
    }

    public void loginSucceeded(String email) {
        attempts.remove(key(email));
    }

    public int getMaxAttempts() { return maxAttempts; }
    public int getLockDurationMinutes() { return lockDurationMinutes; }
}
