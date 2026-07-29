package com.corwin.system.notify.interfaces.web;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.web.ctx.CtxUtil;
import com.corwin.framework.web.request.PageSpecFactory;
import com.corwin.framework.web.response.ApiResponse;
import com.corwin.framework.web.response.PageResult;
import com.corwin.system.auth.published.Authenticated;
import com.corwin.system.notify.application.service.NotificationAppService;
import com.corwin.system.notify.application.view.NotificationPullView;
import com.corwin.system.notify.application.view.NotificationView;
import com.corwin.system.notify.interfaces.web.req.NotificationPageReq;
import com.corwin.system.notify.interfaces.web.res.NotificationPullRes;
import com.corwin.system.notify.interfaces.web.res.NotificationRes;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通知消息控制器
 *
 * @author Corwin 2026/3/30
 */
@ApiMeta(module = ApiModuleCode.SYSTEM)
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationAppService notificationAppService;

    /**
     * 分页查询通知消息
     */
    @PostMapping("/page")
    @Authenticated
    public ApiResponse<PageResult<NotificationRes>> page(@RequestBody NotificationPageReq req) {
        Long userId = CtxUtil.getPrincipal().userId();
        UserType userType = CtxUtil.getPrincipal().userType();
        String status = req.status() == null || req.status().isBlank() ? "all" : req.status();
        PageSpec spec = PageSpecFactory.of(req.page(), null);
        PageData<NotificationView> pageData = notificationAppService.page(userId, userType, status, spec);
        PageResult<NotificationRes> pageResult = PageResult.of(pageData, NotificationController::toRes);
        return ApiResponse.ok(pageResult);
    }

    /**
     * 查询未读通知列表
     */
    @GetMapping("/unread")
    @Authenticated
    public ApiResponse<List<NotificationRes>> unread(@RequestParam(defaultValue = "10") int limit) {
        Long userId = CtxUtil.getPrincipal().userId();
        UserType userType = CtxUtil.getPrincipal().userType();
        List<NotificationView> list = notificationAppService.listUnread(userId, userType, limit);
        return ApiResponse.ok(list.stream().map(NotificationController::toRes).toList());
    }

    /**
     * 查询未读通知数量
     */
    @GetMapping("/unread/count")
    @Authenticated
    public ApiResponse<Long> unreadCount() {
        Long userId = CtxUtil.getPrincipal().userId();
        UserType userType = CtxUtil.getPrincipal().userType();
        return ApiResponse.ok(notificationAppService.getUnreadCount(userId, userType));
    }

    /**
     * 拉取指定时间点之后的未读通知
     * <p>
     * 按 createdAt 升序返回，用于增量拉取。
     */
    @GetMapping("/unread/pull")
    @Authenticated
    public ApiResponse<NotificationPullRes> pullUnread(@RequestParam(required = false, defaultValue = "0") long after,
            @RequestParam(defaultValue = "50") int limit) {
        Long userId = CtxUtil.getPrincipal().userId();
        UserType userType = CtxUtil.getPrincipal().userType();
        return ApiResponse.ok(toPullRes(notificationAppService.pullUnread(userId, userType, after, limit)));
    }

    /**
     * 标记单条通知为已读
     */
    @PutMapping("/{id}/read")
    @Authenticated
    public ApiResponse<Boolean> markRead(@PathVariable Long id) {
        Long userId = CtxUtil.getPrincipal().userId();
        UserType userType = CtxUtil.getPrincipal().userType();
        notificationAppService.markRead(userId, userType, id);
        return ApiResponse.ok(true);
    }

    /**
     * 标记全部通知为已读
     */
    @PutMapping("/read-all")
    @Authenticated
    public ApiResponse<Boolean> markAllRead() {
        Long userId = CtxUtil.getPrincipal().userId();
        UserType userType = CtxUtil.getPrincipal().userType();
        notificationAppService.markAllRead(userId, userType);
        return ApiResponse.ok(true);
    }

    private static NotificationRes toRes(NotificationView view) {
        return new NotificationRes(view.id(), view.title(), view.content(), view.type(), view.priority(), view.route(),
                view.createdAt(), view.read());
    }

    private static NotificationPullRes toPullRes(NotificationPullView view) {
        return new NotificationPullRes(view.items().stream().map(NotificationController::toRes).toList(),
                view.lastPullAt());
    }

}
