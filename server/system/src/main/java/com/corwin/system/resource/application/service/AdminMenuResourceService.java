package com.corwin.system.resource.application.service;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.web.auth.AuthPrincipal;
import com.corwin.system.auth.published.SecurityContextService;
import com.corwin.system.file.application.service.StaticAssetQueryService;
import com.corwin.system.normalfeature.domain.model.NormalFeature;
import com.corwin.system.normalfeature.domain.model.NormalFeaturePermission;
import com.corwin.system.normalfeature.domain.repo.NormalFeaturePermissionRepository;
import com.corwin.system.normalfeature.domain.repo.NormalFeatureRepository;
import com.corwin.system.resource.application.view.AdminMenuResourceView;
import com.corwin.system.resource.application.view.AdminMenuResourcesView;
import com.corwin.system.resource.domain.model.Function;
import com.corwin.system.resource.domain.model.FunctionPermission;
import com.corwin.system.resource.domain.model.FunctionType;
import com.corwin.system.resource.domain.model.Menu;
import com.corwin.system.resource.domain.model.MenuFunction;
import com.corwin.system.resource.domain.model.MenuType;
import com.corwin.system.resource.domain.model.Permission;
import com.corwin.system.resource.domain.repo.FunctionPermissionRepository;
import com.corwin.system.resource.domain.repo.FunctionRepository;
import com.corwin.system.resource.domain.repo.MenuFunctionRepository;
import com.corwin.system.resource.domain.repo.MenuRepository;
import com.corwin.system.resource.domain.repo.PermissionRepository;
import com.corwin.system.role.domain.model.RoleMenu;
import com.corwin.system.role.domain.repo.RoleMenuRepository;
import com.corwin.system.user.domain.model.UserRole;
import com.corwin.system.user.domain.repo.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Corwin 2026/5/31
 */
@Service
@RequiredArgsConstructor
public class AdminMenuResourceService {

    private final SecurityContextService securityContextService;
    private final PermissionService permissionService;
    private final PermissionRepository permissionRepository;
    private final MenuRepository menuRepository;
    private final FunctionRepository functionRepository;
    private final MenuFunctionRepository menuFunctionRepository;
    private final FunctionPermissionRepository functionPermissionRepository;
    private final RoleMenuRepository roleMenuRepository;
    private final UserRoleRepository userRoleRepository;
    private final StaticAssetQueryService staticAssetQueryService;
    private final NormalFeatureRepository normalFeatureRepository;
    private final NormalFeaturePermissionRepository normalFeaturePermissionRepository;

    private static final Map<String, ExternalToolRoute> EXTERNAL_TOOL_ROUTE_MAP = Map.of(
            "jsonfmt", new ExternalToolRoute("/jsonfmt", "pages/JsonFormatterPage.vue", 10),
            "reminder", new ExternalToolRoute("/todo", "pages/TodoBoardPage.vue", 20),
            "datasource", new ExternalToolRoute("/datasource", "pages/DataSourceAdminPage.vue", 30),
            "schemaforge", new ExternalToolRoute("/schemaforge", "pages/SchemaForgePage.vue", 40),
            "storage", new ExternalToolRoute("/storage", "pages/StoragePage.vue", 50),
            "clinic", new ExternalToolRoute("/clinic/management", "pages/clinic/ClinicManagementPage.vue", 60));

    public AdminMenuResourcesView currentAdminMenuResources() {
        Optional<AuthPrincipal> principalOptional = securityContextService.currentOptional();
        if (principalOptional.isEmpty()) {
            return new AdminMenuResourcesView(List.of());
        }
        AuthPrincipal principal = principalOptional.get();
        if (principal.userType() != UserType.INTERNAL) {
            return new AdminMenuResourcesView(List.of());
        }
        Set<String> permissionCodes = permissionService.permissionCodesForCurrent();
        if (principal.admin()) {
            return new AdminMenuResourcesView(buildAdminResources(resolveGrantedPermissions(permissionCodes)));
        }
        return new AdminMenuResourcesView(buildInternalResources(principal.userId(), resolveGrantedPermissions(permissionCodes)));
    }

