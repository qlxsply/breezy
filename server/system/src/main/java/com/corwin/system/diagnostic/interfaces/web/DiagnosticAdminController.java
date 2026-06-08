package com.corwin.system.diagnostic.interfaces.web;

import com.corwin.system.auth.published.Authorize;
import com.corwin.framework.constant.UserType;
import com.corwin.framework.web.response.ApiResponse;
import com.corwin.system.diagnostic.application.command.StartDiagnosticCommand;
import com.corwin.system.diagnostic.application.command.UpdateDiagnosticConfigCommand;
import com.corwin.system.diagnostic.application.service.DiagnosticCommandAppService;
import com.corwin.system.diagnostic.application.service.DiagnosticQueryAppService;
import com.corwin.system.diagnostic.application.view.DiagnosticCapabilityView;
import com.corwin.system.diagnostic.application.view.DiagnosticSessionView;
import com.corwin.system.diagnostic.domain.model.DiagnosticEvent;
import com.corwin.system.diagnostic.domain.model.DiagnosticEventType;
import com.corwin.system.diagnostic.domain.model.DiagnosticSnapshot;
import com.corwin.system.diagnostic.interfaces.web.req.StartDiagnosticReq;
import com.corwin.system.diagnostic.interfaces.web.req.UpdateDiagnosticConfigReq;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Corwin 2026/4/16
 */
@ApiMeta(module = ApiModuleCode.SYSTEM)
@RestController
@RequestMapping("/api/admin/diagnostic")
public class DiagnosticAdminController {

    private final DiagnosticCommandAppService commandAppService;
    private final DiagnosticQueryAppService queryAppService;

    public DiagnosticAdminController(DiagnosticCommandAppService commandAppService,
                                     DiagnosticQueryAppService queryAppService) {
        this.commandAppService = commandAppService;
        this.queryAppService = queryAppService;
    }

    @PostMapping("/start")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"diag.start"})
    public ApiResponse<DiagnosticSessionView> start(@RequestBody(required = false) StartDiagnosticReq req) {
        StartDiagnosticReq resolved = req == null ? new StartDiagnosticReq(null, null, null, null, null, null, null, null) : req;
        return ApiResponse.ok(commandAppService.start(new StartDiagnosticCommand(resolved.intervalMs(),
                resolved.historyCapacity(), resolved.eventCapacity(), resolved.items(), resolved.deepMode(),
                resolved.slowRequestThresholdMs(), resolved.slowSqlThresholdMs(), resolved.ttlSeconds())));
    }

    @PostMapping("/config")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"diag.edit"})
    public ApiResponse<DiagnosticSessionView> updateConfig(@RequestBody(required = false) UpdateDiagnosticConfigReq req) {
        UpdateDiagnosticConfigReq resolved =
                req == null ? new UpdateDiagnosticConfigReq(null, null, null, null, null, null, null, null) : req;
        return ApiResponse.ok(commandAppService.updateConfig(new UpdateDiagnosticConfigCommand(resolved.intervalMs(),
                resolved.historyCapacity(), resolved.eventCapacity(), resolved.items(), resolved.deepMode(),
                resolved.slowRequestThresholdMs(), resolved.slowSqlThresholdMs(), resolved.ttlSeconds())));
    }

    @PostMapping("/stop")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"diag.stop"})
    public ApiResponse<Boolean> stop() {
        return ApiResponse.ok(commandAppService.stop());
    }

    @GetMapping("/status")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"diag.view"})
    public ApiResponse<DiagnosticSessionView> status() {
        return ApiResponse.ok(queryAppService.status());
    }

    @GetMapping("/snapshots/latest")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"diag.view"})
    public ApiResponse<DiagnosticSnapshot> latestSnapshot() {
        return ApiResponse.ok(queryAppService.latestSnapshot().orElse(null));
    }

    @GetMapping("/snapshots/history")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"diag.view"})
    public ApiResponse<List<DiagnosticSnapshot>> history(@RequestParam(defaultValue = "120") int limit) {
        return ApiResponse.ok(queryAppService.history(limit));
    }

    @GetMapping("/events")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"diag.view"})
    public ApiResponse<List<DiagnosticEvent>> events(@RequestParam(defaultValue = "100") int limit,
                                                     @RequestParam(required = false) DiagnosticEventType type) {
        return ApiResponse.ok(queryAppService.events(limit, type));
    }

    @GetMapping("/capabilities")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"diag.view"})
    public ApiResponse<DiagnosticCapabilityView> capabilities() {
        return ApiResponse.ok(queryAppService.capabilities());
    }
}
