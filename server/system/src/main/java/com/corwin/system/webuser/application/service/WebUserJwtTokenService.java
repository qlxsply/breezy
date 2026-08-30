package com.corwin.system.webuser.application.service;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.util.HighDate;
import com.corwin.framework.web.auth.AuthPrincipal;
import com.corwin.system.auth.application.service.AuthConfigService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * Service for issuing and parsing JWT tokens for external web user authentication.
 *
 * @author Corwin 2026/5/11
 */
@Component
public class WebUserJwtTokenService {

  private final AuthConfigService authConfigService;

  public WebUserJwtTokenService(AuthConfigService authConfigService) {
    this.authConfigService = authConfigService;
  }

  /**
   * Issue a signed JWT access token for the given principal.
   *
   * @param principal the authenticated principal
   * @param tokenVersion the token version for revocation support
   * @return the issued token and its expiration time
   */
  public IssuedAccessToken issue(AuthPrincipal principal, long tokenVersion) {
    Instant now = HighDate.realInstant();
    Instant expiresAt = now.plus(authConfigService.userAccessTokenTtl());
    String token =
        Jwts.builder()
            .subject(String.valueOf(principal.userId()))
            .issuer(authConfigService.userJwtIssuer())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .claim("uid", principal.userId())
            .claim("account", principal.username())
            .claim("type", principal.userType().name())
            .claim("ver", tokenVersion)
            .claim("perms", principal.permissionCodes().stream().sorted().toList())
            .signWith(secretKey())
            .compact();
    return new IssuedAccessToken(token, expiresAt);
  }

  /**
   * Parse and verify a signed JWT access token.
   *
   * @param token the JWT string
   * @return the parsed payload
   */
  public WebUserJwtPayload parse(String token) {
    Claims claims =
        Jwts.parser().verifyWith(secretKey()).build().parseSignedClaims(token).getPayload();
    Long userId = claims.get("uid", Long.class);
    String account = claims.get("account", String.class);
    String type = claims.get("type", String.class);
    Long version = claims.get("ver", Long.class);
    return new WebUserJwtPayload(
        userId,
        account,
        UserType.valueOf(type),
        version,
        claims.getIssuedAt() == null ? null : claims.getIssuedAt().toInstant(),
        claims.getExpiration() == null ? null : claims.getExpiration().toInstant());
  }

  public record WebUserJwtPayload(
      Long userId,
      String account,
      UserType userType,
      Long tokenVersion,
      Instant issuedAt,
      Instant expiresAt) {}

  public record IssuedAccessToken(String token, Instant expiresAt) {}

  private SecretKey secretKey() {
    byte[] secret = authConfigService.userJwtSecret().getBytes(StandardCharsets.UTF_8);
    return Keys.hmacShaKeyFor(secret);
  }
}
