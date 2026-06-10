package com.corwin.system.resource.interfaces.web;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.web.response.ApiResponse;
import com.corwin.system.auth.published.Authenticated;
import com.corwin.system.resource.application.service.AdminMenuResourceService;
import com.corwin.system.resource.application.view.AdminMenuResourceView;
import com.corwin.system.resource.application.view.AdminMenuResourcesView;
import com.corwin.system.resource.interfaces.web.res.AdminMenuResourceRes;
import com.corwin.system.resource.interfaces.web.res.AdminMenuResourcesRes;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Corwin 2026/5/31
 */
@ApiMeta(module = ApiModuleCode.SYSTEM)
@RestController
@RequestMapping("/api/admin/menu-resources")
@RequiredArgsConstructor
public class AdminMenuResourceController {

    private final AdminMenuResourceService adminMenuResourceService;

    @GetMapping
    @Authenticated(userTypes = {UserType.INTERNAL})
    public ApiResponse<AdminMenuResourcesRes> currentAdminMenuResources() {
        return ApiResponse.ok(toRes(adminMenuResourceService.currentAdminMenuResources()));
    }

    private AdminMenuResourcesRes toRes(AdminMenuResourcesView view) {
        return new AdminMenuResourcesRes(view.resources().stream().map(this::toRes).toList());
    }

    private AdminMenuResourceRes toRes(AdminMenuResourceView view) {
        return new AdminMenuResourceRes(view.id(), view.parentId(), view.name(), view.icon(), view.description(),
                view.code(), view.type(), view.scope(), view.openMode(), view.url(), view.loadTarget(),
                view.orderNo(), view.level(), view.enabled(), view.guestAccess());
    }
}
