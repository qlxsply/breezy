package com.corwin.system.config.interfaces.web;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.web.response.ApiResponse;
import com.corwin.system.auth.published.Authorize;
import com.corwin.system.config.application.config.SystemConfigWrapper;
import com.corwin.system.config.interfaces.web.res.PasswordPolicyConfigRes;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Corwin 2026/7/13
 */
@ApiMeta(module = ApiModuleCode.SYSTEM)
@RestController
@RequestMapping("/api/sys/configs/password-policy")
@RequiredArgsConstructor
public class PasswordPolicyConfigController {

    @GetMapping
    @Authorize(userType = UserType.INTERNAL, permissions = {"sys.use"})
    public ApiResponse<PasswordPolicyConfigRes> get() {
        return ApiResponse.ok(new PasswordPolicyConfigRes(
                SystemConfigWrapper.passwordMinLength(),
                SystemConfigWrapper.passwordRequireDigit(),
                SystemConfigWrapper.passwordRequireLetter(),
                SystemConfigWrapper.passwordRequireUpper(),
                SystemConfigWrapper.passwordRequireLower(),
                SystemConfigWrapper.passwordRequireSpecial(),
                SystemConfigWrapper.passwordForceChangeOnFirstLogin(),
                SystemConfigWrapper.passwordForceChangeOnReset()
        ));
    }
}
