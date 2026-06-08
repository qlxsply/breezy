package com.corwin.system.notify.interfaces.web;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.web.ctx.CtxUtil;
import com.corwin.framework.web.response.ApiResponse;
import com.corwin.system.auth.published.Authenticated;
import com.corwin.system.auth.published.PermitAll;
import com.corwin.system.notify.application.command.SavePushSubscriptionCommand;
import com.corwin.system.notify.application.service.PushHealthAppService;
import com.corwin.system.notify.application.service.PushSubscriptionAppService;
import com.corwin.system.notify.application.view.PushHealthDeliveryView;
import com.corwin.system.notify.application.view.PushHealthSubscriptionView;
import com.corwin.system.notify.application.view.PushHealthView;
import com.corwin.system.notify.interfaces.web.req.SavePushSubscriptionReq;
import com.corwin.system.notify.interfaces.web.res.*;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Web Push 推送管理控制器
 *
 * @author Corwin 2026/3/19
 */
@ApiMeta(module = ApiModuleCode.SYSTEM)
@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class PushController {

    private final PushSubscriptionAppService pushSubscriptionAppService;
    private final PushHealthAppService pushHealthAppService;

    /**
     * 获取 Web Push 公钥
     */
    @GetMapping("/public-key")
    @PermitAll
    public ApiResponse<PushPublicKeyRes> publicKey() {
        String publicKey = pushSubscriptionAppService.getPublicKey();
        return ApiResponse.ok(new PushPublicKeyRes(publicKey));
    }

    /**
     * 保存或更新推送订阅
     */
    @PostMapping("/subscriptions")
    @Authenticated
    public ApiResponse<Boolean> saveSubscription(@Valid @RequestBody SavePushSubscriptionReq req) {
        Long userId = CtxUtil.getPrincipal().userId();
        UserType userType = CtxUtil.getPrincipal().userType();
        SavePushSubscriptionCommand command = new SavePushSubscriptionCommand(req.deviceId().trim(),
                req.endpoint().trim(), req.p256dh().trim(), req.auth().trim());
        pushSubscriptionAppService.saveSubscription(userId, userType, command);
        return ApiResponse.ok(true);
    }

    /**
     * 删除指定设备的推送订阅
     */
    @DeleteMapping("/subscriptions/{deviceId}")
    @Authenticated
    public ApiResponse<Boolean> removeSubscription(@PathVariable String deviceId) {
        Long userId = CtxUtil.getPrincipal().userId();
        UserType userType = CtxUtil.getPrincipal().userType();
        pushSubscriptionAppService.removeSubscription(userId, userType, deviceId);
        return ApiResponse.ok(true);
    }

    /**
     * 查询推送健康状态
     */
    @GetMapping("/health")
    @Authenticated
    public ApiResponse<PushHealthRes> health() {
        Long userId = CtxUtil.getPrincipal().userId();
        UserType userType = CtxUtil.getPrincipal().userType();
        PushHealthView view = pushHealthAppService.queryHealth(userId, userType);
        return ApiResponse.ok(toHealthRes(view));
    }

    /**
     * 发送高优先级推送健康检查消息
     */
    @PostMapping("/health/test")
    @Authenticated
    public ApiResponse<PushHealthTestRes> sendHealthTest() {
        Long userId = CtxUtil.getPrincipal().userId();
        UserType userType = CtxUtil.getPrincipal().userType();
        PushHealthDeliveryView delivery = pushHealthAppService.sendHighPriorityHealthCheck(userId, userType);
        PushHealthTestRes res = new PushHealthTestRes("健康检查推送已发送", toDeliveryRes(delivery));
        return ApiResponse.ok(res);
    }

    private PushHealthRes toHealthRes(PushHealthView view) {
        List<PushHealthSubscriptionRes> subscriptions = view.subscriptions().stream().map(this::toSubscriptionRes)
                .toList();
        return new PushHealthRes(view.vapidReady(), view.vapidSubject(), view.activeSubscriptionCount(),
                view.inactiveSubscriptionCount(), view.latestSubscriptionUpdatedAt(), view.latestSubscriptionPushAt(),
                view.latestSubscriptionError(), subscriptions, toDeliveryRes(view.latestDelivery()));
    }

    private PushHealthSubscriptionRes toSubscriptionRes(PushHealthSubscriptionView view) {
        return new PushHealthSubscriptionRes(view.deviceId(), view.active(), view.endpointHost(), view.updatedAt(),
                view.lastPushAt(), view.lastError());
    }

    private PushHealthDeliveryRes toDeliveryRes(PushHealthDeliveryView view) {
        if (view == null) {
            return null;
        }
        return new PushHealthDeliveryRes(view.id(), view.msgType(), view.priority(), view.status(), view.route(),
                view.createdAt(), view.sentAt(), view.ackedAt());
    }
}
