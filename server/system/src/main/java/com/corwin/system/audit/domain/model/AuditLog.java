package com.corwin.system.audit.domain.model;

import com.corwin.system.resource.domain.model.ApiMethod;
import com.corwin.system.resource.domain.model.ApiProtocol;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 * JPA entity representing an audit log record.
 * Maps to the {@code sys_audit_log} table and captures the full lifecycle
 * of a single audited request, including operator context, request details,
 * response summary, and execution timings.
 *
 * @author Corwin 2026/4/19
 */
@Getter
@Entity
@Table(name = "sys_audit_log", indexes = {@Index(name = "idx_sys_audit_log_trace_id", columnList = "trace_id"),
        @Index(name = "idx_sys_audit_log_request_id", columnList = "request_id"),
        @Index(name = "idx_sys_audit_log_operator_user_id", columnList = "operator_user_id"),
        @Index(name = "idx_sys_audit_log_application_code", columnList = "application_code"),
        @Index(name = "idx_sys_audit_log_path", columnList = "application_code,http_method,path_pattern"),
        @Index(name = "idx_sys_audit_log_resource_action", columnList = "audit_resource,audit_action"),
        @Index(name = "idx_sys_audit_log_success", columnList = "success"),
        @Index(name = "idx_sys_audit_log_created_at", columnList = "created_at")})
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trace_id", length = 128)
    private String traceId;

    @Column(name = "request_id", length = 128)
    private String requestId;

    @Column(name = "operator_user_id")
    private Long operatorUserId;

    @Column(name = "operator_username", length = 128)
    private String operatorUsername;

    @Column(name = "operator_user_type", length = 32)
    private String operatorUserType;

    @Column(name = "application_code", length = 128)
    private String applicationCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "protocol", length = 16)
    private ApiProtocol protocol;

    @Enumerated(EnumType.STRING)
    @Column(name = "http_method", length = 16)
    private ApiMethod httpMethod;

    @Column(name = "path_pattern", length = 512)
    private String pathPattern;

    @Column(name = "request_uri", length = 1024)
    private String requestUri;

    @Lob
    @Column(name = "permission_codes")
    private String permissionCodes;

    @Column(name = "audit_resource", length = 128)
    private String auditResource;

    @Column(name = "audit_action", length = 128)
    private String auditAction;

    @Column(name = "audit_description", length = 512)
    private String auditDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "audit_level", length = 16)
    private AuditLevel auditLevel;

    @Column(name = "request_ip", length = 128)
    private String requestIp;

    @Column(name = "user_agent", length = 1024)
    private String userAgent;

    @Lob
    @Column(name = "request_param_summary")
    private String requestParamSummary;

    @Lob
    @Column(name = "request_body_summary")
    private String requestBodySummary;

    @Lob
    @Column(name = "response_summary")
    private String responseSummary;

    @Column(name = "success")
    private Boolean success;

    @Column(name = "error_code", length = 128)
    private String errorCode;

    @Lob
    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AuditLog() {
    }

    public AuditLog(String traceId, String requestId, Long operatorUserId, String operatorUsername,
            String operatorUserType, String applicationCode, ApiProtocol protocol, ApiMethod httpMethod,
            String pathPattern, String requestUri, String permissionCodes, String auditResource, String auditAction,
            String auditDescription, AuditLevel auditLevel, String requestIp, String userAgent,
            String requestParamSummary, String requestBodySummary, String responseSummary, Boolean success,
            String errorCode, String errorMessage, Instant startedAt, Instant endedAt, Long durationMs,
            Instant createdAt) {
        this.traceId = traceId;
        this.requestId = requestId;
        this.operatorUserId = operatorUserId;
        this.operatorUsername = operatorUsername;
        this.operatorUserType = operatorUserType;
        this.applicationCode = applicationCode;
        this.protocol = protocol;
        this.httpMethod = httpMethod;
        this.pathPattern = pathPattern;
        this.requestUri = requestUri;
        this.permissionCodes = permissionCodes;
        this.auditResource = auditResource;
        this.auditAction = auditAction;
        this.auditDescription = auditDescription;
        this.auditLevel = auditLevel;
        this.requestIp = requestIp;
        this.userAgent = userAgent;
        this.requestParamSummary = requestParamSummary;
        this.requestBodySummary = requestBodySummary;
        this.responseSummary = responseSummary;
        this.success = success;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.durationMs = durationMs;
        this.createdAt = createdAt;
    }
}
