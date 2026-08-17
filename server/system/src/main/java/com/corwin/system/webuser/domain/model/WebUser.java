package com.corwin.system.webuser.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 * Aggregate root for the external web user account.
 * Stores account status, registration info, login tracking, and token version for revocation.
 *
 * @author Corwin 2026/5/11
 */
@Getter
@Entity
@Table(name = "tb_user", indexes = {
        @Index(name = "idx_tb_user_status", columnList = "status"),
        @Index(name = "idx_tb_user_primary_identity", columnList = "primary_identity_id")
})
public class WebUser {

    /** Primary key, auto-generated identity. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Display name shown in the UI. */
    @Column(name = "display_name", length = 128)
    private String displayName;

    /** Nickname of the user. */
    @Column(name = "nickname", length = 128)
    private String nickname;

    /** Account status: ACTIVE, DISABLED, or CANCELLED. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private WebUserStatus status;

    /** Method used during registration (e.g. USERNAME_PASSWORD, OAUTH). */
    @Enumerated(EnumType.STRING)
    @Column(name = "register_method", nullable = false, length = 32)
    private WebUserRegisterMethod registerMethod;

    /** Channel through which the user registered (e.g. WEB, APP). */
    @Enumerated(EnumType.STRING)
    @Column(name = "register_channel", nullable = false, length = 32)
    private WebUserRegisterChannel registerChannel;

    /** Source identifier for the registration (e.g. campaign code). */
    @Column(name = "register_source", length = 128)
    private String registerSource;

    /** IP address at registration time. */
    @Column(name = "register_ip", length = 128)
    private String registerIp;

    /** User-Agent header at registration time. */
    @Column(name = "register_user_agent", length = 255)
    private String registerUserAgent;

    /** ID of the primary identity used for display as the account identifier. */
    @Column(name = "primary_identity_id")
    private Long primaryIdentityId;

    /** Token version, incremented on revocation to invalidate existing tokens. */
    @Column(name = "token_version", nullable = false)
    private Long tokenVersion;

    /** Tokens issued before this instant are considered invalid. */
    @Column(name = "token_not_before")
    private Instant tokenNotBefore;

    /** Timestamp of the most recent successful login. */
    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /** IP address of the most recent login. */
    @Column(name = "last_login_ip", length = 128)
    private String lastLoginIp;

    /** Timestamp when the account was disabled. */
    @Column(name = "disabled_at")
    private Instant disabledAt;

    /** Who disabled the account. */
    @Column(name = "disabled_by", length = 64)
    private String disabledBy;

    /** Reason for disabling the account. */
    @Column(name = "disabled_reason", length = 255)
    private String disabledReason;

    /** Timestamp when the account was cancelled. */
    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    /** Reason for cancelling the account. */
    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

    /** Timestamp when the record was created. */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Who created the record. */
    @Column(name = "created_by", length = 64)
    private String createdBy;

    /** Timestamp when the record was last updated. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Who last updated the record. */
    @Column(name = "updated_by", length = 64)
    private String updatedBy;

    /** JPA required no-arg constructor. */
    protected WebUser() {
    }

    /**
     * Construct a new active web user with registration details.
     *
     * @param displayName       the display name
     * @param nickname          the nickname
     * @param registerMethod    the registration method
     * @param registerChannel   the registration channel
     * @param registerSource    the registration source
     * @param registerIp        the registration IP
     * @param registerUserAgent the registration User-Agent
     * @param operator          the operator who created the user
     */
    public WebUser(String displayName, String nickname, WebUserRegisterMethod registerMethod,
            WebUserRegisterChannel registerChannel, String registerSource, String registerIp, String registerUserAgent,
            String operator) {
        Instant now = HighDate.mockInstant();
        this.displayName = displayName;
        this.nickname = nickname;
        this.status = WebUserStatus.ACTIVE;
        this.registerMethod = registerMethod;
        this.registerChannel = registerChannel;
        this.registerSource = registerSource;
        this.registerIp = registerIp;
        this.registerUserAgent = registerUserAgent;
        this.tokenVersion = 1L;
        this.tokenNotBefore = HighDate.realInstant();
        this.createdAt = now;
        this.updatedAt = now;
        this.createdBy = operator;
        this.updatedBy = operator;
    }

    /**
     * Update the user's display name and nickname.
     */
    public void updateProfile(String displayName, String nickname, String operator) {
        this.displayName = displayName;
        this.nickname = nickname;
        touch(operator);
    }

    /**
     * Set the primary identity used as the user's account identifier.
     */
    public void setPrimaryIdentityId(Long primaryIdentityId, String operator) {
        this.primaryIdentityId = primaryIdentityId;
        touch(operator);
    }

    /**
     * Record a successful login event.
     */
    public void markLoginSuccess(String loginIp, String operator) {
        this.lastLoginAt = HighDate.mockInstant();
        this.lastLoginIp = loginIp;
        touch(operator);
    }

    /**
     * Invalidate all existing tokens by incrementing the token version.
     */
    public void revokeTokens(String operator) {
        this.tokenVersion = (tokenVersion == null ? 1L : tokenVersion + 1L);
        this.tokenNotBefore = HighDate.realInstant();
        touch(operator);
    }

    /**
     * Disable the account with a reason. All tokens are revoked.
     */
    public void disable(String reason, String operator) {
        this.status = WebUserStatus.DISABLED;
        this.disabledAt = HighDate.mockInstant();
        this.disabledBy = operator;
        this.disabledReason = reason;
        revokeTokens(operator);
    }

    /**
     * Re-enable a previously disabled account. Tokens are revoked.
     */
    public void enable(String operator) {
        this.status = WebUserStatus.ACTIVE;
        this.disabledAt = null;
        this.disabledBy = null;
        this.disabledReason = null;
        revokeTokens(operator);
    }

    /**
     * Cancel/close the account permanently. Tokens are revoked.
     */
    public void cancel(String reason, String operator) {
        this.status = WebUserStatus.CANCELLED;
        this.cancelledAt = HighDate.mockInstant();
        this.cancelReason = reason;
        revokeTokens(operator);
    }

    /**
     * Check if the user is allowed to log in (status is ACTIVE).
     */
    public boolean canLogin() {
        return status == WebUserStatus.ACTIVE;
    }

    private void touch(String operator) {
        this.updatedAt = HighDate.mockInstant();
        this.updatedBy = operator;
    }
}
