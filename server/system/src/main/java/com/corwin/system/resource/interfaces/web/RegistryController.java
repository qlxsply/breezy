package com.corwin.system.resource.interfaces.web;

import com.corwin.framework.web.response.ApiResponse;
import com.corwin.system.auth.published.PermitAll;
import com.corwin.system.resource.application.service.RegistryService;
import com.corwin.system.resource.application.view.RegistryResourceView;
import com.corwin.system.resource.application.view.RegistryView;
import com.corwin.system.resource.interfaces.web.res.RegistryRes;
import com.corwin.system.resource.interfaces.web.res.RegistryResourceRes;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Corwin 2026/5/7
 */
@ApiMeta(module = ApiModuleCode.SYSTEM)
@RestController
@RequestMapping("/api/registry")
@RequiredArgsConstructor
public class RegistryController {

    private final RegistryService registryService;

    @GetMapping
    @PermitAll
    public ApiResponse<RegistryRes> currentRegistry() {
        return ApiResponse.ok(toRes(registryService.currentRegistry()));
    }

    private RegistryRes toRes(RegistryView view) {
        return new RegistryRes(view.resources().stream().map(this::toRes).toList());
    }

    private RegistryResourceRes toRes(RegistryResourceView view) {
        return new RegistryResourceRes(view.id(), view.parentId(), view.name(), view.icon(), view.description(),
                view.code(), view.type(), view.scope(), view.openMode(), view.url(), view.loadTarget(),
                view.orderNo(), view.level(), view.enabled(), view.guestAccess());
    }
}
