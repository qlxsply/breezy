package com.corwin.system.notify.application.service;

import com.corwin.system.notify.domain.model.MsgPriority;
import com.corwin.system.notify.published.MsgType;

/**
 * 消息行为配置对象 (用于从 sys_config 的 JSON 反序列化)
 * @author Corwin 2026/3/16
 */
public class MsgTypeConfigModel {
    private String msgType;
    private String route;
    private String priority;
    private Boolean sseEnabled;
    private Boolean webPushEnabled;
    private Boolean panelAutoOpen;
    private Boolean osNotificationEnabled;

    public String getMsgType() {
        return msgType;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Boolean getSseEnabled() {
        return sseEnabled;
    }

    public void setSseEnabled(Boolean sseEnabled) {
        this.sseEnabled = sseEnabled;
    }

    public Boolean getWebPushEnabled() {
        return webPushEnabled;
    }

    public void setWebPushEnabled(Boolean webPushEnabled) {
        this.webPushEnabled = webPushEnabled;
    }

    public Boolean getPanelAutoOpen() {
        return panelAutoOpen;
    }

    public void setPanelAutoOpen(Boolean panelAutoOpen) {
        this.panelAutoOpen = panelAutoOpen;
    }

    public Boolean getOsNotificationEnabled() {
        return osNotificationEnabled;
    }

    public void setOsNotificationEnabled(Boolean osNotificationEnabled) {
        this.osNotificationEnabled = osNotificationEnabled;
    }

    public MsgType resolveMsgTypeOrNull() {
        if (msgType == null || msgType.isBlank()) {
            return null;
        }
        try {
            return MsgType.valueOf(msgType.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public MsgPriority resolvePriority(MsgPriority fallback) {
        if (priority == null || priority.isBlank()) {
            return fallback;
        }
        try {
            return MsgPriority.valueOf(priority.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    public String normalizeRoute(MsgType resolvedMsgType) {
        String trimmed = route == null ? "" : route.trim();
        if ("/todo-all".equals(trimmed)) {
            trimmed = "/todo/all";
        }
        if (!trimmed.isBlank() && !trimmed.startsWith("/")) {
            trimmed = "";
        }
        if (trimmed.isBlank()) {
            return defaultRoute(resolvedMsgType);
        }
        return trimmed;
    }

    public boolean resolveSseEnabled(boolean fallback) {
        return sseEnabled == null ? fallback : sseEnabled;
    }

    public boolean resolveWebPushEnabled(boolean fallback) {
        return webPushEnabled == null ? fallback : webPushEnabled;
    }

    public boolean resolvePanelAutoOpen(boolean fallback) {
        return panelAutoOpen == null ? fallback : panelAutoOpen;
    }

    public boolean resolveOsNotificationEnabled(boolean fallback) {
        return osNotificationEnabled == null ? fallback : osNotificationEnabled;
    }

    public static MsgTypeConfigModel defaultFor(MsgType msgType) {
        MsgTypeConfigModel model = new MsgTypeConfigModel();
        model.setMsgType(msgType.name());
        model.setRoute(defaultRoute(msgType));
        MsgPriority priority = defaultPriority(msgType);
        model.setPriority(priority.name());
        model.setSseEnabled(true);
        model.setWebPushEnabled(priority == MsgPriority.HIGH);
        model.setPanelAutoOpen(priority != MsgPriority.LOW);
        model.setOsNotificationEnabled(priority == MsgPriority.HIGH);
        return model;
    }

    private static MsgPriority defaultPriority(MsgType msgType) {
        if (msgType == MsgType.SYSTEM_EVENT) {
            return MsgPriority.LOW;
        }
        if (msgType == MsgType.TODO_REMINDER) {
            return MsgPriority.MEDIUM;
        }
        return MsgPriority.MEDIUM;
    }

    private static String defaultRoute(MsgType msgType) {
        if (msgType == MsgType.TODO_REMINDER) {
            return "/todo/all";
        }
        return "";
    }
}
