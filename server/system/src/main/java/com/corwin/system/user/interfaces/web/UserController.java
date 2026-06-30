package com.corwin.system.user.interfaces.web;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.web.response.ApiResponse;
import com.corwin.system.audit.domain.model.AuditAction;
import com.corwin.system.audit.domain.model.AuditLevel;
import com.corwin.system.audit.domain.model.AuditResource;
import com.corwin.system.audit.published.Audit;
import com.corwin.system.auth.application.command.ChangePasswordCommand;
import com.corwin.system.auth.published.Authenticated;
import com.corwin.system.auth.published.PermitAll;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import com.corwin.system.user.application.command.RegisterUserCommand;
import com.corwin.system.user.application.command.UpdateMyProfileCommand;
import com.corwin.system.user.application.service.UserService;
import com.corwin.system.user.application.view.UserProfileView;
import com.corwin.system.user.interfaces.web.req.ChangeMyPasswordReq;
import com.corwin.system.user.interfaces.web.req.RegisterUserReq;
import com.corwin.system.user.interfaces.web.req.UpdateMyProfileReq;
import com.corwin.system.user.interfaces.web.res.UserProfileRes;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @author Corwin 2026/4/19
 */
@ApiMeta(module = ApiModuleCode.SYSTEM)
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    @PermitAll
    @Audit(resource = AuditResource.USER, action = AuditAction.REGISTER, description = "用户注册", level = AuditLevel.HIGH)
    public ApiResponse<UserProfileRes> register(@RequestBody RegisterUserReq req) {
        RegisterUserCommand cmd = new RegisterUserCommand(req.username(), req.nickname(), req.password());
        return ApiResponse.ok(toRes(userService.register(cmd)));
    }

    @GetMapping("/me")
    @Authenticated(userType = UserType.EXTERNAL)
    public ApiResponse<UserProfileRes> me() {
        return ApiResponse.ok(toRes(userService.currentProfile()));
    }

    @PutMapping("/me")
    @Authenticated(userType = UserType.EXTERNAL)
    @Audit(resource = AuditResource.USER, action = AuditAction.UPDATE_PROFILE, description = "用户修改个人资料", level = AuditLevel.MEDIUM)
    public ApiResponse<UserProfileRes> updateMyProfile(@RequestBody UpdateMyProfileReq req) {
        return ApiResponse.ok(toRes(userService.updateMyProfile(new UpdateMyProfileCommand(req.nickname()))));
    }

    @PutMapping("/me/password")
    @Authenticated(userType = UserType.EXTERNAL)
    @Audit(resource = AuditResource.USER, action = AuditAction.CHANGE_PASSWORD, description = "用户修改密码", level = AuditLevel.HIGH)
    public ApiResponse<Boolean> changeMyPassword(@RequestBody ChangeMyPasswordReq req) {
        ChangePasswordCommand cmd = new ChangePasswordCommand(req.oldPassword(), req.newPassword());
        return ApiResponse.ok(userService.changeMyPassword(cmd));
    }

    @PostMapping("/me/logout")
    @Authenticated(userType = UserType.EXTERNAL)
    @Audit(resource = AuditResource.USER, action = AuditAction.LOGOUT, description = "用户退出登录", level = AuditLevel.MEDIUM)
    public ApiResponse<Boolean> logout() {
        return ApiResponse.ok(userService.logout());
    }

    private static UserProfileRes toRes(UserProfileView view) {
        return new UserProfileRes(String.valueOf(view.id()), view.username(), view.nickname(), view.userType(),
                view.status(), view.mustChangePassword(), view.createdAt(), view.updatedAt());
    }
}
