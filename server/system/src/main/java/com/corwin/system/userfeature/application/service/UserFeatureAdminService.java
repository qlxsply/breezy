package com.corwin.system.userfeature.application.service;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.framework.error.BizException;
import com.corwin.framework.web.ctx.CtxUtil;
import com.corwin.framework.web.sort.PageSpecSorts;
import com.corwin.system.resource.application.service.ApiPermissionCache;
import com.corwin.system.resource.domain.model.Permission;
import com.corwin.system.resource.domain.repo.PermissionRepository;
import com.corwin.system.userfeature.application.command.ApplicationOverrideCommand;
import com.corwin.system.userfeature.application.command.FeatureOverrideCommand;
import com.corwin.system.userfeature.application.command.SaveUserFeaturePackageCommand;
import com.corwin.system.userfeature.application.command.SaveUserFeatureUserManagementCommand;
import com.corwin.system.userfeature.application.view.*;
import com.corwin.system.userfeature.domain.model.*;
import com.corwin.system.userfeature.domain.repo.*;
import com.corwin.system.webuser.domain.model.WebUserIdentity;
import com.corwin.system.webuser.domain.model.WebUserIdentityType;
import com.corwin.system.webuser.domain.repo.WebUserIdentityRepository;
import com.corwin.system.webuser.domain.repo.WebUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Application service for user feature administration.
 *
 * <p>Provides use-case orchestration for managing product applications, user application packages,
 * package access bindings, and per-user access overrides in the admin console.</p>
 *
 * @author Corwin 2026/6/14
 */
@Service
@RequiredArgsConstructor
public class UserFeatureAdminService {

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
    private final WebUserIdentityRepository webUserIdentityRepository;
    private final UserFeatureAccessService userFeatureAccessService;
    private final ApiPermissionCache apiPermissionCache;

    /**
     * Returns all applications as a flat catalog view with features and permission counts.
     *
     * @return list of application views
     */
    public List<UserFeatureApplicationView> applications() {
        Map<Long, List<UserFeatureItemView>> featuresByApplicationId = featureViewsByApplicationId();
        Map<Long, Integer> permissionCountByApplicationId = permissionCountByApplicationId();
        return productApplicationRepository.findAll().stream().sorted(applicationComparator())
                .map(application -> new UserFeatureApplicationView(application.getId(),
                        application.getApplicationCode(), application.getApplicationName(),
                        application.getDescription(), application.getIcon(), application.getRoutePath(),
                        application.getComponentPath(), Boolean.TRUE.equals(application.getEnabled()),
                        featuresByApplicationId.getOrDefault(application.getId(), List.of()).size(),
                        permissionCountByApplicationId.getOrDefault(application.getId(), 0),
                        featuresByApplicationId.getOrDefault(application.getId(), List.of()))).toList();
    }

    /**
     * Paginates product applications with optional keyword and enabled filter.
     *
     * @param keyword optional keyword filter
     * @param enabled optional enabled status filter
     * @param spec    the page specification
     * @return paginated application views
     */
    public PageData<UserFeatureApplicationView> pageApplications(String keyword, Boolean enabled, PageSpec spec) {
        PageData<ProductApplication> page = productApplicationRepository.page(keyword, enabled,
                PageSpecSorts.apply(spec));
        return new PageData<>(page.pageNo(), page.pageSize(), page.numberOfElements(), page.totalPages(),
                page.totalElements(), page.elements().stream().map(item -> getApplication(item.getId())).toList());
    }

    /**
     * Gets a single application view by its ID.
     *
     * @param id the application ID
     * @return the application view
     */
    public UserFeatureApplicationView getApplication(Long id) {
        return applications().stream().filter(item -> Objects.equals(item.id(), id)).findFirst()
                .orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
    }

