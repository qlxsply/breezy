package com.corwin.system.resource.interfaces.web;

import com.corwin.framework.web.response.ApiResponse;
import com.corwin.system.auth.published.PermitAll;
import com.corwin.system.resource.application.service.UserToolPageService;
import com.corwin.system.resource.application.view.UserToolPageView;
import com.corwin.system.resource.application.view.UserToolsView;
import com.corwin.system.resource.interfaces.web.res.UserToolPageRes;
import com.corwin.system.resource.interfaces.web.res.UserToolsRes;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Corwin 2026/6/6
 */
@ApiMeta(module = ApiModuleCode.SYSTEM)
@RestController
@RequestMapping("/api/user/tools")
@RequiredArgsConstructor
public class UserToolController {

    private final UserToolPageService userToolPageService;

    @GetMapping
    @PermitAll
    public ApiResponse<UserToolsRes> currentUserTools() {
        return ApiResponse.ok(toRes(userToolPageService.currentUserTools()));
    }

    private UserToolsRes toRes(UserToolsView view) {
        return new UserToolsRes(view.tools().stream().map(this::toRes).toList(), view.permissionCodes());
    }

    private UserToolPageRes toRes(UserToolPageView view) {
        return new UserToolPageRes(view.id(), view.name(), view.icon(), view.description(), view.code(),
                view.path(), view.component(), view.sortNo(), view.level(), view.enabled(), view.guestAccess());
    }
}
