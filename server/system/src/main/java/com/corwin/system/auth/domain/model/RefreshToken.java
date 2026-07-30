package com.corwin.system.auth.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 * JPA entity representing a refresh token for external (web) users, with
 * support for rotation, expiry, and revocation lifecycle.
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
    /** Unique identifier for this refresh token record. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The user ID this refresh token belongs to. */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Unique identifier for the refresh token (for rotation tracking). */
    @Column(name = "refresh_token_id", nullable = false, length = 64)
    private String refreshTokenId;

    /** SHA-256 hash of the raw refresh token for secure lookups. */
    @Column(name = "refresh_token_hash", nullable = false, length = 128)
    private String refreshTokenHash;

    /** Current lifecycle status of this refresh token. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private RefreshTokenStatus status;

    /** Timestamp when this token was issued. */
    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    /** Timestamp after which this token expires. */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Timestamp of when this token was revoked, if applicable. */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    /** Reason for revocation, if applicable. */
    @Column(name = "revoked_reason", length = 512)
    private String revokedReason;

    /** Optional client information (e.g. device, user agent) at time of issuance. */
    @Column(name = "client_info", length = 1024)
    private String clientInfo;

    /** Timestamp when this record was created. */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Timestamp when this record was last updated. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RefreshToken() {
    }

    /**
     * Creates a new active refresh token.
     */
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

    /**
     * Returns whether this token is currently active.
     */
    public boolean active() {
        return status == RefreshTokenStatus.ACTIVE;
    }

    /**
     * Returns whether this token has expired relative to the given time.
     */
    public boolean expired(Instant now) {
        return expiresAt.isBefore(now);
    }

    /**
     * Rotates this token by marking its status as {@link RefreshTokenStatus#ROTATED}.
     */
    public void rotate() {
        this.status = RefreshTokenStatus.ROTATED;
        touch();
    }

    /**
     * Revokes this token with a reason.
     */
    public void revoke(String reason) {
        Instant now = HighDate.mockInstant();
        this.status = RefreshTokenStatus.REVOKED;
        this.revokedAt = now;
        this.revokedReason = reason;
        this.updatedAt = now;
    }

    /**
     * Marks this token as expired.
     */
    public void markExpired() {
        this.status = RefreshTokenStatus.EXPIRED;
        touch();
    }

    private void touch() {
        this.updatedAt = HighDate.mockInstant();
    }
}
