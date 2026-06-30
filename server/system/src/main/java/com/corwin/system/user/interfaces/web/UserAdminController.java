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
import com.corwin.system.user.application.command.CreateUserCommand;
import com.corwin.system.user.application.command.UpdateUserCommand;
import com.corwin.system.user.application.command.UpdateUserRolesCommand;
import com.corwin.system.user.application.service.UserAdminService;
import com.corwin.system.user.application.service.UserRoleService;
import com.corwin.system.user.domain.model.User;
import com.corwin.system.user.interfaces.web.req.CreateUserReq;
import com.corwin.system.user.interfaces.web.req.UpdateUserReq;
import com.corwin.system.user.interfaces.web.req.UpdateUserRolesReq;
import com.corwin.system.user.interfaces.web.req.UserPageReq;
import com.corwin.system.user.interfaces.web.res.UserRes;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Corwin 2026/1/22
 */
@ApiMeta(module = ApiModuleCode.SYSTEM)
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserAdminService userAdminService;
    private final UserRoleService userRoleService;

    @GetMapping
    @Authorize(userType = UserType.INTERNAL, permissions = {"usr.view"})
    public ApiResponse<List<UserRes>> list() {
        return ApiResponse.ok(userAdminService.list().stream().map(UserAdminController::toDto).toList());
    }

    @PostMapping
    @Authorize(userType = UserType.INTERNAL, permissions = {"usr.add"})
    @Audit(resource = AuditResource.USER, action = AuditAction.CREATE, level = AuditLevel.HIGH)
    public ApiResponse<UserRes> create(@RequestBody CreateUserReq req) {
        CreateUserCommand cmd = new CreateUserCommand(req.getUsername(), req.getNickname(), req.getPassword());
        return ApiResponse.ok(toDto(userAdminService.create(cmd)));
    }

    @GetMapping("/{id}")
    @Authorize(userType = UserType.INTERNAL, permissions = {"usr.view"})
    public ApiResponse<UserRes> get(@PathVariable Long id) {
        return ApiResponse.ok(toDto(userAdminService.get(id)));
    }

    @PostMapping("/page")
    @Authorize(userType = UserType.INTERNAL, permissions = {"usr.view"})
    public ApiResponse<PageResult<UserRes>> page(@RequestBody UserPageReq req) {
        var page = userAdminService.page(req.status(), req.usernameLike(), PageSpecFactory.of(req.page(), req.sort()));
        return ApiResponse.ok(PageResult.of(page, UserAdminController::toDto));
    }

    @PutMapping("/{id}")
    @Authorize(userType = UserType.INTERNAL, permissions = {"usr.edit"})
    @Audit(resource = AuditResource.USER, action = AuditAction.UPDATE, level = AuditLevel.HIGH)
    public ApiResponse<UserRes> update(@PathVariable Long id, @RequestBody UpdateUserReq req) {
        UpdateUserCommand cmd = new UpdateUserCommand(req.getNickname(), req.getStatus());
        return ApiResponse.ok(toDto(userAdminService.update(id, cmd)));
    }

    @PostMapping("/{id}/reset-password")
    @Authorize(userType = UserType.INTERNAL, permissions = {"usr.pwd.reset"})
    @Audit(resource = AuditResource.USER, action = AuditAction.RESET_PASSWORD, level = AuditLevel.CRITICAL)
    public ApiResponse<Boolean> resetPassword(@PathVariable Long id) {
        userAdminService.resetPassword(id);
        return ApiResponse.ok(true);
    }

    @GetMapping("/{id}/roles")
    @Authorize(userType = UserType.INTERNAL, permissions = {"usr.role.view"})
    public ApiResponse<List<String>> userRoles(@PathVariable Long id) {
        return ApiResponse.ok(userRoleService.userRoles(id).stream().map(String::valueOf).toList());
    }

    @PutMapping("/{id}/roles")
    @Authorize(userType = UserType.INTERNAL, permissions = {"usr.perm.edit"})
    @Audit(resource = AuditResource.USER_ROLE, action = AuditAction.UPDATE, level = AuditLevel.HIGH)
    public ApiResponse<Boolean> updateUserRoles(@PathVariable Long id, @RequestBody UpdateUserRolesReq req) {
        UpdateUserRolesCommand cmd = new UpdateUserRolesCommand(req.getRoleIds());
        return ApiResponse.ok(userRoleService.updateUserRoles(id, cmd));
    }

    @DeleteMapping("/{id}")
    @Authorize(userType = UserType.INTERNAL, permissions = {"usr.del"})
    @Audit(resource = AuditResource.USER, action = AuditAction.DELETE, level = AuditLevel.CRITICAL)
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        userAdminService.delete(id);
        return ApiResponse.ok(true);
    }

    private static UserRes toDto(User user) {
        return new UserRes(String.valueOf(user.getId()), user.getUsername(), user.getNickname(), user.getUserType(),
                user.getUserStatus(), user.isDeletedFlag(), user.getCreatedBy(), user.getCreatedAt(),
                user.getUpdatedBy(), user.getUpdatedAt());
    }
}
