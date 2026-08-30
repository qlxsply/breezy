package com.corwin.system.role.application.service;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.framework.error.BizException;
import com.corwin.framework.web.auth.AuthPrincipal;
import com.corwin.framework.web.ctx.CtxUtil;
import com.corwin.framework.web.sort.PageSpecSorts;
import com.corwin.system.auth.application.service.InternalPermissionSessionService;
import com.corwin.system.resource.application.service.ApiPermissionCache;
import com.corwin.system.role.application.command.CreateRoleCommand;
import com.corwin.system.role.application.command.UpdateRoleCommand;
import com.corwin.system.role.domain.model.Role;
import com.corwin.system.role.domain.repo.RoleRepository;
import com.corwin.system.role.domain.repo.RoleResourceRepository;
import com.corwin.system.user.domain.model.UserRole;
import com.corwin.system.user.domain.repo.UserRoleRepository;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for role CRUD administration.
 *
 * @author Corwin 2026/1/23
 */
@Service
@RequiredArgsConstructor
public class RoleAdminService {

  private final RoleRepository roleRepository;
  private final RoleResourceRepository roleResourceRepository;
  private final UserRoleRepository userRoleRepository;
  private final ApiPermissionCache apiPermissionCache;
  private final InternalPermissionSessionService internalPermissionSessionService;

  /**
   * Returns all roles ordered by ID ascending.
   *
   * @return list of all roles
   */
  public List<Role> list() {
    return roleRepository.findAllByOrderByIdAsc();
  }

  /**
   * Paginated query with optional keyword and enabled filter.
   *
   * @param keyword optional search keyword
   * @param enabled optional enabled filter
   * @param spec pagination specification
   * @return paginated result
   */
  public PageData<Role> page(String keyword, Boolean enabled, PageSpec spec) {
    return roleRepository.page(keyword, enabled, PageSpecSorts.apply(spec));
  }

  /**
   * Retrieves a role by ID.
   *
   * @param id the role ID
   * @return the role entity
   * @throws BizException if not found
   */
  public Role get(Long id) {
    return roleRepository.findById(id).orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
  }

  /**
   * Creates a new role from the given command.
   *
   * @param cmd the create command
   * @return the newly created role
   */
  @Transactional
  public Role create(CreateRoleCommand cmd) {
    String code = normalizeCode(cmd.code());
    BizAssert.state(!roleRepository.existsByCode(code), BaseError.CONFLICT);
    String name = normalizeName(cmd.name());
    boolean enabled = cmd.enabled() == null || cmd.enabled();
    Role role = new Role(code, name, null, false, operatorId());
    if (!enabled) {
      role.disable(operatorId());
    }
    return roleRepository.save(role);
  }

  /**
   * Updates an existing role.
   *
   * @param id the role ID
   * @param cmd the update command
   * @return the updated role
   */
  @Transactional
  public Role update(Long id, UpdateRoleCommand cmd) {
    Role role = get(id);
    String code = normalizeCode(cmd.code());
    if (!role.getCode().equals(code) && roleRepository.existsByCode(code)) {
      BizAssert.fail(BaseError.CONFLICT);
    }
    String name = normalizeName(cmd.name());
    boolean enabled = cmd.enabled() == null ? role.isEnabled() : cmd.enabled();
    role.update(code, name, null, operatorId());
    if (enabled) {
      role.enable(operatorId());
    } else {
      role.disable(operatorId());
    }
    return roleRepository.save(role);
  }

  /**
   * Deletes a role and its related associations, and kicks out affected user sessions.
   *
   * @param id the role ID
   */
  @Transactional
  public void delete(Long id) {
    Role role = get(id);
    List<Long> affectedUserIds =
        userRoleRepository.findByRoleId(id).stream()
            .map(UserRole::getUserId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    roleRepository.delete(role);
    roleResourceRepository.deleteByRoleId(id);
    userRoleRepository.deleteByRoleId(id);
    apiPermissionCache.clearAll();
    internalPermissionSessionService.kickOutActiveSessions(affectedUserIds, operatorName());
  }

  private String normalizeCode(String code) {
    BizAssert.notBlank(code, BaseError.MISSING_PARAMETER);
    return code.trim();
  }

  private String normalizeName(String name) {
    BizAssert.notBlank(name, BaseError.MISSING_PARAMETER);
    return name.trim();
  }

  private Long operatorId() {
    AuthPrincipal principal = CtxUtil.getPrincipal();
    Long operatorId = principal == null ? null : principal.userId();
    BizAssert.notNull(operatorId, BaseError.FORBIDDEN);
    return operatorId;
  }

  private String operatorName() {
    AuthPrincipal principal = CtxUtil.getPrincipal();
    if (principal == null) {
      return "system";
    }
    String username = principal.username();
    if (username == null || username.isBlank()) {
      Long userId = principal.userId();
      return userId == null ? "system" : String.valueOf(userId);
    }
    return username;
  }
}
