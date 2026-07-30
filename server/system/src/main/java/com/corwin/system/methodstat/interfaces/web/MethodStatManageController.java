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
 * REST controller providing management endpoints for method statistics collection switches and data clearing.
 * @author Corwin 2026/3/25
 */
@ApiMeta(module = ApiModuleCode.METHODSTAT)
@RestController
@RequestMapping("/api/method-stat/manage")
@RequiredArgsConstructor
public class MethodStatManageController {

    private final MethodStatManageAppService manageAppService;

    /**
     * Retrieve the current global statistics collection switch state.
     * @return the global switch state
     */
    @GetMapping("/global-switch")
    @Authorize(userType = UserType.ADMIN, permissions = {"mst.switch.view"})
    public ApiResponse<MethodStatGlobalSwitchRes> globalSwitch() {
        var view = manageAppService.getGlobalSwitch();
        return ApiResponse.ok(new MethodStatGlobalSwitchRes(view.enabled()));
    }

    /**
     * Enable or disable the global statistics collection.
     * @param req the request containing the new enabled state
     * @return updated global switch state
     */
    @PutMapping("/global-switch")
    @Authorize(userType = UserType.ADMIN, permissions = {"mst.switch.edit"})
    public ApiResponse<MethodStatGlobalSwitchRes> updateGlobalSwitch(@RequestBody MethodStatGlobalSwitchUpdateReq req) {
        var view = manageAppService.setGlobalSwitch(req.enabled());
        return ApiResponse.ok(new MethodStatGlobalSwitchRes(view.enabled()));
    }

    /**
     * Retrieve the per-method statistics switch state for the given key.
     * @param key the method key
     * @return the method switch state
     */
    @GetMapping("/method-switch")
    @Authorize(userType = UserType.ADMIN, permissions = {"mst.switch.view"})
    public ApiResponse<MethodStatMethodSwitchRes> methodSwitch(@RequestParam("key") String key) {
        var view = manageAppService.getMethodSwitch(key);
        return ApiResponse.ok(new MethodStatMethodSwitchRes(view.key(), view.enabled()));
    }

    /**
     * Enable or disable statistics collection for a specific method.
     * @param req the request containing the method key and new state
     * @return updated method switch state
     */
    @PutMapping("/method-switch")
    @Authorize(userType = UserType.ADMIN, permissions = {"mst.switch.edit"})
    public ApiResponse<MethodStatMethodSwitchRes> updateMethodSwitch(@RequestBody MethodStatMethodSwitchUpdateReq req) {
        var view = manageAppService.setMethodSwitch(req.key(), req.enabled());
        return ApiResponse.ok(new MethodStatMethodSwitchRes(view.key(), view.enabled()));
    }

    /**
     * Enable or disable statistics collection for all registered methods at once.
     * @param req the request containing the new enabled state
     * @return empty success response
     */
    @PutMapping("/method-switch/all")
    @Authorize(userType = UserType.ADMIN, permissions = {"mst.switch.edit"})
    public ApiResponse<Object> updateAllMethodSwitch(@RequestBody MethodStatAllMethodSwitchUpdateReq req) {
        manageAppService.setAllMethodSwitch(req.enabled());
        return ApiResponse.ok();
    }

    /**
     * Clear aggregated statistics for a specific method by its key.
     * @param key the method key
     * @return empty success response
     */
    @DeleteMapping("/stats")
    @Authorize(userType = UserType.ADMIN, permissions = {"mst.stat.clear"})
    public ApiResponse<Object> clearMethodStats(@RequestParam("key") String key) {
        manageAppService.clearMethodStats(key);
        return ApiResponse.ok();
    }

    /**
     * Clear all aggregated statistics across every method.
     * @return empty success response
     */
    @DeleteMapping("/stats/all")
    @Authorize(userType = UserType.ADMIN, permissions = {"mst.stat.clear"})
    public ApiResponse<Object> clearAllStats() {
        manageAppService.clearAllStats();
        return ApiResponse.ok();
    }

}
