package com.corwin.system.role.interfaces.web;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.web.request.PageSpecFactory;
import com.corwin.framework.web.response.ApiResponse;
import com.corwin.framework.web.response.PageResult;
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
import com.corwin.system.role.interfaces.web.req.RolePageReq;
import com.corwin.system.role.interfaces.web.req.UpdateRoleGrantReq;
import com.corwin.system.role.interfaces.web.req.UpdateRoleReq;
import com.corwin.system.role.interfaces.web.res.RoleGrantResourceRes;
import com.corwin.system.role.interfaces.web.res.RoleGrantSelectionRes;
import com.corwin.system.role.interfaces.web.res.RoleRes;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing role administration and grant management endpoints.
 *
 * @author Corwin 2026/1/23
 */
@ApiMeta(module = ApiModuleCode.SYSTEM)
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleAdminController {

  private final RoleAdminService roleAdminService;
  private final RoleGrantService roleGrantService;

  /** Returns all roles. */
  @GetMapping
  @Authorize(
      userType = UserType.ADMIN,
      permissions = {"rol.view"})
  public ApiResponse<List<RoleRes>> list() {
    return ApiResponse.ok(
        roleAdminService.list().stream().map(RoleAdminController::toDto).toList());
  }

  /** Paginated role list with keyword and enabled filters. */
  @PostMapping("/page")
  @Authorize(
      userType = UserType.ADMIN,
      permissions = {"rol.view"})
  public ApiResponse<PageResult<RoleRes>> page(@RequestBody RolePageReq req) {
    return ApiResponse.ok(
        PageResult.of(
            roleAdminService.page(
                req.keyword(), req.enabled(), PageSpecFactory.of(req.page(), req.sort())),
            RoleAdminController::toDto));
  }

  /** Creates a new role. */
  /** Creates a new role. */
  @PostMapping
  @Authorize(
      userType = UserType.ADMIN,
      permissions = {"rol.add"})
  @Audit(resource = AuditResource.ROLE, action = AuditAction.CREATE, level = AuditLevel.HIGH)
  public ApiResponse<RoleRes> create(@RequestBody CreateRoleReq req) {
    CreateRoleCommand cmd = new CreateRoleCommand(req.getCode(), req.getName(), req.getEnabled());
    return ApiResponse.ok(toDto(roleAdminService.create(cmd)));
  }

  /** Retrieves a single role by ID. */
  /** Retrieves a single role by ID. */
  @GetMapping("/{id}")
  @Authorize(
      userType = UserType.ADMIN,
      permissions = {"rol.view"})
  public ApiResponse<RoleRes> get(@PathVariable Long id) {
    return ApiResponse.ok(toDto(roleAdminService.get(id)));
  }

  /** Updates an existing role. */
  /** Updates an existing role. */
  @PutMapping("/{id}")
  @Authorize(
      userType = UserType.ADMIN,
      permissions = {"rol.edit"})
  @Audit(resource = AuditResource.ROLE, action = AuditAction.UPDATE, level = AuditLevel.HIGH)
  public ApiResponse<RoleRes> update(@PathVariable Long id, @RequestBody UpdateRoleReq req) {
    UpdateRoleCommand cmd = new UpdateRoleCommand(req.getCode(), req.getName(), req.getEnabled());
    return ApiResponse.ok(toDto(roleAdminService.update(id, cmd)));
  }

  /** Deletes a role by ID. */
  /** Deletes a role by ID. */
  @DeleteMapping("/{id}")
  @Authorize(
      userType = UserType.ADMIN,
      permissions = {"rol.del"})
  @Audit(resource = AuditResource.ROLE, action = AuditAction.DELETE, level = AuditLevel.CRITICAL)
  public ApiResponse<Boolean> delete(@PathVariable Long id) {
    roleAdminService.delete(id);
    return ApiResponse.ok(true);
  }

  /** Returns all resources available for role grant configuration. */
  @GetMapping("/grant-resources")
  @Authorize(
      userType = UserType.ADMIN,
      permissions = {"rol.perm.view"})
  public ApiResponse<List<RoleGrantResourceRes>> grantResources() {
    return ApiResponse.ok(
        roleGrantService.grantResources().stream().map(this::toGrantRes).toList());
  }

  /** Returns the currently granted resource IDs for a given role. */
  @GetMapping("/{id}/grant")
  @Authorize(
      userType = UserType.ADMIN,
      permissions = {"rol.perm.view"})
  public ApiResponse<RoleGrantSelectionRes> roleGrantSelection(@PathVariable Long id) {
    return ApiResponse.ok(toGrantSelectionRes(roleGrantService.roleGrantSelection(id)));
  }

  /** Updates the resource grants assigned to a role. */
  @PutMapping("/{id}/grant")
  @Authorize(
      userType = UserType.ADMIN,
      permissions = {"rol.perm.edit"})
  @Audit(resource = AuditResource.ROLE_GRANT, action = AuditAction.GRANT, level = AuditLevel.HIGH)
  public ApiResponse<Boolean> updateRoleGrant(
      @PathVariable Long id, @RequestBody UpdateRoleGrantReq req) {
    UpdateRoleGrantCommand cmd = new UpdateRoleGrantCommand(req.resourceIds());
    return ApiResponse.ok(roleGrantService.updateRoleGrant(id, cmd));
  }

  private RoleGrantResourceRes toGrantRes(RoleGrantResourceView view) {
    return new RoleGrantResourceRes(
        view.id(),
        view.parentId(),
        view.resourceId(),
        view.name(),
        view.code(),
        view.type(),
        view.description(),
        view.enabled(),
        view.selectable(),
        view.orderNo());
  }

  private RoleGrantSelectionRes toGrantSelectionRes(RoleGrantSelectionView view) {
    return new RoleGrantSelectionRes(view.resourceIds().stream().map(String::valueOf).toList());
  }

  private static RoleRes toDto(Role role) {
    return new RoleRes(
        String.valueOf(role.getId()),
        role.getCode(),
        role.getName(),
        role.isEnabled(),
        role.getCreatedBy(),
        role.getCreatedAt(),
        role.getUpdatedBy(),
        role.getUpdatedAt());
  }
}
