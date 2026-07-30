package com.corwin.system.userfeature.interfaces.web;

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
import com.corwin.system.userfeature.application.command.ApplicationOverrideCommand;
import com.corwin.system.userfeature.application.command.FeatureOverrideCommand;
import com.corwin.system.userfeature.application.command.SaveUserFeaturePackageCommand;
import com.corwin.system.userfeature.application.command.SaveUserFeatureUserManagementCommand;
import com.corwin.system.userfeature.application.service.UserFeatureAdminService;
import com.corwin.system.userfeature.application.view.UserFeatureApplicationView;
import com.corwin.system.userfeature.application.view.UserFeatureItemView;
import com.corwin.system.userfeature.application.view.UserFeaturePackageView;
import com.corwin.system.userfeature.application.view.UserFeatureUserManagementView;
import com.corwin.system.userfeature.interfaces.web.req.SaveUserFeaturePackageReq;
import com.corwin.system.userfeature.interfaces.web.req.SaveUserFeatureUserManagementReq;
import com.corwin.system.userfeature.interfaces.web.req.UpdateUserFeatureStatusReq;
import com.corwin.system.userfeature.interfaces.web.req.UserFeatureApplicationPageReq;
import com.corwin.system.userfeature.interfaces.web.req.UserFeaturePackagePageReq;
import com.corwin.system.userfeature.interfaces.web.res.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for user feature administration.
 *
 * <p>Provides endpoints for managing product applications, user application packages,
 * and per-user feature access overrides in the admin console.</p>
 *
 * @author Corwin 2026/6/14
 */
@ApiMeta(module = ApiModuleCode.SYSTEM)
@RestController
@RequestMapping("/api/user-features")
@RequiredArgsConstructor
public class UserFeatureAdminController {

    private final UserFeatureAdminService userFeatureAdminService;

    /**
     * Paginates product applications with keyword and enabled filter.
     *
     * @param req the page request with optional keyword and enabled filter
     * @return paginated application response
     */
    @PostMapping("/applications/page")
    @Authorize(userType = UserType.ADMIN, permissions = {"ufa.view"})
    public ApiResponse<PageResult<UserFeatureApplicationRes>> pageApplications(
            @RequestBody UserFeatureApplicationPageReq req) {
        var page = userFeatureAdminService.pageApplications(req == null ? null : req.keyword(),
                req == null ? null : req.enabled(),
                PageSpecFactory.of(req == null ? null : req.page(), req == null ? null : req.sort()));
        return ApiResponse.ok(PageResult.of(page, UserFeatureAdminController::toApplicationRes));
    }

    /**
     * Returns all applications as a flat catalog for selection dropdowns.
     *
     * @return list of all application responses
     */
    @GetMapping("/applications/catalog")
    @Authorize(userType = UserType.ADMIN, permissions = {"ufa.view"})
    public ApiResponse<List<UserFeatureApplicationRes>> applicationCatalog() {
        List<UserFeatureApplicationView> applications = userFeatureAdminService.applications();
        return ApiResponse.ok(applications.stream().map(UserFeatureAdminController::toApplicationRes).toList());
    }

    /**
     * Gets a single application by its ID.
     *
     * @param id the application ID
     * @return the application response
     */
    @GetMapping("/applications/{id}")
    @Authorize(userType = UserType.ADMIN, permissions = {"ufa.view"})
    public ApiResponse<UserFeatureApplicationRes> getApplication(@PathVariable Long id) {
        return ApiResponse.ok(toApplicationRes(userFeatureAdminService.getApplication(id)));
    }

    /**
     * Enables or disables a product application.
     *
     * @param id  the application ID
     * @param req the status update request
     * @return true on success
     */
    @PutMapping("/applications/{id}/status")
    @Authorize(userType = UserType.ADMIN, permissions = {"ufa.edit"})
    @Audit(resource = AuditResource.NORMAL_FEATURE, action = AuditAction.UPDATE, level = AuditLevel.HIGH)
    public ApiResponse<Boolean> updateApplicationStatus(@PathVariable Long id,
            @RequestBody UpdateUserFeatureStatusReq req) {
        return ApiResponse.ok(userFeatureAdminService.updateApplicationStatus(id, req.enabled()));
    }

