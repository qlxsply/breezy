package com.corwin.system.resource.application.service;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.web.ctx.CtxUtil;
import com.corwin.system.resource.application.view.MyPermissionsDetailView;
import com.corwin.system.resource.domain.model.Permission;
import com.corwin.system.resource.domain.model.Resource;
import com.corwin.system.resource.domain.model.ResourcePermission;
import com.corwin.system.resource.domain.repo.PermissionRepository;
import com.corwin.system.resource.domain.repo.ResourcePermissionRepository;
import com.corwin.system.resource.domain.repo.ResourceRepository;
import com.corwin.system.role.domain.model.Role;
import com.corwin.system.role.domain.model.RoleResource;
import com.corwin.system.role.domain.repo.RoleRepository;
import com.corwin.system.role.domain.repo.RoleResourceRepository;
import com.corwin.system.user.domain.model.DefaultUser;
import com.corwin.system.user.domain.model.UserRole;
import com.corwin.system.user.domain.repo.UserRoleRepository;
import com.corwin.system.userfeature.application.service.UserFeatureAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Corwin 2026/6/29
 */
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final UserRoleRepository userRoleRepository;
    private final RoleResourceRepository roleResourceRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final ResourcePermissionRepository resourcePermissionRepository;
    private final ResourceRepository resourceRepository;
    private final UserFeatureAccessService userFeatureAccessService;

    public MyPermissionsDetailView getPermissionDetailForCurrent() {
        Long userId = CtxUtil.getPrincipal().userId();
        String username = CtxUtil.getPrincipal().username();
        UserType userType = CtxUtil.getPrincipal().userType();
        if (userId == null) {
            return new MyPermissionsDetailView(username == null ? "GUEST" : username, List.of(), List.of());
        }
        List<String> roleNames = userType == UserType.INTERNAL
                ? roleRepository.findByIdIn(userRoleRepository.findByUserId(userId).stream().map(UserRole::getRoleId).toList())
                .stream().map(Role::getName).toList()
                : List.of();
        List<String> permissionCodes = permissionCodesForUser(userId, userType).stream().sorted().toList();
        return new MyPermissionsDetailView(username == null ? String.valueOf(userId) : username, roleNames,
                permissionCodes);
    }

    public Set<String> permissionCodesForCurrent() {
        UserType userType = CtxUtil.getPrincipal().userType();
        if (userType == null || userType == UserType.GUEST) {
            return Set.of();
        }
        Long userId = CtxUtil.getPrincipal().userId();
        if (userId == null) {
            return Set.of();
        }
        return permissionCodesForUser(userId, userType);
    }

    public Set<String> permissionCodesForUser(Long userId, UserType userType) {
        if (userType == UserType.EXTERNAL) {
            return userFeatureAccessService.permissionCodesForExternalUser(userId);
        }
        if (DefaultUser.isAdmin(userId)) {
            return permissionRepository.findAll().stream()
                    .filter(permission -> permission.getUserScope() == UserType.INTERNAL)
                    .map(Permission::getCode)
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(code -> !code.isBlank())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        List<Long> permissionIds = internalPermissionIdsForUser(userId);
        if (permissionIds.isEmpty()) {
            return Set.of();
        }
        return permissionRepository.findAllById(permissionIds).stream()
                .filter(permission -> permission.getUserScope() == UserType.INTERNAL)
                .map(Permission::getCode)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(code -> !code.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public List<Permission> assignablePermissionsForInternal() {
        return permissionRepository.findAll().stream()
                .filter(permission -> permission.getUserScope() == UserType.INTERNAL)
                .sorted(Comparator.comparing(Permission::getCode, Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(Permission::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    private List<Long> internalPermissionIdsForUser(Long userId) {
        if (DefaultUser.isAdmin(userId)) {
            return permissionRepository.findAll().stream()
                    .filter(permission -> permission.getUserScope() == UserType.INTERNAL)
                    .map(Permission::getId)
                    .toList();
        }
        LinkedHashSet<Long> merged = new LinkedHashSet<>(roleDerivedPermissionIdsForUser(userId));
        if (merged.isEmpty()) {
            return List.of();
        }
        Set<Long> allowedIds = permissionRepository.findAllById(merged).stream()
                .filter(permission -> permission.getUserScope() == UserType.INTERNAL)
                .map(Permission::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return allowedIds.stream().toList();
    }

    private List<Long> roleDerivedPermissionIdsForUser(Long userId) {
        List<Long> roleIds = userRoleRepository.findByUserId(userId).stream()
                .map(UserRole::getRoleId)
                .toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> roleResourceIds = roleResourceRepository.findByRoleIdIn(roleIds).stream()
                .map(RoleResource::getResourceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (roleResourceIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Resource> resourceById = resourceRepository.findAllById(roleResourceIds).stream()
                .filter(resource -> resource.getId() != null)
                .filter(resource -> Boolean.TRUE.equals(resource.getEnabled()))
                .collect(Collectors.toMap(Resource::getId, value -> value, (left, right) -> left, LinkedHashMap::new));
        LinkedHashSet<Long> buttonResourceIds = resourceById.values().stream()
                .filter(Resource::canBindPermission)
                .map(Resource::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (buttonResourceIds.isEmpty()) {
            return List.of();
        }

        return resourcePermissionRepository.findByResourceIdIn(buttonResourceIds).stream()
                .map(ResourcePermission::getPermissionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }
}
