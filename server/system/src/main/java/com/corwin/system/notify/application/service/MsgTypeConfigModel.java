package com.corwin.system.notify.application.service;

import com.corwin.system.notify.domain.model.MsgPriority;
import com.corwin.system.notify.published.MsgType;

/**
 * Configuration model for per-message-type delivery behavior.
 * <p>Deserialized from JSON stored in sys_config to determine push routing,
 * priority, and notification display settings for each message type.</p>
 *
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

    /**
     * Resolves the message type from the configured string value.
     *
     * @return the resolved {@link MsgType}, or null if invalid
     */
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

    /**
     * Resolves the priority from configuration, falling back to the given default.
     *
     * @param fallback the default priority if not configured
     * @return the resolved priority
     */
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

    /**
     * Normalizes the configured route, falling back to the default for the given message type.
     *
     * @param resolvedMsgType the resolved message type
     * @return a valid route string starting with "/"
     */
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

    /**
     * Resolves whether SSE push is enabled, defaulting to the given fallback.
     *
     * @param fallback the default value if not configured
     * @return true if SSE push is enabled
     */
    public boolean resolveSseEnabled(boolean fallback) {
        return sseEnabled == null ? fallback : sseEnabled;
    }

    /**
     * Resolves whether Web Push is enabled, defaulting to the given fallback.
     *
     * @param fallback the default value if not configured
     * @return true if Web Push is enabled
     */
    public boolean resolveWebPushEnabled(boolean fallback) {
        return webPushEnabled == null ? fallback : webPushEnabled;
    }

    /**
     * Resolves whether the notification panel should auto-open, defaulting to the given fallback.
     *
     * @param fallback the default value if not configured
     * @return true if panel auto-open is enabled
     */
    public boolean resolvePanelAutoOpen(boolean fallback) {
        return panelAutoOpen == null ? fallback : panelAutoOpen;
    }

    /**
     * Resolves whether OS-level notifications are enabled, defaulting to the given fallback.
     *
     * @param fallback the default value if not configured
     * @return true if OS notifications are enabled
     */
    public boolean resolveOsNotificationEnabled(boolean fallback) {
        return osNotificationEnabled == null ? fallback : osNotificationEnabled;
    }

    /**
     * Creates a default configuration model for the given message type.
     *
     * @param msgType the message type
     * @return the default configuration
     */
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
