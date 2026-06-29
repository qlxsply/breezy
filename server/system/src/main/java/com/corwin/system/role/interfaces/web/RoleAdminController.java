package com.corwin.system.role.interfaces.web;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.web.response.ApiResponse;
import com.corwin.system.audit.domain.model.AuditAction;
import com.corwin.system.audit.domain.model.AuditLevel;
import com.corwin.system.audit.domain.model.AuditResource;
import com.corwin.system.audit.published.Audit;
import com.corwin.system.auth.published.Authorize;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import com.corwin.system.role.application.command.CreateRoleCommand;
import com.corwin.system.role.application.command.UpdateRoleCommand;
import com.corwin.system.role.application.command.UpdateRoleGrantCommand;
import com.corwin.system.role.application.service.RoleAdminService;
import com.corwin.system.role.application.service.RoleGrantService;
import com.corwin.system.role.application.view.RoleGrantResourceView;
import com.corwin.system.role.application.view.RoleGrantSelectionView;
import com.corwin.system.role.domain.model.Role;
import com.corwin.system.role.interfaces.web.req.CreateRoleReq;
import com.corwin.system.role.interfaces.web.req.UpdateRoleGrantReq;
import com.corwin.system.role.interfaces.web.req.UpdateRoleReq;
import com.corwin.system.role.interfaces.web.res.RoleGrantResourceRes;
import com.corwin.system.role.interfaces.web.res.RoleGrantSelectionRes;
import com.corwin.system.role.interfaces.web.res.RoleRes;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Corwin 2026/1/23
 */
@ApiMeta(module = ApiModuleCode.SYSTEM)
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleAdminController {

    private final RoleAdminService roleAdminService;
    private final RoleGrantService roleGrantService;

    @GetMapping
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"rol.view"})
    public ApiResponse<List<RoleRes>> list() {
        return ApiResponse.ok(roleAdminService.list().stream().map(RoleAdminController::toDto).toList());
    }

    @PostMapping
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"rol.add"})
    @Audit(resource = AuditResource.ROLE, action = AuditAction.CREATE, level = AuditLevel.HIGH)
    public ApiResponse<RoleRes> create(@RequestBody CreateRoleReq req) {
        CreateRoleCommand cmd = new CreateRoleCommand(req.getCode(), req.getName(), req.getEnabled());
        return ApiResponse.ok(toDto(roleAdminService.create(cmd)));
    }

    @GetMapping("/{id}")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"rol.view"})
    public ApiResponse<RoleRes> get(@PathVariable Long id) {
        return ApiResponse.ok(toDto(roleAdminService.get(id)));
    }

    @PutMapping("/{id}")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"rol.edit"})
    @Audit(resource = AuditResource.ROLE, action = AuditAction.UPDATE, level = AuditLevel.HIGH)
    public ApiResponse<RoleRes> update(@PathVariable Long id, @RequestBody UpdateRoleReq req) {
        UpdateRoleCommand cmd = new UpdateRoleCommand(req.getCode(), req.getName(), req.getEnabled());
        return ApiResponse.ok(toDto(roleAdminService.update(id, cmd)));
    }

    @DeleteMapping("/{id}")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"rol.del"})
    @Audit(resource = AuditResource.ROLE, action = AuditAction.DELETE, level = AuditLevel.CRITICAL)
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        roleAdminService.delete(id);
        return ApiResponse.ok(true);
    }

    @GetMapping("/grant-resources")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"rol.perm.view"})
    public ApiResponse<List<RoleGrantResourceRes>> grantResources() {
        return ApiResponse.ok(roleGrantService.grantResources().stream().map(this::toGrantRes).toList());
    }

    @GetMapping("/{id}/grant")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"rol.perm.view"})
    public ApiResponse<RoleGrantSelectionRes> roleGrantSelection(@PathVariable Long id) {
        return ApiResponse.ok(toGrantSelectionRes(roleGrantService.roleGrantSelection(id)));
    }

    @PutMapping("/{id}/grant")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"rol.perm.edit"})
    @Audit(resource = AuditResource.ROLE_GRANT, action = AuditAction.GRANT, level = AuditLevel.HIGH)
    public ApiResponse<Boolean> updateRoleGrant(@PathVariable Long id, @RequestBody UpdateRoleGrantReq req) {
        UpdateRoleGrantCommand cmd = new UpdateRoleGrantCommand(req.menuIds(), req.functionIds());
        return ApiResponse.ok(roleGrantService.updateRoleGrant(id, cmd));
    }

    private RoleGrantResourceRes toGrantRes(RoleGrantResourceView view) {
        return new RoleGrantResourceRes(view.id(), view.parentId(), view.menuId(), view.functionId(), view.name(), view.code(),
                view.type(), view.description(), view.enabled(), view.selectable(), view.orderNo());
    }

    private RoleGrantSelectionRes toGrantSelectionRes(RoleGrantSelectionView view) {
        return new RoleGrantSelectionRes(view.menuIds().stream().map(String::valueOf).toList(),
                view.functionIds().stream().map(String::valueOf).toList());
    }

    private static RoleRes toDto(Role role) {
        return new RoleRes(String.valueOf(role.getId()), role.getCode(), role.getName(), role.isEnabled(),
                role.getCreatedBy(), role.getCreatedAt(), role.getUpdatedBy(), role.getUpdatedAt());
    }
}
