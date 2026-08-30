package com.corwin.system.config.interfaces.web;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.web.filter.SensitiveRequestBody;
import com.corwin.framework.web.request.PageSpecFactory;
import com.corwin.framework.web.response.ApiResponse;
import com.corwin.framework.web.response.PageResult;
import com.corwin.system.audit.domain.model.AuditAction;
import com.corwin.system.audit.domain.model.AuditLevel;
import com.corwin.system.audit.domain.model.AuditResource;
import com.corwin.system.audit.published.Audit;
import com.corwin.system.auth.published.Authenticated;
import com.corwin.system.auth.published.Authorize;
import com.corwin.system.config.application.command.ResetConfigCommand;
import com.corwin.system.config.application.command.UpdateConfigCommand;
import com.corwin.system.config.application.command.ValidateConfigCommand;
import com.corwin.system.config.application.service.ConfigCommandService;
import com.corwin.system.config.application.service.ConfigQueryService;
import com.corwin.system.config.application.view.ConfigChangeView;
import com.corwin.system.config.application.view.ConfigEffectiveView;
import com.corwin.system.config.application.view.ConfigManagementView;
import com.corwin.system.config.application.view.ConfigValidationView;
import com.corwin.system.config.interfaces.web.req.BatchResetConfigReq;
import com.corwin.system.config.interfaces.web.req.ConfigPageReq;
import com.corwin.system.config.interfaces.web.req.ResetConfigReq;
import com.corwin.system.config.interfaces.web.req.UpdateConfigReq;
import com.corwin.system.config.interfaces.web.req.ValidateConfigReq;
import com.corwin.system.config.interfaces.web.res.ConfigChangeRes;
import com.corwin.system.config.interfaces.web.res.ConfigEffectiveRes;
import com.corwin.system.config.interfaces.web.res.ConfigRes;
import com.corwin.system.config.interfaces.web.res.ConfigValidationRes;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @author Corwin 2026/7/31
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sys/configs")
@ApiMeta(module = ApiModuleCode.SYSTEM)
public class ConfigController {

  private final ConfigQueryService configQueryService;
  private final ConfigCommandService configCommandService;

  @PostMapping("/page")
  @Authorize(
      userType = UserType.ADMIN,
      permissions = {"cfg.view"})
  public ApiResponse<PageResult<ConfigRes>> page(@Valid @RequestBody ConfigPageReq req) {
    var page =
        configQueryService.page(
            req.keyword(), req.module(), req.group(), PageSpecFactory.of(req.page(), null));
    return ApiResponse.ok(PageResult.of(page, ConfigController::toRes));
  }

  @GetMapping("/{configKey}")
  @Authorize(
      userType = UserType.ADMIN,
      permissions = {"cfg.view"})
  public ApiResponse<ConfigRes> detail(@PathVariable String configKey) {
    return ApiResponse.ok(toRes(configQueryService.detail(configKey)));
  }

  @GetMapping("/effective/{configKey}")
  @Authenticated(allowExpiredCredentials = true)
  public ApiResponse<ConfigEffectiveRes> effective(@PathVariable String configKey) {
    return ApiResponse.ok(toRes(configQueryService.effective(configKey)));
  }

  @PostMapping("/{configKey}/validate")
  @Authorize(
      userType = UserType.ADMIN,
      permissions = {"cfg.edit"})
  @SensitiveRequestBody
  public ApiResponse<ConfigValidationRes> validate(
      @PathVariable String configKey, @Valid @RequestBody ValidateConfigReq req) {
    var view = configCommandService.validate(new ValidateConfigCommand(configKey, req.value()));
    return ApiResponse.ok(toRes(view));
  }

  @PutMapping("/{configKey}")
  @Authorize(
      userType = UserType.ADMIN,
      permissions = {"cfg.edit"})
  @SensitiveRequestBody
  @Audit(
      resource = AuditResource.CONFIG,
      action = AuditAction.UPDATE,
      level = AuditLevel.HIGH,
      recordRequest = false)
  public ApiResponse<ConfigChangeRes> update(
      @PathVariable String configKey, @Valid @RequestBody UpdateConfigReq req) {
    var view =
        configCommandService.update(
            new UpdateConfigCommand(configKey, req.expectedRevision(), req.reason(), req.value()));
    return ApiResponse.ok(toRes(view));
  }

  @PostMapping("/{configKey}/reset-default")
  @Authorize(
      userType = UserType.ADMIN,
      permissions = {"cfg.edit"})
  @Audit(
      resource = AuditResource.CONFIG,
      action = AuditAction.UPDATE,
      level = AuditLevel.HIGH,
      recordRequest = false)
  public ApiResponse<ConfigChangeRes> resetDefault(
      @PathVariable String configKey, @Valid @RequestBody ResetConfigReq req) {
    var view =
        configCommandService.resetDefault(
            new ResetConfigCommand(configKey, req.expectedRevision(), req.reason()));
    return ApiResponse.ok(toRes(view));
  }

  @PostMapping("/batch-reset-default")
  @Authorize(
      userType = UserType.ADMIN,
      permissions = {"cfg.edit"})
  @Audit(
      resource = AuditResource.CONFIG,
      action = AuditAction.UPDATE,
      level = AuditLevel.HIGH,
      recordRequest = false)
  public ApiResponse<List<ConfigChangeRes>> batchResetDefault(
      @Valid @RequestBody BatchResetConfigReq req) {
    var commands =
        req.items().stream()
            .map(
                item ->
                    new ResetConfigCommand(item.configKey(), item.expectedRevision(), req.reason()))
            .toList();
    return ApiResponse.ok(
        configCommandService.resetDefaults(commands).stream()
            .map(ConfigController::toRes)
            .toList());
  }

  private static ConfigRes toRes(ConfigManagementView view) {
    return new ConfigRes(
        view.key(),
        view.module(),
        view.group(),
        view.title(),
        view.description(),
        view.schemaVersion(),
        view.persistedRevision(),
        view.effectiveRevision(),
        view.configured(),
        view.activationPolicy(),
        view.editPolicy(),
        view.source(),
        view.status(),
        view.pendingRestart(),
        view.effectiveValue(),
        view.persistedValue(),
        view.defaultValue(),
        view.fields(),
        view.sensitiveValuePresence(),
        view.editorId(),
        view.loadWarning());
  }

  private static ConfigValidationRes toRes(ConfigValidationView view) {
    return new ConfigValidationRes(view.valid(), view.violations());
  }

  private static ConfigEffectiveRes toRes(ConfigEffectiveView view) {
    return new ConfigEffectiveRes(
        view.key(), view.title(), view.description(), view.effectiveValue());
  }

  private static ConfigChangeRes toRes(ConfigChangeView view) {
    return new ConfigChangeRes(view.key(), view.persistedRevision(), view.pendingRestart());
  }
}
