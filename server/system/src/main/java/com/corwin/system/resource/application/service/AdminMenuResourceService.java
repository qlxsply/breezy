package com.corwin.system.resource.application.service;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.web.auth.AuthPrincipal;
import com.corwin.system.auth.published.SecurityContextService;
import com.corwin.system.resource.application.view.AdminMenuResourceView;
import com.corwin.system.resource.application.view.AdminMenuResourcesView;
import com.corwin.system.resource.domain.model.Function;
import com.corwin.system.resource.domain.model.FunctionType;
import com.corwin.system.resource.domain.model.Menu;
import com.corwin.system.resource.domain.model.MenuFunction;
import com.corwin.system.resource.domain.repo.FunctionRepository;
import com.corwin.system.resource.domain.repo.MenuFunctionRepository;
import com.corwin.system.resource.domain.repo.MenuRepository;
import com.corwin.system.role.domain.model.RoleMenu;
import com.corwin.system.role.domain.model.RoleFunction;
import com.corwin.system.role.domain.repo.RoleFunctionRepository;
import com.corwin.system.role.domain.repo.RoleMenuRepository;
import com.corwin.system.user.domain.model.UserRole;
import com.corwin.system.user.domain.repo.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * @author Corwin 2026/5/31
 */
@Service
@RequiredArgsConstructor
public class AdminMenuResourceService {

    private final SecurityContextService securityContextService;
    private final MenuRepository menuRepository;
    private final FunctionRepository functionRepository;
    private final MenuFunctionRepository menuFunctionRepository;
    private final RoleFunctionRepository roleFunctionRepository;
    private final RoleMenuRepository roleMenuRepository;
    private final UserRoleRepository userRoleRepository;

    public AdminMenuResourcesView currentAdminMenuResources() {
        Optional<AuthPrincipal> principalOptional = securityContextService.currentOptional();
        if (principalOptional.isEmpty()) {
            return new AdminMenuResourcesView(List.of());
        }
        AuthPrincipal principal = principalOptional.get();
        if (principal.userType() != UserType.INTERNAL) {
            return new AdminMenuResourcesView(List.of());
        }
        if (principal.admin()) {
            return new AdminMenuResourcesView(buildTree(buildAdminResources()));
        }
        return new AdminMenuResourcesView(buildTree(buildInternalResources(principal.userId())));
    }

    private List<AdminMenuResourceView> buildAdminResources() {
        Map<Long, Menu> menuById = menuRepository.findAll().stream().filter(this::menuVisibleAndEnabled)
                .filter(menu -> menu.getId() != null)
                .collect(Collectors.toMap(Menu::getId, value -> value, (left, right) -> left, LinkedHashMap::new));
        List<Menu> menus = menuById.values().stream().sorted(menuComparator()).toList();
        List<Function> functions = functionRepository.findAll().stream().filter(this::functionEnabled)
                .collect(Collectors.toMap(Function::getId, value -> value, (left, right) -> left, LinkedHashMap::new))
                .values().stream().toList();
        Map<Long, Function> functionById = functions.stream()
                .collect(Collectors.toMap(Function::getId, value -> value, (left, right) -> left, LinkedHashMap::new));
        List<MenuFunction> menuFunctions = menuFunctionRepository.findAll().stream()
                .filter(menuFunction -> Boolean.TRUE.equals(menuFunction.getVisible()))
                .filter(menuFunction -> functionById.containsKey(menuFunction.getFunctionId()))
                .filter(menuFunction -> menuById.containsKey(menuFunction.getMenuId()))
                .sorted(menuFunctionComparator()).toList();

        List<AdminMenuResourceView> resources = new ArrayList<>();
        menus.forEach(menu -> resources.add(toMenuResource(menu)));
        menuFunctions.forEach(menuFunction -> {
            Function function = functionById.get(menuFunction.getFunctionId());
            if (function != null) {
                resources.add(toFunctionResource(menuFunction, function));
            }
        });
        return resources;
    }

