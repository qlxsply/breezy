package com.corwin.system.auth.interfaces.web;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.web.response.ApiResponse;
import com.corwin.system.auth.application.command.ChangePasswordCommand;
import com.corwin.system.auth.application.command.LoginCommand;
import com.corwin.system.auth.interfaces.web.req.ChangePasswordReq;
import com.corwin.system.auth.interfaces.web.req.LoginReq;
import com.corwin.system.auth.interfaces.web.req.RefreshTokenReq;
import com.corwin.system.auth.interfaces.web.res.AuthUserRes;
import com.corwin.system.auth.interfaces.web.res.LoginResponseRes;
import com.corwin.system.auth.published.Authenticated;
import com.corwin.system.auth.published.PermitAll;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import com.corwin.system.user.application.service.UserConfigAppService;
import com.corwin.system.user.application.view.UserConfigView;
import com.corwin.system.user.interfaces.web.res.UserConfigsRes;
import com.corwin.system.webuser.application.service.WebUserAuthService;
import com.corwin.system.webuser.application.view.WebUserAuthView;
import com.corwin.system.webuser.application.view.WebUserLoginView;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Corwin 2026/1/22
 */
@ApiMeta(module = ApiModuleCode.SYSTEM)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final WebUserAuthService webUserAuthService;
    private final UserConfigAppService userConfigAppService;

    public AuthController(WebUserAuthService webUserAuthService, UserConfigAppService userConfigAppService) {
        this.webUserAuthService = webUserAuthService;
        this.userConfigAppService = userConfigAppService;
    }

    @PostMapping("/login")
    @PermitAll
    public ApiResponse<LoginResponseRes> login(@RequestBody LoginReq req) {
        WebUserLoginView result = webUserAuthService.login(new LoginCommand(req.account(), req.password()));
        return ApiResponse.ok(new LoginResponseRes(result.token(), result.refreshToken(), result.accessTokenExpiresAt(),
                result.refreshTokenExpiresAt(), toAuthDto(result.user())));
    }

    @PostMapping("/refresh")
    @PermitAll
    public ApiResponse<LoginResponseRes> refresh(@RequestBody RefreshTokenReq req) {
        WebUserLoginView result = webUserAuthService.refresh(req.refreshToken());
        return ApiResponse.ok(new LoginResponseRes(result.token(), result.refreshToken(), result.accessTokenExpiresAt(),
                result.refreshTokenExpiresAt(), toAuthDto(result.user())));
    }

    @GetMapping("/me")
    @Authenticated(userTypes = {UserType.EXTERNAL})
    public ApiResponse<AuthUserRes> me() {
        return ApiResponse.ok(toAuthDto(webUserAuthService.currentUser()));
    }

    @PostMapping("/logout")
    @Authenticated(userTypes = {UserType.EXTERNAL})
    public ApiResponse<Boolean> logout() {
        return ApiResponse.ok(webUserAuthService.logout());
    }

    @PutMapping("/password")
    @Authenticated(userTypes = {UserType.EXTERNAL})
    public ApiResponse<Boolean> changePassword(@RequestBody ChangePasswordReq req) {
        return ApiResponse.ok(
                webUserAuthService.changePassword(new ChangePasswordCommand(req.oldPassword(), req.newPassword())));
    }

    private AuthUserRes toAuthDto(WebUserAuthView view) {
        List<UserConfigsRes> configs = userConfigAppService.getMergedConfigs(view.id()).stream()
                .map(AuthController::toUserConfigsRes).toList();
        return new AuthUserRes(String.valueOf(view.id()), view.account(), view.userType(), view.mustChangePassword(),
                configs);
    }

    private static UserConfigsRes toUserConfigsRes(UserConfigView view) {
        return new UserConfigsRes(view.code(), view.description(), view.valueType(), view.value());
    }
}
