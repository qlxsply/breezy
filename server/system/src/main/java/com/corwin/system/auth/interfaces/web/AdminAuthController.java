package com.corwin.system.auth.interfaces.web;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.web.response.ApiResponse;
import com.corwin.system.auth.application.command.ChangePasswordCommand;
import com.corwin.system.auth.application.command.LoginCommand;
import com.corwin.system.auth.application.service.AuthService;
import com.corwin.system.auth.application.view.AuthUserView;
import com.corwin.system.auth.application.view.LoginView;
import com.corwin.system.auth.interfaces.web.req.ChangePasswordReq;
import com.corwin.system.auth.interfaces.web.req.LoginReq;
import com.corwin.system.auth.interfaces.web.res.AuthUserRes;
import com.corwin.system.auth.interfaces.web.res.LoginResponseRes;
import com.corwin.system.auth.published.Authenticated;
import com.corwin.system.auth.published.PermitAll;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import com.corwin.system.user.application.service.UserConfigAppService;
import com.corwin.system.user.application.view.UserConfigView;
import com.corwin.system.user.interfaces.web.res.UserConfigsRes;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Corwin 2026/5/11
 */
@ApiMeta(module = ApiModuleCode.SYSTEM)
@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AuthService authService;
    private final UserConfigAppService userConfigAppService;

    public AdminAuthController(AuthService authService, UserConfigAppService userConfigAppService) {
        this.authService = authService;
        this.userConfigAppService = userConfigAppService;
    }

    @PostMapping("/login")
    @PermitAll
    public ApiResponse<LoginResponseRes> login(@RequestBody LoginReq req) {
        LoginView result = authService.login(new LoginCommand(req.account(), req.password()));
        return ApiResponse.ok(new LoginResponseRes(result.token(), result.refreshToken(), result.accessTokenExpiresAt(),
                result.refreshTokenExpiresAt(), toAuthDto(result.user())));
    }

    @GetMapping("/me")
    @Authenticated(userType = UserType.ADMIN)
    public ApiResponse<AuthUserRes> me() {
        return ApiResponse.ok(toAuthDto(authService.currentUser()));
    }

    @PostMapping("/logout")
    @Authenticated(userType = UserType.ADMIN)
    public ApiResponse<Boolean> logout() {
        return ApiResponse.ok(authService.logout());
    }

    @PutMapping("/password")
    @Authenticated(userType = UserType.ADMIN)
    public ApiResponse<Boolean> changePassword(@RequestBody ChangePasswordReq req) {
        return ApiResponse.ok(authService.changePassword(new ChangePasswordCommand(req.oldPassword(), req.newPassword())));
    }

    private AuthUserRes toAuthDto(AuthUserView view) {
        if (view == null) {
            List<UserConfigsRes> configs = userConfigAppService.getMergedConfigs(null).stream()
                    .map(AdminAuthController::toUserConfigsRes).toList();
            return new AuthUserRes(null, null, UserType.GUEST, false, configs);
        }
        List<UserConfigsRes> configs = userConfigAppService.getMergedConfigs(view.id()).stream()
                .map(AdminAuthController::toUserConfigsRes).toList();
        return new AuthUserRes(String.valueOf(view.id()), view.account(), view.userType(), view.mustChangePassword(),
                configs);
    }

    private static UserConfigsRes toUserConfigsRes(UserConfigView view) {
        return new UserConfigsRes(view.code(), view.description(), view.valueType(), view.value());
    }
}