    /**
     * Paginates user application packages with keyword and enabled filter.
     *
     * @param req the page request with optional keyword and enabled filter
     * @return paginated package response
     */
    @PostMapping("/packages/page")
    @Authorize(userType = UserType.ADMIN, permissions = {"ufp.view"})
    public ApiResponse<PageResult<UserFeaturePackageRes>> pagePackages(@RequestBody UserFeaturePackagePageReq req) {
        var page = userFeatureAdminService.pagePackages(req == null ? null : req.keyword(),
                req == null ? null : req.enabled(),
                PageSpecFactory.of(req == null ? null : req.page(), req == null ? null : req.sort()));
        return ApiResponse.ok(PageResult.of(page, UserFeatureAdminController::toPackageRes));
    }

    /**
     * Gets a single user application package by its ID.
     *
     * @param id the package ID
     * @return the package response
     */
    @GetMapping("/packages/{id}")
    @Authorize(userType = UserType.ADMIN, permissions = {"ufp.view"})
    public ApiResponse<UserFeaturePackageRes> getPackage(@PathVariable Long id) {
        return ApiResponse.ok(toPackageRes(userFeatureAdminService.getPackage(id)));
    }

    /**
     * Creates a new user application package.
     *
     * @param req the package creation request
     * @return the created package response
     */
    @PostMapping("/packages")
    @Authorize(userType = UserType.ADMIN, permissions = {"ufp.edit"})
    @Audit(resource = AuditResource.NORMAL_FEATURE, action = AuditAction.CREATE, description = "创建用户应用包",
            level = AuditLevel.HIGH)
    public ApiResponse<UserFeaturePackageRes> createPackage(@RequestBody SaveUserFeaturePackageReq req) {
        return ApiResponse.ok(toPackageRes(userFeatureAdminService.createPackage(toPackageCommand(req))));
    }

    /**
     * Updates an existing user application package.
     *
     * @param id  the package ID
     * @param req the package update request
     * @return the updated package response
     */
    @PutMapping("/packages/{id}")
    @Authorize(userType = UserType.ADMIN, permissions = {"ufp.edit"})
    @Audit(resource = AuditResource.NORMAL_FEATURE, action = AuditAction.UPDATE, description = "更新用户应用包",
            level = AuditLevel.HIGH)
    public ApiResponse<UserFeaturePackageRes> updatePackage(@PathVariable Long id,
            @RequestBody SaveUserFeaturePackageReq req) {
        return ApiResponse.ok(toPackageRes(userFeatureAdminService.updatePackage(id, toPackageCommand(req))));
    }

    /**
     * Enables or disables a user application package.
     *
     * @param id  the package ID
     * @param req the status update request
     * @return true on success
     */
    @PutMapping("/packages/{id}/status")
    @Authorize(userType = UserType.ADMIN, permissions = {"ufp.edit"})
    @Audit(resource = AuditResource.NORMAL_FEATURE, action = AuditAction.UPDATE, description = "更新用户应用包状态",
            level = AuditLevel.HIGH)
    public ApiResponse<Boolean> updatePackageStatus(@PathVariable Long id, @RequestBody UpdateUserFeatureStatusReq req) {
        return ApiResponse.ok(userFeatureAdminService.updatePackageStatus(id, req.enabled()));
    }

    /**
     * Deletes a user application package by its ID.
     * <p>Also removes all associated application/feature access records and package member mappings.</p>
     *
     * @param id the package ID
     * @return true on success
     */
    @DeleteMapping("/packages/{id}")
    @Authorize(userType = UserType.ADMIN, permissions = {"ufp.edit"})
    @Audit(resource = AuditResource.NORMAL_FEATURE, action = AuditAction.DELETE, description = "删除用户应用包",
            level = AuditLevel.CRITICAL)
    public ApiResponse<Boolean> deletePackage(@PathVariable Long id) {
        return ApiResponse.ok(userFeatureAdminService.deletePackage(id));
    }

    /**
     * Gets the full user feature management view for a specific user.
     * <p>Returns assigned packages, application access state with inherited/effective visibility, and per-feature overrides.</p>
     *
     * @param userId the user ID
     * @return the user management response
     */
    @GetMapping("/users/{userId}/management")
    @Authorize(userType = UserType.ADMIN, permissions = {"ufu.view"})
    public ApiResponse<UserFeatureUserManagementRes> getUserManagement(@PathVariable Long userId) {
        return ApiResponse.ok(toUserManagementRes(userFeatureAdminService.getUserManagement(userId)));
    }

