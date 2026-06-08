package com.corwin.system.auth.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 * @author Corwin 2026/4/19
 */
@Getter
@Entity
@Table(name = "sys_login_session",
        indexes = {@Index(name = "idx_sys_login_session_token_hash", columnList = "token_hash", unique = true),
                @Index(name = "idx_sys_login_session_user_status", columnList = "user_id,session_status")})
public class LoginSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token_id", nullable = false, length = 64)
    private String tokenId;

    @Column(name = "token_hash", nullable = false, length = 128)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_status", nullable = false, length = 16)
    private SessionStatus sessionStatus;

    @Lob
    @Column(name = "permission_snapshot_json", nullable = false)
    private String permissionSnapshotJson;

    @Lob
    @Column(name = "menu_snapshot_json", nullable = false)
    private String menuSnapshotJson;

    @Lob
    @Column(name = "function_snapshot_json", nullable = false)
    private String functionSnapshotJson;

    @Column(name = "login_ip", length = 128)
    private String loginIp;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "login_at", nullable = false)
    private Instant loginAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "last_access_at", nullable = false)
    private Instant lastAccessAt;

    @Column(name = "logout_at")
    private Instant logoutAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_by", length = 64)
    private String revokedBy;

    @Column(name = "revoked_reason", length = 512)
    private String revokedReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", length = 64)
    private String createdBy;

    @Column(name = "updated_by", length = 64)
    private String updatedBy;

    protected LoginSession() {
    }

    public LoginSession(Long userId, String tokenId, String tokenHash, String loginIp, String userAgent,
            String permissionSnapshotJson, String menuSnapshotJson, String functionSnapshotJson, Instant expiresAt,
            String operator) {
        Instant now = HighDate.mockInstant();
        this.userId = userId;
        this.tokenId = tokenId;
        this.tokenHash = tokenHash;
        this.loginIp = loginIp;
        this.userAgent = userAgent;
        this.permissionSnapshotJson = permissionSnapshotJson;
        this.menuSnapshotJson = menuSnapshotJson;
        this.functionSnapshotJson = functionSnapshotJson;
        this.loginAt = now;
        this.expiresAt = expiresAt;
        this.lastAccessAt = now;
        this.sessionStatus = SessionStatus.ACTIVE;
        this.createdAt = now;
        this.updatedAt = now;
        this.createdBy = operator;
        this.updatedBy = operator;
    }

    public boolean active() {
        return sessionStatus == SessionStatus.ACTIVE;
    }

    public boolean expired(Instant now) {
        return expiresAt.isBefore(now);
    }

    public void refreshLastAccess(Instant now, String operator) {
        this.lastAccessAt = now;
        touch(operator, now);
    }

    public void revoke(String operator) {
        this.sessionStatus = SessionStatus.REVOKED;
        Instant now = HighDate.mockInstant();
        this.revokedAt = now;
        this.revokedBy = operator;
        touch(operator, now);
    }

    public void kickOut(String operator) {
        this.sessionStatus = SessionStatus.KICKED_OUT;
        Instant now = HighDate.mockInstant();
        this.revokedAt = now;
        this.revokedBy = operator;
        touch(operator, now);
    }

    public void markExpired(String operator) {
        this.sessionStatus = SessionStatus.EXPIRED;
        this.revokedAt = HighDate.mockInstant();
        this.revokedBy = operator;
        touch(operator, this.revokedAt);
    }

    private void touch(String operator, Instant now) {
        this.updatedAt = now;
        this.updatedBy = operator;
    }
}