    /**
     * Enables or disables a product application.
     *
     * @param id      the application ID
     * @param enabled true to enable, false to disable
     * @return true on success
     */
    @Transactional
    public boolean updateApplicationStatus(Long id, boolean enabled) {
        ProductApplication application = productApplicationRepository.findById(id)
                .orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
        if (enabled) {
            application.enable(operatorId());
        } else {
            application.disable(operatorId());
        }
        productApplicationRepository.save(application);
        apiPermissionCache.clearAll();
        return true;
    }

    /**
     * Returns all user application packages with their full application/feature access details.
     *
     * @return list of package views
     */
    public List<UserFeaturePackageView> packages() {
        Map<Long, ProductApplication> applicationById = productApplicationRepository.findAll().stream()
                .filter(item -> item.getId() != null).collect(
                        Collectors.toMap(ProductApplication::getId, item -> item, (left, right) -> left,
                                LinkedHashMap::new));
        Map<Long, List<UserFeatureItemView>> featuresByApplicationId = featureViewsByApplicationId();
        Map<Long, List<UserPackageApplicationAccess>> accessesByPackageId = userApplicationPackageRepository.findAll()
                .stream().map(UserApplicationPackage::getId).filter(Objects::nonNull).collect(
                        Collectors.toMap(id -> id, id -> userPackageApplicationAccessRepository.findByPackageId(id),
                                (left, right) -> left, LinkedHashMap::new));
        Map<Long, List<UserPackageFeatureAccess>> featureAccessesByPackageId = userApplicationPackageRepository.findAll()
                .stream().map(UserApplicationPackage::getId).filter(Objects::nonNull).collect(
                        Collectors.toMap(id -> id, id -> userPackageFeatureAccessRepository.findByPackageId(id),
                                (left, right) -> left, LinkedHashMap::new));

        return userApplicationPackageRepository.findAll().stream().sorted(packageComparator())
                .map(pkg -> toPackageView(pkg, applicationById, featuresByApplicationId,
                        accessesByPackageId.getOrDefault(pkg.getId(), List.of()),
                        featureAccessesByPackageId.getOrDefault(pkg.getId(), List.of()))).toList();
    }

    /**
     * Paginates user application packages with optional keyword and enabled filter.
     *
     * @param keyword optional keyword filter
     * @param enabled optional enabled status filter
     * @param spec    the page specification
     * @return paginated package views
     */
    public PageData<UserFeaturePackageView> pagePackages(String keyword, Boolean enabled, PageSpec spec) {
        PageData<UserApplicationPackage> page = userApplicationPackageRepository.page(keyword, enabled,
                PageSpecSorts.apply(spec));
        return new PageData<>(page.pageNo(), page.pageSize(), page.numberOfElements(), page.totalPages(),
                page.totalElements(), page.elements().stream().map(item -> getPackage(item.getId())).toList());
    }

    /**
     * Gets a single package view by its ID.
     *
     * @param id the package ID
     * @return the package view
     */
    public UserFeaturePackageView getPackage(Long id) {
        return packages().stream().filter(item -> Objects.equals(item.id(), id)).findFirst()
                .orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
    }

    /**
     * Creates a new user application package with its application/feature accesses.
     *
     * @param cmd the package creation command
     * @return the created package view
     */
    @Transactional
    public UserFeaturePackageView createPackage(SaveUserFeaturePackageCommand cmd) {
        BizAssert.notNull(cmd, BaseError.INVALID_PARAMETER);
        String code = normalizeCode(cmd.code());
        BizAssert.state(!userApplicationPackageRepository.existsByCode(code), BaseError.CONFLICT);
        UserApplicationPackage entity = new UserApplicationPackage(code, normalizeName(cmd.name()),
                defaultPackageType(cmd.packageType()), trimToNull(cmd.description()), cmd.defaultPackage(), false, 0,
                operatorId());
        if (!cmd.enabled()) {
            entity.disable(operatorId());
        }
        entity = userApplicationPackageRepository.save(entity);
        savePackageAccesses(entity.getId(), cmd.applicationAccesses());
        return getPackage(entity.getId());
    }

