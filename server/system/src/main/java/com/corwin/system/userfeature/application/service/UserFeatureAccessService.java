package com.corwin.system.userfeature.application.service;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.framework.error.BizException;
import com.corwin.system.resource.domain.model.Permission;
import com.corwin.system.resource.domain.repo.PermissionRepository;
import com.corwin.system.userfeature.domain.model.*;
import com.corwin.system.userfeature.domain.repo.*;
import com.corwin.system.webuser.domain.model.WebUser;
import com.corwin.system.webuser.domain.repo.WebUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Corwin 2026/6/14
 */
@Service
@RequiredArgsConstructor
public class UserFeatureAccessService {

    private final ProductApplicationRepository productApplicationRepository;
    private final ProductFeatureRepository productFeatureRepository;
    private final ProductFeaturePermissionBindingRepository productFeaturePermissionBindingRepository;
    private final UserApplicationPackageRepository userApplicationPackageRepository;
    private final UserApplicationPackageMemberRepository userApplicationPackageMemberRepository;
    private final UserPackageApplicationAccessRepository userPackageApplicationAccessRepository;
    private final UserPackageFeatureAccessRepository userPackageFeatureAccessRepository;
    private final UserApplicationOverrideRepository userApplicationOverrideRepository;
    private final UserFeatureOverrideRepository userFeatureOverrideRepository;
    private final PermissionRepository permissionRepository;
    private final WebUserRepository webUserRepository;

    public List<ProductApplication> accessibleApplicationsForUser(Long userId) {
        requireExternalUser(userId);
        UserFeatureAccessSnapshot snapshot = buildSnapshot(userId);
        if (snapshot.accessibleApplicationIds().isEmpty()) {
            return List.of();
        }
        return snapshot.applicationsInOrder().stream().filter(application -> application.getId() != null)
                .filter(application -> snapshot.accessibleApplicationIds().contains(application.getId())).toList();
    }

    public List<Long> effectiveFeatureIdsForUser(Long userId) {
        requireExternalUser(userId);
        return buildSnapshot(userId).effectiveFeatureIds();
    }

    public Set<String> permissionCodesForExternalUser(Long userId) {
        requireExternalUser(userId);
        UserFeatureAccessSnapshot snapshot = buildSnapshot(userId);
        if (snapshot.effectiveFeatureIds().isEmpty()) {
            return Set.of();
        }
        List<ProductFeaturePermissionBinding> bindings = productFeaturePermissionBindingRepository.findByFeatureIdIn(
                snapshot.effectiveFeatureIds());
        if (bindings.isEmpty()) {
            return Set.of();
        }

        LinkedHashSet<Long> permissionIds = bindings.stream().map(ProductFeaturePermissionBinding::getPermissionId)
                .filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, Permission> permissionById = permissionRepository.findAllById(permissionIds).stream()
                .filter(permission -> permission.getUserScope() == UserType.USER).collect(
                        Collectors.toMap(Permission::getId, permission -> permission, (left, right) -> right,
                                LinkedHashMap::new));

        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (ProductFeaturePermissionBinding binding : bindings) {
            Permission permission = permissionById.get(binding.getPermissionId());
            if (permission == null || permission.getCode() == null || permission.getCode().isBlank()) {
                continue;
            }
            result.add(permission.getCode().trim());
        }
        return Set.copyOf(result);
    }

