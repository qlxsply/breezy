package com.corwin.system.user.application.service;

import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.framework.error.BizException;
import com.corwin.framework.web.auth.AuthPrincipal;
import com.corwin.framework.web.ctx.CtxUtil;
import com.corwin.system.auth.application.service.InternalPermissionSessionService;
import com.corwin.system.resource.application.service.ApiPermissionCache;
import com.corwin.system.role.domain.repo.RoleRepository;
import com.corwin.system.user.application.command.UpdateUserRolesCommand;
import com.corwin.system.user.domain.model.DefaultUser;
import com.corwin.system.user.domain.model.UserRole;
import com.corwin.system.user.domain.repo.UserRepository;
import com.corwin.system.user.domain.repo.UserRoleRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for managing user-role assignments. Handles role query, bulk update, and
 * cascading permission cache invalidation.
 *
 * @author Corwin 2026/1/23
 */
@Service
@RequiredArgsConstructor
public class UserRoleService {

  private final UserRoleRepository userRoleRepository;
  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final ApiPermissionCache apiPermissionCache;
  private final InternalPermissionSessionService internalPermissionSessionService;

  /**
   * Returns the role IDs assigned to a given user.
   *
   * @param userId the user ID
   * @return a list of role IDs
   */
  public List<Long> userRoles(Long userId) {
    return userRoleRepository.findByUserId(userId).stream().map(UserRole::getRoleId).toList();
  }

  /**
   * Replaces all role assignments for a user, clearing the permission cache and evicting active
   * sessions for the affected user.
   *
   * @param userId the user ID
   * @param cmd the command containing the new role IDs
   * @return true if the update was performed
   */
  @Transactional
  public boolean updateUserRoles(Long userId, UpdateUserRolesCommand cmd) {
    if (DefaultUser.isReserved(userId)) {
      throw new BizException(BaseError.FORBIDDEN);
    }
    userRepository.findById(userId).orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
    userRoleRepository.deleteByUserId(userId);
    List<Long> roleIds = cmd.roleIds() == null ? Collections.emptyList() : cmd.roleIds();
    List<UserRole> next = new ArrayList<>();
    Long operator = operator();
    for (Long roleId : new LinkedHashSet<>(roleIds)) {
      if (roleId == null) {
        continue;
      }
      if (!roleRepository.existsById(roleId)) {
        continue;
      }
      next.add(new UserRole(userId, roleId, operator));
    }
    userRoleRepository.saveAll(next);
    apiPermissionCache.clearAll();
    internalPermissionSessionService.kickOutActiveSessions(List.of(userId), operatorName());
    return true;
  }

  private Long operator() {
    AuthPrincipal principal = CtxUtil.getPrincipal();
    Long operator = principal == null ? null : principal.userId();
    BizAssert.notNull(operator, BaseError.FORBIDDEN);
    return operator;
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
