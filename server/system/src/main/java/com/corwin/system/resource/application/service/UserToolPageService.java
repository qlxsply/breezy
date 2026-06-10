package com.corwin.system.resource.application.service;

import com.corwin.framework.web.auth.AuthPrincipal;
import com.corwin.system.auth.published.SecurityContextService;
import com.corwin.system.normalfeature.domain.model.NormalFeature;
import com.corwin.system.normalfeature.domain.model.NormalFeaturePermission;
import com.corwin.system.normalfeature.domain.repo.NormalFeaturePermissionRepository;
import com.corwin.system.normalfeature.domain.repo.NormalFeatureRepository;
import com.corwin.system.resource.application.view.UserToolPageView;
import com.corwin.system.resource.application.view.UserToolsView;
import com.corwin.system.resource.domain.model.Permission;
import com.corwin.system.resource.domain.repo.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Corwin 2026/6/6
 */
@Service
@RequiredArgsConstructor
public class UserToolPageService {

    private final SecurityContextService securityContextService;
    private final PermissionService permissionService;
    private final PermissionRepository permissionRepository;
    private final NormalFeatureRepository normalFeatureRepository;
    private final NormalFeaturePermissionRepository normalFeaturePermissionRepository;

    private static final Map<String, ExternalToolRoute> EXTERNAL_TOOL_ROUTE_MAP = Map.of(
            "jsonfmt", new ExternalToolRoute("/jsonfmt", "pages/JsonFormatterPage.vue", 10),
            "reminder", new ExternalToolRoute("/todo", "pages/TodoBoardPage.vue", 20),
            "datasource", new ExternalToolRoute("/datasource", "pages/DataSourceAdminPage.vue", 30),
            "schemaforge", new ExternalToolRoute("/schemaforge", "pages/SchemaForgePage.vue", 40),
            "storage", new ExternalToolRoute("/storage", "pages/StoragePage.vue", 50),
            "clinic", new ExternalToolRoute("/clinic/management", "pages/clinic/ClinicManagementPage.vue", 60));

    public UserToolsView currentUserTools() {
        Optional<AuthPrincipal> principalOptional = securityContextService.currentOptional();
        if (principalOptional.isEmpty()) {
            return new UserToolsView(List.of(), List.of());
        }
        AuthPrincipal principal = principalOptional.get();
        Set<String> permissionCodes = permissionService.permissionCodesForCurrent();
        List<Permission> grantedPermissions = resolveGrantedPermissions(permissionCodes);
        return new UserToolsView(buildTools(grantedPermissions), grantedPermissions.stream()
                .map(Permission::getCode)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(code -> !code.isBlank())
                .toList());
    }

    private List<UserToolPageView> buildTools(List<Permission> grantedPermissions) {
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

        List<UserToolPageView> resources = new ArrayList<>();
        normalFeatureRepository.findByIdIn(accessibleFeatureIds).stream()
                .filter(feature -> feature.getId() != null)
                .filter(feature -> Boolean.TRUE.equals(feature.getEnabled()))
                .map(this::toExternalToolPage)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(UserToolPageView::sortNo)
                        .thenComparing(UserToolPageView::code, Comparator.nullsLast(String::compareToIgnoreCase)))
                .forEach(resources::add);
        return resources;
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
                .sorted(Comparator.comparing(Permission::getCode, Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(Permission::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    private UserToolPageView toExternalToolPage(NormalFeature feature) {
        if (feature.getCode() == null || feature.getCode().isBlank()) {
            return null;
        }
        ExternalToolRoute route = EXTERNAL_TOOL_ROUTE_MAP.get(feature.getCode().trim().toLowerCase());
        if (route == null) {
            return null;
        }
        return new UserToolPageView("nf:" + feature.getId(), feature.getName(), null, feature.getDescription(),
                feature.getCode(), route.path(), route.component(), route.orderNo(),
                Boolean.TRUE.equals(feature.getSystemBuiltin()) ? "SYSTEM" : "CUSTOM", true, false);
    }

    private record ExternalToolRoute(
            String path,
            String component,
            int orderNo
    ) {
    }
}
