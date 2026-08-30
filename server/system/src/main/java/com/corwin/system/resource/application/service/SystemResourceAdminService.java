package com.corwin.system.resource.application.service;

import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.framework.error.BizException;
import com.corwin.system.resource.application.view.SystemResourceDetailView;
import com.corwin.system.resource.application.view.SystemResourcePermissionSelectionView;
import com.corwin.system.resource.application.view.SystemResourceTreeItemView;
import com.corwin.system.resource.domain.model.Permission;
import com.corwin.system.resource.domain.model.Resource;
import com.corwin.system.resource.domain.model.ResourcePermission;
import com.corwin.system.resource.domain.model.ResourceType;
import com.corwin.system.resource.domain.repo.ResourcePermissionRepository;
import com.corwin.system.resource.domain.repo.ResourceRepository;
import com.corwin.system.role.domain.repo.RoleResourceRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for managing the system resource tree (directories, menus, functions,
 * buttons) and their permission bindings.
 *
 * <p>Provides use-case orchestration for CRUD operations on resources, tree building, permission
 * assignment, and subtree deletion with integrity checks.
 *
 * @author Corwin 2026/6/29
 */
@Service
@RequiredArgsConstructor
public class SystemResourceAdminService {

  private final ResourceRepository resourceRepository;
  private final ResourcePermissionRepository resourcePermissionRepository;
  private final PermissionService permissionService;
  private final RoleResourceRepository roleResourceRepository;
  private final ApiPermissionCache apiPermissionCache;

  /**
   * Returns the full system resource tree with permission binding information.
   *
   * @return the list of root-level tree items with children recursively attached
   */
  public List<SystemResourceTreeItemView> tree() {
    List<Resource> resources = sortedResources(resourceRepository.findAll());
    Map<Long, List<Long>> permissionIdsByResourceId = permissionIdsByResourceId(resources);
    Map<Long, List<Resource>> childrenByParentId = childrenByParentId(resources);
    return childrenByParentId.getOrDefault(null, List.of()).stream()
        .map(resource -> buildTree(resource, childrenByParentId, permissionIdsByResourceId))
        .toList();
  }

  /**
   * Retrieves the detail of a specific resource by its ID.
   *
   * @param id the resource ID
   * @return the resource detail view
   */
  public SystemResourceDetailView get(Long id) {
    Resource resource = requireResource(id);
    return toDetailView(
        resource,
        permissionIdsByResourceId(List.of(resource)).getOrDefault(resource.getId(), List.of()));
  }

  /**
   * Returns the permission IDs currently bound to a resource.
   *
   * @param resourceId the resource ID
   * @return the permission selection view
   */
  public SystemResourcePermissionSelectionView permissions(Long resourceId) {
    Resource resource = requireResource(resourceId);
    BizAssert.state(resource.canBindPermission(), BaseError.INVALID_PARAMETER);
    List<Long> permissionIds =
        permissionIdsByResourceId(List.of(resource)).getOrDefault(resource.getId(), List.of());
    return new SystemResourcePermissionSelectionView(permissionIds);
  }

  /**
   * Creates a new system resource with validation.
   *
   * @param parentId parent resource ID, null for root resources
   * @param code unique resource code
   * @param name resource display name
   * @param resourceType resource type
   * @param path front-end route path
   * @param component front-end component path
   * @param icon icon identifier
   * @param sortNo sort order
   * @param visible whether the resource is visible
   * @param enabled whether the resource is enabled
   * @param defaultEntry whether this is the default entry
   * @param systemBuiltin whether system-defined
   * @param remark optional remark
   * @return the created resource detail view
   */
  @Transactional
  public SystemResourceDetailView create(
      Long parentId,
      String code,
      String name,
      ResourceType resourceType,
      String path,
      String component,
      String icon,
      Integer sortNo,
      Boolean visible,
      Boolean enabled,
      Boolean defaultEntry,
      Boolean systemBuiltin,
      String remark) {
    Resource parent = parentId == null ? null : requireResource(parentId);
    validateNewResource(parent, code, name, resourceType, sortNo, path, component);
    Resource resource =
        new Resource(
            code,
            parentId,
            name,
            resourceType,
            normalizePath(resourceType, path),
            normalizeComponent(resourceType, component),
            normalizeIcon(resourceType, icon),
            sortNo,
            defaultVisible(visible),
            defaultEntryAllowed(resourceType, defaultEntry),
            systemBuiltin,
            remark);
    applyStatus(resource, visible, enabled, defaultEntry, systemBuiltin);
    return toDetailView(resourceRepository.save(resource), List.of());
  }