    /**
     * Saves the full user feature management configuration for a specific user.
     * <p>Updates package assignments, application-level overrides, and feature-level overrides.</p>
     *
     * @param userId the user ID
     * @param req    the user management configuration request
     * @return true on success
     */
    @PutMapping("/users/{userId}/management")
    @Authorize(userType = UserType.ADMIN, permissions = {"ufu.edit"})
    @Audit(resource = AuditResource.NORMAL_FEATURE, action = AuditAction.UPDATE_USER,
            description = "维护用户应用功能配置", level = AuditLevel.HIGH)
    public ApiResponse<Boolean> saveUserManagement(@PathVariable Long userId,
            @RequestBody SaveUserFeatureUserManagementReq req) {
        return ApiResponse.ok(userFeatureAdminService.saveUserManagement(userId, toUserManagementCommand(req)));
    }

    private static SaveUserFeaturePackageCommand toPackageCommand(SaveUserFeaturePackageReq req) {
        var accesses = req.applicationAccesses() == null ? List.<SaveUserFeaturePackageCommand.ApplicationAccessCommand>of() : req.applicationAccesses()
                .stream().map(item -> new SaveUserFeaturePackageCommand.ApplicationAccessCommand(item.applicationId(),
                        item.featureAccessScope(), item.featureIds())).toList();
        return new SaveUserFeaturePackageCommand(req.code(), req.name(), req.packageType(), req.description(),
                req.enabled(), req.defaultPackage(), accesses);
    }

    private static SaveUserFeatureUserManagementCommand toUserManagementCommand(SaveUserFeatureUserManagementReq req) {
        var appOverrides = req.applicationOverrides() == null ? List.<ApplicationOverrideCommand>of() : req.applicationOverrides()
                .stream().map(item -> new ApplicationOverrideCommand(item.applicationId(), item.overrideType(),
                        item.featureAccessScope())).toList();
        var featureOverrides = req.featureOverrides() == null ? List.<FeatureOverrideCommand>of() : req.featureOverrides()
                .stream()
                .map(item -> new FeatureOverrideCommand(item.applicationId(), item.featureId(), item.overrideType()))
                .toList();
        return new SaveUserFeatureUserManagementCommand(req.packageIds(), appOverrides, featureOverrides);
    }

    private static UserFeatureApplicationRes toApplicationRes(UserFeatureApplicationView view) {
        return new UserFeatureApplicationRes(String.valueOf(view.id()), view.code(), view.name(), view.description(),
                view.icon(), view.routePath(), view.componentPath(), view.enabled(), view.featureCount(),
                view.permissionBindingCount(),
                view.features().stream().map(UserFeatureAdminController::toItemRes).toList());
    }

    private static UserFeatureItemRes toItemRes(UserFeatureItemView view) {
        return new UserFeatureItemRes(String.valueOf(view.id()), String.valueOf(view.applicationId()), view.code(),
                view.name(), view.description(), view.enabled(), view.permissionCodes());
    }

    private static UserFeaturePackageRes toPackageRes(UserFeaturePackageView view) {
        return new UserFeaturePackageRes(String.valueOf(view.id()), view.code(), view.name(), view.packageType(),
                view.description(), view.enabled(), view.defaultPackage(), view.applicationAccesses().stream()
                .map(item -> new UserFeaturePackageApplicationAccessRes(item.applicationId(), item.applicationCode(),
                        item.applicationName(), item.featureAccessScope(), item.featureIds(),
                        item.features().stream().map(UserFeatureAdminController::toItemRes).toList())).toList());
    }

    private static UserFeatureUserManagementRes toUserManagementRes(UserFeatureUserManagementView view) {
        return new UserFeatureUserManagementRes(view.userId(), view.account(), view.packageIds(),
                view.packages().stream()
                        .map(item -> new UserFeaturePackageOptionRes(item.id(), item.code(), item.name(),
                                item.packageType(), item.description(), item.enabled(), item.defaultPackage()))
                        .toList(), view.applications().stream()
                .map(item -> new UserFeatureUserApplicationRes(item.id(), item.code(), item.name(), item.description(),
                        item.icon(), item.routePath(), item.componentPath(), item.enabled(), item.inheritedVisible(),
                        item.effectiveVisible(), item.packageAccessScope(), item.overrideType(),
                        item.overrideAccessScope(), item.features().stream()
                        .map(feature -> new UserFeatureUserFeatureRes(feature.id(), feature.applicationId(),
                                feature.applicationCode(), feature.code(), feature.name(), feature.description(),
                                feature.enabled(), feature.permissionCodes(), feature.inheritedEnabled(),
                                feature.effectiveEnabled(), feature.overrideType())).toList())).toList());
    }
}
