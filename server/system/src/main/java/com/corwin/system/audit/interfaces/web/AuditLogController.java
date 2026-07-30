package com.corwin.system.audit.interfaces.web;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.json.Json;
import com.corwin.framework.web.request.PageSpecFactory;
import com.corwin.framework.web.response.ApiResponse;
import com.corwin.framework.web.response.PageResult;
import com.corwin.system.audit.application.service.AuditLogService;
import com.corwin.system.audit.domain.model.AuditLog;
import com.corwin.system.audit.interfaces.web.req.AuditLogPageReq;
import com.corwin.system.audit.interfaces.web.res.AuditLogRes;
import com.corwin.system.auth.published.Authorize;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for audit log management.
 * Provides endpoints for paginated listing and single-record retrieval,
 * restricted to ADMIN users with the {@code audit.log.view} permission.
 *
 * @author Corwin 2026/4/19
 */
@ApiMeta(module = ApiModuleCode.SYSTEM)
@RestController
@RequestMapping("/api/sys/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * Paginated listing of audit logs with dynamic filters.
     *
     * @param req the page request with filter and sort parameters
     * @return a paginated result of {@link AuditLogRes} items
     */
    @PostMapping("/page")
    @Authorize(userType = UserType.ADMIN, permissions = {"audit.log.view"})
    public ApiResponse<PageResult<AuditLogRes>> page(@RequestBody AuditLogPageReq req) {
        var page = auditLogService.page(req.traceId(), req.operatorUserId(), req.operatorUsername(),
                req.applicationCode(), req.requestUri(), req.auditResource(), req.auditAction(), req.auditLevel(),
                req.success(), req.startAt(), req.endAt(), PageSpecFactory.of(req.page(), req.sort()));
        return ApiResponse.ok(PageResult.of(page, AuditLogController::toRes));
    }

    /**
     * Retrieves a single audit log by its ID.
     *
     * @param id the audit log primary key
     * @return the matching {@link AuditLogRes}
     */
    @GetMapping("/{id}")
    @Authorize(userType = UserType.ADMIN, permissions = {"audit.log.view"})
    public ApiResponse<AuditLogRes> get(@PathVariable("id") Long id) {
        return ApiResponse.ok(toRes(auditLogService.get(id)));
    }

    private static AuditLogRes toRes(AuditLog auditLog) {
        return new AuditLogRes(auditLog.getId(), auditLog.getTraceId(), auditLog.getRequestId(),
                auditLog.getOperatorUserId(), auditLog.getOperatorUsername(), auditLog.getOperatorUserType(),
                auditLog.getApplicationCode(), auditLog.getProtocol(), auditLog.getHttpMethod(),
                auditLog.getPathPattern(), auditLog.getRequestUri(),
                parsePermissionCodes(auditLog.getPermissionCodes()), auditLog.getAuditResource(),
                auditLog.getAuditAction(), auditLog.getAuditDescription(), auditLog.getAuditLevel(),
                auditLog.getRequestIp(), auditLog.getUserAgent(), auditLog.getRequestParamSummary(),
                auditLog.getRequestBodySummary(), auditLog.getResponseSummary(),
                Boolean.TRUE.equals(auditLog.getSuccess()), auditLog.getErrorCode(), auditLog.getErrorMessage(),
                auditLog.getStartedAt(), auditLog.getEndedAt(), auditLog.getDurationMs(), auditLog.getCreatedAt());
    }

    private static List<String> parsePermissionCodes(String rawPermissionCodes) {
        if (rawPermissionCodes == null || rawPermissionCodes.isBlank()) {
            return List.of();
        }
        try {
            return Json.parse(rawPermissionCodes, new TypeReference<List<String>>() {
            });
        } catch (RuntimeException ex) {
            return List.of(rawPermissionCodes);
        }
    }
}
