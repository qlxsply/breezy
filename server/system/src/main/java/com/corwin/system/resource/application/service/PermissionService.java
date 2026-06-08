package com.corwin.system.resource.application.service;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.framework.error.BizException;
import com.corwin.framework.web.auth.AuthPrincipal;
import com.corwin.framework.web.ctx.CtxUtil;
import com.corwin.system.auth.application.service.InternalPermissionSessionService;
import com.corwin.system.normalfeature.application.service.NormalFeatureService;
import com.corwin.system.resource.application.view.MyPermissionsDetailView;
import com.corwin.system.resource.domain.model.FunctionPermission;
import com.corwin.system.resource.domain.model.Permission;
import com.corwin.system.resource.domain.model.PermissionUserScope;
import com.corwin.system.resource.domain.repo.FunctionPermissionRepository;
import com.corwin.system.resource.domain.repo.PermissionRepository;
import com.corwin.system.role.domain.model.Role;
import com.corwin.system.role.domain.model.RoleFunction;
import com.corwin.system.role.domain.repo.RoleFunctionRepository;
import com.corwin.system.role.domain.repo.RoleRepository;
import com.corwin.system.user.domain.model.DefaultUser;
import com.corwin.system.user.domain.model.User;
import com.corwin.system.user.domain.model.UserRole;
import com.corwin.system.user.domain.repo.UserRepository;
import com.corwin.system.user.domain.repo.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Corwin 2026/1/22
 */
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final UserRoleRepository userRoleRepository;
    private final RoleFunctionRepository roleFunctionRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final FunctionPermissionRepository functionPermissionRepository;
    private final ApiPermissionCache apiPermissionCache;
    private final UserRepository userRepository;
    private final NormalFeatureService normalFeatureService;
    private final InternalPermissionSessionService internalPermissionSessionService;

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
            return normalFeatureService.permissionCodesForNormalUser(userId);
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
        if (DefaultUser.isAdmin(userId)) {
            return permissionRepository.findAll().stream()
                    .filter(permission -> Boolean.TRUE.equals(permission.getEnabled())).map(Permission::getCode)
                    .filter(Objects::nonNull).map(String::trim).filter(code -> !code.isBlank())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        List<Long> permissionIds = internalPermissionIdsForUser(userId);
        if (permissionIds.isEmpty()) {
            return Set.of();
        }
        return permissionRepository.findAllById(permissionIds).stream()
                .filter(permission -> Boolean.TRUE.equals(permission.getEnabled()))
                .filter(permission -> permission.getUserScope() == PermissionUserScope.INTERNAL
                        || permission.getUserScope() == PermissionUserScope.COMMON)
                .map(Permission::getCode).filter(Objects::nonNull).map(String::trim).filter(code -> !code.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public List<Permission> assignablePermissionsForInternal() {
        return permissionRepository.findAll().stream()
                .filter(permission -> permission.getUserScope() == PermissionUserScope.INTERNAL
                        || permission.getUserScope() == PermissionUserScope.COMMON)
                .sorted(Comparator.comparing(Permission::getCode, Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(Permission::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    public List<Long> effectivePermissionIdsForUser(Long userId) {
        requireInternalUser(userId);
        return internalPermissionIdsForUser(userId);
    }

    private List<Long> internalPermissionIdsForUser(Long userId) {
        if (DefaultUser.isAdmin(userId)) {
            return permissionRepository.findAll().stream()
                    .filter(permission -> Boolean.TRUE.equals(permission.getEnabled()))
                    .filter(permission -> permission.getUserScope() == PermissionUserScope.INTERNAL
                            || permission.getUserScope() == PermissionUserScope.COMMON)
                    .map(Permission::getId)
                    .toList();
        }
        LinkedHashSet<Long> merged = new LinkedHashSet<>(roleDerivedPermissionIdsForUser(userId));
        if (merged.isEmpty()) {
            return List.of();
        }
        Set<Long> allowedIds = permissionRepository.findAllById(merged).stream()
                .filter(permission -> Boolean.TRUE.equals(permission.getEnabled()))
                .filter(permission -> permission.getUserScope() == PermissionUserScope.INTERNAL
                        || permission.getUserScope() == PermissionUserScope.COMMON)
                .map(Permission::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return allowedIds.stream().toList();
    }

    private void requireInternalUser(Long userId) {
        BizAssert.notNull(userId, BaseError.INVALID_PARAMETER);
        if (DefaultUser.isAdmin(userId)) {
            return;
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
        BizAssert.state(user.getUserType() == UserType.INTERNAL, BaseError.INVALID_PARAMETER);
    }

    private List<Long> roleDerivedPermissionIdsForUser(Long userId) {
        List<Long> roleIds = userRoleRepository.findByUserId(userId).stream().map(UserRole::getRoleId).toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> functionIds = roleFunctionRepository.findByRoleIdIn(roleIds).stream()
                .map(RoleFunction::getFunctionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (functionIds.isEmpty()) {
            return List.of();
        }
        return functionPermissionRepository.findByFunctionIdIn(functionIds).stream()
                .map(FunctionPermission::getPermissionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }
}
