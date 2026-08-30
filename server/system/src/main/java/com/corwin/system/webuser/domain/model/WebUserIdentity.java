package com.corwin.system.webuser.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;

/**
 * An identity (username, email, phone, or OAuth) bound to a web user account. Each identity can be
 * used for login and is tracked with a normalized hash for lookups.
 *
 * @author Corwin 2026/5/11
 */
@Getter
@Entity
@Table(
    name = "tb_identity",
    indexes = {
      @Index(name = "idx_tb_identity_user", columnList = "user_id"),
      @Index(name = "idx_tb_identity_hash", columnList = "identity_type,identity_hash"),
      @Index(name = "idx_tb_identity_provider", columnList = "provider_code,provider_subject")
    })
public class WebUserIdentity {

  /** Primary key, auto-generated identity. */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** ID of the owning web user. */
  @Column(name = "user_id", nullable = false)
  private Long userId;

  /** Type of identity: USERNAME, EMAIL, PHONE, or OAUTH. */
  @Enumerated(EnumType.STRING)
  @Column(name = "identity_type", nullable = false, length = 16)
  private WebUserIdentityType identityType;

  /** The raw identity value (e.g. email address, phone number). */
  @Column(name = "identity_value", nullable = false, length = 128)
  private String identityValue;

  /** Normalized form of the identity value (lowercase, stripped). */
  @Column(name = "normalized_value", length = 128)
  private String normalizedValue;

  /** SHA-256 hash of the type+normalized value used for unique lookups. */
  @Column(name = "identity_hash", length = 128)
  private String identityHash;

  /** OAuth provider code (e.g. "google", "github"), non-null for OAUTH type. */
  @Column(name = "provider_code", length = 64)
  private String providerCode;

  /** OAuth provider subject/ID, unique per provider. */
  @Column(name = "provider_subject", length = 128)
  private String providerSubject;

  /** Whether the identity has been verified (e.g. email confirmed). */
  @Column(name = "verified", nullable = false)
  private Boolean verified;

  /** Whether login using this identity is enabled. */
  @Column(name = "login_enabled", nullable = false)
  private Boolean loginEnabled;

  /** Binding status: ACTIVE, RELEASED, or HISTORICAL. */
  @Enumerated(EnumType.STRING)
  @Column(name = "bind_status", nullable = false, length = 16)
  private WebUserIdentityBindStatus bindStatus;

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
  protected WebUserIdentity() {}

  /** Construct a new identity bound to the given user. */
  public WebUserIdentity(
      Long userId,
      WebUserIdentityType identityType,
      String identityValue,
      String normalizedValue,
      String identityHash,
      String providerCode,
      String providerSubject,
      boolean verified,
      boolean loginEnabled,
      String operator) {
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

  /** Release/unbind this identity from the user account. */
  public void release(String operator) {
    this.bindStatus = WebUserIdentityBindStatus.RELEASED;
    this.loginEnabled = false;
    touch(operator);
  }

  /** Disable login using this identity. */
  public void disableLogin(String operator) {
    this.loginEnabled = false;
    touch(operator);
  }

  private void touch(String operator) {
    this.updatedAt = HighDate.mockInstant();
    this.updatedBy = operator;
  }
}
