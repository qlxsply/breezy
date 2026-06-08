package com.corwin.system.auth.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 * 用户 Refresh Token
 *
 * @author Corwin 2026/4/19
 */
@Getter
@Entity
@Table(name = "sys_refresh_token", indexes = {@Index(name = "idx_sys_refresh_token_user_id", columnList = "user_id"),
        @Index(name = "idx_sys_refresh_token_user_status", columnList = "user_id,status"),
        @Index(name = "idx_sys_refresh_token_token_id", columnList = "refresh_token_id", unique = true),
        @Index(name = "idx_sys_refresh_token_hash", columnList = "refresh_token_hash", unique = true),
        @Index(name = "idx_sys_refresh_token_expires_at", columnList = "expires_at")})
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "refresh_token_id", nullable = false, length = 64)
    private String refreshTokenId;

    @Column(name = "refresh_token_hash", nullable = false, length = 128)
    private String refreshTokenHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private RefreshTokenStatus status;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_reason", length = 512)
    private String revokedReason;

    @Column(name = "client_info", length = 1024)
    private String clientInfo;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RefreshToken() {
    }

    public RefreshToken(Long userId, String refreshTokenId, String refreshTokenHash, Instant expiresAt,
            String clientInfo) {
        Instant now = HighDate.mockInstant();
        this.userId = userId;
        this.refreshTokenId = refreshTokenId;
        this.refreshTokenHash = refreshTokenHash;
        this.status = RefreshTokenStatus.ACTIVE;
        this.issuedAt = now;
        this.expiresAt = expiresAt;
        this.clientInfo = clientInfo;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public boolean active() {
        return status == RefreshTokenStatus.ACTIVE;
    }

    public boolean expired(Instant now) {
        return expiresAt.isBefore(now);
    }

    public void rotate() {
        this.status = RefreshTokenStatus.ROTATED;
        touch();
    }

    public void revoke(String reason) {
        Instant now = HighDate.mockInstant();
        this.status = RefreshTokenStatus.REVOKED;
        this.revokedAt = now;
        this.revokedReason = reason;
        this.updatedAt = now;
    }

    public void markExpired() {
        this.status = RefreshTokenStatus.EXPIRED;
        touch();
    }

    private void touch() {
        this.updatedAt = HighDate.mockInstant();
    }
}
