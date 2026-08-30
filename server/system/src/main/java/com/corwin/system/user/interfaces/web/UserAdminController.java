package com.corwin.system.user.interfaces.web;

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
import com.corwin.system.user.application.command.*;
import com.corwin.system.user.application.service.UserAdminService;
import com.corwin.system.user.application.service.UserRoleService;
import com.corwin.system.user.domain.model.User;
import com.corwin.system.user.interfaces.web.req.*;
import com.corwin.system.user.interfaces.web.res.UserRes;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for admin user management operations including CRUD, pagination, password reset,
 * role assignment, and batch operations.
 *
 * @author Corwin 2026/1/22
 */
@ApiMeta(module = ApiModuleCode.SYSTEM)
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserAdminController {

  private final UserAdminService userAdminService;
  private final UserRoleService userRoleService;

  /**
   * Returns all users ordered by ID ascending.
   *
   * @return a list of all users
   */
  @GetMapping
  @Authorize(
      userType = UserType.ADMIN,
      permissions = {"usr.view"})
  public ApiResponse<List<UserRes>> list() {
    return ApiResponse.ok(
        userAdminService.list().stream().map(UserAdminController::toDto).toList());
  }

  /**
   * Creates a new admin user with the given credentials and optional role assignments.
   *
   * @param req the create user request
   * @return the created user
   */
  @PostMapping
  @Authorize(
      userType = UserType.ADMIN,
      permissions = {"usr.add"})
  @Audit(resource = AuditResource.USER, action = AuditAction.CREATE, level = AuditLevel.HIGH)
  public ApiResponse<UserRes> create(@RequestBody CreateUserReq req) {
    CreateUserCommand cmd =
        new CreateUserCommand(
            req.getUsername(), req.getNickname(), req.getPassword(), req.getRoleIds());
    return ApiResponse.ok(toDto(userAdminService.create(cmd)));
  }

  /**
   * Retrieves a user by ID.
   *
   * @param id the user ID
   * @return the user
   */
  @GetMapping("/{id}")
  @Authorize(
      userType = UserType.ADMIN,
      permissions = {"usr.view"})
  public ApiResponse<UserRes> get(@PathVariable Long id) {
    return ApiResponse.ok(toDto(userAdminService.get(id)));
  }

  /**
   * Paginates users with optional status and username filters.
   *
   * @param req the page request with optional filters
   * @return a paginated result of users
   */
  @PostMapping("/page")
  @Authorize(
      userType = UserType.ADMIN,
      permissions = {"usr.view"})
  public ApiResponse<PageResult<UserRes>> page(@RequestBody UserPageReq req) {
    var page =
        userAdminService.page(
            req.status(), req.usernameLike(), PageSpecFactory.of(req.page(), req.sort()));
    return ApiResponse.ok(PageResult.of(page, UserAdminController::toDto));
  }

  /**
   * Updates the nickname and/or status of an existing user.
   *
   * @param id the user ID
   * @param req the update request
   * @return the updated user
   */
  @PutMapping("/{id}")
  @Authorize(
      userType = UserType.ADMIN,
      permissions = {"usr.edit"})
  @Audit(resource = AuditResource.USER, action = AuditAction.UPDATE, level = AuditLevel.HIGH)
  public ApiResponse<UserRes> update(@PathVariable Long id, @RequestBody UpdateUserReq req) {
    UpdateUserCommand cmd = new UpdateUserCommand(req.getNickname(), req.getStatus());
    return ApiResponse.ok(toDto(userAdminService.update(id, cmd)));
  }

  /**
   * Updates the status of multiple users in batch.
   *
   * @param req the batch status update request
   * @return true if successful
   */
  @PutMapping("/batch/status")
  @Authorize(
      userType = UserType.ADMIN,
      permissions = {"usr.edit"})
  @Audit(resource = AuditResource.USER, action = AuditAction.UPDATE, level = AuditLevel.HIGH)
  public ApiResponse<Boolean> batchUpdateStatus(@RequestBody BatchUpdateUserStatusReq req) {
    BatchUpdateUserStatusCommand cmd =
        new BatchUpdateUserStatusCommand(req.userIds(), req.status());
    userAdminService.batchUpdateStatus(cmd);
    return ApiResponse.ok(true);
  }

  /**
   * Resets a user's password to the default value.
   *
   * @param id the user ID
   * @return true if successful
   */
  @PostMapping("/{id}/reset-password")
  @Authorize(
      userType = UserType.ADMIN,
      permissions = {"usr.pwd.reset"})
  @Audit(
      resource = AuditResource.USER,
      action = AuditAction.RESET_PASSWORD,
      level = AuditLevel.CRITICAL)
  public ApiResponse<Boolean> resetPassword(@PathVariable Long id) {
    userAdminService.resetPassword(id);
    return ApiResponse.ok(true);
  }

  /**
   * Resets passwords for multiple users in batch.
   *
   * @param req the batch request containing user IDs
   * @return true if successful
   */
  @PostMapping("/batch/reset-password")
  @Authorize(
      userType = UserType.ADMIN,
      permissions = {"usr.pwd.reset"})
  @Audit(
      resource = AuditResource.USER,
      action = AuditAction.RESET_PASSWORD,
      level = AuditLevel.CRITICAL)
  public ApiResponse<Boolean> batchResetPassword(@RequestBody BatchUserIdsReq req) {
    userAdminService.batchResetPassword(new BatchUserIdsCommand(req.userIds()));
    return ApiResponse.ok(true);
  }

  /**
   * Returns the role IDs assigned to a given user.
   *
   * @param id the user ID
   * @return a list of role ID strings
   */
  @GetMapping("/{id}/roles")
  @Authorize(
      userType = UserType.ADMIN,
      permissions = {"usr.role.view"})
  public ApiResponse<List<String>> userRoles(@PathVariable Long id) {
    return ApiResponse.ok(userRoleService.userRoles(id).stream().map(String::valueOf).toList());
  }

  /**
   * Replaces all role assignments for a user.
   *
   * @param id the user ID
   * @param req the request containing the new role IDs
   * @return true if successful
   */
  @PutMapping("/{id}/roles")
  @Authorize(
      userType = UserType.ADMIN,
      permissions = {"usr.perm.edit"})
  @Audit(resource = AuditResource.USER_ROLE, action = AuditAction.UPDATE, level = AuditLevel.HIGH)
  public ApiResponse<Boolean> updateUserRoles(
      @PathVariable Long id, @RequestBody UpdateUserRolesReq req) {
    UpdateUserRolesCommand cmd = new UpdateUserRolesCommand(req.getRoleIds());
    return ApiResponse.ok(userRoleService.updateUserRoles(id, cmd));
  }

  /**
   * Deletes a user and their associated role assignments.
   *
   * @param id the user ID
   * @return true if successful
   */
  @DeleteMapping("/{id}")
  @Authorize(
      userType = UserType.ADMIN,
      permissions = {"usr.del"})
  @Audit(resource = AuditResource.USER, action = AuditAction.DELETE, level = AuditLevel.CRITICAL)
  public ApiResponse<Boolean> delete(@PathVariable Long id) {
    userAdminService.delete(id);
    return ApiResponse.ok(true);
  }

  /**
   * Deletes multiple users in batch.
   *
   * @param req the batch request containing user IDs
   * @return true if successful
   */
  @PostMapping("/batch/delete")
  @Authorize(
      userType = UserType.ADMIN,
      permissions = {"usr.del"})
  @Audit(resource = AuditResource.USER, action = AuditAction.DELETE, level = AuditLevel.CRITICAL)
  public ApiResponse<Boolean> batchDelete(@RequestBody BatchUserIdsReq req) {
    userAdminService.batchDelete(new BatchUserIdsCommand(req.userIds()));
    return ApiResponse.ok(true);
  }

  private static UserRes toDto(User user) {
    return new UserRes(
        String.valueOf(user.getId()),
        user.getUsername(),
        user.getNickname(),
        user.getUserType(),
        user.getUserStatus(),
        user.isDeletedFlag(),
        user.getCreatedBy(),
        user.getCreatedAt(),
        user.getUpdatedBy(),
        user.getUpdatedAt());
  }
}
