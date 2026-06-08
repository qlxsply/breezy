package com.corwin.system.config.interfaces.web;

import com.corwin.framework.config.StoredConfig;
import com.corwin.framework.constant.UserType;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.framework.web.auth.AuthPrincipal;
import com.corwin.framework.web.ctx.CtxUtil;
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

import java.util.List;

/**
 * @author Corwin 2026/5/5
 */
@ApiMeta(module = ApiModuleCode.SYSTEM)
@RestController
@RequestMapping("/api/sys/configs")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigAdminService appService;
    private final NotificationDispatcher notificationDispatcher;

    @GetMapping
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"cfg.view"})
    public ApiResponse<PageResult<ConfigRes>> list(@RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        var page = appService.page(keyword, PageSpec.of(pageNo, pageSize, List.of()));
        return ApiResponse.ok(PageResult.of(page, this::toRes));
    }

    @PutMapping("/{code}")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"cfg.edit"})
    @Audit(resource = AuditResource.CONFIG, action = AuditAction.UPDATE, level = AuditLevel.HIGH)
    public ApiResponse<Boolean> updateValue(@PathVariable String code, @RequestBody UpdateConfigReq req) {
        boolean result = appService.updateValue(new UpdateConfigValueCommand(code, req.value()));
        return ApiResponse.ok(result);
    }

    @PostMapping("/preview/client-ip")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"cfg.view"})
    public ApiResponse<ConfigClientIpPreviewRes> previewClientIp(@RequestBody ConfigClientIpPreviewReq req,
            HttpServletRequest request) {
        ConfigClientIpPreviewView view = appService.previewClientIp(req.mode(), request.getRemoteAddr(),
                request.getHeader("X-Real-IP"), request.getHeader("X-Forwarded-For"),
                request.getHeader("CF-Connecting-IP"), request.getHeader("True-Client-IP"));
        return ApiResponse.ok(toClientIpPreviewRes(view));
    }

    @PostMapping("/preview/time-offset")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"cfg.view"})
    public ApiResponse<ConfigTimeOffsetPreviewRes> previewTimeOffset(@RequestBody ConfigTimeOffsetPreviewReq req) {
        ConfigTimeOffsetPreviewView view = appService.previewTimeOffset(req.offsetSeconds(), req.targetEpochMillis());
        return ApiResponse.ok(toTimeOffsetPreviewRes(view));
    }

    @PostMapping("/preview/msg-push")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"cfg.preview.push"})
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
        return new ConfigRes(config.code(), config.scope(), config.description(), config.valueType(),
                config.value(), config.level(), config.personalized());
    }

    private ConfigClientIpPreviewRes toClientIpPreviewRes(ConfigClientIpPreviewView view) {
        return new ConfigClientIpPreviewRes(view.mode(), view.resolvedIp(), view.remoteAddr(), view.xRealIp(),
                view.xForwardedFor(), view.cfConnectingIp(), view.trueClientIp());
    }

    private ConfigTimeOffsetPreviewRes toTimeOffsetPreviewRes(ConfigTimeOffsetPreviewView view) {
        return new ConfigTimeOffsetPreviewRes(view.serverNowEpochMillis(), view.targetEpochMillis(),
                view.calculatedOffsetSeconds(), view.offsetSeconds(), view.mockedEpochMillis());
    }

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

    private Long currentUserId() {
        AuthPrincipal principal = CtxUtil.getPrincipal();
        Long userId = principal == null ? null : principal.userId();
        BizAssert.notNull(userId, BaseError.FORBIDDEN);
        return userId;
    }
}