    /**
     * Updates an existing user application package.
     *
     * @param id  the package ID
     * @param cmd the package update command
     * @return the updated package view
     */
    @Transactional
    public UserFeaturePackageView updatePackage(Long id, SaveUserFeaturePackageCommand cmd) {
        BizAssert.notNull(cmd, BaseError.INVALID_PARAMETER);
        UserApplicationPackage entity = userApplicationPackageRepository.findById(id)
                .orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
        String code = normalizeCode(cmd.code());
        if (!Objects.equals(entity.getPackageCode(), code)) {
            BizAssert.state(!userApplicationPackageRepository.existsByCode(code), BaseError.CONFLICT);
        }
        entity.update(code, normalizeName(cmd.name()), defaultPackageType(cmd.packageType()),
                trimToNull(cmd.description()), cmd.defaultPackage(), 0, operatorId());
        if (cmd.enabled()) {
            entity.enable(operatorId());
        } else {
            entity.disable(operatorId());
        }
        userApplicationPackageRepository.save(entity);
        savePackageAccesses(entity.getId(), cmd.applicationAccesses());
        return getPackage(entity.getId());
    }

    /**
     * Enables or disables a user application package.
     *
     * @param id      the package ID
     * @param enabled true to enable, false to disable
     * @return true on success
     */
    @Transactional
    public boolean updatePackageStatus(Long id, boolean enabled) {
        UserApplicationPackage entity = userApplicationPackageRepository.findById(id)
                .orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
        if (enabled) {
            entity.enable(operatorId());
        } else {
            entity.disable(operatorId());
        }
        userApplicationPackageRepository.save(entity);
        apiPermissionCache.clearAll();
        return true;
    }

    /**
     * Deletes a user application package and all its associated access records and members.
     *
     * @param id the package ID
     * @return true on success
     */
    @Transactional
    public boolean deletePackage(Long id) {
        UserApplicationPackage entity = userApplicationPackageRepository.findById(id)
                .orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
        userPackageFeatureAccessRepository.deleteByPackageId(id);
        userPackageApplicationAccessRepository.deleteByPackageId(id);
        userApplicationPackageMemberRepository.deleteByPackageId(id);
        userApplicationPackageRepository.delete(entity);
        apiPermissionCache.clearAll();
        return true;
    }