  /**
   * Updates an existing system resource with full validation.
   *
   * @param id the resource ID
   * @param parentId new parent resource ID
   * @param code new resource code
   * @param name new resource name
   * @param resourceType new resource type
   * @param path new front-end route path
   * @param component new front-end component path
   * @param icon new icon identifier
   * @param sortNo new sort order
   * @param visible new visibility
   * @param enabled new enabled status
   * @param defaultEntry new default entry status
   * @param systemBuiltin new system-built status
   * @param remark new remark
   * @return the updated resource detail view
   */
  @Transactional
  public SystemResourceDetailView update(
      Long id,
      Long parentId,
      String code,
      String name,
      ResourceType resourceType,
      String path,
      String component,
      String icon,
      Integer sortNo,
      Boolean visible,
      Boolean enabled,
      Boolean defaultEntry,
      Boolean systemBuiltin,
      String remark) {
    Resource resource = requireResource(id);
    Resource parent = parentId == null ? null : requireResource(parentId);
    List<Resource> allResources = resourceRepository.findAll();
    validateExistingResource(
        resource, parent, code, name, resourceType, sortNo, path, component, allResources);

    resource.changeCode(code);
    resource.changeType(resourceType);
    resource.moveTo(parentId);
    resource.modifyBasicInfo(name, normalizeIcon(resourceType, icon), remark);
    resource.modifyRoute(
        normalizePath(resourceType, path), normalizeComponent(resourceType, component));
    resource.changeSortNo(sortNo);
    applyStatus(resource, visible, enabled, defaultEntry, systemBuiltin);
    Resource saved = resourceRepository.save(resource);
    if (!resourceType.canBindPermission()) {
      resourcePermissionRepository.deleteByResourceIdIn(List.of(id));
    }
    List<Long> permissionIds =
        permissionIdsByResourceId(List.of(saved)).getOrDefault(saved.getId(), List.of());
    return toDetailView(saved, permissionIds);
  }

  /**
   * Deletes a resource and its entire subtree.
   *
   * <p>Also removes associated role-resource and resource-permission bindings. System-built
   * resources cannot be deleted.
   *
   * @param id the root resource ID of the subtree to delete
   * @return true if deletion was successful
   */
  @Transactional
  public boolean delete(Long id) {
    Resource target = requireResource(id);
    List<Resource> allResources = resourceRepository.findAll();
    LinkedHashSet<Long> subtreeIds = collectSubtreeIds(id, allResources);
    List<Resource> subtreeResources =
        allResources.stream().filter(resource -> subtreeIds.contains(resource.getId())).toList();
    BizAssert.state(
        subtreeResources.stream()
            .noneMatch(resource -> Boolean.TRUE.equals(resource.getSystemBuiltin())),
        BaseError.FORBIDDEN);

    roleResourceRepository.deleteByResourceIdIn(subtreeIds);
    resourcePermissionRepository.deleteByResourceIdIn(subtreeIds);
    Map<Long, Resource> resourceById =
        allResources.stream()
            .filter(resource -> resource.getId() != null)
            .collect(
                Collectors.toMap(
                    Resource::getId,
                    Function.identity(),
                    (left, right) -> left,
                    LinkedHashMap::new));
    subtreeResources.stream()
        .sorted(
            Comparator.<Resource, Integer>comparing(resource -> depthOf(resource, resourceById))
                .reversed())
        .forEach(resource -> resourceRepository.deleteById(resource.getId()));
    apiPermissionCache.clearAll();
    return true;
  }

