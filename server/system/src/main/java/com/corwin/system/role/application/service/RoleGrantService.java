package com.corwin.system.role.application.service;

import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.framework.error.BizException;
import com.corwin.framework.web.auth.AuthPrincipal;
import com.corwin.framework.web.ctx.CtxUtil;
import com.corwin.system.auth.application.service.InternalPermissionSessionService;
import com.corwin.system.resource.application.service.ApiPermissionCache;
import com.corwin.system.resource.domain.model.Resource;
import com.corwin.system.resource.domain.repo.ResourceRepository;
import com.corwin.system.role.application.command.UpdateRoleGrantCommand;
import com.corwin.system.role.application.view.RoleGrantResourceView;
import com.corwin.system.role.application.view.RoleGrantSelectionView;
import com.corwin.system.role.domain.model.RoleResource;
import com.corwin.system.role.domain.repo.RoleRepository;
import com.corwin.system.role.domain.repo.RoleResourceRepository;
import com.corwin.system.user.domain.model.UserRole;
import com.corwin.system.user.domain.repo.UserRoleRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for role-resource grant management.
 *
 * @author Corwin 2026/6/29
 */
@Service
@RequiredArgsConstructor
public class RoleGrantService {

  private final RoleRepository roleRepository;
  private final RoleResourceRepository roleResourceRepository;
  private final UserRoleRepository userRoleRepository;
  private final ResourceRepository resourceRepository;
  private final ApiPermissionCache apiPermissionCache;
  private final InternalPermissionSessionService internalPermissionSessionService;

  /**
   * Returns the currently granted resource IDs for a given role.
   *
   * @param roleId the role ID
   * @return selection view with the list of resource IDs
   */
  public RoleGrantSelectionView roleGrantSelection(Long roleId) {
    requireRole(roleId);
    List<Long> resourceIds =
        roleResourceRepository.findByRoleId(roleId).stream()
            .map(RoleResource::getResourceId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    return new RoleGrantSelectionView(resourceIds);
  }

  /**
   * Returns all visible resources available for granting, sorted by parent and sort order.
   *
   * @return list of grantable resource views
   */
  public List<RoleGrantResourceView> grantResources() {
    return resourceRepository.findAll().stream()
        .filter(resource -> resource.getId() != null)
        .filter(resource -> Boolean.TRUE.equals(resource.getVisible()))
        .sorted(resourceComparator())
        .map(this::toGrantResourceView)
        .toList();
  }

  /**
   * Updates the granted resources for a role, clears permission cache and kicks out affected
   * sessions.
   *
   * @param roleId the role ID
   * @param cmd the grant update command containing resource IDs
   * @return true on success
   */
  @Transactional
  public boolean updateRoleGrant(Long roleId, UpdateRoleGrantCommand cmd) {
    requireRole(roleId);
    List<Long> affectedUserIds =
        userRoleRepository.findByRoleId(roleId).stream()
            .map(UserRole::getUserId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    roleResourceRepository.deleteByRoleId(roleId);
    List<Long> resourceIds = normalizeResourceIds(cmd == null ? null : cmd.resourceIds());
    Long operatorId = operatorId();
    ArrayList<RoleResource> next = new ArrayList<>();
    for (Long resourceId : resourceIds) {
      next.add(new RoleResource(roleId, resourceId, operatorId));
    }
    roleResourceRepository.saveAll(next);
    apiPermissionCache.clearAll();
    internalPermissionSessionService.kickOutActiveSessions(affectedUserIds, operatorName());
    return true;
  }

  private void requireRole(Long roleId) {
    roleRepository.findById(roleId).orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
  }

  private List<Long> normalizeResourceIds(List<Long> resourceIds) {
    if (resourceIds == null || resourceIds.isEmpty()) {
      return List.of();
    }
    Set<Long> allowedResourceIds =
        grantResources().stream()
            .filter(RoleGrantResourceView::selectable)
            .map(RoleGrantResourceView::resourceId)
            .filter(Objects::nonNull)
            .map(Long::valueOf)
            .collect(Collectors.toCollection(LinkedHashSet::new));

    LinkedHashSet<Long> normalized = new LinkedHashSet<>();
    for (Long resourceId : resourceIds) {
      if (resourceId == null || resourceId <= 0) {
        continue;
      }
      BizAssert.state(allowedResourceIds.contains(resourceId), BaseError.INVALID_PARAMETER);
      normalized.add(resourceId);
    }
    return List.copyOf(normalized);
  }

  private RoleGrantResourceView toGrantResourceView(Resource resource) {
    Long resourceId = resource.getId();
    return new RoleGrantResourceView(
        "resource:" + resourceId,
        resource.getParentId() == null ? null : "resource:" + resource.getParentId(),
        String.valueOf(resourceId),
        resource.getName(),
        resource.getCode(),
        resource.getResourceType().name(),
        resource.getRemark(),
        Boolean.TRUE.equals(resource.getEnabled()),
        true,
        resource.getSortNo() == null ? 0 : resource.getSortNo());
  }

  private Comparator<Resource> resourceComparator() {
    return Comparator.comparing(Resource::getParentId, Comparator.nullsFirst(Long::compareTo))
        .thenComparing(Resource::getSortNo, Comparator.nullsLast(Integer::compareTo))
        .thenComparing(Resource::getId, Comparator.nullsLast(Long::compareTo));
  }

  private Long operatorId() {
    AuthPrincipal principal = CtxUtil.getPrincipal();
    Long operatorId = principal == null ? null : principal.userId();
    BizAssert.notNull(operatorId, BaseError.FORBIDDEN);
    return operatorId;
  }

  private String operatorName() {
    AuthPrincipal principal = CtxUtil.getPrincipal();
    String operatorName = principal == null ? null : principal.username();
    if (operatorName == null || operatorName.isBlank()) {
      Long operatorId = principal == null ? null : principal.userId();
      return operatorId == null ? "system" : String.valueOf(operatorId);
    }
    return operatorName;
  }
}