    private List<AdminMenuResourceView> buildInternalResources(Long userId) {
        List<Long> roleIds = userRoleRepository.findByUserId(userId).stream().map(UserRole::getRoleId)
                .filter(Objects::nonNull).distinct().toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Menu> menuById = menuRepository.findAll().stream().filter(this::menuVisibleAndEnabled)
                .filter(menu -> menu.getId() != null)
                .collect(Collectors.toMap(Menu::getId, value -> value, (left, right) -> left, LinkedHashMap::new));

        Map<Long, Function> functionById = functionRepository.findAll().stream().filter(this::functionEnabled)
                .filter(function -> function.getId() != null)
                .collect(Collectors.toMap(Function::getId, value -> value, (left, right) -> left, LinkedHashMap::new));

        Map<Long, MenuFunction> menuFunctionById = menuFunctionRepository.findAll().stream()
                .filter(mapping -> mapping.getId() != null).filter(mapping -> Boolean.TRUE.equals(mapping.getVisible()))
                .filter(mapping -> functionById.containsKey(mapping.getFunctionId()))
                .filter(mapping -> menuById.containsKey(mapping.getMenuId())).collect(
                        Collectors.toMap(MenuFunction::getId, value -> value, (left, right) -> left,
                                LinkedHashMap::new));

        Set<Long> roleFunctionIds = roleFunctionRepository.findByRoleIdIn(roleIds).stream().map(RoleFunction::getFunctionId)
                .filter(Objects::nonNull).filter(functionById::containsKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<Long> includedMenuFunctionIds = menuFunctionById.values().stream()
                .filter(mapping -> roleFunctionIds.contains(mapping.getFunctionId())).map(MenuFunction::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        includeMenuFunctionAncestors(includedMenuFunctionIds, menuFunctionById);

        Set<Long> includedMenuIds = roleMenuRepository.findByRoleIdIn(roleIds).stream().map(RoleMenu::getMenuId)
                .filter(Objects::nonNull).filter(menuById::containsKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        includedMenuFunctionIds.stream().map(menuFunctionById::get).filter(Objects::nonNull).map(MenuFunction::getMenuId)
                .filter(Objects::nonNull).filter(menuById::containsKey).forEach(includedMenuIds::add);
        includeMenuAncestors(includedMenuIds, menuById);

        List<AdminMenuResourceView> resources = new ArrayList<>();
        includedMenuIds.stream().map(menuById::get).filter(Objects::nonNull).sorted(menuComparator())
                .map(this::toMenuResource).forEach(resources::add);
        includedMenuFunctionIds.stream().map(menuFunctionById::get).filter(Objects::nonNull)
                .sorted(menuFunctionComparator()).forEach(menuFunction -> {
                    Function function = functionById.get(menuFunction.getFunctionId());
                    if (function != null) {
                        resources.add(toFunctionResource(menuFunction, function));
                    }
                });
        return resources;
    }

    private List<AdminMenuResourceView> buildTree(List<AdminMenuResourceView> flatResources) {
        if (flatResources.isEmpty()) {
            return List.of();
        }
        Map<String, AdminMenuResourceView> resourceById = flatResources.stream().collect(
                Collectors.toMap(AdminMenuResourceView::id, value -> value, (left, right) -> left, LinkedHashMap::new));
        Map<String, List<AdminMenuResourceView>> childrenByParentId = new LinkedHashMap<>();
        for (AdminMenuResourceView resource : flatResources) {
            childrenByParentId.computeIfAbsent(resource.parentId(), key -> new ArrayList<>()).add(resource);
        }
        return flatResources.stream()
                .filter(resource -> resource.parentId() == null || !resourceById.containsKey(resource.parentId()))
                .map(resource -> buildTreeNode(resource, childrenByParentId))
                .toList();
    }

    private AdminMenuResourceView buildTreeNode(AdminMenuResourceView resource,
            Map<String, List<AdminMenuResourceView>> childrenByParentId) {
        List<AdminMenuResourceView> children = childrenByParentId.getOrDefault(resource.id(), List.of()).stream()
                .map(child -> buildTreeNode(child, childrenByParentId)).toList();
        return new AdminMenuResourceView(resource.id(), resource.parentId(), resource.name(), resource.icon(),
                resource.code(), resource.type(), resource.url(), resource.loadTarget(), resource.orderNo(), children);
    }

    private void includeMenuFunctionAncestors(Set<Long> includedIds, Map<Long, MenuFunction> menuFunctionById) {
        List<Long> queue = new ArrayList<>(includedIds);
        int index = 0;
        while (index < queue.size()) {
            MenuFunction current = menuFunctionById.get(queue.get(index++));
            if (current == null || current.getParentId() == null) {
                continue;
            }
            MenuFunction parent = menuFunctionById.get(current.getParentId());
            if (parent != null && includedIds.add(parent.getId())) {
                queue.add(parent.getId());
            }
        }
    }

    private void includeMenuAncestors(Set<Long> includedIds, Map<Long, Menu> menuById) {
        List<Long> queue = new ArrayList<>(includedIds);
        int index = 0;
        while (index < queue.size()) {
            Menu current = menuById.get(queue.get(index++));
            if (current == null || current.getParentId() == null) {
                continue;
            }
            Menu parent = menuById.get(current.getParentId());
            if (parent != null && includedIds.add(parent.getId())) {
                queue.add(parent.getId());
            }
        }
    }

    private AdminMenuResourceView toMenuResource(Menu menu) {
        return new AdminMenuResourceView("menu:" + menu.getId(),
                menu.getParentId() == null ? null : "menu:" + menu.getParentId(), menu.getName(),
                menu.getIcon(), menu.getCode(), menu.resolveMenuType().name(), blankToEmpty(menu.getPath()),
                blankToEmpty(menu.getComponent()), menu.getSortNo(), List.of());
    }

    private AdminMenuResourceView toFunctionResource(MenuFunction menuFunction, Function function) {
        return new AdminMenuResourceView("mf:" + menuFunction.getId(),
                menuFunction.getParentId() == null ? "menu:" + menuFunction.getMenuId() : "mf:" + menuFunction.getParentId(),
                function.getName(), null, function.getCode(), resolveType(function), "NONE", "NONE",
                menuFunction.getSortNo() == null ? 0 : menuFunction.getSortNo(), List.of());
    }

    private boolean menuVisibleAndEnabled(Menu menu) {
        return Boolean.TRUE.equals(menu.getEnabled()) && Boolean.TRUE.equals(menu.getVisible());
    }

    private boolean functionEnabled(Function function) {
        return Boolean.TRUE.equals(function.getEnabled());
    }

    private Comparator<Menu> menuComparator() {
        return Comparator.comparing(Menu::getSortNo, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(Menu::getId, Comparator.nullsLast(Long::compareTo));
    }

    private Comparator<MenuFunction> menuFunctionComparator() {
        return Comparator.comparing(MenuFunction::getMenuId, Comparator.nullsLast(Long::compareTo))
                .thenComparing(MenuFunction::getParentId, Comparator.nullsLast(Long::compareTo))
                .thenComparing(MenuFunction::getSortNo, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(MenuFunction::getId, Comparator.nullsLast(Long::compareTo));
    }

    private String resolveType(Function function) {
        return function.getFunctionType() == FunctionType.BUTTON ? "BUTTON" : "FEATURE";
    }

    private String blankToEmpty(String value) {
        return value == null ? "" : value;
    }
}
