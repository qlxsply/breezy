package com.corwin.system.resource.interfaces.web;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.web.response.ApiResponse;
import com.corwin.system.audit.domain.model.AuditAction;
import com.corwin.system.audit.domain.model.AuditLevel;
import com.corwin.system.audit.domain.model.AuditResource;
import com.corwin.system.audit.published.Audit;
import com.corwin.system.auth.published.Authorize;
import com.corwin.system.resource.application.service.ApiAdminService;
import com.corwin.system.resource.domain.model.Api;
import com.corwin.system.resource.interfaces.web.res.ApiRes;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        return new ApiRes(api.getId(), api.getModule(), api.getProtocol(), api.getHttpMethod(),
                api.getPathPattern(), api.getHandlerClass(), api.getHandlerMethod(),
                Boolean.TRUE.equals(api.getPermissionDeclared()), api.getAccessType(), api.getUserTypes(),
                Boolean.TRUE.equals(api.getAuditDeclared()), api.getAuditResource(), api.getAuditAction(),
                api.getAuditDescription(), Boolean.TRUE.equals(api.getEnabled()), 0, false);
    }
}