    /**
     * Gets the full user feature management view for a user, including inherited and effective access.
     *
     * @param userId the user ID
     * @return the user management view
     */
    public UserFeatureUserManagementView getUserManagement(Long userId) {
        var user = webUserRepository.findById(userId).orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
        String account = resolveAccount(userId);

        List<String> packageIds = userApplicationPackageMemberRepository.findByUserId(userId).stream()
                .map(UserApplicationPackageMember::getPackageId).filter(Objects::nonNull).map(String::valueOf).toList();
        List<UserFeaturePackageOptionView> packages = userApplicationPackageRepository.findAll().stream()
                .sorted(packageComparator())
                .map(pkg -> new UserFeaturePackageOptionView(String.valueOf(pkg.getId()), pkg.getPackageCode(),
                        pkg.getPackageName(), pkg.getPackageType(), pkg.getDescription(),
                        Boolean.TRUE.equals(pkg.getEnabled()), Boolean.TRUE.equals(pkg.getDefaultPackage()))).toList();

        List<Long> selectedPackageIds = packageIds.stream().map(this::parseId).toList();
        List<UserPackageApplicationAccess> packageApplicationAccesses = selectedPackageIds.isEmpty() ? List.of() : userPackageApplicationAccessRepository.findByPackageIdIn(
                selectedPackageIds);
        List<UserPackageFeatureAccess> packageFeatureAccesses = selectedPackageIds.isEmpty() ? List.of() : userPackageFeatureAccessRepository.findByPackageIdIn(
                selectedPackageIds);
        Map<Long, String> packageScopeByApplicationId = summarizePackageScopeByApplicationId(
                packageApplicationAccesses);
        Set<Long> inheritedApplicationIds = new LinkedHashSet<>(packageScopeByApplicationId.keySet());
        Set<Long> inheritedFullAccessApplicationIds = packageApplicationAccesses.stream()
                .filter(access -> access.getFeatureAccessScope() == ApplicationFeatureAccessScope.FULL)
                .map(UserPackageApplicationAccess::getApplicationId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> inheritedFeatureIds = packageFeatureAccesses.stream().map(UserPackageFeatureAccess::getFeatureId)
                .filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, UserApplicationOverride> applicationOverrideByApplicationId = userApplicationOverrideRepository.findByUserId(
                userId).stream().filter(item -> item.getApplicationId() != null).collect(
                Collectors.toMap(UserApplicationOverride::getApplicationId, item -> item, (left, right) -> right,
                        LinkedHashMap::new));
        Map<Long, UserFeatureOverride> featureOverrideByFeatureId = userFeatureOverrideRepository.findByUserId(userId)
                .stream().filter(item -> item.getFeatureId() != null).collect(
                        Collectors.toMap(UserFeatureOverride::getFeatureId, item -> item, (left, right) -> right,
                                LinkedHashMap::new));
        Set<Long> effectiveApplicationIds = userFeatureAccessService.accessibleApplicationsForUser(userId).stream()
                .map(ProductApplication::getId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> effectiveFeatureIds = new LinkedHashSet<>(
                userFeatureAccessService.effectiveFeatureIdsForUser(userId));

        Map<Long, List<UserFeatureItemView>> featureViewsByApplicationId = featureViewsByApplicationId();
        List<UserFeatureUserApplicationView> applications = productApplicationRepository.findAll().stream()
                .sorted(applicationComparator()).map(application -> {
                    Long applicationId = application.getId();
                    UserApplicationOverride override = applicationId == null ? null : applicationOverrideByApplicationId.get(
                            applicationId);
                    List<UserFeatureUserFeatureView> features = featureViewsByApplicationId.getOrDefault(applicationId,
                            List.of()).stream().map(feature -> {
                        UserFeatureOverride featureOverride = featureOverrideByFeatureId.get(
                                parseId(feature.id().toString()));
                        boolean inheritedEnabled = inheritedFullAccessApplicationIds.contains(
                                applicationId) || inheritedFeatureIds.contains(feature.id());
                        return new UserFeatureUserFeatureView(String.valueOf(feature.id()),
                                String.valueOf(applicationId), application.getApplicationCode(), feature.code(),
                                feature.name(), feature.description(), feature.enabled(), feature.permissionCodes(),
                                inheritedEnabled, effectiveFeatureIds.contains(feature.id()),
                                featureOverride == null ? UserAccessOverrideType.NONE : featureOverride.getOverrideType());
                    }).toList();
                    return new UserFeatureUserApplicationView(String.valueOf(applicationId),
                            application.getApplicationCode(), application.getApplicationName(),
                            application.getDescription(), application.getIcon(), application.getRoutePath(),
                            application.getComponentPath(), Boolean.TRUE.equals(application.getEnabled()),
                            inheritedApplicationIds.contains(applicationId),
                            effectiveApplicationIds.contains(applicationId),
                            packageScopeByApplicationId.getOrDefault(applicationId, "NONE"),
                            override == null ? UserAccessOverrideType.NONE : override.getOverrideType(),
                            override == null ? null : override.getFeatureAccessScope(), features);
                }).toList();
        return new UserFeatureUserManagementView(String.valueOf(userId), account, packageIds, packages, applications);
    }

    /**
     * Saves the full user feature management configuration.
     * <p>Replaces package memberships, application overrides, and feature overrides atomically.</p>
     *
     * @param userId the user ID
     * @param cmd    the user management command
     * @return true on success
     */
    @Transactional
    public boolean saveUserManagement(Long userId, SaveUserFeatureUserManagementCommand cmd) {
        webUserRepository.findById(userId).orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
        Set<Long> packageIds = resolvePackageIds(cmd == null ? null : cmd.packageIds());
        userApplicationPackageMemberRepository.deleteByUserId(userId);
        if (!packageIds.isEmpty()) {
            userApplicationPackageMemberRepository.saveAll(packageIds.stream()
                    .map(packageId -> new UserApplicationPackageMember(packageId, userId, operatorId())).toList());
        }

        userApplicationOverrideRepository.deleteByUserId(userId);
        userFeatureOverrideRepository.deleteByUserId(userId);

        List<UserApplicationOverride> nextApplicationOverrides = buildApplicationOverrides(userId,
                cmd == null ? List.of() : cmd.applicationOverrides());
        List<UserFeatureOverride> nextFeatureOverrides = buildFeatureOverrides(userId,
                cmd == null ? List.of() : cmd.featureOverrides());
        if (!nextApplicationOverrides.isEmpty()) {
            userApplicationOverrideRepository.saveAll(nextApplicationOverrides);
        }
        if (!nextFeatureOverrides.isEmpty()) {
            userFeatureOverrideRepository.saveAll(nextFeatureOverrides);
        }
        apiPermissionCache.clearAll();
        return true;
    }

    /**
     * Persists the application/feature access bindings for a package, replacing any existing bindings.
     *
     * @param packageId the package ID
     * @param commands  the application access commands
     */
    private void savePackageAccesses(Long packageId,
            List<SaveUserFeaturePackageCommand.ApplicationAccessCommand> commands) {
        userPackageFeatureAccessRepository.deleteByPackageId(packageId);
        userPackageApplicationAccessRepository.deleteByPackageId(packageId);
        if (commands == null || commands.isEmpty()) {
            apiPermissionCache.clearAll();
            return;
        }
        Map<Long, ProductApplication> applicationById = productApplicationRepository.findAll().stream()
                .filter(item -> item.getId() != null).collect(
                        Collectors.toMap(ProductApplication::getId, item -> item, (left, right) -> left,
                                LinkedHashMap::new));
        Map<Long, Set<Long>> featureIdsByApplicationId = productFeatureRepository.findAll().stream()
                .filter(item -> item.getId() != null && item.getApplicationId() != null).collect(
                        Collectors.groupingBy(ProductFeature::getApplicationId, LinkedHashMap::new,
                                Collectors.mapping(ProductFeature::getId,
                                        Collectors.toCollection(LinkedHashSet::new))));

        List<UserPackageApplicationAccess> applicationAccesses = new ArrayList<>();
        List<UserPackageFeatureAccess> featureAccesses = new ArrayList<>();
        Set<Long> seenApplicationIds = new LinkedHashSet<>();
        for (SaveUserFeaturePackageCommand.ApplicationAccessCommand command : commands) {
            if (command == null || command.applicationId() == null || command.applicationId().isBlank()) {
                continue;
            }
            Long applicationId = parseId(command.applicationId());
            BizAssert.state(applicationById.containsKey(applicationId), BaseError.INVALID_PARAMETER);
            BizAssert.state(seenApplicationIds.add(applicationId), BaseError.CONFLICT);
            ApplicationFeatureAccessScope scope = command.featureAccessScope() == null ? ApplicationFeatureAccessScope.FULL : command.featureAccessScope();
            applicationAccesses.add(
                    new UserPackageApplicationAccess(packageId, applicationId, scope, false, operatorId()));
            if (scope == ApplicationFeatureAccessScope.PARTIAL) {
                Set<Long> allowedFeatureIds = featureIdsByApplicationId.getOrDefault(applicationId, Set.of());
                for (String featureIdRaw : command.featureIds() == null ? List.<String>of() : command.featureIds()) {
                    Long featureId = parseId(featureIdRaw);
                    BizAssert.state(allowedFeatureIds.contains(featureId), BaseError.INVALID_PARAMETER);
                    featureAccesses.add(
                            new UserPackageFeatureAccess(packageId, applicationId, featureId, false, operatorId()));
                }
            }
        }
        if (!applicationAccesses.isEmpty()) {
            userPackageApplicationAccessRepository.saveAll(applicationAccesses);
        }
        if (!featureAccesses.isEmpty()) {
            userPackageFeatureAccessRepository.saveAll(featureAccesses);
        }
        apiPermissionCache.clearAll();
    }

    /**
     * Builds a list of {@link UserApplicationOverride} entities from the given commands, validating application existence.
     *
     * @param userId   the user ID
     * @param commands the application override commands
     * @return list of application override entities
     */
    private List<UserApplicationOverride> buildApplicationOverrides(Long userId,
            List<ApplicationOverrideCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            return List.of();
        }
        Set<Long> allowedApplicationIds = productApplicationRepository.findAll().stream().map(ProductApplication::getId)
                .filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        List<UserApplicationOverride> result = new ArrayList<>();
        Set<Long> seen = new LinkedHashSet<>();
        for (ApplicationOverrideCommand command : commands) {
            if (command == null || command.applicationId() == null || command.applicationId().isBlank()) {
                continue;
            }
            UserAccessOverrideType overrideType = command.overrideType() == null ? UserAccessOverrideType.NONE : command.overrideType();
            if (overrideType == UserAccessOverrideType.NONE) {
                continue;
            }
            Long applicationId = parseId(command.applicationId());
            BizAssert.state(allowedApplicationIds.contains(applicationId), BaseError.INVALID_PARAMETER);
            BizAssert.state(seen.add(applicationId), BaseError.CONFLICT);
            ApplicationFeatureAccessScope scope = overrideType == UserAccessOverrideType.ENABLE ? (command.featureAccessScope() == null ? ApplicationFeatureAccessScope.FULL : command.featureAccessScope()) : ApplicationFeatureAccessScope.PARTIAL;
            result.add(new UserApplicationOverride(userId, applicationId, overrideType, scope, null, operatorId()));
        }
        return result;
    }

    /**
     * Builds a list of {@link UserFeatureOverride} entities from the given commands, validating feature existence.
     *
     * @param userId   the user ID
     * @param commands the feature override commands
     * @return list of feature override entities
     */
    private List<UserFeatureOverride> buildFeatureOverrides(Long userId, List<FeatureOverrideCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            return List.of();
        }
        Map<Long, ProductFeature> featureById = productFeatureRepository.findAll().stream()
                .filter(item -> item.getId() != null).collect(
                        Collectors.toMap(ProductFeature::getId, item -> item, (left, right) -> left,
                                LinkedHashMap::new));
        List<UserFeatureOverride> result = new ArrayList<>();
        Set<Long> seen = new LinkedHashSet<>();
        for (FeatureOverrideCommand command : commands) {
            if (command == null || command.featureId() == null || command.featureId().isBlank()) {
                continue;
            }
            UserAccessOverrideType overrideType = command.overrideType() == null ? UserAccessOverrideType.NONE : command.overrideType();
            if (overrideType == UserAccessOverrideType.NONE) {
                continue;
            }
            Long featureId = parseId(command.featureId());
            ProductFeature feature = featureById.get(featureId);
            BizAssert.notNull(feature, BaseError.INVALID_PARAMETER);
            Long applicationId = parseId(command.applicationId());
            BizAssert.state(Objects.equals(feature.getApplicationId(), applicationId), BaseError.INVALID_PARAMETER);
            BizAssert.state(seen.add(featureId), BaseError.CONFLICT);
            result.add(new UserFeatureOverride(userId, applicationId, featureId, overrideType, null, operatorId()));
        }
        return result;
    }

