package com.corwin.reminder.interfaces.web;

import com.corwin.reminder.application.command.UpsertScheduleCommand;
import com.corwin.reminder.application.service.ScheduleAppService;
import com.corwin.reminder.domain.model.RecurrenceRule;
import com.corwin.reminder.domain.model.ScheduleEvent;
import com.corwin.reminder.interfaces.web.req.SchedulePageReq;
import com.corwin.reminder.interfaces.web.req.UpsertScheduleReq;
import com.corwin.reminder.interfaces.web.res.ScheduleRes;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import com.corwin.system.auth.published.Authorize;
import com.corwin.framework.constant.UserType;
import com.corwin.framework.web.request.PageSpecFactory;
import com.corwin.framework.web.response.ApiResponse;
import com.corwin.framework.web.response.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 *
 * @author Corwin 2026/1/12
 */
@ApiMeta(module = ApiModuleCode.REMINDER)
@Authorize(userType = UserType.USER, permissions = {"tdo.use"})
@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleAppService service;

    @PostMapping
    public ApiResponse<Long> upsert(@RequestBody UpsertScheduleReq req) {
        UpsertScheduleReq.RuleReq ruleReq = req.getRule();
        UpsertScheduleCommand.RuleCommand rule = null;
        if (ruleReq != null) {
            rule = new UpsertScheduleCommand.RuleCommand(ruleReq.getFrequency(), ruleReq.getInterval(),
                    ruleReq.getDaysOfWeek(), ruleReq.getDayOfMonth(), ruleReq.getMonthOfYear(), ruleReq.getUntil(),
                    ruleReq.getCountLimit());
        }
        UpsertScheduleCommand cmd = new UpsertScheduleCommand(req.getId(), req.getTitle(), req.getStartTime(),
                req.getEndTime(), req.getEventTimeZoneId(), rule, req.getAdvanceSecondsList(), req.getNote());
        return ApiResponse.ok(service.upsert(cmd));
    }

    @GetMapping("/{id}")
    public ApiResponse<ScheduleRes> get(@PathVariable Long id) {
        return ApiResponse.ok(toDto(service.get(id)));
    }

    @PostMapping("/page")
    public ApiResponse<PageResult<ScheduleRes>> page(@RequestBody SchedulePageReq req) {
        var page = service.page(req.status(), req.titleLike(), PageSpecFactory.of(req.page(), req.sort()));
        return ApiResponse.ok(PageResult.of(page, ScheduleController::toDto));
    }

    @PostMapping("/{id}/pause")
    public ApiResponse<Object> pause(@PathVariable Long id) {
        service.pause(id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/activate")
    public ApiResponse<Object> activate(@PathVariable Long id) {
        service.activate(id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<Object> cancel(@PathVariable Long id) {
        service.cancel(id);
        return ApiResponse.ok();
    }

    private static ScheduleRes toDto(ScheduleEvent e) {
        RecurrenceRule r = e.getRule();
        ScheduleRes.RuleRes rule = new ScheduleRes.RuleRes(r.getFrequency(), r.getInterval(), r.getDaysOfWeek(),
                r.getDayOfMonth(), r.getMonthOfYear(), r.getUntil(), r.getCountLimit());

        List<Integer> adv = e.getAdvanceSecondsList() == null ? List.of() : e.getAdvanceSecondsList();
        return new ScheduleRes(e.getId(), e.getTitle(), e.getStartTime(), e.getEndTime(), e.getEventTimeZoneId(), rule,
                adv, e.getStatus(), e.getNote());
    }
}
