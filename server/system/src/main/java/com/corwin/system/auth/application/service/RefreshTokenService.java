package com.corwin.system.auth.application.service;

import com.corwin.framework.error.BizException;
import com.corwin.framework.util.HighDate;
import com.corwin.system.auth.application.error.AuthError;
import com.corwin.system.auth.domain.model.RefreshToken;
import com.corwin.system.auth.domain.model.RefreshTokenStatus;
import com.corwin.system.auth.domain.repo.RefreshTokenRepository;
import com.corwin.system.auth.infrastructure.security.OpaqueTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Service for issuing, rotating, and revoking refresh tokens for external users.
 *
 * @author Corwin 2026/6/7
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final OpaqueTokenService opaqueTokenService;
    private final AuthConfigService authConfigService;

    /**
     * Issues a new refresh token for the given user.
     *
     * @param userId     the user ID
     * @param clientInfo optional client information
     * @return the issued refresh token details
     */
    @Transactional
    public IssuedRefreshToken issue(Long userId, String clientInfo) {
        Instant expiresAt = HighDate.mockInstant().plus(authConfigService.userRefreshTokenTtl());
        String rawToken = opaqueTokenService.generateToken();
        String tokenHash = opaqueTokenService.hash(rawToken);
        refreshTokenRepository.save(new RefreshToken(userId, newTokenId(), tokenHash, expiresAt, clientInfo));
        return new IssuedRefreshToken(userId, rawToken, expiresAt);
    }

    /**
     * Rotates an existing refresh token: revokes the old one and issues a new one.
     *
     * @param rawRefreshToken the raw (unhashed) current refresh token
     * @param clientInfo      optional client information
     * @return the newly issued refresh token details
     */
    @Transactional
    public IssuedRefreshToken rotate(String rawRefreshToken, String clientInfo) {
        RefreshToken current = requireActiveToken(rawRefreshToken);
        current.rotate();
        refreshTokenRepository.save(current);
        return issue(current.getUserId(), clientInfo);
    }

    /**
     * Revokes all active refresh tokens for a given user.
     *
     * @param userId the user ID
     * @param reason the revocation reason
     */
    @Transactional
    public void revokeActiveTokens(Long userId, String reason) {
        List<RefreshToken> activeTokens = refreshTokenRepository.findByUserIdAndStatus(userId, RefreshTokenStatus.ACTIVE);
        activeTokens.stream()
                .filter(Objects::nonNull)
                .forEach(token -> token.revoke(reason));
        if (!activeTokens.isEmpty()) {
            refreshTokenRepository.saveAll(activeTokens);
        }
    }

    private RefreshToken requireActiveToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new BizException(AuthError.INVALID_TOKEN);
        }
        String tokenHash = opaqueTokenService.hash(rawRefreshToken.trim());
        RefreshToken token = refreshTokenRepository.findByRefreshTokenHash(tokenHash)
                .orElseThrow(() -> new BizException(AuthError.INVALID_TOKEN));
        Instant now = HighDate.mockInstant();
        if (token.expired(now)) {
            token.markExpired();
            refreshTokenRepository.save(token);
            throw new BizException(AuthError.TOKEN_EXPIRED);
        }
        if (!token.active()) {
            if (token.getStatus() == RefreshTokenStatus.REVOKED || token.getStatus() == RefreshTokenStatus.ROTATED) {
                throw new BizException(AuthError.TOKEN_REVOKED);
            }
            throw new BizException(AuthError.INVALID_TOKEN);
        }
        return token;
    }

    private String newTokenId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Record representing a newly issued refresh token with its expiry time.
     */
    public record IssuedRefreshToken(
            Long userId,
            String rawToken,
            Instant expiresAt
    ) {
    }
}
