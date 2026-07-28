package com.corwin.system.resource.application.service;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.web.auth.AuthPrincipal;
import com.corwin.system.auth.published.SecurityContextService;
import com.corwin.system.resource.application.view.UserToolPageView;
import com.corwin.system.resource.application.view.UserToolsView;
import com.corwin.system.userfeature.application.service.UserFeatureAccessService;
import com.corwin.system.userfeature.domain.model.ProductApplication;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author Corwin 2026/6/6
 */
@Service
@RequiredArgsConstructor
public class UserToolPageService {

    private final SecurityContextService securityContextService;
    private final PermissionService permissionService;
    private final UserFeatureAccessService userFeatureAccessService;

    public UserToolsView currentUserTools() {
        Optional<AuthPrincipal> principalOptional = securityContextService.currentOptional();
        if (principalOptional.isEmpty()) {
            return new UserToolsView(List.of(), List.of());
        }
        AuthPrincipal principal = principalOptional.get();
        if (principal.userType() != UserType.USER || principal.userId() == null) {
            return new UserToolsView(List.of(), List.of());
        }
        Set<String> permissionCodes = permissionService.permissionCodesForCurrent();
        List<String> grantedPermissionCodes = permissionCodes.stream().filter(Objects::nonNull).map(String::trim)
                .filter(code -> !code.isBlank()).sorted().toList();
        return new UserToolsView(buildTools(principal), grantedPermissionCodes);
    }

    private List<UserToolPageView> buildTools(AuthPrincipal principal) {
        if (principal.userId() == null) {
            return List.of();
        }
        return userFeatureAccessService.accessibleApplicationsForUser(principal.userId()).stream()
                .filter(application -> application.getId() != null)
                .filter(application -> application.getRoutePath() != null && !application.getRoutePath().isBlank())
                .filter(application -> application.getComponentPath() != null && !application.getComponentPath()
                        .isBlank()).map(this::toExternalToolPage).filter(Objects::nonNull)
                .sorted(Comparator.comparing(UserToolPageView::sortNo)
                        .thenComparing(UserToolPageView::code, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    private UserToolPageView toExternalToolPage(ProductApplication application) {
        return new UserToolPageView("app:" + application.getId(), application.getApplicationName(),
                application.getIcon(), application.getDescription(), application.getApplicationCode(),
                application.getRoutePath(), application.getComponentPath(),
                application.getDisplayOrder() == null ? 0 : application.getDisplayOrder(),
                Boolean.TRUE.equals(application.getSystemBuiltIn()) ? "SYSTEM" : "CUSTOM", true, false);
    }
}
