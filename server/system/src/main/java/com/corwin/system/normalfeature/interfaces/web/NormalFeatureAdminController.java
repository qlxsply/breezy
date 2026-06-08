package com.corwin.system.normalfeature.interfaces.web;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.web.request.PageSpecFactory;
import com.corwin.framework.web.response.ApiResponse;
import com.corwin.framework.web.response.PageResult;
import com.corwin.system.audit.domain.model.AuditAction;
import com.corwin.system.audit.domain.model.AuditLevel;
import com.corwin.system.audit.domain.model.AuditResource;
import com.corwin.system.audit.published.Audit;
import com.corwin.system.auth.published.Authorize;
import com.corwin.system.normalfeature.application.command.SaveNormalFeatureGroupCommand;
import com.corwin.system.normalfeature.application.command.SaveNormalFeatureUserManagementCommand;
import com.corwin.system.normalfeature.application.command.UpdateNormalFeatureCommand;
import com.corwin.system.normalfeature.application.service.NormalFeatureGroupAdminService;
import com.corwin.system.normalfeature.application.service.NormalFeatureService;
import com.corwin.system.normalfeature.application.view.*;
import com.corwin.system.normalfeature.interfaces.web.req.*;
import com.corwin.system.normalfeature.interfaces.web.res.*;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Corwin 2026/5/5
 */
@ApiMeta(module = ApiModuleCode.SYSTEM)
@RestController
@RequestMapping("/api/normal-features")
@RequiredArgsConstructor
public class NormalFeatureAdminController {

    private final NormalFeatureService normalFeatureService;
    private final NormalFeatureGroupAdminService normalFeatureGroupAdminService;

