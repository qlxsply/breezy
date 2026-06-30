package com.corwin.system.methodstat.interfaces.web;

import com.corwin.system.methodstat.application.service.MethodStatManageAppService;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import com.corwin.system.methodstat.interfaces.web.req.MethodStatAllMethodSwitchUpdateReq;
import com.corwin.system.methodstat.interfaces.web.req.MethodStatGlobalSwitchUpdateReq;
import com.corwin.system.methodstat.interfaces.web.req.MethodStatMethodSwitchUpdateReq;
import com.corwin.system.methodstat.interfaces.web.res.MethodStatGlobalSwitchRes;
import com.corwin.system.methodstat.interfaces.web.res.MethodStatMethodSwitchRes;
import com.corwin.system.auth.published.Authorize;
import com.corwin.framework.constant.UserType;
import com.corwin.framework.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @author Corwin 2026/3/25
 */
@ApiMeta(module = ApiModuleCode.METHODSTAT)
@RestController
@RequestMapping("/api/method-stat/manage")
@RequiredArgsConstructor
public class MethodStatManageController {

    private final MethodStatManageAppService manageAppService;

    @GetMapping("/global-switch")
    @Authorize(userType = UserType.INTERNAL, permissions = {"mst.switch.view"})
    public ApiResponse<MethodStatGlobalSwitchRes> globalSwitch() {
        var view = manageAppService.getGlobalSwitch();
        return ApiResponse.ok(new MethodStatGlobalSwitchRes(view.enabled()));
    }

    @PutMapping("/global-switch")
    @Authorize(userType = UserType.INTERNAL, permissions = {"mst.switch.edit"})
    public ApiResponse<MethodStatGlobalSwitchRes> updateGlobalSwitch(@RequestBody MethodStatGlobalSwitchUpdateReq req) {
        var view = manageAppService.setGlobalSwitch(req.enabled());
        return ApiResponse.ok(new MethodStatGlobalSwitchRes(view.enabled()));
    }

    @GetMapping("/method-switch")
    @Authorize(userType = UserType.INTERNAL, permissions = {"mst.switch.view"})
    public ApiResponse<MethodStatMethodSwitchRes> methodSwitch(@RequestParam("key") String key) {
        var view = manageAppService.getMethodSwitch(key);
        return ApiResponse.ok(new MethodStatMethodSwitchRes(view.key(), view.enabled()));
    }

    @PutMapping("/method-switch")
    @Authorize(userType = UserType.INTERNAL, permissions = {"mst.switch.edit"})
    public ApiResponse<MethodStatMethodSwitchRes> updateMethodSwitch(@RequestBody MethodStatMethodSwitchUpdateReq req) {
        var view = manageAppService.setMethodSwitch(req.key(), req.enabled());
        return ApiResponse.ok(new MethodStatMethodSwitchRes(view.key(), view.enabled()));
    }

    @PutMapping("/method-switch/all")
    @Authorize(userType = UserType.INTERNAL, permissions = {"mst.switch.edit"})
    public ApiResponse<Object> updateAllMethodSwitch(@RequestBody MethodStatAllMethodSwitchUpdateReq req) {
        manageAppService.setAllMethodSwitch(req.enabled());
        return ApiResponse.ok();
    }

    @DeleteMapping("/stats")
    @Authorize(userType = UserType.INTERNAL, permissions = {"mst.stat.clear"})
    public ApiResponse<Object> clearMethodStats(@RequestParam("key") String key) {
        manageAppService.clearMethodStats(key);
        return ApiResponse.ok();
    }

    @DeleteMapping("/stats/all")
    @Authorize(userType = UserType.INTERNAL, permissions = {"mst.stat.clear"})
    public ApiResponse<Object> clearAllStats() {
        manageAppService.clearAllStats();
        return ApiResponse.ok();
    }

}
