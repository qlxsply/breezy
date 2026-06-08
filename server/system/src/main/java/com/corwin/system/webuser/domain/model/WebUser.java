package com.corwin.system.webuser.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 * @author Corwin 2026/5/11
 */
@Getter
@Entity
@Table(name = "tb_user", indexes = {
        @Index(name = "idx_tb_user_status", columnList = "status"),
        @Index(name = "idx_tb_user_primary_identity", columnList = "primary_identity_id")
})
public class WebUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "display_name", length = 128)
    private String displayName;

    @Column(name = "nickname", length = 128)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private WebUserStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "register_method", nullable = false, length = 32)
    private WebUserRegisterMethod registerMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "register_channel", nullable = false, length = 32)
    private WebUserRegisterChannel registerChannel;

    @Column(name = "register_source", length = 128)
    private String registerSource;

    @Column(name = "register_ip", length = 128)
    private String registerIp;

    @Column(name = "register_user_agent", length = 255)
    private String registerUserAgent;

    @Column(name = "primary_identity_id")
    private Long primaryIdentityId;

    @Column(name = "token_version", nullable = false)
    private Long tokenVersion;

    @Column(name = "token_not_before")
    private Instant tokenNotBefore;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "last_login_ip", length = 128)
    private String lastLoginIp;

    @Column(name = "disabled_at")
    private Instant disabledAt;

    @Column(name = "disabled_by", length = 64)
    private String disabledBy;

    @Column(name = "disabled_reason", length = 255)
    private String disabledReason;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by", length = 64)
    private String createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", length = 64)
    private String updatedBy;

    protected WebUser() {
    }

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
        this.tokenNotBefore = now;
        this.createdAt = now;
        this.updatedAt = now;
        this.createdBy = operator;
        this.updatedBy = operator;
    }

    public void updateProfile(String displayName, String nickname, String operator) {
        this.displayName = displayName;
        this.nickname = nickname;
        touch(operator);
    }

    public void setPrimaryIdentityId(Long primaryIdentityId, String operator) {
        this.primaryIdentityId = primaryIdentityId;
        touch(operator);
    }

    public void markLoginSuccess(String loginIp, String operator) {
        this.lastLoginAt = HighDate.mockInstant();
        this.lastLoginIp = loginIp;
        touch(operator);
    }

    public void revokeTokens(String operator) {
        this.tokenVersion = (tokenVersion == null ? 1L : tokenVersion + 1L);
        this.tokenNotBefore = HighDate.mockInstant();
        touch(operator);
    }

    public void disable(String reason, String operator) {
        this.status = WebUserStatus.DISABLED;
        this.disabledAt = HighDate.mockInstant();
        this.disabledBy = operator;
        this.disabledReason = reason;
        revokeTokens(operator);
    }

    public void enable(String operator) {
        this.status = WebUserStatus.ACTIVE;
        this.disabledAt = null;
        this.disabledBy = null;
        this.disabledReason = null;
        revokeTokens(operator);
    }

    public void cancel(String reason, String operator) {
        this.status = WebUserStatus.CANCELLED;
        this.cancelledAt = HighDate.mockInstant();
        this.cancelReason = reason;
        revokeTokens(operator);
    }

    public boolean canLogin() {
        return status == WebUserStatus.ACTIVE;
    }

    private void touch(String operator) {
        this.updatedAt = HighDate.mockInstant();
        this.updatedBy = operator;
    }
}
