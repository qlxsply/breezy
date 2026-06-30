package com.corwin.system.user.interfaces.web;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.web.response.ApiResponse;
import com.corwin.framework.web.response.PageResult;
import com.corwin.system.auth.published.Authenticated;
import com.corwin.system.auth.application.service.LoginLogService;
import com.corwin.system.auth.domain.model.LoginEvent;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import com.corwin.system.user.application.command.UpdateAdminProfileCommand;
import com.corwin.system.user.application.service.AdminProfileAppService;
import com.corwin.system.user.application.view.AdminProfileLoginActivityView;
import com.corwin.system.user.application.view.AdminProfileView;
import com.corwin.system.user.interfaces.web.req.UpdateAdminProfileReq;
import com.corwin.system.user.interfaces.web.res.AdminProfileLoginActivityRes;
import com.corwin.system.user.interfaces.web.res.AdminProfileRes;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Corwin 2026/6/4
 */
@ApiMeta(module = ApiModuleCode.SYSTEM)
@RestController
@RequestMapping("/api/admin/profile")
@RequiredArgsConstructor
public class AdminProfileController {

    private final AdminProfileAppService adminProfileAppService;
    private final LoginLogService loginLogService;

    @GetMapping
    @Authenticated(userType = UserType.INTERNAL)
    public ApiResponse<AdminProfileRes> currentProfile() {
        return ApiResponse.ok(toRes(adminProfileAppService.currentProfile()));
    }

    @GetMapping("/login-activities")
    @Authenticated(userType = UserType.INTERNAL)
    public ApiResponse<PageResult<AdminProfileLoginActivityRes>> pageLoginActivities(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize) {
        String username = adminProfileAppService.currentUsername();
        var page = loginLogService.pageOwnLoginActivities(username, new PageSpec(pageNo, pageSize, List.of()));
        return ApiResponse.ok(PageResult.of(page, AdminProfileController::toActivityRes));
    }

    @PutMapping
    @Authenticated(userType = UserType.INTERNAL)
    public ApiResponse<AdminProfileRes> updateMyProfile(@RequestBody UpdateAdminProfileReq req) {
        return ApiResponse.ok(toRes(adminProfileAppService.updateMyProfile(new UpdateAdminProfileCommand(req.nickname()))));
    }

    private static AdminProfileRes toRes(AdminProfileView view) {
        return new AdminProfileRes(String.valueOf(view.id()), view.username(), view.nickname(), view.userType(),
                view.status(), view.mustChangePassword(), view.lastPasswordChangedAt(), view.createdAt(),
                view.updatedAt(), view.recentActivities().stream().map(AdminProfileController::toActivityRes).toList());
    }

    private static AdminProfileLoginActivityRes toActivityRes(AdminProfileLoginActivityView view) {
        return new AdminProfileLoginActivityRes(String.valueOf(view.id()), view.eventType(), view.success(),
                view.loginIp(), view.remark(), view.occurredAt());
    }

    private static AdminProfileLoginActivityRes toActivityRes(LoginEvent event) {
        return new AdminProfileLoginActivityRes(String.valueOf(event.getId()), event.getEventType(), event.isSuccess(),
                event.getLoginIp(), event.getRemark(), event.getOccurredAt());
    }
}
