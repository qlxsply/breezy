package com.corwin.system.config.interfaces.web;

import com.corwin.framework.config.StoredConfig;
import com.corwin.framework.constant.UserType;
import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.framework.web.auth.AuthPrincipal;
import com.corwin.framework.web.ctx.CtxUtil;
import com.corwin.framework.web.request.PageSpecFactory;
import com.corwin.framework.web.response.ApiResponse;
import com.corwin.framework.web.response.PageResult;
import com.corwin.system.audit.domain.model.AuditAction;
import com.corwin.system.audit.domain.model.AuditLevel;
import com.corwin.system.audit.domain.model.AuditResource;
import com.corwin.system.audit.published.Audit;
import com.corwin.system.auth.published.Authorize;
import com.corwin.system.config.application.command.UpdateConfigValueCommand;
import com.corwin.system.config.application.service.ConfigAdminService;
import com.corwin.system.config.application.view.ConfigClientIpPreviewView;
import com.corwin.system.config.application.view.ConfigTimeOffsetPreviewView;
import com.corwin.system.config.interfaces.web.req.ConfigClientIpPreviewReq;
import com.corwin.system.config.interfaces.web.req.ConfigPageReq;
import com.corwin.system.config.interfaces.web.req.ConfigTimeOffsetPreviewReq;
import com.corwin.system.config.interfaces.web.req.UpdateConfigReq;
import com.corwin.system.config.interfaces.web.res.ConfigClientIpPreviewRes;
import com.corwin.system.config.interfaces.web.res.ConfigRes;
import com.corwin.system.config.interfaces.web.res.ConfigTimeOffsetPreviewRes;
import com.corwin.system.notify.application.service.NotificationDispatcher;
import com.corwin.system.notify.interfaces.web.req.PreviewMsgPushReq;
import com.corwin.system.notify.published.MsgType;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for system configuration management.
 * Provides endpoints for listing, updating, and previewing system configurations
 * including client IP resolution and time offset.
 *
 * @author Corwin 2026/5/5
 */
