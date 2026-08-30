package com.corwin.system.webuser.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;

/**
 * An authentication credential (e.g. password, passkey, TOTP) bound to a web user. Stores the
 * hashed secret and algorithm used for verification.
 *
 * @author Corwin 2026/5/11
 */
@Getter
@Entity
@Table(
    name = "tb_credential",
    indexes = {
      @Index(name = "idx_tb_credential_user", columnList = "user_id"),
      @Index(name = "idx_tb_credential_identity", columnList = "identity_id"),
      @Index(name = "idx_tb_credential_type", columnList = "credential_type,status")
    })
public class WebUserCredential {

  /** Primary key, auto-generated identity. */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** ID of the owning web user. */
  @Column(name = "user_id", nullable = false)
  private Long userId;

  /** ID of the identity this credential is associated with. */
  @Column(name = "identity_id")
  private Long identityId;

  /** Type of credential: PASSWORD, PASSKEY, or TOTP. */
  @Enumerated(EnumType.STRING)
  @Column(name = "credential_type", nullable = false, length = 16)
  private WebUserCredentialType credentialType;

  /** Hashed secret value. */
  @Column(name = "secret_hash", nullable = false, length = 128)
  private String secretHash;

  /** Algorithm used to hash the secret (e.g. bcrypt). */
  @Column(name = "secret_algo", nullable = false, length = 32)
  private String secretAlgo;

  /** Credential status: ACTIVE or REVOKED. */
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 16)
  private WebUserCredentialStatus status;

  /** Expiration timestamp for the credential, if applicable. */
  @Column(name = "expires_at")
  private Instant expiresAt;

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
  protected WebUserCredential() {}

  /** Construct a new active credential for the given user. */
  public WebUserCredential(
      Long userId,
      Long identityId,
      WebUserCredentialType credentialType,
      String secretHash,
      String secretAlgo,
      String operator) {
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

  /** Update the credential's hashed secret. */
  public void updateSecret(String secretHash, String secretAlgo, String operator) {
    this.secretHash = secretHash;
    this.secretAlgo = secretAlgo;
    touch(operator);
  }

  /** Revoke this credential, making it invalid for authentication. */
  public void revoke(String operator) {
    this.status = WebUserCredentialStatus.REVOKED;
    touch(operator);
  }

  private void touch(String operator) {
    this.updatedAt = HighDate.mockInstant();
    this.updatedBy = operator;
  }
}
