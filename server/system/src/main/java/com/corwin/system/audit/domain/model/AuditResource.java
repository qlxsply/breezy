package com.corwin.system.audit.domain.model;

import com.corwin.framework.dict.DictEnumDefinition;

/**
 * Enumeration of auditable resource types.
 * Defines the system resources that can be tracked in audit logs,
 * such as users, roles, configurations, and system files.
 *
 * @author Corwin 2026/5/21
 */
public enum AuditResource implements DictEnumDefinition {
    USER("用户"),
    EXTERNAL_USER("用户"),
    USER_ROLE("用户角色"),
    ROLE("角色"),
    ROLE_GRANT("角色授权"),
    API("接口"),
    NORMAL_FEATURE("功能配置"),
    CONFIG("系统配置"),
    DICT("数据字典"),
    LOGIN_LOG("登录日志"),
    AUDIT_LOG("审计日志"),
    SYSTEM_FILE("系统文件");

    private final String label;

    AuditResource(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
