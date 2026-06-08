package com.corwin.system.role.application.service;

import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.framework.error.BizException;
import com.corwin.framework.web.auth.AuthPrincipal;
import com.corwin.framework.web.ctx.CtxUtil;
import com.corwin.system.auth.application.service.InternalPermissionSessionService;
import com.corwin.system.resource.application.service.ApiPermissionCache;
import com.corwin.system.resource.domain.model.Function;
import com.corwin.system.resource.domain.model.FunctionPermission;
import com.corwin.system.resource.domain.model.Menu;
import com.corwin.system.resource.domain.model.MenuType;
import com.corwin.system.resource.domain.model.MenuFunction;
import com.corwin.system.resource.domain.model.Permission;
import com.corwin.system.resource.domain.model.PermissionUserScope;
import com.corwin.system.resource.domain.repo.FunctionPermissionRepository;
import com.corwin.system.resource.domain.repo.FunctionRepository;
import com.corwin.system.resource.domain.repo.MenuFunctionRepository;
import com.corwin.system.resource.domain.repo.MenuRepository;
import com.corwin.system.resource.domain.repo.PermissionRepository;
import com.corwin.system.role.application.command.UpdateRoleGrantCommand;
import com.corwin.system.role.application.view.RoleGrantResourceView;
import com.corwin.system.role.application.view.RoleGrantSelectionView;
import com.corwin.system.role.domain.model.RoleFunction;
import com.corwin.system.role.domain.model.RoleMenu;
import com.corwin.system.role.domain.repo.RoleFunctionRepository;
import com.corwin.system.role.domain.repo.RoleMenuRepository;
import com.corwin.system.role.domain.repo.RoleRepository;
import com.corwin.system.user.domain.model.UserRole;
import com.corwin.system.user.domain.repo.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Corwin 2026/5/7
 */
@Service
@RequiredArgsConstructor
public class RoleGrantService {

    private final RoleRepository roleRepository;
    private final RoleMenuRepository roleMenuRepository;
    private final RoleFunctionRepository roleFunctionRepository;
    private final UserRoleRepository userRoleRepository;
    private final MenuRepository menuRepository;
    private final FunctionRepository functionRepository;
    private final MenuFunctionRepository menuFunctionRepository;
    private final FunctionPermissionRepository functionPermissionRepository;
    private final PermissionRepository permissionRepository;
    private final ApiPermissionCache apiPermissionCache;
    private final InternalPermissionSessionService internalPermissionSessionService;

