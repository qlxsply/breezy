package com.corwin.system.auth.interfaces.web;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.web.request.PageSpecFactory;
import com.corwin.framework.web.response.ApiResponse;
import com.corwin.framework.web.response.PageResult;
import com.corwin.system.auth.application.service.LoginLogService;
import com.corwin.system.auth.domain.model.LoginEvent;
import com.corwin.system.auth.interfaces.web.req.LoginLogPageReq;
import com.corwin.system.auth.interfaces.web.res.LoginLogRes;
import com.corwin.system.auth.published.Authorize;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Corwin 2026/1/23
 */
@ApiMeta(module = ApiModuleCode.SYSTEM)
@RestController
@RequestMapping("/api/sys/login-logs")
@RequiredArgsConstructor
public class LoginLogController {

    private final LoginLogService loginLogService;

    @PostMapping("/page")
    @Authorize(userType = UserType.INTERNAL, permissions = {"log.view"})
    public ApiResponse<PageResult<LoginLogRes>> page(@RequestBody LoginLogPageReq req) {
        var page = loginLogService.page(req.userAccount(), req.startAt(), req.endAt(),
                PageSpecFactory.of(req.page(), req.sort()));
        return ApiResponse.ok(PageResult.of(page, LoginLogController::toDto));
    }

    private static LoginLogRes toDto(LoginEvent log) {
        return new LoginLogRes(log.getId(), log.getUserId(), log.getUsername(), log.getEventType(), log.isSuccess(),
                log.getLoginIp(), log.getFailureReason(), log.getSessionId(), log.getOperatorId(), log.getOccurredAt(),
                log.getRemark());
    }
}
