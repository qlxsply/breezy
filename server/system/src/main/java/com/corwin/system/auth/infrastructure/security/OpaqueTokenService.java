package com.corwin.system.auth.infrastructure.security;

import com.corwin.framework.util.SignUtil;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Service for generating cryptographically secure opaque tokens and computing
 * their SHA-256 hashes for secure storage.
 *
 * @author Corwin 2026/4/19
 */
@Component
public class OpaqueTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generates a random 32-byte token encoded as a URL-safe Base64 string.
     *
     * @return the generated opaque token
     */
    public String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Computes the SHA-256 hash of the given token for secure lookups.
     *
     * @param token the raw token to hash
     * @return the hex-encoded SHA-256 hash
     */
    public String hash(String token) {
        return SignUtil.sha256(token);
    }

}