    public RoleGrantSelectionView roleGrantSelection(Long roleId) {
        requireRole(roleId);
        List<Long> menuIds = roleMenuRepository.findByRoleId(roleId).stream()
                .map(RoleMenu::getMenuId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<Long> functionIds = roleFunctionRepository.findByRoleId(roleId).stream()
                .map(RoleFunction::getFunctionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return new RoleGrantSelectionView(menuIds, functionIds);
    }

    public List<RoleGrantResourceView> grantResources() {
        List<Menu> menus = menuRepository.findAll().stream()
                .filter(menu -> Boolean.TRUE.equals(menu.getVisible()))
                .sorted(Comparator.comparing(Menu::getSortNo, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(Menu::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
        Map<Long, Function> functionById = functionRepository.findAll().stream()
                .filter(function -> function.getId() != null)
                .collect(Collectors.toMap(Function::getId, value -> value, (left, right) -> left, LinkedHashMap::new));
        List<MenuFunction> menuFunctions = menuFunctionRepository.findAll().stream()
                .filter(mapping -> mapping.getId() != null)
                .filter(mapping -> Boolean.TRUE.equals(mapping.getVisible()))
                .filter(mapping -> functionById.containsKey(mapping.getFunctionId()))
                .sorted(Comparator.comparing(MenuFunction::getMenuId, Comparator.nullsLast(Long::compareTo))
                        .thenComparing(MenuFunction::getParentId, Comparator.nullsLast(Long::compareTo))
                        .thenComparing(MenuFunction::getSortNo, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(MenuFunction::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();

        Map<Long, List<String>> permissionCodesByFunctionId = permissionCodesByFunctionId(menuFunctions.stream()
                .map(MenuFunction::getFunctionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new)));

        ArrayList<RoleGrantResourceView> resources = new ArrayList<>();
        for (Menu menu : menus) {
            resources.add(new RoleGrantResourceView(
                    "menu:" + menu.getId(),
                    menu.getParentId() == null ? null : "menu:" + menu.getParentId(),
                    String.valueOf(menu.getId()),
                    null,
                    menu.getName(),
                    menu.getCode(),
                    menu.resolveMenuType().name(),
                    menu.getRemark(),
                    Boolean.TRUE.equals(menu.getEnabled()),
                    true,
                    menu.getSortNo() == null ? 0 : menu.getSortNo(),
                    List.of()));
        }
        for (MenuFunction menuFunction : menuFunctions) {
            Function function = functionById.get(menuFunction.getFunctionId());
            if (function == null) {
                continue;
            }
            resources.add(new RoleGrantResourceView(
                    "mf:" + menuFunction.getId(),
                    menuFunction.getParentId() == null ? "menu:" + menuFunction.getMenuId()
                            : "mf:" + menuFunction.getParentId(),
                    null,
                    String.valueOf(function.getId()),
                    function.getName(),
                    function.getCode(),
                    resolveGrantFunctionType(function),
                    function.getDescription(),
                    Boolean.TRUE.equals(function.getEnabled()),
                    true,
                    menuFunction.getSortNo() == null ? 0 : menuFunction.getSortNo(),
                    permissionCodesByFunctionId.getOrDefault(function.getId(), List.of())));
        }
        return resources;
    }

    @Transactional
    public boolean updateRoleGrant(Long roleId, UpdateRoleGrantCommand cmd) {
        requireRole(roleId);
        List<Long> affectedUserIds = userRoleRepository.findByRoleId(roleId).stream()
                .map(UserRole::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        roleMenuRepository.deleteByRoleId(roleId);
        roleFunctionRepository.deleteByRoleId(roleId);
        List<Long> menuIds = normalizeMenuIds(cmd == null ? null : cmd.menuIds());
        List<Long> functionIds = normalizeFunctionIds(cmd == null ? null : cmd.functionIds());
        Long operatorId = operatorId();
        ArrayList<RoleMenu> menus = new ArrayList<>();
        for (Long menuId : menuIds) {
            menus.add(new RoleMenu(roleId, menuId, operatorId));
        }
        ArrayList<RoleFunction> next = new ArrayList<>();
        for (Long functionId : functionIds) {
            next.add(new RoleFunction(roleId, functionId, operatorId));
        }
        roleMenuRepository.saveAll(menus);
        roleFunctionRepository.saveAll(next);
        apiPermissionCache.clearAll();
        internalPermissionSessionService.kickOutActiveSessions(affectedUserIds, operatorName());
        return true;
    }

    private void requireRole(Long roleId) {
        roleRepository.findById(roleId).orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
    }

    private List<Long> normalizeMenuIds(List<Long> menuIds) {
        if (menuIds == null || menuIds.isEmpty()) {
            return List.of();
        }
        Set<Long> allowedMenuIds = grantResources().stream()
                .map(RoleGrantResourceView::menuId)
                .filter(Objects::nonNull)
                .map(Long::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        LinkedHashSet<Long> normalized = new LinkedHashSet<>();
        for (Long menuId : menuIds) {
            if (menuId == null || menuId <= 0) {
                continue;
            }
            BizAssert.state(allowedMenuIds.contains(menuId), BaseError.INVALID_PARAMETER);
            normalized.add(menuId);
        }
        return List.copyOf(normalized);
    }

    private List<Long> normalizeFunctionIds(List<Long> functionIds) {
        if (functionIds == null || functionIds.isEmpty()) {
            return List.of();
        }
        Set<Long> allowedFunctionIds = grantResources().stream()
                .filter(RoleGrantResourceView::selectable)
                .map(RoleGrantResourceView::functionId)
                .filter(Objects::nonNull)
                .map(Long::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        LinkedHashSet<Long> normalized = new LinkedHashSet<>();
        for (Long functionId : functionIds) {
            if (functionId == null || functionId <= 0) {
                continue;
            }
            BizAssert.state(allowedFunctionIds.contains(functionId), BaseError.INVALID_PARAMETER);
            normalized.add(functionId);
        }
        return List.copyOf(normalized);
    }

    private Map<Long, List<String>> permissionCodesByFunctionId(Set<Long> functionIds) {
        if (functionIds.isEmpty()) {
            return Map.of();
        }
        List<FunctionPermission> mappings = functionPermissionRepository.findByFunctionIdIn(functionIds);
        if (mappings.isEmpty()) {
            return Map.of();
        }
        LinkedHashSet<Long> permissionIds = mappings.stream()
                .map(FunctionPermission::getPermissionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, String> permissionCodeById = permissionRepository.findAllById(permissionIds).stream()
                .filter(permission -> Boolean.TRUE.equals(permission.getEnabled()))
                .filter(permission -> permission.getUserScope() == PermissionUserScope.INTERNAL
                        || permission.getUserScope() == PermissionUserScope.COMMON)
                .filter(permission -> permission.getCode() != null && !permission.getCode().isBlank())
                .collect(Collectors.toMap(Permission::getId, permission -> permission.getCode().trim(),
                        (left, right) -> left, LinkedHashMap::new));

        LinkedHashMap<Long, LinkedHashSet<String>> grouped = new LinkedHashMap<>();
        for (FunctionPermission mapping : mappings) {
            String permissionCode = permissionCodeById.get(mapping.getPermissionId());
            if (permissionCode == null) {
                continue;
            }
            grouped.computeIfAbsent(mapping.getFunctionId(), __ -> new LinkedHashSet<>()).add(permissionCode);
        }

        LinkedHashMap<Long, List<String>> result = new LinkedHashMap<>();
        for (Map.Entry<Long, LinkedHashSet<String>> entry : grouped.entrySet()) {
            result.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return result;
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

    private String resolveGrantFunctionType(Function function) {
        return switch (function.getFunctionType()) {
            case PAGE, QUERY, ACTION, BUTTON, INVISIBLE -> "BUTTON";
        };
    }
}