    @PostMapping("/page")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"nfm.view"})
    public ApiResponse<PageResult<NormalFeatureRes>> page(@RequestBody NormalFeaturePageReq req) {
        var rows = normalFeatureService.manageableFeatures();
        var keyword = req == null ? null : req.keyword();
        var enabled = req == null ? null : req.enabled();
        var filtered = rows.stream().filter(item -> {
            if (enabled != null && item.enabled() != enabled) {
                return false;
            }
            if (keyword == null || keyword.isBlank()) {
                return true;
            }
            String kw = keyword.trim().toLowerCase();
            return item.code().toLowerCase().contains(kw) || item.name().toLowerCase()
                    .contains(kw) || (item.description() != null && item.description().toLowerCase().contains(kw));
        }).toList();
        var spec = PageSpecFactory.of(req == null ? null : req.page(), req == null ? null : req.sort());
        int total = filtered.size();
        int from = Math.clamp((long) (spec.pageNo() - 1) * spec.pageSize(), 0, total);
        int to = Math.min(from + spec.pageSize(), total);
        return ApiResponse.ok(PageResult.of(spec.pageNo(), spec.pageSize(), total,
                filtered.subList(from, to).stream().map(NormalFeatureAdminController::toRes).toList()));
    }

    @GetMapping({"/features", "/resources"})
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"nfm.view"})
    public ApiResponse<List<NormalFeatureRes>> features() {
        return ApiResponse.ok(
                normalFeatureService.manageableFeatures().stream().map(NormalFeatureAdminController::toRes).toList());
    }

    @GetMapping("/features/{id}")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"nfm.view"})
    public ApiResponse<NormalFeatureRes> getFeature(@PathVariable Long id) {
        return ApiResponse.ok(normalFeatureService.manageableFeatures().stream()
                .filter(item -> String.valueOf(item.id()).equals(String.valueOf(id))).findFirst()
                .map(NormalFeatureAdminController::toRes).orElseThrow());
    }

    @PutMapping("/features/{id}/status")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"nfm.default.save"})
    @Audit(resource = AuditResource.NORMAL_FEATURE, action = AuditAction.UPDATE, level = AuditLevel.HIGH)
    public ApiResponse<Boolean> updateFeatureStatus(@PathVariable Long id, @RequestBody UpdateFeatureStatusReq req) {
        return ApiResponse.ok(normalFeatureService.updateFeatureStatus(id, req.enabled()));
    }

    @GetMapping("/default")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"nfm.view"})
    public ApiResponse<NormalFeatureSelectionRes> defaultFeatures() {
        return ApiResponse.ok(new NormalFeatureSelectionRes(normalFeatureService.globalFeatureIds()));
    }

    @PutMapping("/default")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"nfm.default.save"})
    @Audit(resource = AuditResource.NORMAL_FEATURE, action = AuditAction.UPDATE_DEFAULT, level = AuditLevel.HIGH)
    public ApiResponse<Boolean> updateDefaultFeatures(@RequestBody UpdateNormalFeatureReq req) {
        return ApiResponse.ok(
                normalFeatureService.updateGlobalFeatures(new UpdateNormalFeatureCommand(req.resolvedFeatureIds())));
    }

    @GetMapping("/users/{userId}")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"nfm.user.view"})
    public ApiResponse<NormalFeatureSelectionRes> userFeatures(@PathVariable Long userId) {
        return ApiResponse.ok(new NormalFeatureSelectionRes(normalFeatureService.effectiveFeatureIdsForUser(userId)));
    }

    @PutMapping("/users/{userId}")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"nfm.user.save"})
    @Audit(resource = AuditResource.NORMAL_FEATURE, action = AuditAction.UPDATE_USER, level = AuditLevel.HIGH)
    public ApiResponse<Boolean> updateUserFeatures(@PathVariable Long userId, @RequestBody UpdateNormalFeatureReq req) {
        return ApiResponse.ok(normalFeatureService.updateUserFeatures(userId,
                new UpdateNormalFeatureCommand(req.resolvedFeatureIds())));
    }

    @GetMapping("/users/{userId}/management")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"nfm.user.view"})
    public ApiResponse<NormalFeatureUserManagementRes> userManagement(@PathVariable Long userId) {
        return ApiResponse.ok(toUserManagementRes(normalFeatureService.getUserManagement(userId)));
    }

    @PutMapping("/users/{userId}/management")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"nfm.user.save"})
    @Audit(resource = AuditResource.NORMAL_FEATURE, action = AuditAction.UPDATE_USER,
            description = "维护用户功能配置", level = AuditLevel.HIGH)
    public ApiResponse<Boolean> saveUserManagement(@PathVariable Long userId,
            @RequestBody SaveNormalFeatureUserManagementReq req) {
        var overrides = req.overrides() == null ? List.<SaveNormalFeatureUserManagementCommand.FeatureOverrideCommand>of() : req.overrides()
                .stream()
                .map(item -> new SaveNormalFeatureUserManagementCommand.FeatureOverrideCommand(item.featureId(),
                        item.overrideType())).toList();
        return ApiResponse.ok(normalFeatureService.saveUserManagement(userId,
                new SaveNormalFeatureUserManagementCommand(req.groupIds(), overrides)));
    }

    @PostMapping("/groups/page")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"nfm.view"})
    public ApiResponse<PageResult<NormalFeatureGroupRes>> pageGroups(@RequestBody NormalFeatureGroupPageReq req) {
        var page = normalFeatureGroupAdminService.page(req == null ? null : req.keyword(),
                req == null ? null : req.enabled(),
                PageSpecFactory.of(req == null ? null : req.page(), req == null ? null : req.sort()));
        return ApiResponse.ok(PageResult.of(page, NormalFeatureAdminController::toGroupRes));
    }

    @GetMapping("/groups/{id}")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"nfm.view"})
    public ApiResponse<NormalFeatureGroupRes> getGroup(@PathVariable Long id) {
        return ApiResponse.ok(toGroupRes(normalFeatureGroupAdminService.get(id)));
    }

    @PostMapping("/groups")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"nfm.user.save"})
    @Audit(resource = AuditResource.NORMAL_FEATURE, action = AuditAction.CREATE, description = "创建用户分组",
            level = AuditLevel.HIGH)
    public ApiResponse<NormalFeatureGroupRes> createGroup(@RequestBody SaveNormalFeatureGroupReq req) {
        return ApiResponse.ok(toGroupRes(normalFeatureGroupAdminService.create(toGroupCommand(req))));
    }

    @PutMapping("/groups/{id}")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"nfm.user.save"})
    @Audit(resource = AuditResource.NORMAL_FEATURE, action = AuditAction.UPDATE, description = "更新用户分组",
            level = AuditLevel.HIGH)
    public ApiResponse<NormalFeatureGroupRes> updateGroup(@PathVariable Long id,
            @RequestBody SaveNormalFeatureGroupReq req) {
        return ApiResponse.ok(toGroupRes(normalFeatureGroupAdminService.update(id, toGroupCommand(req))));
    }

    @PutMapping("/groups/{id}/status")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"nfm.user.save"})
    @Audit(resource = AuditResource.NORMAL_FEATURE, action = AuditAction.UPDATE, description = "更新用户分组状态",
            level = AuditLevel.HIGH)
    public ApiResponse<Boolean> updateGroupStatus(@PathVariable Long id, @RequestBody UpdateFeatureStatusReq req) {
        return ApiResponse.ok(normalFeatureGroupAdminService.updateStatus(id, req.enabled()));
    }

    @DeleteMapping("/groups/{id}")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"nfm.user.save"})
    @Audit(resource = AuditResource.NORMAL_FEATURE, action = AuditAction.DELETE, description = "删除用户分组",
            level = AuditLevel.CRITICAL)
    public ApiResponse<Boolean> deleteGroup(@PathVariable Long id) {
        return ApiResponse.ok(normalFeatureGroupAdminService.delete(id));
    }

    private static NormalFeatureRes toRes(NormalFeatureView view) {
        return new NormalFeatureRes(String.valueOf(view.id()), view.code(), view.name(), view.description(),
                view.enabled(), view.permissionCodes());
    }

    private static NormalFeatureGroupRes toGroupRes(NormalFeatureGroupView view) {
        return new NormalFeatureGroupRes(String.valueOf(view.id()), view.code(), view.name(), view.groupType(),
                view.description(), view.enabled(), view.defaultGroup(), view.featureIds(),
                view.features().stream().map(NormalFeatureAdminController::toFeatureItemRes).toList());
    }

    private static NormalFeatureItemRes toFeatureItemRes(NormalFeatureItemView view) {
        return new NormalFeatureItemRes(String.valueOf(view.id()), view.code(), view.name(), view.description(),
                view.enabled());
    }

    private static SaveNormalFeatureGroupCommand toGroupCommand(SaveNormalFeatureGroupReq req) {
        return new SaveNormalFeatureGroupCommand(req.code(), req.name(), req.groupType(), req.description(),
                req.enabled(), req.defaultGroup(), req.featureIds());
    }

    private static NormalFeatureUserManagementRes toUserManagementRes(NormalFeatureUserManagementView view) {
        return new NormalFeatureUserManagementRes(String.valueOf(view.userId()), view.account(), view.groupIds(),
                view.features().stream().map(NormalFeatureAdminController::toUserFeatureRes).toList());
    }

    private static NormalFeatureUserFeatureRes toUserFeatureRes(NormalFeatureUserFeatureView view) {
        return new NormalFeatureUserFeatureRes(String.valueOf(view.id()), view.code(), view.name(), view.description(),
                view.enabled(), view.permissionCodes(), view.groupEnabled(), view.effectiveEnabled(),
                view.overrideType());
    }
}