  /**
   * Updates the permission bindings for a resource, replacing existing bindings.
   *
   * @param resourceId the resource ID (must be a BUTTON-type resource)
   * @param permissionIds the new list of permission IDs to bind
   * @return true if the update was successful
   */
  @Transactional
  public boolean updatePermissions(Long resourceId, List<Long> permissionIds) {
    Resource resource = requireResource(resourceId);
    BizAssert.state(resource.canBindPermission(), BaseError.INVALID_PARAMETER);
    LinkedHashSet<Long> normalizedPermissionIds = normalizePermissionIds(permissionIds);
    resourcePermissionRepository.deleteByResourceIdIn(List.of(resourceId));
    if (!normalizedPermissionIds.isEmpty()) {
      ArrayList<ResourcePermission> bindings = new ArrayList<>();
      for (Long permissionId : normalizedPermissionIds) {
        bindings.add(new ResourcePermission(resourceId, permissionId, false));
      }
      resourcePermissionRepository.saveAll(bindings);
    }
    apiPermissionCache.clearAll();
    return true;
  }

  private SystemResourceTreeItemView buildTree(
      Resource resource,
      Map<Long, List<Resource>> childrenByParentId,
      Map<Long, List<Long>> permissionIdsByResourceId) {
    List<SystemResourceTreeItemView> children =
        childrenByParentId.getOrDefault(resource.getId(), List.of()).stream()
            .map(child -> buildTree(child, childrenByParentId, permissionIdsByResourceId))
            .toList();
    return new SystemResourceTreeItemView(
        resource.getId(),
        resource.getParentId(),
        resource.getCode(),
        resource.getName(),
        resource.getResourceType(),
        resource.getPath(),
        resource.getComponent(),
        resource.getIcon(),
        defaultSortNo(resource),
        Boolean.TRUE.equals(resource.getVisible()),
        Boolean.TRUE.equals(resource.getEnabled()),
        Boolean.TRUE.equals(resource.getDefaultEntry()),
        Boolean.TRUE.equals(resource.getSystemBuiltin()),
        resource.getRemark(),
        permissionIdsByResourceId.getOrDefault(resource.getId(), List.of()),
        children);
  }

  private SystemResourceDetailView toDetailView(Resource resource, List<Long> permissionIds) {
    return new SystemResourceDetailView(
        resource.getId(),
        resource.getParentId(),
        resource.getCode(),
        resource.getName(),
        resource.getResourceType(),
        resource.getPath(),
        resource.getComponent(),
        resource.getIcon(),
        defaultSortNo(resource),
        Boolean.TRUE.equals(resource.getVisible()),
        Boolean.TRUE.equals(resource.getEnabled()),
        Boolean.TRUE.equals(resource.getDefaultEntry()),
        Boolean.TRUE.equals(resource.getSystemBuiltin()),
        resource.getRemark(),
        permissionIds);
  }

  private void validateNewResource(
      Resource parent,
      String code,
      String name,
      ResourceType resourceType,
      Integer sortNo,
      String path,
      String component) {
    BizAssert.notBlank(code, BaseError.MISSING_PARAMETER);
    BizAssert.notBlank(name, BaseError.MISSING_PARAMETER);
    BizAssert.notNull(resourceType, BaseError.MISSING_PARAMETER);
    BizAssert.notNull(sortNo, BaseError.MISSING_PARAMETER);
    BizAssert.state(!resourceRepository.existsByCode(code), BaseError.CONFLICT);
    validateParentRule(parent, resourceType);
    validateRouteRule(resourceType, path, component);
  }

