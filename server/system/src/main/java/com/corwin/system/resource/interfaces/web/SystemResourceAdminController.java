package com.corwin.system.resource.interfaces.web;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.web.response.ApiResponse;
import com.corwin.system.auth.published.Authorize;
import com.corwin.system.resource.application.service.SystemResourceAdminService;
import com.corwin.system.resource.application.view.SystemResourceDetailView;
import com.corwin.system.resource.application.view.SystemResourcePermissionSelectionView;
import com.corwin.system.resource.application.view.SystemResourceTreeItemView;
import com.corwin.system.resource.interfaces.web.req.SaveSystemResourceReq;
import com.corwin.system.resource.interfaces.web.req.UpdateSystemResourcePermissionsReq;
import com.corwin.system.resource.interfaces.web.res.SystemResourceDetailRes;
import com.corwin.system.resource.interfaces.web.res.SystemResourcePermissionSelectionRes;
import com.corwin.system.resource.interfaces.web.res.SystemResourceTreeItemRes;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Corwin 2026/6/29
 */
@ApiMeta(module = ApiModuleCode.SYSTEM)
@RestController
@RequestMapping("/api/system/resources")
@RequiredArgsConstructor
public class SystemResourceAdminController {

    private final SystemResourceAdminService systemResourceAdminService;

    @GetMapping("/tree")
    @Authorize(userType = UserType.INTERNAL, permissions = {"res.view"})
    public ApiResponse<List<SystemResourceTreeItemRes>> tree() {
        return ApiResponse.ok(systemResourceAdminService.tree().stream().map(this::toTreeRes).toList());
    }

    @GetMapping("/{id}")
    @Authorize(userType = UserType.INTERNAL, permissions = {"res.view"})
    public ApiResponse<SystemResourceDetailRes> get(@PathVariable Long id) {
        return ApiResponse.ok(toDetailRes(systemResourceAdminService.get(id)));
    }

    @PostMapping
    @Authorize(userType = UserType.INTERNAL, permissions = {"res.add"})
    public ApiResponse<SystemResourceDetailRes> create(@RequestBody SaveSystemResourceReq req) {
        return ApiResponse.ok(toDetailRes(systemResourceAdminService.create(req.parentId(), req.code(), req.name(),
                req.resourceType(), req.path(), req.component(), req.icon(), req.sortNo(), req.visible(),
                req.enabled(), req.defaultEntry(), req.systemBuiltin(), req.remark())));
    }

    @PutMapping("/{id}")
    @Authorize(userType = UserType.INTERNAL, permissions = {"res.edit"})
    public ApiResponse<SystemResourceDetailRes> update(@PathVariable Long id, @RequestBody SaveSystemResourceReq req) {
        return ApiResponse.ok(toDetailRes(systemResourceAdminService.update(id, req.parentId(), req.code(), req.name(),
                req.resourceType(), req.path(), req.component(), req.icon(), req.sortNo(), req.visible(),
                req.enabled(), req.defaultEntry(), req.systemBuiltin(), req.remark())));
    }

    @DeleteMapping("/{id}")
    @Authorize(userType = UserType.INTERNAL, permissions = {"res.del"})
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        return ApiResponse.ok(systemResourceAdminService.delete(id));
    }

    @GetMapping("/{resourceId}/permissions")
    @Authorize(userType = UserType.INTERNAL, permissions = {"res.perm.view"})
    public ApiResponse<SystemResourcePermissionSelectionRes> permissions(@PathVariable Long resourceId) {
        return ApiResponse.ok(toPermissionRes(systemResourceAdminService.permissions(resourceId)));
    }

    @PutMapping("/{resourceId}/permissions")
    @Authorize(userType = UserType.INTERNAL, permissions = {"res.perm.edit"})
    public ApiResponse<Boolean> updatePermissions(@PathVariable Long resourceId,
            @RequestBody UpdateSystemResourcePermissionsReq req) {
        return ApiResponse.ok(systemResourceAdminService.updatePermissions(resourceId,
                req == null ? null : req.permissionIds()));
    }

    private SystemResourceTreeItemRes toTreeRes(SystemResourceTreeItemView view) {
        return new SystemResourceTreeItemRes(view.id(), view.parentId(), view.code(), view.name(), view.resourceType(),
                view.path(), view.component(), view.icon(), view.sortNo(), view.visible(), view.enabled(),
                view.defaultEntry(), view.systemBuiltin(), view.remark(),
                view.permissionIds().stream().map(String::valueOf).toList(),
                view.children().stream().map(this::toTreeRes).toList());
    }

    private static SystemResourceDetailRes toDetailRes(SystemResourceDetailView view) {
        return new SystemResourceDetailRes(view.id(), view.parentId(), view.code(), view.name(), view.resourceType(),
                view.path(), view.component(), view.icon(), view.sortNo(), view.visible(), view.enabled(),
                view.defaultEntry(), view.systemBuiltin(), view.remark(),
                view.permissionIds().stream().map(String::valueOf).toList());
    }

    private static SystemResourcePermissionSelectionRes toPermissionRes(SystemResourcePermissionSelectionView view) {
        return new SystemResourcePermissionSelectionRes(view.permissionIds().stream().map(String::valueOf).toList());
    }
}
