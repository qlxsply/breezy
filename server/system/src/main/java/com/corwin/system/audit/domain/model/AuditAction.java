package com.corwin.system.audit.domain.model;

import com.corwin.framework.dict.DictEnumDefinition;

/**
 * @author Corwin 2026/5/21
 */
public enum AuditAction implements DictEnumDefinition {
    VIEW("查看"),
    CREATE("创建"),
    UPDATE("更新"),
    DELETE("删除"),
    ENABLE("启用"),
    DISABLE("停用"),
    RESET_PASSWORD("重置密码"),
    CHANGE_PASSWORD("修改密码"),
    UPDATE_PROFILE("修改个人资料"),
    REGISTER("注册"),
    LOGOUT("退出登录"),
    PUBLISH("发布"),
    GRANT("授权"),
    START("启动"),
    STOP("停止"),
    PREVIEW("预览"),
    UPDATE_DEFAULT("维护默认配置"),
    UPDATE_USER("维护用户配置");

    private final String label;

    AuditAction(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