  private void validateExistingResource(
      Resource resource,
      Resource parent,
      String code,
      String name,
      ResourceType resourceType,
      Integer sortNo,
      String path,
      String component,
      List<Resource> allResources) {
    BizAssert.notBlank(code, BaseError.MISSING_PARAMETER);
    BizAssert.notBlank(name, BaseError.MISSING_PARAMETER);
    BizAssert.notNull(resourceType, BaseError.MISSING_PARAMETER);
    BizAssert.notNull(sortNo, BaseError.MISSING_PARAMETER);
    resourceRepository
        .findByCode(code)
        .filter(found -> !Objects.equals(found.getId(), resource.getId()))
        .ifPresent(
            found -> {
              throw new BizException(BaseError.CONFLICT);
            });
    if (parent != null) {
      BizAssert.state(
          !Objects.equals(parent.getId(), resource.getId()), BaseError.INVALID_PARAMETER);
      BizAssert.state(
          !collectSubtreeIds(resource.getId(), allResources).contains(parent.getId()),
          BaseError.INVALID_PARAMETER);
    }
    validateParentRule(parent, resourceType);
    validateChildrenRule(resource, resourceType, allResources);
    validateRouteRule(resourceType, path, component);
  }

  private void validateParentRule(Resource parent, ResourceType resourceType) {
    if (parent == null) {
      BizAssert.state(
          resourceType == ResourceType.DIRECTORY || resourceType == ResourceType.MENU,
          BaseError.INVALID_PARAMETER);
      return;
    }
    BizAssert.state(parent.canHaveChild(resourceType), BaseError.INVALID_PARAMETER);
  }

  private void validateChildrenRule(
      Resource resource, ResourceType nextType, List<Resource> allResources) {
    List<Resource> children =
        allResources.stream()
            .filter(item -> Objects.equals(item.getParentId(), resource.getId()))
            .toList();
    if (children.isEmpty()) {
      return;
    }
    BizAssert.state(nextType.canHaveChildren(), BaseError.INVALID_PARAMETER);
    for (Resource child : children) {
      BizAssert.state(nextType.canHaveChild(child.getResourceType()), BaseError.INVALID_PARAMETER);
    }
  }

  private void validateRouteRule(ResourceType resourceType, String path, String component) {
    if (resourceType == ResourceType.MENU || resourceType == ResourceType.FUNCTION) {
      BizAssert.notBlank(path, BaseError.MISSING_PARAMETER);
      BizAssert.notBlank(component, BaseError.MISSING_PARAMETER);
    }
  }

