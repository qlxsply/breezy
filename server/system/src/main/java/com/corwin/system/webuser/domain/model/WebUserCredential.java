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
@Table(name = "tb_credential", indexes = {
        @Index(name = "idx_tb_credential_user", columnList = "user_id"),
        @Index(name = "idx_tb_credential_identity", columnList = "identity_id"),
        @Index(name = "idx_tb_credential_type", columnList = "credential_type,status")
})
public class WebUserCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "identity_id")
    private Long identityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "credential_type", nullable = false, length = 16)
    private WebUserCredentialType credentialType;

    @Column(name = "secret_hash", nullable = false, length = 128)
    private String secretHash;

    @Column(name = "secret_algo", nullable = false, length = 32)
    private String secretAlgo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private WebUserCredentialStatus status;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by", length = 64)
    private String createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", length = 64)
    private String updatedBy;

    protected WebUserCredential() {
    }

    public WebUserCredential(Long userId, Long identityId, WebUserCredentialType credentialType, String secretHash,
            String secretAlgo, String operator) {
        Instant now = HighDate.mockInstant();
        this.userId = userId;
        this.identityId = identityId;
        this.credentialType = credentialType;
        this.secretHash = secretHash;
        this.secretAlgo = secretAlgo;
        this.status = WebUserCredentialStatus.ACTIVE;
        this.createdAt = now;
        this.updatedAt = now;
        this.createdBy = operator;
        this.updatedBy = operator;
    }

    public void updateSecret(String secretHash, String secretAlgo, String operator) {
        this.secretHash = secretHash;
        this.secretAlgo = secretAlgo;
        touch(operator);
    }

    public void revoke(String operator) {
        this.status = WebUserCredentialStatus.REVOKED;
        touch(operator);
    }

    private void touch(String operator) {
        this.updatedAt = HighDate.mockInstant();
        this.updatedBy = operator;
    }
}
