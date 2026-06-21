package com.corwin.system.resource.interfaces.web;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.web.response.ApiResponse;
import com.corwin.framework.web.response.PageResult;
import com.corwin.system.audit.domain.model.AuditAction;
import com.corwin.system.audit.domain.model.AuditLevel;
import com.corwin.system.audit.domain.model.AuditResource;
import com.corwin.system.audit.published.Audit;
import com.corwin.system.auth.published.Authorize;
import com.corwin.system.resource.application.service.ApiAdminService;
import com.corwin.system.resource.domain.model.Api;
import com.corwin.system.resource.domain.model.ApiAccessType;
import com.corwin.system.resource.domain.model.ApiAuditDeclaredStatus;
import com.corwin.system.resource.domain.model.ApiPermissionDeclaredStatus;
import com.corwin.system.resource.domain.model.ApiStatus;
import com.corwin.system.resource.domain.repo.ApiPageQuery;
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
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"api.view"})
    public ApiResponse<List<ApiRes>> list() {
        return ApiResponse.ok(apiAdminService.listAll().stream().map(this::toDto).toList());
    }

    @GetMapping("/page")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"api.view"})
    public ApiResponse<PageResult<ApiRes>> page(@RequestParam(required = false) String module,
            @RequestParam(required = false) String pathPattern,
            @RequestParam(required = false) String handlerClass,
            @RequestParam(required = false) String handlerMethod,
            @RequestParam(required = false) String permissionDeclared,
            @RequestParam(required = false) String accessType,
            @RequestParam(required = false) String userType,
            @RequestParam(required = false) String auditDeclared,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageSpec pageSpec = PageSpec.of(pageNo, pageSize, List.of());
        ApiPageQuery query = new ApiPageQuery(module, pathPattern, handlerClass, handlerMethod,
                resolvePermissionDeclared(permissionDeclared), resolveAccessType(accessType), resolveUserType(userType),
                resolveAuditDeclared(auditDeclared), resolveEnabled(status));
        return ApiResponse.ok(PageResult.of(apiAdminService.page(query, pageSpec), this::toDto));
    }

    @PutMapping("/{id}/publish")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"api.pub"})
    @Audit(resource = AuditResource.API, action = AuditAction.PUBLISH, level = AuditLevel.HIGH)
    public ApiResponse<ApiRes> publish(@PathVariable("id") Long id) {
        return ApiResponse.ok(toDto(apiAdminService.publish(id)));
    }

    @PutMapping("/{id}/disable")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"api.off"})
    @Audit(resource = AuditResource.API, action = AuditAction.DISABLE, level = AuditLevel.HIGH)
    public ApiResponse<ApiRes> disable(@PathVariable Long id) {
        return ApiResponse.ok(toDto(apiAdminService.disable(id)));
    }

    private ApiRes toDto(Api api) {
        return new ApiRes(api.getId(), api.getModule(), api.getProtocol(), api.getHttpMethod(), api.getPathPattern(),
                api.getHandlerClass(), api.getHandlerMethod(), Boolean.TRUE.equals(api.getPermissionDeclared()),
                api.getAccessType(), api.getUserTypes(), Boolean.TRUE.equals(api.getAuditDeclared()),
                api.getAuditResource(), api.getAuditAction(), api.getAuditDescription(),
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
