package com.ecommerce.security.jwt;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

/**
 * JWTs can't be "deleted" on the server (they're stateless), so to support logout
 * we keep a simple in-memory blacklist of tokens that should no longer be accepted.
 *
 * NOTE: In production, use Redis instead of a Java Set so the blacklist survives
 * restarts and works across multiple server instances. This in-memory version is
 * just for learning/demo purposes.
 */
@Service
public class TokenBlacklistService {

    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();

    public void blacklist(String token) {
        blacklistedTokens.add(token);
    }

    public boolean isBlacklisted(String token) {
        return blacklistedTokens.contains(token);
    }
}
