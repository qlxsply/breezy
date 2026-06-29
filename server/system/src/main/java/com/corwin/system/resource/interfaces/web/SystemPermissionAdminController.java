package com.corwin.system.resource.interfaces.web;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.web.response.ApiResponse;
import com.corwin.system.auth.published.Authorize;
import com.corwin.system.resource.application.service.PermissionService;
import com.corwin.system.resource.domain.model.Permission;
import com.corwin.system.resource.interfaces.web.res.PermissionRes;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Corwin 2026/6/29
 */
@ApiMeta(module = ApiModuleCode.SYSTEM)
@RestController
@RequestMapping("/api/system/permissions")
@RequiredArgsConstructor
public class SystemPermissionAdminController {

    private final PermissionService permissionService;

    @GetMapping
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"res.perm.view", "res.perm.edit"}, anyPermission = true)
    public ApiResponse<List<PermissionRes>> list() {
        return ApiResponse.ok(permissionService.assignablePermissionsForInternal().stream()
                .map(SystemPermissionAdminController::toRes)
                .toList());
    }

    private static PermissionRes toRes(Permission permission) {
        return new PermissionRes(permission.getId(), permission.getCode(), permission.getName(),
                permission.getUserScope(), permission.getDescription(), Boolean.TRUE.equals(permission.getEnabled()));
    }
}
