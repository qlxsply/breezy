package com.corwin.reminder.interfaces.web;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.framework.web.auth.AuthPrincipal;
import com.corwin.framework.web.ctx.CtxUtil;
import com.corwin.framework.web.response.ApiResponse;
import com.corwin.reminder.application.service.ReminderAppService;
import com.corwin.reminder.application.view.ReminderPollView;
import com.corwin.reminder.interfaces.web.req.ReminderPollReq;
import com.corwin.reminder.interfaces.web.res.ReminderPollRes;
import com.corwin.system.auth.published.Authorize;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 *
 * @author Corwin 2026/1/12
 */
@ApiMeta(module = ApiModuleCode.REMINDER)
@Authorize(userTypes = UserType.EXTERNAL, permissions = {"tdo.use"})
@RestController
@RequestMapping("/api/reminder")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderAppService reminderAppService;

    /**
     * Poll outbox reminders newer than the provided start time.
     */
    @PostMapping("/outbox/poll")
    public ApiResponse<ReminderPollRes> poll(@RequestBody ReminderPollReq req) {
        BizAssert.notNull(req, BaseError.BODY_NOT_READABLE);
        BizAssert.notNull(req.getStartTime(), BaseError.MISSING_PARAMETER);

        Long userId = currentUserId();
        Instant startTime = req.getStartTime();
        ReminderPollView result = reminderAppService.poll(userId, startTime);
        return ApiResponse.ok(new ReminderPollRes(result.cutoffTime(), result.items()));
    }

    @PostMapping("/outbox/{id}/ack")
    public ApiResponse<Object> ack(@PathVariable Long id) {
        Long userId = currentUserId();
        reminderAppService.ackOutbox(userId, id);
        return ApiResponse.ok();
    }

    @PostMapping("/deliveries/{deliveryId}/ack")
    public ApiResponse<Object> ackDelivery(@PathVariable Long deliveryId) {
        Long userId = currentUserId();
        reminderAppService.ackDelivery(userId, deliveryId);
        return ApiResponse.ok();
    }

    private Long currentUserId() {
        AuthPrincipal principal = CtxUtil.getPrincipal();
        Long userId = principal == null ? null : principal.userId();
        BizAssert.notNull(userId, BaseError.FORBIDDEN);
        return userId;
    }
}