    /**
     * Assembles a {@link UserFeaturePackageView} from the raw entities and pre-fetched reference data.
     */
    private UserFeaturePackageView toPackageView(UserApplicationPackage pkg,
            Map<Long, ProductApplication> applicationById, Map<Long, List<UserFeatureItemView>> featuresByApplicationId,
            List<UserPackageApplicationAccess> applicationAccesses, List<UserPackageFeatureAccess> featureAccesses) {
        Map<Long, List<String>> selectedFeatureIdsByApplicationId = featureAccesses.stream()
                .filter(item -> item.getApplicationId() != null && item.getFeatureId() != null).collect(
                        Collectors.groupingBy(UserPackageFeatureAccess::getApplicationId, LinkedHashMap::new,
                                Collectors.mapping(item -> String.valueOf(item.getFeatureId()), Collectors.toList())));
        List<UserFeaturePackageApplicationAccessView> accessViews = applicationAccesses.stream()
                .sorted(Comparator.comparing(UserPackageApplicationAccess::getApplicationId,
                        Comparator.nullsLast(Long::compareTo))).map(access -> {
                    ProductApplication application = applicationById.get(access.getApplicationId());
                    List<String> featureIds = selectedFeatureIdsByApplicationId.getOrDefault(access.getApplicationId(),
                            List.of());
                    List<UserFeatureItemView> features = featuresByApplicationId.getOrDefault(access.getApplicationId(),
                                    List.of()).stream().filter(feature -> featureIds.contains(String.valueOf(feature.id())))
                            .toList();
                    return new UserFeaturePackageApplicationAccessView(String.valueOf(access.getApplicationId()),
                            application == null ? "" : application.getApplicationCode(),
                            application == null ? "" : application.getApplicationName(), access.getFeatureAccessScope(),
                            featureIds, features);
                }).toList();
        return new UserFeaturePackageView(pkg.getId(), pkg.getPackageCode(), pkg.getPackageName(), pkg.getPackageType(),
                pkg.getDescription(), Boolean.TRUE.equals(pkg.getEnabled()),
                Boolean.TRUE.equals(pkg.getDefaultPackage()), accessViews);
    }

