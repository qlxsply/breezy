package com.corwin.system.auth.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 * @author Corwin 2026/1/23
 */
@Getter
@Entity
@Table(name = "sys_login_event", indexes = {@Index(name = "idx_sys_login_log_user", columnList = "user_id"),
        @Index(name = "idx_sys_login_log_event_type", columnList = "event_type"),
        @Index(name = "idx_sys_login_log_occurred_at", columnList = "occurred_at")})
public class LoginEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", length = 64)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private LoginEventType eventType;

    @Column(name = "success", nullable = false)
    private boolean success;

    @Column(name = "failure_reason", length = 128)
    private String failureReason;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "login_ip", length = 128)
    private String loginIp;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "remark", length = 255)
    private String remark;

    protected LoginEvent() {
    }

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
