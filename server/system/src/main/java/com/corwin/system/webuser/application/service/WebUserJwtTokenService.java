package com.corwin.system.webuser.application.service;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.web.auth.AuthPrincipal;
import com.corwin.system.auth.application.service.AuthConfigService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Set;

/**
 * @author Corwin 2026/5/11
 */
@Component
public class WebUserJwtTokenService {

    private final AuthConfigService authConfigService;

    public WebUserJwtTokenService(AuthConfigService authConfigService) {
        this.authConfigService = authConfigService;
    }

    public String issue(AuthPrincipal principal, long tokenVersion) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(authConfigService.externalAccessTokenTtl());
        return Jwts.builder()
                .subject(String.valueOf(principal.userId()))
                .issuer(authConfigService.externalJwtIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim("uid", principal.userId())
                .claim("account", principal.username())
                .claim("type", principal.userType().name())
                .claim("ver", tokenVersion)
                .claim("perms", principal.permissionCodes().stream().sorted().toList())
                .signWith(secretKey())
                .compact();
    }

    public WebUserJwtPayload parse(String token) {
        Claims claims = Jwts.parser().verifyWith(secretKey()).build().parseSignedClaims(token).getPayload();
        Long userId = claims.get("uid", Long.class);
        String account = claims.get("account", String.class);
        String type = claims.get("type", String.class);
        Long version = claims.get("ver", Long.class);
        return new WebUserJwtPayload(userId, account, UserType.valueOf(type), version,
                claims.getIssuedAt() == null ? null : claims.getIssuedAt().toInstant(),
                claims.getExpiration() == null ? null : claims.getExpiration().toInstant());
    }

    public record WebUserJwtPayload(
            Long userId,
            String account,
            UserType userType,
            Long tokenVersion,
            Instant issuedAt,
            Instant expiresAt
    ) {
    }

    private SecretKey secretKey() {
        byte[] secret = authConfigService.externalJwtSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(secret);
    }
}
