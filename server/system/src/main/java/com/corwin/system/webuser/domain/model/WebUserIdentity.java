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
@Table(name = "tb_identity", indexes = {
        @Index(name = "idx_tb_identity_user", columnList = "user_id"),
        @Index(name = "idx_tb_identity_hash", columnList = "identity_type,identity_hash"),
        @Index(name = "idx_tb_identity_provider", columnList = "provider_code,provider_subject")
})
public class WebUserIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "identity_type", nullable = false, length = 16)
    private WebUserIdentityType identityType;

    @Column(name = "identity_value", nullable = false, length = 128)
    private String identityValue;

    @Column(name = "normalized_value", length = 128)
    private String normalizedValue;

    @Column(name = "identity_hash", length = 128)
    private String identityHash;

    @Column(name = "provider_code", length = 64)
    private String providerCode;

    @Column(name = "provider_subject", length = 128)
    private String providerSubject;

    @Column(name = "verified", nullable = false)
    private Boolean verified;

    @Column(name = "login_enabled", nullable = false)
    private Boolean loginEnabled;

    @Enumerated(EnumType.STRING)
    @Column(name = "bind_status", nullable = false, length = 16)
    private WebUserIdentityBindStatus bindStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by", length = 64)
    private String createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", length = 64)
    private String updatedBy;

    protected WebUserIdentity() {
    }

    public WebUserIdentity(Long userId, WebUserIdentityType identityType, String identityValue,
            String normalizedValue, String identityHash, String providerCode, String providerSubject, boolean verified,
            boolean loginEnabled, String operator) {
        Instant now = HighDate.mockInstant();
        this.userId = userId;
        this.identityType = identityType;
        this.identityValue = identityValue;
        this.normalizedValue = normalizedValue;
        this.identityHash = identityHash;
        this.providerCode = providerCode;
        this.providerSubject = providerSubject;
        this.verified = verified;
        this.loginEnabled = loginEnabled;
        this.bindStatus = WebUserIdentityBindStatus.ACTIVE;
        this.createdAt = now;
        this.updatedAt = now;
        this.createdBy = operator;
        this.updatedBy = operator;
    }

    public void release(String operator) {
        this.bindStatus = WebUserIdentityBindStatus.RELEASED;
        this.loginEnabled = false;
        touch(operator);
    }

    public void disableLogin(String operator) {
        this.loginEnabled = false;
        touch(operator);
    }

    private void touch(String operator) {
        this.updatedAt = HighDate.mockInstant();
        this.updatedBy = operator;
    }
}
