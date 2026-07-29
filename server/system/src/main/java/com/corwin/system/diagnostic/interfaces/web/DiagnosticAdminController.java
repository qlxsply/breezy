package com.corwin.system.diagnostic.interfaces.web;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.web.response.ApiResponse;
import com.corwin.system.auth.published.Authorize;
import com.corwin.system.diagnostic.application.command.StartDiagnosticCommand;
import com.corwin.system.diagnostic.application.command.UpdateDiagnosticConfigCommand;
import com.corwin.system.diagnostic.application.service.DiagnosticCommandAppService;
import com.corwin.system.diagnostic.application.service.DiagnosticQueryAppService;
import com.corwin.system.diagnostic.application.view.DiagnosticCapabilityView;
import com.corwin.system.diagnostic.application.view.DiagnosticSessionView;
import com.corwin.system.diagnostic.domain.model.DiagnosticEvent;
import com.corwin.system.diagnostic.domain.model.DiagnosticEventType;
import com.corwin.system.diagnostic.domain.model.DiagnosticSnapshot;
import com.corwin.system.diagnostic.interfaces.web.req.DiagnosticEventListReq;
import com.corwin.system.diagnostic.interfaces.web.req.DiagnosticHistoryReq;
import com.corwin.system.diagnostic.interfaces.web.req.StartDiagnosticReq;
import com.corwin.system.diagnostic.interfaces.web.req.UpdateDiagnosticConfigReq;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import org.springframework.web.bind.annotation.*;

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
    @Authorize(userType = UserType.ADMIN, permissions = {"diag.start"})
    public ApiResponse<DiagnosticSessionView> start(@RequestBody StartDiagnosticReq req) {
        return ApiResponse.ok(commandAppService.start(
                new StartDiagnosticCommand(req.intervalMs(), req.historyCapacity(), req.eventCapacity(), req.items(),
                        req.deepMode(), req.slowRequestThresholdMs(), req.slowSqlThresholdMs(), req.ttlSeconds())));
    }

    @PostMapping("/config")
    @Authorize(userType = UserType.ADMIN, permissions = {"diag.edit"})
    public ApiResponse<DiagnosticSessionView> updateConfig(@RequestBody UpdateDiagnosticConfigReq req) {
        return ApiResponse.ok(commandAppService.updateConfig(
                new UpdateDiagnosticConfigCommand(req.intervalMs(), req.historyCapacity(), req.eventCapacity(),
                        req.items(), req.deepMode(), req.slowRequestThresholdMs(), req.slowSqlThresholdMs(),
                        req.ttlSeconds())));
    }

    @PostMapping("/stop")
    @Authorize(userType = UserType.ADMIN, permissions = {"diag.stop"})
    public ApiResponse<Boolean> stop() {
        return ApiResponse.ok(commandAppService.stop());
    }

    @GetMapping("/status")
    @Authorize(userType = UserType.ADMIN, permissions = {"diag.view"})
    public ApiResponse<DiagnosticSessionView> status() {
        return ApiResponse.ok(queryAppService.status());
    }

    @GetMapping("/snapshots/latest")
    @Authorize(userType = UserType.ADMIN, permissions = {"diag.view"})
    public ApiResponse<DiagnosticSnapshot> latestSnapshot() {
        return ApiResponse.ok(queryAppService.latestSnapshot().orElse(null));
    }

    @PostMapping("/snapshots/history")
    @Authorize(userType = UserType.ADMIN, permissions = {"diag.view"})
    public ApiResponse<List<DiagnosticSnapshot>> history(@RequestBody DiagnosticHistoryReq req) {
        int limit = req == null || req.limit() == null ? 120 : req.limit();
        return ApiResponse.ok(queryAppService.history(limit));
    }

    @PostMapping("/events")
    @Authorize(userType = UserType.ADMIN, permissions = {"diag.view"})
    public ApiResponse<List<DiagnosticEvent>> events(@RequestBody DiagnosticEventListReq req) {
        int limit = req == null || req.limit() == null ? 100 : req.limit();
        DiagnosticEventType type = req == null ? null : req.type();
        return ApiResponse.ok(queryAppService.events(limit, type));
    }

    @GetMapping("/capabilities")
    @Authorize(userType = UserType.ADMIN, permissions = {"diag.view"})
    public ApiResponse<DiagnosticCapabilityView> capabilities() {
        return ApiResponse.ok(queryAppService.capabilities());
    }
}
