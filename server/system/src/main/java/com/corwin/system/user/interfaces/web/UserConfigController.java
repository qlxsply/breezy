package com.corwin.system.user.interfaces.web;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.web.ctx.CtxUtil;
import com.corwin.framework.web.response.ApiResponse;
import com.corwin.system.auth.published.Authorize;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import com.corwin.system.user.application.service.UserConfigAppService;
import com.corwin.system.user.application.view.UserConfigView;
import com.corwin.system.user.interfaces.web.res.UserConfigsRes;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 *
 * @author Corwin 2026/3/30
 */
@ApiMeta(module = ApiModuleCode.SYSTEM)
@RestController
@RequestMapping("/api/sys/configs/my")
@RequiredArgsConstructor
public class UserConfigController {

    private final UserConfigAppService userConfigAppService;

    @GetMapping
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"pro.cfg.view"})
    public ApiResponse<List<UserConfigsRes>> getMyConfigs() {
        Long userId = CtxUtil.getPrincipal().userId();
        return ApiResponse.ok(
                userConfigAppService.getMergedConfigs(userId).stream().map(UserConfigController::toRes).toList());
    }

    @PutMapping("/{code}")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"pro.cfg.edit"})
    public ApiResponse<Boolean> updateMyConfig(@PathVariable String code, @RequestBody Map<String, String> body) {
        Long userId = CtxUtil.getPrincipal().userId();
        String value = body.get("value");
        userConfigAppService.updateMyConfig(userId, code, value);
        return ApiResponse.ok(true);
    }

    private static UserConfigsRes toRes(UserConfigView view) {
        return new UserConfigsRes(view.code(), view.description(), view.valueType(), view.value());
    }
}