    public void assignDefaultPackagesToUser(Long userId) {
        requireExternalUser(userId);
        List<UserApplicationPackage> defaults = userApplicationPackageRepository.findByDefaultPackageTrueAndEnabledTrue();
        if (defaults.isEmpty()) {
            return;
        }
        Set<Long> existingPackageIds = userApplicationPackageMemberRepository.findByUserId(userId).stream()
                .map(UserApplicationPackageMember::getPackageId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<UserApplicationPackageMember> next = defaults.stream().map(UserApplicationPackage::getId)
                .filter(Objects::nonNull).filter(packageId -> !existingPackageIds.contains(packageId))
                .map(packageId -> new UserApplicationPackageMember(packageId, userId, 0L)).toList();
        if (!next.isEmpty()) {
            userApplicationPackageMemberRepository.saveAll(next);
        }
    }

    private UserFeatureAccessSnapshot buildSnapshot(Long userId) {
        List<ProductApplication> applications = productApplicationRepository.findAll().stream()
                .filter(application -> Boolean.TRUE.equals(application.getEnabled()))
                .sorted(Comparator.comparing(ProductApplication::getDisplayOrder,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ProductApplication::getId, Comparator.nullsLast(Long::compareTo))).toList();
        Map<Long, ProductApplication> applicationById = applications.stream()
                .filter(application -> application.getId() != null).collect(
                        Collectors.toMap(ProductApplication::getId, application -> application, (left, right) -> left,
                                LinkedHashMap::new));

        List<ProductFeature> features = productFeatureRepository.findAll().stream()
                .filter(feature -> feature.getId() != null).filter(feature -> feature.getApplicationId() != null)
                .filter(feature -> Boolean.TRUE.equals(feature.getEnabled()))
                .filter(feature -> applicationById.containsKey(feature.getApplicationId()))
                .sorted(Comparator.comparing(ProductFeature::getApplicationId, Comparator.nullsLast(Long::compareTo))
                        .thenComparing(ProductFeature::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ProductFeature::getId, Comparator.nullsLast(Long::compareTo))).toList();

        Set<Long> enabledApplicationIds = applicationById.keySet();
        Set<Long> enabledFeatureIds = features.stream().map(ProductFeature::getId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<Long> packageIds = resolveEnabledPackageIds(userId);
        Map<Long, UserApplicationOverride> applicationOverrideByApplicationId = userApplicationOverrideRepository.findByUserId(
                        userId).stream().filter(override -> override.getApplicationId() != null)
                .filter(override -> override.getOverrideType() != null)
                .filter(override -> enabledApplicationIds.contains(override.getApplicationId())).collect(
                        Collectors.toMap(UserApplicationOverride::getApplicationId, override -> override,
                                (left, right) -> right, LinkedHashMap::new));
        Map<Long, Map<Long, UserFeatureOverride>> featureOverridesByApplicationId = userFeatureOverrideRepository.findByUserId(
                        userId).stream()
                .filter(override -> override.getApplicationId() != null && override.getFeatureId() != null)
                .filter(override -> override.getOverrideType() != null)
                .filter(override -> enabledApplicationIds.contains(override.getApplicationId()))
                .filter(override -> enabledFeatureIds.contains(override.getFeatureId())).collect(
                        Collectors.groupingBy(UserFeatureOverride::getApplicationId, LinkedHashMap::new,
                                Collectors.toMap(UserFeatureOverride::getFeatureId, override -> override,
                                        (left, right) -> right, LinkedHashMap::new)));

        List<UserPackageApplicationAccess> packageApplicationAccesses = packageIds.isEmpty() ? List.of() : userPackageApplicationAccessRepository.findByPackageIdIn(
                        packageIds).stream().filter(access -> access.getApplicationId() != null)
                .filter(access -> enabledApplicationIds.contains(access.getApplicationId())).toList();
        Map<Long, List<UserPackageApplicationAccess>> packageAccessByApplicationId = packageApplicationAccesses.stream()
                .collect(Collectors.groupingBy(UserPackageApplicationAccess::getApplicationId, LinkedHashMap::new,
                        Collectors.toList()));
        List<UserPackageFeatureAccess> packageFeatureAccesses = packageIds.isEmpty() ? List.of() : userPackageFeatureAccessRepository.findByPackageIdIn(
                        packageIds).stream()
                .filter(access -> access.getApplicationId() != null && access.getFeatureId() != null)
                .filter(access -> enabledApplicationIds.contains(access.getApplicationId()))
                .filter(access -> enabledFeatureIds.contains(access.getFeatureId())).toList();
        Map<Long, Set<Long>> packageFeatureIdsByApplicationId = packageFeatureAccesses.stream().collect(
                Collectors.groupingBy(UserPackageFeatureAccess::getApplicationId, LinkedHashMap::new,
                        Collectors.mapping(UserPackageFeatureAccess::getFeatureId,
                                Collectors.toCollection(LinkedHashSet::new))));

        LinkedHashSet<Long> accessibleApplicationIds = new LinkedHashSet<>();
        LinkedHashSet<Long> effectiveFeatureIds = new LinkedHashSet<>();
        for (ProductApplication application : applications) {
            Long applicationId = application.getId();
            if (applicationId == null) {
                continue;
            }
            UserApplicationOverride applicationOverride = applicationOverrideByApplicationId.get(applicationId);
            List<UserPackageApplicationAccess> packageAccesses = packageAccessByApplicationId.getOrDefault(
                    applicationId, List.of());
            boolean applicationAccessible = resolveApplicationAccessible(applicationOverride, packageAccesses);
            if (!applicationAccessible) {
                continue;
            }
            accessibleApplicationIds.add(applicationId);

            Set<Long> packageFeatureIds = packageFeatureIdsByApplicationId.getOrDefault(applicationId, Set.of());
            Map<Long, UserFeatureOverride> featureOverrideByFeatureId = featureOverridesByApplicationId.getOrDefault(
                    applicationId, Map.of());
            for (ProductFeature feature : features) {
                if (!applicationId.equals(feature.getApplicationId()) || feature.getId() == null) {
                    continue;
                }
                if (featureAccessible(feature.getId(), applicationOverride, packageAccesses, packageFeatureIds,
                        featureOverrideByFeatureId.get(feature.getId()))) {
                    effectiveFeatureIds.add(feature.getId());
                }
            }
        }
        return new UserFeatureAccessSnapshot(applications, List.copyOf(effectiveFeatureIds),
                Set.copyOf(accessibleApplicationIds));
    }

    private boolean resolveApplicationAccessible(UserApplicationOverride applicationOverride,
            List<UserPackageApplicationAccess> packageAccesses) {
        if (applicationOverride != null) {
            if (applicationOverride.getOverrideType() == UserAccessOverrideType.DISABLE) {
                return false;
            }
            if (applicationOverride.getOverrideType() == UserAccessOverrideType.ENABLE) {
                return true;
            }
        }
        return !packageAccesses.isEmpty();
    }

    private boolean featureAccessible(Long featureId, UserApplicationOverride applicationOverride,
            List<UserPackageApplicationAccess> packageAccesses, Set<Long> packageFeatureIds,
            UserFeatureOverride featureOverride) {
        if (applicationOverride != null && applicationOverride.getOverrideType() == UserAccessOverrideType.DISABLE) {
            return false;
        }
        if (featureOverride != null && featureOverride.getOverrideType() == UserAccessOverrideType.DISABLE) {
            return false;
        }
        if (applicationOverride != null && applicationOverride.getOverrideType() == UserAccessOverrideType.ENABLE) {
            if (applicationOverride.getFeatureAccessScope() == ApplicationFeatureAccessScope.FULL) {
                return true;
            }
            return featureOverride != null && featureOverride.getOverrideType() == UserAccessOverrideType.ENABLE;
        }

        boolean packageGranted = packageAccesses.stream().anyMatch(
                access -> access.getFeatureAccessScope() == ApplicationFeatureAccessScope.FULL) || packageFeatureIds.contains(
                featureId);
        if (packageGranted) {
            return true;
        }
        return featureOverride != null && featureOverride.getOverrideType() == UserAccessOverrideType.ENABLE && !packageAccesses.isEmpty();
    }

    private List<Long> resolveEnabledPackageIds(Long userId) {
        List<Long> packageIds = userApplicationPackageMemberRepository.findByUserId(userId).stream()
                .map(UserApplicationPackageMember::getPackageId).filter(Objects::nonNull).distinct().toList();
        if (packageIds.isEmpty()) {
            return List.of();
        }
        return userApplicationPackageRepository.findByIdIn(packageIds).stream()
                .filter(pkg -> Boolean.TRUE.equals(pkg.getEnabled())).map(UserApplicationPackage::getId)
                .filter(Objects::nonNull).toList();
    }

    private void requireExternalUser(Long userId) {
        BizAssert.notNull(userId, BaseError.INVALID_PARAMETER);
        WebUser user = webUserRepository.findById(userId).orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
        BizAssert.notNull(user.getId(), BaseError.INVALID_PARAMETER);
    }

    private record UserFeatureAccessSnapshot(
            List<ProductApplication> applicationsInOrder,
            List<Long> effectiveFeatureIds,
            Set<Long> accessibleApplicationIds
    ) {
    }
}
