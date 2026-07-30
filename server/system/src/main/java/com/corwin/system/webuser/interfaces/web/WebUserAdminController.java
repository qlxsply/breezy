package com.corwin.system.webuser.interfaces.web;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.web.request.PageSpecFactory;
import com.corwin.framework.web.response.ApiResponse;
import com.corwin.framework.web.response.PageResult;
import com.corwin.system.audit.domain.model.AuditAction;
import com.corwin.system.audit.domain.model.AuditLevel;
import com.corwin.system.audit.domain.model.AuditResource;
import com.corwin.system.audit.published.Audit;
import com.corwin.system.auth.published.Authorize;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import com.corwin.system.webuser.application.command.UpdateWebUserCommand;
import com.corwin.system.webuser.application.service.WebUserAdminService;
import com.corwin.system.webuser.application.view.WebUserAdminView;
import com.corwin.system.webuser.interfaces.web.req.UpdateWebUserReq;
import com.corwin.system.webuser.interfaces.web.req.WebUserPageReq;
import com.corwin.system.webuser.interfaces.web.res.WebUserRes;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for external user administration.
 * Provides paginated listing, retrieval, and status update endpoints for admin users.
 *
 * @author Corwin 2026/5/11
 */
@ApiMeta(module = ApiModuleCode.SYSTEM)
@RestController
@RequestMapping("/api/external-users")
public class WebUserAdminController {

    private final WebUserAdminService webUserAdminService;

    public WebUserAdminController(WebUserAdminService webUserAdminService) {
        this.webUserAdminService = webUserAdminService;
    }

    /**
     * Paginated listing of external users with optional keyword and status filter.
     */
    @PostMapping("/page")
    @Authorize(userType = UserType.ADMIN, permissions = {"usr.view"})
    public ApiResponse<PageResult<WebUserRes>> page(@RequestBody WebUserPageReq req) {
        var page = webUserAdminService.page(req.keyword(), req.status(), PageSpecFactory.of(req.page(), req.sort()));
        return ApiResponse.ok(PageResult.of(page, WebUserAdminController::toRes));
    }

    /**
     * Retrieve a single external user by ID.
     */
    @GetMapping("/{id}")
    @Authorize(userType = UserType.ADMIN, permissions = {"usr.view"})
    public ApiResponse<WebUserRes> get(@PathVariable Long id) {
        return ApiResponse.ok(toRes(webUserAdminService.get(id)));
    }

    /**
     * Update the status of an external user (e.g. enable/disable).
     */
    @PutMapping("/{id}")
    @Authorize(userType = UserType.ADMIN, permissions = {"usr.edit"})
    @Audit(resource = AuditResource.EXTERNAL_USER, action = AuditAction.UPDATE, level = AuditLevel.HIGH)
    public ApiResponse<WebUserRes> update(@PathVariable Long id, @RequestBody UpdateWebUserReq req) {
        return ApiResponse.ok(toRes(webUserAdminService.update(id, new UpdateWebUserCommand(req.status()))));
    }

    private static WebUserRes toRes(WebUserAdminView view) {
        return new WebUserRes(String.valueOf(view.id()), view.account(), view.nickname(), view.userType(),
                view.status(), view.lastLoginAt(), view.createdAt(), view.updatedAt());
    }
}
