package com.corwin.system.resource.interfaces.web;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.web.request.PageSpecFactory;
import com.corwin.framework.web.response.ApiResponse;
import com.corwin.framework.web.response.PageResult;
import com.corwin.system.audit.domain.model.AuditAction;
import com.corwin.system.audit.domain.model.AuditLevel;
import com.corwin.system.audit.domain.model.AuditResource;
import com.corwin.system.audit.published.Audit;
import com.corwin.system.auth.published.Authorize;
import com.corwin.system.resource.application.service.ApiAdminService;
import com.corwin.system.resource.domain.model.*;
import com.corwin.system.resource.domain.repo.ApiPageQuery;
import com.corwin.system.resource.interfaces.web.req.ApiPageReq;
import com.corwin.system.resource.interfaces.web.req.UpdateApiSortOptionsReq;
import com.corwin.system.resource.interfaces.web.res.ApiRes;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Corwin 2026/1/23
 */
@ApiMeta(module = ApiModuleCode.SYSTEM)
@RestController
@RequestMapping("/api/apis")
@RequiredArgsConstructor
public class ApiAdminController {

    private final ApiAdminService apiAdminService;

    @GetMapping
    @Authorize(userType = UserType.ADMIN, permissions = {"api.view"})
    public ApiResponse<List<ApiRes>> list() {
        return ApiResponse.ok(apiAdminService.listAll().stream().map(this::toDto).toList());
    }

    @PostMapping("/page")
    @Authorize(userType = UserType.ADMIN, permissions = {"api.view"})
    public ApiResponse<PageResult<ApiRes>> page(@RequestBody ApiPageReq req) {
        PageSpec pageSpec = PageSpecFactory.of(req.page(), null);
        ApiPageQuery query = new ApiPageQuery(req.module(), req.pathPattern(), req.handlerClass(), req.handlerMethod(),
                resolvePermissionDeclared(req.permissionDeclared()), resolveAccessType(req.accessType()),
                resolveUserType(req.userType()), resolveAuditDeclared(req.auditDeclared()),
                resolveEnabled(req.status()));
        return ApiResponse.ok(PageResult.of(apiAdminService.page(query, pageSpec), this::toDto));
    }

    @GetMapping("/{id}")
    @Authorize(userType = UserType.ADMIN, permissions = {"api.view"})
    public ApiResponse<ApiRes> detail(@PathVariable Long id) {
        return ApiResponse.ok(toDto(apiAdminService.get(id)));
    }

    @PutMapping("/{id}/publish")
    @Authorize(userType = UserType.ADMIN, permissions = {"api.pub"})
    @Audit(resource = AuditResource.API, action = AuditAction.PUBLISH, level = AuditLevel.HIGH)
    public ApiResponse<ApiRes> publish(@PathVariable("id") Long id) {
        return ApiResponse.ok(toDto(apiAdminService.publish(id)));
    }

    @PutMapping("/{id}/disable")
    @Authorize(userType = UserType.ADMIN, permissions = {"api.off"})
    @Audit(resource = AuditResource.API, action = AuditAction.DISABLE, level = AuditLevel.HIGH)
    public ApiResponse<ApiRes> disable(@PathVariable Long id) {
        return ApiResponse.ok(toDto(apiAdminService.disable(id)));
    }

    @PutMapping("/{id}/sort-options")
    @Authorize(userType = UserType.ADMIN, permissions = {"api.edit"})
    @Audit(resource = AuditResource.API, action = AuditAction.UPDATE, level = AuditLevel.HIGH)
    public ApiResponse<ApiRes> updateSortOptions(@PathVariable Long id, @RequestBody UpdateApiSortOptionsReq req) {
        return ApiResponse.ok(toDto(apiAdminService.updateSortOptions(id, req.sortOptionsJson())));
    }

    private ApiRes toDto(Api api) {
        return new ApiRes(api.getId(), api.getModule(), api.getProtocol(), api.getHttpMethod(), api.getPathPattern(),
                api.getHandlerClass(), api.getHandlerMethod(), Boolean.TRUE.equals(api.getPermissionDeclared()),
                api.getAccessType(), api.getUserType(), Boolean.TRUE.equals(api.getAuditDeclared()),
                api.getAuditResource(), api.getAuditAction(), api.getAuditDescription(), api.getSortOptionsJson(),
                Boolean.TRUE.equals(api.getEnabled()), 0, false);
    }

    private Boolean resolvePermissionDeclared(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return switch (ApiPermissionDeclaredStatus.valueOf(raw.trim().toUpperCase())) {
            case DECLARED -> true;
            case UNDECLARED -> false;
        };
    }

    private ApiAccessType resolveAccessType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return ApiAccessType.valueOf(raw.trim().toUpperCase());
    }

    private UserType resolveUserType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return UserType.valueOf(raw.trim().toUpperCase());
    }

    private Boolean resolveAuditDeclared(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return switch (ApiAuditDeclaredStatus.valueOf(raw.trim().toUpperCase())) {
            case ENABLED -> true;
            case DISABLED -> false;
        };
    }

    private Boolean resolveEnabled(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return switch (ApiStatus.valueOf(raw.trim().toUpperCase())) {
            case ACTIVE -> true;
            case DISABLED -> false;
            default -> null;
        };
    }
}
