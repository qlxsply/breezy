package com.corwin.system.notify.interfaces.web.req;

/**
 * @author Corwin 2026/3/20
 */
public record PreviewMsgPushReq(
        String msgType,
        String route,
        String priority,
        Boolean sseEnabled,
        Boolean webPushEnabled,
        Boolean panelAutoOpen,
        Boolean osNotificationEnabled
) {
}
