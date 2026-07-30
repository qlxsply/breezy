package com.corwin.system.notify.interfaces.web.req;

/**
 * Request DTO for previewing a message push with configurable behavior overrides.
 *
 * @param msgType              the message type
 * @param route                the front-end route
 * @param priority             the priority override
 * @param sseEnabled           whether SSE push is enabled
 * @param webPushEnabled       whether Web Push is enabled
 * @param panelAutoOpen        whether to auto-open the notification panel
 * @param osNotificationEnabled whether to send OS-level notification
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
