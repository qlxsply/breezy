package com.corwin.system.auth.interfaces.web;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.web.response.ApiResponse;
import com.corwin.system.auth.application.command.ChangePasswordCommand;
import com.corwin.system.auth.application.command.LoginCommand;
import com.corwin.system.auth.application.service.AuthService;
import com.corwin.system.auth.application.view.AuthUserView;
import com.corwin.system.auth.application.view.LoginView;
import com.corwin.system.auth.infrastructure.web.AdminAuthCookieService;
import com.corwin.system.auth.interfaces.web.req.ChangePasswordReq;
import com.corwin.system.auth.interfaces.web.req.LoginReq;
import com.corwin.system.auth.interfaces.web.res.AdminLoginResponseRes;
import com.corwin.system.auth.interfaces.web.res.AuthUserRes;
import com.corwin.system.auth.published.Authenticated;
import com.corwin.system.auth.published.PermitAll;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import com.corwin.system.user.application.service.UserConfigAppService;
import com.corwin.system.user.application.view.UserConfigView;
import com.corwin.system.user.interfaces.web.res.UserConfigsRes;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for internal (admin) user authentication: login,
 * current user info, logout, and password change.
 *
 * @author Corwin 2026/5/11
 */
@ApiMeta(module = ApiModuleCode.SYSTEM)
@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AuthService authService;
    private final UserConfigAppService userConfigAppService;
    private final AdminAuthCookieService cookieService;

    public AdminAuthController(AuthService authService, UserConfigAppService userConfigAppService,
            AdminAuthCookieService cookieService) {
        this.authService = authService;
        this.userConfigAppService = userConfigAppService;
        this.cookieService = cookieService;
    }

    /**
     * Authenticates an admin user with account and password.
     */
    @PostMapping("/login")
    @PermitAll
    public ApiResponse<AdminLoginResponseRes> login(@RequestBody LoginReq req, HttpServletResponse response) {
        LoginView result = authService.login(new LoginCommand(req.account(), req.password()));
        cookieService.writeSession(response, result.token(), result.accessTokenExpiresAt());
        cookieService.rotateCsrf(response);
        return ApiResponse.ok(new AdminLoginResponseRes(result.accessTokenExpiresAt(), toAuthDto(result.user())));
    }

    @GetMapping("/csrf")
    @PermitAll
    public ApiResponse<Boolean> csrf(HttpServletResponse response) {
        cookieService.rotateCsrf(response);
        return ApiResponse.ok(true);
    }

    /**
     * Returns the current admin session, or null when no session exists.
     */
    @GetMapping("/me")
    @Authenticated(userType = UserType.ADMIN, allowExpiredCredentials = true, optional = true)
    public ApiResponse<AuthUserRes> me() {
        AuthUserView user = authService.currentUser();
        return ApiResponse.ok(user == null ? null : toAuthDto(user));
    }

    /**
     * Logs out the current admin user by revoking the session.
     */
    @PostMapping("/logout")
    @PermitAll
    public ApiResponse<Boolean> logout(HttpServletResponse response) {
        try {
            return ApiResponse.ok(authService.logout());
        } finally {
            cookieService.clearSession(response);
            cookieService.rotateCsrf(response);
        }
    }

    /**
     * Changes the current admin user's password.
     */
    @PutMapping("/password")
    @Authenticated(userType = UserType.ADMIN, allowExpiredCredentials = true)
    public ApiResponse<Boolean> changePassword(@RequestBody ChangePasswordReq req) {
        ChangePasswordCommand cmd = new ChangePasswordCommand(req.oldPassword(), req.newPassword());
        return ApiResponse.ok(authService.changePassword(cmd));
    }

    private AuthUserRes toAuthDto(AuthUserView view) {
        List<UserConfigsRes> configs = userConfigAppService.getMergedConfigs(view.id()).stream()
                .map(AdminAuthController::toUserConfigsRes).toList();
        return new AuthUserRes(String.valueOf(view.id()), view.account(), view.userType(), view.mustChangePassword(),
                configs);
    }

    private static UserConfigsRes toUserConfigsRes(UserConfigView view) {
        return new UserConfigsRes(view.code(), view.description(), view.valueType(), view.value());
    }
}
