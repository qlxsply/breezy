package com.corwin.system.auth.infrastructure.security;

import com.corwin.framework.util.SignUtil;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * @author Corwin 2026/4/19
 */
@Component
public class OpaqueTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    public String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hash(String token) {
        return SignUtil.sha256(token);
    }

}