    /**
     * Groups feature item views by application ID, sorted by display order.
     */
    private Map<Long, List<UserFeatureItemView>> featureViewsByApplicationId() {
        Map<Long, List<String>> permissionCodesByFeatureId = permissionCodesByFeatureId();
        return productFeatureRepository.findAll().stream()
                .filter(item -> item.getId() != null && item.getApplicationId() != null)
                .sorted(Comparator.comparing(ProductFeature::getApplicationId, Comparator.nullsLast(Long::compareTo))
                        .thenComparing(ProductFeature::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ProductFeature::getId, Comparator.nullsLast(Long::compareTo))).collect(
                        Collectors.groupingBy(ProductFeature::getApplicationId, LinkedHashMap::new, Collectors.mapping(
                                feature -> new UserFeatureItemView(feature.getId(), feature.getApplicationId(),
                                        feature.getFeatureCode(), feature.getFeatureName(), feature.getDescription(),
                                        Boolean.TRUE.equals(feature.getEnabled()),
                                        permissionCodesByFeatureId.getOrDefault(feature.getId(), List.of())),
                                Collectors.toList())));
    }

    /**
     * Counts permission bindings grouped by application ID.
     */
    private Map<Long, Integer> permissionCountByApplicationId() {
        return productFeaturePermissionBindingRepository.findAll().stream()
                .filter(item -> item.getApplicationId() != null).collect(
                        Collectors.groupingBy(ProductFeaturePermissionBinding::getApplicationId, LinkedHashMap::new,
                                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));
    }

