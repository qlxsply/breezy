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
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Admin REST controller for managing system resources (menu/function/button tree).
 *
 * <p>Provides CRUD operations for the system resource tree, including permission binding
 * management. All endpoints require ADMIN authentication.
 *
 * @author Corwin 2026/6/29
 */
@ApiMeta(module = ApiModuleCode.SYSTEM)
@RestController
@RequestMapping("/api/system/resources")
@RequiredArgsConstructor
public class SystemResourceAdminController {

  private final SystemResourceAdminService systemResourceAdminService;

  /**
   * Returns the full resource tree with permission bindings.
   *
   * @return the list of tree items
   */
  @GetMapping("/tree")
  @Authorize(
      userType = UserType.ADMIN,
      permissions = {"res.view"})
  public ApiResponse<List<SystemResourceTreeItemRes>> tree() {
    return ApiResponse.ok(systemResourceAdminService.tree().stream().map(this::toTreeRes).toList());
  }

  /**
   * Returns the detail of a specific resource by its ID.
   *
   * @param id the resource ID
   * @return the resource detail
   */
  @GetMapping("/{id}")
  @Authorize(
      userType = UserType.ADMIN,
      permissions = {"res.view"})
  public ApiResponse<SystemResourceDetailRes> get(@PathVariable Long id) {
    return ApiResponse.ok(toDetailRes(systemResourceAdminService.get(id)));
  }

  /**
   * Creates a new system resource.
   *
   * @param req the request containing resource properties
   * @return the created resource detail
   */
  @PostMapping
  @Authorize(
      userType = UserType.ADMIN,
      permissions = {"res.add"})
  public ApiResponse<SystemResourceDetailRes> create(@RequestBody SaveSystemResourceReq req) {
    return ApiResponse.ok(
        toDetailRes(
            systemResourceAdminService.create(
                req.parentId(),
                req.code(),
                req.name(),
                req.resourceType(),
                req.path(),
                req.component(),
                req.icon(),
                req.sortNo(),
                req.visible(),
                req.enabled(),
                req.defaultEntry(),
                req.systemBuiltin(),
                req.remark())));
  }

  /**
   * Updates an existing system resource.
   *
   * @param id the resource ID
   * @param req the request containing updated resource properties
   * @return the updated resource detail
   */
  @PutMapping("/{id}")
  @Authorize(
      userType = UserType.ADMIN,
      permissions = {"res.edit"})
  public ApiResponse<SystemResourceDetailRes> update(
      @PathVariable Long id, @RequestBody SaveSystemResourceReq req) {
    return ApiResponse.ok(
        toDetailRes(
            systemResourceAdminService.update(
                id,
                req.parentId(),
                req.code(),
                req.name(),
                req.resourceType(),
                req.path(),
                req.component(),
                req.icon(),
                req.sortNo(),
                req.visible(),
                req.enabled(),
                req.defaultEntry(),
                req.systemBuiltin(),
                req.remark())));
  }

  /**
   * Deletes a system resource and all its descendants.
   *
   * @param id the resource ID
   * @return true if the deletion was successful
   */
  @DeleteMapping("/{id}")
  @Authorize(
      userType = UserType.ADMIN,
      permissions = {"res.del"})
  public ApiResponse<Boolean> delete(@PathVariable Long id) {
    return ApiResponse.ok(systemResourceAdminService.delete(id));
  }

  /**
   * Returns the permission IDs currently bound to a resource.
   *
   * @param resourceId the resource ID
   * @return the permission selection data
   */
  @GetMapping("/{resourceId}/permissions")
  @Authorize(
      userType = UserType.ADMIN,
      permissions = {"res.perm.view"})
  public ApiResponse<SystemResourcePermissionSelectionRes> permissions(
      @PathVariable Long resourceId) {
    return ApiResponse.ok(toPermissionRes(systemResourceAdminService.permissions(resourceId)));
  }

  /**
   * Updates the permission bindings for a resource.
   *
   * @param resourceId the resource ID
   * @param req the request containing the new permission IDs
   * @return true if the update was successful
   */
  @PutMapping("/{resourceId}/permissions")
  @Authorize(
      userType = UserType.ADMIN,
      permissions = {"res.perm.edit"})
  public ApiResponse<Boolean> updatePermissions(
      @PathVariable Long resourceId, @RequestBody UpdateSystemResourcePermissionsReq req) {
    return ApiResponse.ok(
        systemResourceAdminService.updatePermissions(
            resourceId, req == null ? null : req.permissionIds()));
  }

  private SystemResourceTreeItemRes toTreeRes(SystemResourceTreeItemView view) {
    return new SystemResourceTreeItemRes(
        view.id(),
        view.parentId(),
        view.code(),
        view.name(),
        view.resourceType(),
        view.path(),
        view.component(),
        view.icon(),
        view.sortNo(),
        view.visible(),
        view.enabled(),
        view.defaultEntry(),
        view.systemBuiltin(),
        view.remark(),
        view.permissionIds().stream().map(String::valueOf).toList(),
        view.children().stream().map(this::toTreeRes).toList());
  }

  private static SystemResourceDetailRes toDetailRes(SystemResourceDetailView view) {
    return new SystemResourceDetailRes(
        view.id(),
        view.parentId(),
        view.code(),
        view.name(),
        view.resourceType(),
        view.path(),
        view.component(),
        view.icon(),
        view.sortNo(),
        view.visible(),
        view.enabled(),
        view.defaultEntry(),
        view.systemBuiltin(),
        view.remark(),
        view.permissionIds().stream().map(String::valueOf).toList());
  }

  private static SystemResourcePermissionSelectionRes toPermissionRes(
      SystemResourcePermissionSelectionView view) {
    return new SystemResourcePermissionSelectionRes(
        view.permissionIds().stream().map(String::valueOf).toList());
  }
}
