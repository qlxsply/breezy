package com.corwin.system.resource.interfaces.web;

import com.corwin.system.audit.domain.model.AuditLevel;
import com.corwin.system.audit.published.Audit;
import com.corwin.system.resource.application.service.PermissionService;
import com.corwin.system.resource.application.view.MyPermissionsDetailView;
import com.corwin.system.resource.domain.model.Permission;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import com.corwin.system.resource.interfaces.web.res.MyPermissionsDetailRes;
import com.corwin.system.resource.interfaces.web.res.MyPermissionsRes;
import com.corwin.system.resource.interfaces.web.res.PermissionRes;
import com.corwin.system.auth.published.Authorize;
import com.corwin.framework.constant.UserType;
import com.corwin.framework.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for permission queries.
 *
 * <p>Provides endpoints for listing assignable permissions and retrieving
 * the current user's granted permissions and details.</p>
 *
 * @author Corwin 2026/1/22
 */
@ApiMeta(module = ApiModuleCode.SYSTEM)
@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    /**
     * Lists all permissions assignable to admin roles.
     *
     * @return the list of permissions
     */
    @GetMapping
    @Authorize(userType = UserType.ADMIN, permissions = {"rol.perm.view", "rol.perm.edit"}, anyPermission = true)
    public ApiResponse<List<PermissionRes>> list() {
        return ApiResponse.ok(permissionService.assignablePermissionsForInternal().stream()
                .map(PermissionController::toRes)
                .toList());
    }

    /**
     * Returns the permission codes granted to the current admin user.
     *
     * @return the current user's permission codes
     */
    @GetMapping("/me")
    @Authorize(userType = UserType.ADMIN, permissions = {"sys.use"})
    public ApiResponse<MyPermissionsRes> myPermissions() {
        return ApiResponse.ok(new MyPermissionsRes(permissionService.permissionCodesForCurrent().stream().toList()));
    }

    /**
     * Returns detailed permission info for the current admin user,
     * including username, role names, and permission codes.
     *
     * @return the current user's permission details
     */
    @GetMapping("/me/details")
    @Authorize(userType = UserType.ADMIN, permissions = {"sys.use"})
    public ApiResponse<MyPermissionsDetailRes> myPermissionsDetails() {
        return ApiResponse.ok(toRes(permissionService.getPermissionDetailForCurrent()));
    }

    private static MyPermissionsDetailRes toRes(MyPermissionsDetailView view) {
        return new MyPermissionsDetailRes(view.username(), view.roles(), view.permissionCodes());
    }

    private static PermissionRes toRes(Permission permission) {
        return new PermissionRes(permission.getId(), permission.getCode(), permission.getName(),
                permission.getUserScope());
    }

}