    private List<AdminMenuResourceView> buildExternalResources(List<Permission> grantedPermissions) {
        if (grantedPermissions.isEmpty()) {
            return List.of();
        }
        Set<Long> grantedPermissionIds = grantedPermissions.stream()
                .map(Permission::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (grantedPermissionIds.isEmpty()) {
            return List.of();
        }

        Set<Long> accessibleFeatureIds = normalFeaturePermissionRepository.findAll().stream()
                .filter(mapping -> mapping.getPermissionId() != null)
                .filter(mapping -> grantedPermissionIds.contains(mapping.getPermissionId()))
                .map(NormalFeaturePermission::getFeatureId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (accessibleFeatureIds.isEmpty()) {
            return List.of();
        }

        List<AdminMenuResourceView> resources = new ArrayList<>();
        normalFeatureRepository.findByIdIn(accessibleFeatureIds).stream()
                .filter(feature -> feature.getId() != null)
                .filter(feature -> Boolean.TRUE.equals(feature.getEnabled()))
                .map(this::toExternalToolResource)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(AdminMenuResourceView::orderNo)
                        .thenComparing(AdminMenuResourceView::code, Comparator.nullsLast(String::compareToIgnoreCase)))
                .forEach(resources::add);
        resources.addAll(buildPermissionResources(grantedPermissions));
        return resources;
    }

    private List<AdminMenuResourceView> buildAdminResources(List<Permission> grantedPermissions) {
        List<Menu> menus = menuRepository.findAll().stream().filter(this::menuVisibleAndEnabled)
                .sorted(menuComparator()).toList();
        List<Function> functions = functionRepository.findAll().stream().filter(this::functionEnabled)
                .collect(Collectors.toMap(Function::getId, value -> value, (left, right) -> left, LinkedHashMap::new))
                .values().stream().toList();
        Map<Long, Function> functionById = functions.stream()
                .collect(Collectors.toMap(Function::getId, value -> value, (left, right) -> left, LinkedHashMap::new));
        List<MenuFunction> menuFunctions = menuFunctionRepository.findAll().stream()
                .filter(menuFunction -> Boolean.TRUE.equals(menuFunction.getVisible()))
                .filter(menuFunction -> functionById.containsKey(menuFunction.getFunctionId()))
                .sorted(menuFunctionComparator()).toList();

        List<AdminMenuResourceView> resources = new ArrayList<>();
        menus.forEach(menu -> resources.add(toMenuResource(menu)));
        menuFunctions.forEach(menuFunction -> {
            Function function = functionById.get(menuFunction.getFunctionId());
            if (function != null) {
                resources.add(toFunctionResource(menuFunction, function));
            }
        });
        resources.addAll(buildPermissionResources(grantedPermissions));
        return resources;
    }

    private List<AdminMenuResourceView> buildInternalResources(Long userId, List<Permission> grantedPermissions) {
        if (grantedPermissions.isEmpty()) {
            return List.of();
        }
        Map<Long, Permission> permissionById = grantedPermissions.stream()
                .filter(permission -> permission.getId() != null)
                .collect(Collectors.toMap(Permission::getId, value -> value, (left, right) -> left,
                        LinkedHashMap::new));
        Set<Long> grantedPermissionIds = permissionById.keySet();
        if (grantedPermissionIds.isEmpty()) {
            return buildPermissionResources(grantedPermissions);
        }

        Map<Long, Function> functionById = functionRepository.findAll().stream().filter(this::functionEnabled)
                .filter(function -> function.getId() != null)
                .collect(Collectors.toMap(Function::getId, value -> value, (left, right) -> left, LinkedHashMap::new));

        Set<Long> directFunctionIds = functionPermissionRepository.findAll().stream()
                .filter(mapping -> mapping.getPermissionId() != null && grantedPermissionIds.contains(mapping.getPermissionId()))
                .map(FunctionPermission::getFunctionId).filter(Objects::nonNull)
                .filter(functionById::containsKey).collect(Collectors.toCollection(LinkedHashSet::new));

        Map<Long, MenuFunction> menuFunctionById = menuFunctionRepository.findAll().stream()
                .filter(mapping -> mapping.getId() != null)
                .filter(mapping -> Boolean.TRUE.equals(mapping.getVisible()))
                .filter(mapping -> functionById.containsKey(mapping.getFunctionId()))
                .collect(Collectors.toMap(MenuFunction::getId, value -> value, (left, right) -> left,
                        LinkedHashMap::new));

        Set<Long> includedMenuFunctionIds = menuFunctionById.values().stream()
                .filter(mapping -> directFunctionIds.contains(mapping.getFunctionId())).map(MenuFunction::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        includeMenuFunctionAncestors(includedMenuFunctionIds, menuFunctionById);

        Map<Long, Menu> menuById = menuRepository.findAll().stream().filter(this::menuVisibleAndEnabled)
                .filter(menu -> menu.getId() != null)
                .collect(Collectors.toMap(Menu::getId, value -> value, (left, right) -> left, LinkedHashMap::new));
        Set<Long> includedMenuIds = roleMenuRepository.findByRoleIdIn(userRoleRepository.findByUserId(userId)
                        .stream().map(UserRole::getRoleId).filter(Objects::nonNull).distinct().toList())
                .stream().map(RoleMenu::getMenuId).filter(Objects::nonNull).filter(menuById::containsKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (includedMenuIds.isEmpty()) {
            includedMenuIds = includedMenuFunctionIds.stream().map(menuFunctionById::get).filter(Objects::nonNull)
                    .map(MenuFunction::getMenuId).filter(Objects::nonNull).filter(menuById::containsKey)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        includeMenuAncestors(includedMenuIds, menuById);

        List<AdminMenuResourceView> resources = new ArrayList<>();
        includedMenuIds.stream().map(menuById::get).filter(Objects::nonNull).sorted(menuComparator())
                .map(this::toMenuResource).forEach(resources::add);
        includedMenuFunctionIds.stream().map(menuFunctionById::get).filter(Objects::nonNull).sorted(menuFunctionComparator())
                .forEach(menuFunction -> {
                    Function function = functionById.get(menuFunction.getFunctionId());
                    if (function != null) {
                        resources.add(toFunctionResource(menuFunction, function));
                    }
                });
        resources.addAll(buildPermissionResources(grantedPermissions));
        return resources;
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

    private List<Permission> resolveGrantedPermissions(Set<String> permissionCodes) {
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            return List.of();
        }
        Set<String> normalized = permissionCodes.stream().filter(Objects::nonNull).map(String::trim)
                .filter(code -> !code.isBlank()).collect(Collectors.toCollection(LinkedHashSet::new));
        if (normalized.isEmpty()) {
            return List.of();
        }
        return permissionRepository.findAll().stream().filter(permission -> Boolean.TRUE.equals(permission.getEnabled()))
                .filter(permission -> normalized.contains(permission.getCode()))
                .sorted(permissionComparator()).toList();
    }

    private List<AdminMenuResourceView> buildPermissionResources(Collection<Permission> permissions) {
        return permissions.stream().filter(permission -> permission.getId() != null).sorted(permissionComparator())
                .map(permission -> new AdminMenuResourceView("perm:" + permission.getId(), null,
                        blankToDefault(permission.getName(), permission.getCode()), null, permission.getDescription(),
                        permission.getCode(), "DATA", "NONE", "NONE", "", "",
                        permission.getId().intValue(), resolveLevel(permission.getSystemBuiltin()),
                        Boolean.TRUE.equals(permission.getEnabled()), false))
                .toList();
    }

    private AdminMenuResourceView toMenuResource(Menu menu) {
        return new AdminMenuResourceView("menu:" + menu.getId(),
                menu.getParentId() == null ? null : "menu:" + menu.getParentId(), menu.getName(),
                resolveMenuIcon(menu.getIcon()),
                menu.getRemark(), menu.getCode(), "MENU", resolveScope(menu.getPath(), menu.resolveMenuType()), resolveOpenMode(menu),
                blankToEmpty(menu.getPath()), blankToEmpty(menu.getComponent()),
                menu.getSortNo() == null ? 0 : menu.getSortNo(), resolveLevel(menu.getSystemBuiltin()),
                Boolean.TRUE.equals(menu.getEnabled()), false);
    }

    private String resolveMenuIcon(String iconCode) {
        return staticAssetQueryService.resolveFileIdByCode(iconCode).orElse(null);
    }

    private AdminMenuResourceView toFunctionResource(MenuFunction menuFunction, Function function) {
        return new AdminMenuResourceView("mf:" + menuFunction.getId(),
                menuFunction.getParentId() == null ? "menu:" + menuFunction.getMenuId() : "mf:" + menuFunction.getParentId(),
                function.getName(), null, function.getDescription(), function.getCode(), resolveType(function),
                "NONE", "NONE", "", "", menuFunction.getSortNo() == null ? 0 : menuFunction.getSortNo(),
                resolveLevel(menuFunction.getSystemBuiltin()), true, false);
    }

    private boolean menuVisibleAndEnabled(Menu menu) {
        return Boolean.TRUE.equals(menu.getEnabled()) && Boolean.TRUE.equals(menu.getVisible());
    }

    private boolean functionEnabled(Function function) {
        return Boolean.TRUE.equals(function.getEnabled());
    }

    private AdminMenuResourceView toExternalToolResource(NormalFeature feature) {
        if (feature.getCode() == null || feature.getCode().isBlank()) {
            return null;
        }
        ExternalToolRoute route = EXTERNAL_TOOL_ROUTE_MAP.get(feature.getCode().trim().toLowerCase());
        if (route == null) {
            return null;
        }
        String id = "nf:" + feature.getId();
        return new AdminMenuResourceView(id, null, feature.getName(), null, feature.getDescription(),
                feature.getCode(), "MENU", "TOOL", "PAGE", route.path(), route.component(), route.orderNo(),
                resolveLevel(feature.getSystemBuiltin()), true, false);
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

    private Comparator<Permission> permissionComparator() {
        return Comparator.comparing(Permission::getCode, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(Permission::getId, Comparator.nullsLast(Long::compareTo));
    }

    private String resolveScope(String path, MenuType menuType) {
        if (menuType == MenuType.DIRECTORY) {
            return "NONE";
        }
        if (path == null || path.isBlank()) {
            return "NONE";
        }
        if (path.startsWith("/admin")) {
            return "SETTING";
        }
        if ("/profile".equals(path)) {
            return "INFO";
        }
        return "TOOL";
    }

    private String resolveOpenMode(Menu menu) {
        if (menu.resolveMenuType() == MenuType.DIRECTORY) {
            return "NONE";
        }
        if ((menu.getPath() == null || menu.getPath().isBlank()) && (menu.getComponent() == null || menu.getComponent().isBlank())) {
            return "NONE";
        }
        return "PAGE";
    }

    private String resolveType(Function function) {
        return function.getFunctionType() == FunctionType.BUTTON ? "BUTTON" : "FEATURE";
    }

    private String resolveLevel(Boolean systemBuiltin) {
        return Boolean.TRUE.equals(systemBuiltin) ? "SYSTEM" : "CUSTOM";
    }

    private String blankToDefault(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return blankToEmpty(fallback);
        }
        return value;
    }

    private String blankToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record ExternalToolRoute(
            String path,
            String component,
            int orderNo
    ) {
    }
}