@ApiMeta(module = ApiModuleCode.SYSTEM)
@RestController
@RequestMapping("/api/sys/configs")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigAdminService appService;
    private final NotificationDispatcher notificationDispatcher;

    /**
     * List configurations with pagination and optional fuzzy search by code or description.
     *
     * @param req the page request with optional codeLike and descriptionLike filters
     * @return paginated configuration entries
     */
    @PostMapping("/page")
    @Authorize(userType = UserType.ADMIN, permissions = {"cfg.view"})
    public ApiResponse<PageResult<ConfigRes>> list(@RequestBody ConfigPageReq req) {
        var page = appService.page(req.codeLike(), req.descriptionLike(), PageSpecFactory.of(req.page(), null));
        return ApiResponse.ok(PageResult.of(page, this::toRes));
    }

    /**
     * Update the value of a configuration identified by its code.
     *
     * @param code the configuration code
     * @param req  the request containing the new value
     * @return true if the update was successful
     */
    @PutMapping("/{code}")
    @Authorize(userType = UserType.ADMIN, permissions = {"cfg.edit"})
    @Audit(resource = AuditResource.CONFIG, action = AuditAction.UPDATE, level = AuditLevel.HIGH)
    public ApiResponse<Boolean> updateValue(@PathVariable String code, @RequestBody UpdateConfigReq req) {
        boolean result = appService.updateValue(new UpdateConfigValueCommand(code, req.value()));
        return ApiResponse.ok(result);
    }

    /**
     * Preview the resolved client IP based on the specified resolution mode and request headers.
     *
     * @param req     the preview request specifying the resolution mode
     * @param request the HTTP servlet request to extract header information
     * @return the client IP preview result
     */
    @PostMapping("/preview/client-ip")
    @Authorize(userType = UserType.ADMIN, permissions = {"cfg.view"})
    public ApiResponse<ConfigClientIpPreviewRes> previewClientIp(@RequestBody ConfigClientIpPreviewReq req,
            HttpServletRequest request) {
        ConfigClientIpPreviewView view = appService.previewClientIp(req.mode(), request.getRemoteAddr(),
                request.getHeader("X-Real-IP"), request.getHeader("X-Forwarded-For"),
                request.getHeader("CF-Connecting-IP"), request.getHeader("True-Client-IP"));
        return ApiResponse.ok(toClientIpPreviewRes(view));
    }

    /**
     * Preview the time offset calculation based on the provided offset seconds or target epoch millis.
     *
     * @param req the preview request with offset or target timestamp
     * @return the time offset preview result
     */
    @PostMapping("/preview/time-offset")
    @Authorize(userType = UserType.ADMIN, permissions = {"cfg.view"})
    public ApiResponse<ConfigTimeOffsetPreviewRes> previewTimeOffset(@RequestBody ConfigTimeOffsetPreviewReq req) {
        ConfigTimeOffsetPreviewView view = appService.previewTimeOffset(req.offsetSeconds(), req.targetEpochMillis());
        return ApiResponse.ok(toTimeOffsetPreviewRes(view));
    }

    /**
     * Preview message push notification by dispatching a test notification
     * with the given route, priority, and delivery options.
     *
     * @param req the preview message push request
     * @return a confirmation message indicating the preview was sent
     */
    @PostMapping("/preview/msg-push")
    @Authorize(userType = UserType.ADMIN, permissions = {"cfg.preview.push"})
    public ApiResponse<String> previewMsgPush(@RequestBody PreviewMsgPushReq req) {
        if (req == null) {
            req = new PreviewMsgPushReq(null, null, null, null, null, null, null);
        }

        Long userId = currentUserId();
        UserType userType = CtxUtil.getPrincipal().userType();
        MsgType resolvedType = resolveMsgType(req.msgType());
        String title = "[Config Preview] Message Push Test";
        String content = "This preview message is used to verify route, priority, and client display.";

        notificationDispatcher.dispatchPreview(userId, userType, resolvedType, title, content, req.route(),
                req.priority(), req.sseEnabled(), req.webPushEnabled(), req.panelAutoOpen(),
                req.osNotificationEnabled());

        return ApiResponse.ok("Preview message sent, msgType=" + resolvedType.name());
    }

    private ConfigRes toRes(StoredConfig config) {
        return new ConfigRes(config.code(), config.scope(), config.description(), config.valueType(), config.value(),
                config.level(), config.personalized());
    }

    private ConfigClientIpPreviewRes toClientIpPreviewRes(ConfigClientIpPreviewView view) {
        return new ConfigClientIpPreviewRes(view.mode(), view.resolvedIp(), view.remoteAddr(), view.xRealIp(),
                view.xForwardedFor(), view.cfConnectingIp(), view.trueClientIp());
    }

    private ConfigTimeOffsetPreviewRes toTimeOffsetPreviewRes(ConfigTimeOffsetPreviewView view) {
        return new ConfigTimeOffsetPreviewRes(view.serverNowEpochMillis(), view.targetEpochMillis(),
                view.calculatedOffsetSeconds(), view.offsetSeconds(), view.mockedEpochMillis());
    }

    /**
     * Resolve the message type from a raw string input, defaulting to BUSINESS_EVENT.
     *
     * @param raw the raw message type string
     * @return the resolved MsgType enum
     */
    private MsgType resolveMsgType(String raw) {
        if (raw == null || raw.isBlank()) {
            return MsgType.BUSINESS_EVENT;
        }
        String normalized = raw.trim().toUpperCase();
        switch (normalized) {
            case "SYSTEM" -> {
                return MsgType.SYSTEM_EVENT;
            }
            case "BUSINESS" -> {
                return MsgType.BUSINESS_EVENT;
            }
            case "TODO", "TODO_REMINDER" -> {
                return MsgType.TODO_REMINDER;
            }
        }
        try {
            return MsgType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return MsgType.BUSINESS_EVENT;
        }
    }

    /**
     * Retrieve the current authenticated user ID from the request context.
     *
     * @return the current user ID
     * @throws com.corwin.framework.error.BizException if the user is not authenticated
     */
    private Long currentUserId() {
        AuthPrincipal principal = CtxUtil.getPrincipal();
        Long userId = principal == null ? null : principal.userId();
        BizAssert.notNull(userId, BaseError.FORBIDDEN);
        return userId;
    }
}
