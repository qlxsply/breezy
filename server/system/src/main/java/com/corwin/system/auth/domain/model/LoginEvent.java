package com.corwin.system.auth.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 * JPA entity representing a login/logout/security event in the system.
 *
 * @author Corwin 2026/1/23
 */
@Getter
@Entity
@Table(name = "sys_login_event", indexes = {@Index(name = "idx_sys_login_log_user", columnList = "user_id"),
        @Index(name = "idx_sys_login_log_event_type", columnList = "event_type"),
        @Index(name = "idx_sys_login_log_occurred_at", columnList = "occurred_at")})
public class LoginEvent {

    /** Unique identifier for this event. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The user ID who triggered the event (may be null for anonymous failures). */
    @Column(name = "user_id")
    private Long userId;

    /** The username at the time of the event. */
    @Column(name = "username", length = 64)
    private String username;

    /** Type of security event (login, logout, token expiry, etc.). */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private LoginEventType eventType;

    /** Whether the event completed successfully. */
    @Column(name = "success", nullable = false)
    private boolean success;

    /** Reason text if the event failed. */
    @Column(name = "failure_reason", length = 128)
    private String failureReason;

    /** The related login session ID, if applicable. */
    @Column(name = "session_id")
    private Long sessionId;

    /** IP address from which the request originated. */
    @Column(name = "login_ip", length = 128)
    private String loginIp;

    /** The operator (admin) ID if this event was triggered by an admin action. */
    @Column(name = "operator_id")
    private Long operatorId;

    /** Timestamp of when the event occurred. */
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    /** Optional remark or additional context for the event. */
    @Column(name = "remark", length = 255)
    private String remark;

    protected LoginEvent() {
    }

    /**
     * Creates a new login event with the given details and the current timestamp.
     */
    public LoginEvent(Long userId, String username, LoginEventType eventType, boolean success, String loginIp,
            String failureReason, String remark) {
        this.userId = userId;
        this.username = username;
        this.eventType = eventType;
        this.success = success;
        this.loginIp = loginIp;
        this.failureReason = failureReason;
        this.occurredAt = HighDate.mockInstant();
        this.remark = remark;
    }
}
