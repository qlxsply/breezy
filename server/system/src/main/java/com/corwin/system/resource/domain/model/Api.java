package com.corwin.system.resource.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;

/**
 * API 元数据
 *
 * @author Corwin 2026/1/23
 */
@Getter
@Entity
@Table(name = "sys_api", indexes = {
        @Index(name = "idx_sys_api_unique", columnList = "module,protocol,http_method,path_pattern", unique = true),
        @Index(name = "idx_sys_api_module", columnList = "module"),
        @Index(name = "idx_sys_api_path", columnList = "module,http_method,path_pattern"),
        @Index(name = "idx_sys_api_access_type", columnList = "access_type")})
public class Api {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "module", nullable = false, length = 128)
    private String module;

    @Enumerated(EnumType.STRING)
    @Column(name = "protocol", nullable = false, length = 16)
    private ApiProtocol protocol;

    @Enumerated(EnumType.STRING)
    @Column(name = "http_method", nullable = false, length = 16)
    private ApiMethod httpMethod;

    @Column(name = "path_pattern", nullable = false, length = 512)
    private String pathPattern;

    @Column(name = "handler_class", length = 512)
    private String handlerClass;

    @Column(name = "handler_method", length = 128)
    private String handlerMethod;

    @Column(name = "permission_declared", nullable = false)
    private Boolean permissionDeclared;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_type", nullable = false, length = 32)
    private ApiAccessType accessType;

    /**
     * JSON 字符串，例如：["ADMIN","USER"]
     */
    @Lob
    @Column(name = "user_type", length = 16)
    private String userType;

    @Column(name = "audit_declared", nullable = false)
    private Boolean auditDeclared;

    @Column(name = "audit_resource", length = 128)
    private String auditResource;

    @Column(name = "audit_action", length = 128)
    private String auditAction;

    @Column(name = "audit_description", length = 512)
    private String auditDescription;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    @Column(name = "system_builtin", nullable = false)
    private Boolean systemBuiltin;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Api() {
    }

    public Api(String module, ApiProtocol protocol, ApiMethod httpMethod, String pathPattern,
            String handlerClass, String handlerMethod, Boolean permissionDeclared, ApiAccessType accessType,
            String userType, Boolean auditDeclared, String auditResource, String auditAction, String auditDescription,
            Boolean systemBuiltin) {
        Instant now = HighDate.mockInstant();
        this.module = module;
        this.protocol = protocol;
        this.httpMethod = httpMethod;
        this.pathPattern = pathPattern;
        this.handlerClass = handlerClass;
        this.handlerMethod = handlerMethod;
        this.permissionDeclared = permissionDeclared;
        this.accessType = accessType;
        this.userType = userType;
        this.auditDeclared = auditDeclared;
        this.auditResource = auditResource;
        this.auditAction = auditAction;
        this.auditDescription = auditDescription;
        this.enabled = true;
        this.systemBuiltin = systemBuiltin;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String module, ApiProtocol protocol, ApiMethod httpMethod, String pathPattern,
            String handlerClass, String handlerMethod, Boolean permissionDeclared, ApiAccessType accessType,
            String userType, Boolean auditDeclared, String auditResource, String auditAction, String auditDescription,
            Boolean systemBuiltin, Boolean enabled) {
        this.module = module;
        this.protocol = protocol;
        this.httpMethod = httpMethod;
        this.pathPattern = pathPattern;
        this.handlerClass = handlerClass;
        this.handlerMethod = handlerMethod;
        this.permissionDeclared = permissionDeclared;
        this.accessType = accessType;
        this.userType = userType;
        this.auditDeclared = auditDeclared;
        this.auditResource = auditResource;
        this.auditAction = auditAction;
        this.auditDescription = auditDescription;
        this.systemBuiltin = systemBuiltin;
        this.enabled = enabled;
        touch();
    }

    public void enable() {
        this.enabled = true;
        touch();
    }

    public void disable() {
        this.enabled = false;
        touch();
    }

    private void touch() {
        this.updatedAt = HighDate.mockInstant();
    }
}
