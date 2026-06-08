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
@Table(name = "tb_current_identity", indexes = {
        @Index(name = "idx_tb_current_identity_hash", columnList = "identity_type,identity_hash", unique = true),
        @Index(name = "idx_tb_current_identity_provider", columnList = "provider_code,provider_subject", unique = true),
        @Index(name = "idx_tb_current_identity_user", columnList = "user_id")
})
public class WebUserCurrentIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "identity_type", nullable = false, length = 16)
    private WebUserIdentityType identityType;

    @Column(name = "identity_hash", length = 128)
    private String identityHash;

    @Column(name = "provider_code", length = 64)
    private String providerCode;

    @Column(name = "provider_subject", length = 128)
    private String providerSubject;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "identity_id", nullable = false)
    private Long identityId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WebUserCurrentIdentity() {
    }

    public WebUserCurrentIdentity(WebUserIdentityType identityType, String identityHash, String providerCode,
            String providerSubject, Long userId, Long identityId) {
        Instant now = HighDate.mockInstant();
        this.identityType = identityType;
        this.identityHash = identityHash;
        this.providerCode = providerCode;
        this.providerSubject = providerSubject;
        this.userId = userId;
        this.identityId = identityId;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void rebind(Long userId, Long identityId) {
        this.userId = userId;
        this.identityId = identityId;
        this.updatedAt = HighDate.mockInstant();
    }
}
