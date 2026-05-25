package com.site.meetingandclass.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory JWT denylist used for logout. Stores the JWT id (jti) until its
 * original expiration, after which it can be safely forgotten because the
 * token is already invalid by signature.
 *
 * NOTE: Single-instance only. For multi-node deployments swap this for Redis
 * or a database-backed implementation behind the same interface.
 */
@Component
public class TokenBlacklist {

    /** jti -> expiration millis */
    private final Map<String, Long> revoked = new ConcurrentHashMap<>();

    public void revoke(String jti, long expirationEpochMs) {
        if (jti != null) {
            revoked.put(jti, expirationEpochMs);
        }
    }

    public boolean isRevoked(String jti) {
        if (jti == null) return false;
        Long exp = revoked.get(jti);
        if (exp == null) return false;
        if (exp < System.currentTimeMillis()) {
            revoked.remove(jti);
            return false;
        }
        return true;
    }

    /** Clean expired entries every 10 minutes to bound memory. */
    @Scheduled(fixedRate = 10 * 60 * 1000L)
    public void purgeExpired() {
        long now = System.currentTimeMillis();
        revoked.entrySet().removeIf(e -> e.getValue() < now);
    }
}