    /**
     * Maps each feature ID to its list of bound permission codes.
     */
    private Map<Long, List<String>> permissionCodesByFeatureId() {
        List<ProductFeaturePermissionBinding> bindings = productFeaturePermissionBindingRepository.findAll();
        LinkedHashSet<Long> permissionIds = bindings.stream().map(ProductFeaturePermissionBinding::getPermissionId)
                .filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, Permission> permissionById = permissionRepository.findAllById(permissionIds).stream()
                .filter(permission -> permission.getCode() != null && !permission.getCode().isBlank()).collect(
                        Collectors.toMap(Permission::getId, permission -> permission, (left, right) -> left,
                                LinkedHashMap::new));
        Map<Long, LinkedHashSet<String>> result = new LinkedHashMap<>();
        for (ProductFeaturePermissionBinding binding : bindings) {
            Permission permission = permissionById.get(binding.getPermissionId());
            if (permission == null || binding.getFeatureId() == null) {
                continue;
            }
            result.computeIfAbsent(binding.getFeatureId(), ignored -> new LinkedHashSet<>())
                    .add(permission.getCode().trim());
        }
        return result.entrySet().stream().collect(
                Collectors.toMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue()), (left, right) -> left,
                        LinkedHashMap::new));
    }

    /**
     * Summarizes the aggregate feature access scope per application from a collection of package application accesses.
     * <p>FULL takes precedence over PARTIAL when both exist for the same application.</p>
     */
    private Map<Long, String> summarizePackageScopeByApplicationId(Collection<UserPackageApplicationAccess> accesses) {
        Map<Long, String> result = new LinkedHashMap<>();
        for (UserPackageApplicationAccess access : accesses) {
            if (access.getApplicationId() == null) {
                continue;
            }
            String current = result.get(access.getApplicationId());
            String next = access.getFeatureAccessScope() == ApplicationFeatureAccessScope.FULL ? "FULL" : "PARTIAL";
            if (current == null || "PARTIAL".equals(current) && "FULL".equals(next)) {
                result.put(access.getApplicationId(), next);
            }
        }
        return result;
    }

    /**
     * Resolves and validates a list of raw package ID strings into a set of valid Long IDs.
     */
    private Set<Long> resolvePackageIds(List<String> packageIds) {
        if (packageIds == null || packageIds.isEmpty()) {
            return Set.of();
        }
        Set<Long> allowedIds = userApplicationPackageRepository.findAll().stream().map(UserApplicationPackage::getId)
                .filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        LinkedHashSet<Long> result = new LinkedHashSet<>();
        for (String packageIdRaw : packageIds) {
            Long packageId = parseId(packageIdRaw);
            BizAssert.state(allowedIds.contains(packageId), BaseError.INVALID_PARAMETER);
            result.add(packageId);
        }
        return Set.copyOf(result);
    }

    /**
     * Resolves the user's account name (username identity) or falls back to the user ID string.
     */
    private String resolveAccount(Long userId) {
        return webUserIdentityRepository.findFirstByUserIdAndIdentityType(userId, WebUserIdentityType.USERNAME)
                .map(WebUserIdentity::getIdentityValue).orElse(String.valueOf(userId));
    }

    private Comparator<ProductApplication> applicationComparator() {
        return Comparator.comparing(ProductApplication::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(ProductApplication::getId, Comparator.nullsLast(Long::compareTo));
    }

    private Comparator<UserApplicationPackage> packageComparator() {
        return Comparator.comparing(UserApplicationPackage::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(UserApplicationPackage::getId, Comparator.nullsLast(Long::compareTo));
    }

    private Long parseId(String raw) {
        try {
            return Long.parseLong(raw.trim());
        } catch (Exception ex) {
            throw new BizException(BaseError.INVALID_PARAMETER);
        }
    }

    private String normalizeCode(String code) {
        BizAssert.notBlank(code, BaseError.MISSING_PARAMETER);
        return code.trim();
    }

    private String normalizeName(String name) {
        BizAssert.notBlank(name, BaseError.MISSING_PARAMETER);
        return name.trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Long operatorId() {
        var principal = CtxUtil.getPrincipal();
        return principal == null ? 0L : principal.userId();
    }

    private com.corwin.system.userfeature.domain.model.UserApplicationPackageType defaultPackageType(
            com.corwin.system.userfeature.domain.model.UserApplicationPackageType packageType) {
        return packageType == null ? com.corwin.system.userfeature.domain.model.UserApplicationPackageType.CUSTOM : packageType;
    }
}
