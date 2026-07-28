package com.corwin.system.resource.application.service;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.web.auth.AuthPrincipal;
import com.corwin.system.auth.published.SecurityContextService;
import com.corwin.system.resource.application.view.AdminMenuResourceView;
import com.corwin.system.resource.application.view.AdminMenuResourcesView;
import com.corwin.system.resource.domain.model.Resource;
import com.corwin.system.resource.domain.model.ResourceType;
import com.corwin.system.resource.domain.repo.ResourceRepository;
import com.corwin.system.role.domain.model.RoleResource;
import com.corwin.system.role.domain.repo.RoleResourceRepository;
import com.corwin.system.user.domain.model.UserRole;
import com.corwin.system.user.domain.repo.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Corwin 2026/6/29
 */
@Service
@RequiredArgsConstructor
public class AdminMenuResourceService {

    private final SecurityContextService securityContextService;
    private final ResourceRepository resourceRepository;
    private final RoleResourceRepository roleResourceRepository;
    private final UserRoleRepository userRoleRepository;

    public AdminMenuResourcesView currentAdminMenuResources() {
        Optional<AuthPrincipal> principalOptional = securityContextService.currentOptional();
        if (principalOptional.isEmpty()) {
            return new AdminMenuResourcesView(List.of());
        }
        AuthPrincipal principal = principalOptional.get();
        if (principal.userType() != UserType.ADMIN) {
            return new AdminMenuResourcesView(List.of());
        }
        if (principal.admin()) {
            return new AdminMenuResourcesView(buildTree(buildAdminResources()));
        }
        return new AdminMenuResourcesView(buildTree(buildInternalResources(principal.userId())));
    }

    private List<AdminMenuResourceView> buildAdminResources() {
        return resourceRepository.findAll().stream().filter(this::resourceVisibleAndEnabled)
                .filter(resource -> resource.getId() != null).sorted(resourceComparator()).map(this::toResourceView)
                .toList();
    }

    private List<AdminMenuResourceView> buildInternalResources(Long userId) {
        List<Long> roleIds = userRoleRepository.findByUserId(userId).stream().map(UserRole::getRoleId)
                .filter(Objects::nonNull).distinct().toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Resource> resourceById = resourceRepository.findAll().stream().filter(this::resourceVisibleAndEnabled)
                .filter(resource -> resource.getId() != null)
                .collect(Collectors.toMap(Resource::getId, value -> value, (left, right) -> left, LinkedHashMap::new));

        LinkedHashSet<Long> includedIds = roleResourceRepository.findByRoleIdIn(roleIds).stream()
                .map(RoleResource::getResourceId).filter(Objects::nonNull).filter(resourceById::containsKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        includeAncestors(includedIds, resourceById);

        return includedIds.stream().map(resourceById::get).filter(Objects::nonNull).sorted(resourceComparator())
                .map(this::toResourceView).toList();
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
                .map(resource -> buildTreeNode(resource, childrenByParentId)).toList();
    }

    private AdminMenuResourceView buildTreeNode(AdminMenuResourceView resource,
            Map<String, List<AdminMenuResourceView>> childrenByParentId) {
        List<AdminMenuResourceView> children = childrenByParentId.getOrDefault(resource.id(), List.of()).stream()
                .map(child -> buildTreeNode(child, childrenByParentId)).toList();
        return new AdminMenuResourceView(resource.id(), resource.parentId(), resource.name(), resource.icon(),
                resource.code(), resource.type(), resource.url(), resource.loadTarget(), resource.orderNo(), children);
    }

    private void includeAncestors(Set<Long> includedIds, Map<Long, Resource> resourceById) {
        List<Long> queue = new ArrayList<>(includedIds);
        int index = 0;
        while (index < queue.size()) {
            Resource current = resourceById.get(queue.get(index++));
            if (current == null || current.getParentId() == null) {
                continue;
            }
            Resource parent = resourceById.get(current.getParentId());
            if (parent != null && includedIds.add(parent.getId())) {
                queue.add(parent.getId());
            }
        }
    }

    private AdminMenuResourceView toResourceView(Resource resource) {
        return new AdminMenuResourceView("resource:" + resource.getId(),
                resource.getParentId() == null ? null : "resource:" + resource.getParentId(), resource.getName(),
                resource.getIcon(), resource.getCode(), resource.getResourceType().name(), resolveUrl(resource),
                resolveLoadTarget(resource), resource.getSortNo() == null ? 0 : resource.getSortNo(), List.of());
    }

    private boolean resourceVisibleAndEnabled(Resource resource) {
        return Boolean.TRUE.equals(resource.getEnabled()) && Boolean.TRUE.equals(resource.getVisible());
    }

    private Comparator<Resource> resourceComparator() {
        return Comparator.comparing(Resource::getParentId, Comparator.nullsFirst(Long::compareTo))
                .thenComparing(Resource::getSortNo, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(Resource::getId, Comparator.nullsLast(Long::compareTo));
    }

    private String resolveUrl(Resource resource) {
        return resource.getResourceType() == ResourceType.BUTTON ? "NONE" : blankToEmpty(resource.getPath());
    }

    private String resolveLoadTarget(Resource resource) {
        return resource.getResourceType() == ResourceType.BUTTON ? "NONE" : blankToEmpty(resource.getComponent());
    }

    private String blankToEmpty(String value) {
        return value == null ? "" : value;
    }
}
