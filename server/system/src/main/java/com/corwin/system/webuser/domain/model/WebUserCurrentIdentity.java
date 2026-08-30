package com.corwin.system.webuser.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;

/**
 * Lookup table mapping a current identity (by hash or OAuth provider) to the active user and
 * identity record. Enables fast login resolution by identity type+hash or provider details.
 *
 * @author Corwin 2026/5/11
 */
@Getter
@Entity
@Table(
    name = "tb_current_identity",
    indexes = {
      @Index(
          name = "idx_tb_current_identity_hash",
          columnList = "identity_type,identity_hash",
          unique = true),
      @Index(
          name = "idx_tb_current_identity_provider",
          columnList = "provider_code,provider_subject",
          unique = true),
      @Index(name = "idx_tb_current_identity_user", columnList = "user_id")
    })
public class WebUserCurrentIdentity {

  /** Primary key, auto-generated identity. */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Type of identity (USERNAME, EMAIL, PHONE, OAUTH). */
  @Enumerated(EnumType.STRING)
  @Column(name = "identity_type", nullable = false, length = 16)
  private WebUserIdentityType identityType;

  /** Hash of the normalized identity value for lookup. */
  @Column(name = "identity_hash", length = 128)
  private String identityHash;

  /** OAuth provider code (e.g. "google"), non-null for OAUTH type. */
  @Column(name = "provider_code", length = 64)
  private String providerCode;

  /** OAuth provider subject/ID. */
  @Column(name = "provider_subject", length = 128)
  private String providerSubject;

  /** ID of the currently owning web user. */
  @Column(name = "user_id", nullable = false)
  private Long userId;

  /** ID of the current identity record. */
  @Column(name = "identity_id", nullable = false)
  private Long identityId;

  /** Timestamp when the record was created. */
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  /** Timestamp when the record was last updated. */
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** JPA required no-arg constructor. */
  protected WebUserCurrentIdentity() {}

  /** Construct a new current identity lookup entry. */
  public WebUserCurrentIdentity(
      WebUserIdentityType identityType,
      String identityHash,
      String providerCode,
      String providerSubject,
      Long userId,
      Long identityId) {
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

  /** Rebind this lookup entry to a different user and identity record. */
  public void rebind(Long userId, Long identityId) {
    this.userId = userId;
    this.identityId = identityId;
    this.updatedAt = HighDate.mockInstant();
  }
}