  private LinkedHashSet<Long> normalizePermissionIds(List<Long> permissionIds) {
    LinkedHashSet<Long> allowedPermissionIds =
        permissionService.assignablePermissionsForInternal().stream()
            .map(Permission::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    LinkedHashSet<Long> normalized = new LinkedHashSet<>();
    if (permissionIds == null) {
      return normalized;
    }
    for (Long permissionId : permissionIds) {
      if (permissionId == null || permissionId <= 0) {
        continue;
      }
      BizAssert.state(allowedPermissionIds.contains(permissionId), BaseError.INVALID_PARAMETER);
      normalized.add(permissionId);
    }
    return normalized;
  }

  private Map<Long, List<Resource>> childrenByParentId(List<Resource> resources) {
    Map<Long, List<Resource>> map = new LinkedHashMap<>();
    Map<Long, Resource> resourceById =
        resources.stream()
            .filter(resource -> resource.getId() != null)
            .collect(
                Collectors.toMap(
                    Resource::getId,
                    Function.identity(),
                    (left, right) -> left,
                    LinkedHashMap::new));
    for (Resource resource : resources) {
      Long parentId = resource.getParentId();
      if (parentId != null && !resourceById.containsKey(parentId)) {
        parentId = null;
      }
      map.computeIfAbsent(parentId, key -> new ArrayList<>()).add(resource);
    }
    map.values().forEach(list -> list.sort(resourceComparator()));
    return map;
  }

  private Map<Long, List<Long>> permissionIdsByResourceId(Collection<Resource> resources) {
    LinkedHashSet<Long> resourceIds =
        resources.stream()
            .map(Resource::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    Map<Long, List<Long>> permissionIdsByResourceId = new LinkedHashMap<>();
    if (resourceIds.isEmpty()) {
      return permissionIdsByResourceId;
    }
    resourcePermissionRepository
        .findByResourceIdIn(resourceIds)
        .forEach(
            binding -> {
              permissionIdsByResourceId
                  .computeIfAbsent(binding.getResourceId(), key -> new ArrayList<>())
                  .add(binding.getPermissionId());
            });
    permissionIdsByResourceId.values().forEach(list -> list.sort(Long::compareTo));
    return permissionIdsByResourceId;
  }

  private LinkedHashSet<Long> collectSubtreeIds(Long rootId, List<Resource> allResources) {
    Map<Long, List<Long>> childrenByParentId = new LinkedHashMap<>();
    allResources.stream()
        .filter(resource -> resource.getId() != null)
        .forEach(
            resource ->
                childrenByParentId
                    .computeIfAbsent(resource.getParentId(), key -> new ArrayList<>())
                    .add(resource.getId()));
    LinkedHashSet<Long> ids = new LinkedHashSet<>();
    ArrayList<Long> queue = new ArrayList<>();
    queue.add(rootId);
    int index = 0;
    while (index < queue.size()) {
      Long current = queue.get(index++);
      if (current == null || !ids.add(current)) {
        continue;
      }
      queue.addAll(childrenByParentId.getOrDefault(current, List.of()));
    }
    return ids;
  }

  private int depthOf(Resource resource, Map<Long, Resource> resourceById) {
    int depth = 0;
    Long current = resource.getParentId();
    while (current != null && resourceById.containsKey(current)) {
      depth += 1;
      current = resourceById.get(current).getParentId();
    }
    return depth;
  }

  private List<Resource> sortedResources(List<Resource> resources) {
    return resources.stream().sorted(resourceComparator()).toList();
  }

  private Comparator<Resource> resourceComparator() {
    return Comparator.comparing(Resource::getSortNo, Comparator.nullsLast(Integer::compareTo))
        .thenComparing(Resource::getId, Comparator.nullsLast(Long::compareTo));
  }

  private Resource requireResource(Long id) {
    BizAssert.notNull(id, BaseError.MISSING_PARAMETER);
    return resourceRepository.findById(id).orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
  }

  private Integer defaultSortNo(Resource resource) {
    return resource.getSortNo() == null ? 0 : resource.getSortNo();
  }

  private boolean defaultVisible(Boolean visible) {
    return visible == null || visible;
  }

  private boolean defaultEntryAllowed(ResourceType resourceType, Boolean defaultEntry) {
    if (resourceType != ResourceType.MENU && resourceType != ResourceType.FUNCTION) {
      return false;
    }
    return Boolean.TRUE.equals(defaultEntry);
  }

  private String normalizePath(ResourceType resourceType, String path) {
    if (resourceType == ResourceType.MENU || resourceType == ResourceType.FUNCTION) {
      return path;
    }
    return null;
  }

  private String normalizeComponent(ResourceType resourceType, String component) {
    if (resourceType == ResourceType.MENU || resourceType == ResourceType.FUNCTION) {
      return component;
    }
    return null;
  }

  private String normalizeIcon(ResourceType resourceType, String icon) {
    if (resourceType == ResourceType.DIRECTORY || resourceType == ResourceType.MENU) {
      return icon;
    }
    return null;
  }

  private void applyStatus(
      Resource resource,
      Boolean visible,
      Boolean enabled,
      Boolean defaultEntry,
      Boolean systemBuiltin) {
    if (Boolean.FALSE.equals(visible)) {
      resource.hide();
    } else {
      resource.show();
    }
    if (Boolean.FALSE.equals(enabled)) {
      resource.disable();
    } else {
      resource.enable();
    }
    if (defaultEntryAllowed(resource.getResourceType(), defaultEntry)) {
      resource.markDefaultEntry();
    } else {
      resource.cancelDefaultEntry();
    }
    if (Boolean.TRUE.equals(systemBuiltin)) {
      resource.markSystemBuiltin();
    } else {
      resource.cancelSystemBuiltin();
    }
  }
}
