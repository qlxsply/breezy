package com.corwin.system.auth.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;

/**
 * JPA entity representing an active login session for internal (admin) users, containing token,
 * permission snapshot, and lifecycle state.
 *
 * @author Corwin 2026/4/19
 */
@Getter
@Entity
@Table(
    name = "sys_login_session",
    indexes = {
      @Index(name = "idx_sys_login_session_token_hash", columnList = "token_hash", unique = true),
      @Index(name = "idx_sys_login_session_user_status", columnList = "user_id,session_status")
    })
public class LoginSession {

  /** Unique identifier for the session. */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** The user ID this session belongs to. */
  @Column(name = "user_id", nullable = false)
  private Long userId;

  /** Unique token identifier string (opaque). */
  @Column(name = "token_id", nullable = false, length = 64)
  private String tokenId;

  /** SHA-256 hash of the session token for secure lookups. */
  @Column(name = "token_hash", nullable = false, length = 128)
  private String tokenHash;

  /** Current lifecycle status of this session. */
  @Enumerated(EnumType.STRING)
  @Column(name = "session_status", nullable = false, length = 16)
  private SessionStatus sessionStatus;

  /** JSON snapshot of granted permission codes at login time. */
  @Lob
  @Column(name = "permission_snapshot_json", nullable = false)
  private String permissionSnapshotJson;

  /** JSON snapshot of accessible menu resources at login time. */
  @Lob
  @Column(name = "menu_snapshot_json", nullable = false)
  private String menuSnapshotJson;

  /** JSON snapshot of accessible function resources at login time. */
  @Lob
  @Column(name = "function_snapshot_json", nullable = false)
  private String functionSnapshotJson;

  /** IP address from which the user logged in. */
  @Column(name = "login_ip", length = 128)
  private String loginIp;

  /** User-Agent header value from the login request. */
  @Column(name = "user_agent", length = 512)
  private String userAgent;

  /** Timestamp of when the session was created (login time). */
  @Column(name = "login_at", nullable = false)
  private Instant loginAt;

  /** Timestamp after which the session expires. */
  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  /** Timestamp of the most recent request using this session. */
  @Column(name = "last_access_at", nullable = false)
  private Instant lastAccessAt;

  /** Timestamp of explicit logout, if any. */
  @Column(name = "logout_at")
  private Instant logoutAt;

  /** Timestamp of when the session was revoked or kicked out. */
  @Column(name = "revoked_at")
  private Instant revokedAt;

  /** Identity of the operator who revoked or kicked out this session. */
  @Column(name = "revoked_by", length = 64)
  private String revokedBy;

  /** Reason for revocation or kick-out. */
  @Column(name = "revoked_reason", length = 512)
  private String revokedReason;

  /** Timestamp when this record was created. */
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  /** Timestamp when this record was last updated. */
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** Creator of this record. */
  @Column(name = "created_by", length = 64)
  private String createdBy;

  /** Last modifier of this record. */
  @Column(name = "updated_by", length = 64)
  private String updatedBy;

  protected LoginSession() {}

  /** Creates a new active login session. */
  public LoginSession(
      Long userId,
      String tokenId,
      String tokenHash,
      String loginIp,
      String userAgent,
      String permissionSnapshotJson,
      String menuSnapshotJson,
      String functionSnapshotJson,
      Instant expiresAt,
      String operator) {
    Instant now = HighDate.realInstant();
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

  /** Returns whether this session is currently active. */
  public boolean active() {
    return sessionStatus == SessionStatus.ACTIVE;
  }

  /** Returns whether this session has expired relative to the given time. */
  public boolean expired(Instant now) {
    return expiresAt.isBefore(now);
  }

  /** Updates the last access timestamp to now. */
  public void refreshLastAccess(Instant now, String operator) {
    this.lastAccessAt = now;
    touch(operator, now);
  }

  /** Revokes this session (e.g. on logout). */
  public void revoke(String operator) {
    this.sessionStatus = SessionStatus.REVOKED;
    Instant now = HighDate.realInstant();
    this.revokedAt = now;
    this.revokedBy = operator;
    touch(operator, now);
  }

  /** Kicks out this session (e.g. on forced logout by admin or new login). */
  public void kickOut(String operator) {
    this.sessionStatus = SessionStatus.KICKED_OUT;
    Instant now = HighDate.realInstant();
    this.revokedAt = now;
    this.revokedBy = operator;
    touch(operator, now);
  }

  /** Marks this session as expired. */
  public void markExpired(String operator) {
    this.sessionStatus = SessionStatus.EXPIRED;
    this.revokedAt = HighDate.realInstant();
    this.revokedBy = operator;
    touch(operator, this.revokedAt);
  }

  private void touch(String operator, Instant now) {
    this.updatedAt = now;
    this.updatedBy = operator;
  }
}
