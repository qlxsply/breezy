package com.corwin.system.normalfeature.application.service;

import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.framework.error.BizException;
import com.corwin.framework.web.ctx.CtxUtil;
import com.corwin.system.normalfeature.application.command.UpdateNormalFeatureCommand;
import com.corwin.system.normalfeature.application.command.SaveNormalFeatureUserManagementCommand;
import com.corwin.system.normalfeature.application.view.NormalFeatureUserFeatureView;
import com.corwin.system.normalfeature.application.view.NormalFeatureUserManagementView;
import com.corwin.system.normalfeature.application.view.NormalFeatureView;
import com.corwin.system.normalfeature.domain.model.NormalFeature;
import com.corwin.system.normalfeature.domain.model.NormalFeatureGroup;
import com.corwin.system.normalfeature.domain.model.NormalFeatureGroupGrant;
import com.corwin.system.normalfeature.domain.model.NormalFeatureGroupMember;
import com.corwin.system.normalfeature.domain.model.NormalFeatureOverrideType;
import com.corwin.system.normalfeature.domain.model.NormalFeaturePermission;
import com.corwin.system.normalfeature.domain.model.NormalFeatureUserOverride;
import com.corwin.system.normalfeature.domain.repo.NormalFeatureGroupGrantRepository;
import com.corwin.system.normalfeature.domain.repo.NormalFeatureGroupMemberRepository;
import com.corwin.system.normalfeature.domain.repo.NormalFeatureGroupRepository;
import com.corwin.system.normalfeature.domain.repo.NormalFeaturePermissionRepository;
import com.corwin.system.normalfeature.domain.repo.NormalFeatureRepository;
import com.corwin.system.normalfeature.domain.repo.NormalFeatureUserOverrideRepository;
import com.corwin.system.resource.application.service.ApiPermissionCache;
import com.corwin.system.resource.domain.model.Permission;
import com.corwin.system.resource.domain.model.PermissionUserScope;
import com.corwin.system.resource.domain.repo.PermissionRepository;
import com.corwin.system.webuser.domain.model.WebUser;
import com.corwin.system.webuser.domain.model.WebUserIdentity;
import com.corwin.system.webuser.domain.model.WebUserIdentityType;
import com.corwin.system.webuser.domain.repo.WebUserRepository;
import com.corwin.system.webuser.domain.repo.WebUserIdentityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Corwin 2026/5/5
 */
@Service
@RequiredArgsConstructor
public class NormalFeatureService {

    private final NormalFeatureRepository normalFeatureRepository;
    private final NormalFeatureGroupRepository normalFeatureGroupRepository;
    private final NormalFeatureGroupGrantRepository normalFeatureGroupGrantRepository;
    private final NormalFeatureGroupMemberRepository normalFeatureGroupMemberRepository;
    private final NormalFeaturePermissionRepository normalFeaturePermissionRepository;
    private final NormalFeatureUserOverrideRepository normalFeatureUserOverrideRepository;
    private final PermissionRepository permissionRepository;
    private final WebUserRepository webUserRepository;
    private final WebUserIdentityRepository webUserIdentityRepository;
    private final ApiPermissionCache apiPermissionCache;

    public List<NormalFeatureView> manageableFeatures() {
        List<NormalFeature> features = allFeaturesInOrder();
        Map<Long, List<String>> permissionCodesByFeatureId = permissionCodesByFeatureId(
                features.stream().map(NormalFeature::getId).filter(Objects::nonNull).toList());
        return features.stream()
                .map(feature -> new NormalFeatureView(feature.getId(), feature.getCode(), feature.getName(),
                        feature.getDescription(), Boolean.TRUE.equals(feature.getEnabled()),
                        permissionCodesByFeatureId.getOrDefault(feature.getId(), List.of())))
                .toList();
    }

    public List<String> globalFeatureIds() {
        return toFeatureIdStrings(enabledFeatureIds());
    }

    public List<String> effectiveFeatureIdsForUser(Long userId) {
        requireExternalUser(userId);
        return toFeatureIdStrings(effectiveFeatureIdsForUserInternal(userId));
    }

    public Set<String> permissionCodesForNormalUser(Long userId) {
        requireExternalUser(userId);
        List<Long> effectiveFeatureIds = effectiveFeatureIdsForUserInternal(userId);
        if (effectiveFeatureIds.isEmpty()) {
            return Set.of();
        }

        List<NormalFeaturePermission> links = normalFeaturePermissionRepository.findByFeatureIdIn(effectiveFeatureIds);
        if (links.isEmpty()) {
            return Set.of();
        }

        LinkedHashSet<Long> permissionIds = links.stream()
                .map(NormalFeaturePermission::getPermissionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, Permission> permissionById = permissionRepository.findAllById(permissionIds).stream()
                .filter(permission -> Boolean.TRUE.equals(permission.getEnabled()))
                .filter(permission -> permission.getUserScope() == PermissionUserScope.EXTERNAL
                        || permission.getUserScope() == PermissionUserScope.COMMON)
                .collect(Collectors.toMap(Permission::getId, permission -> permission, (left, right) -> left,
                        LinkedHashMap::new));

        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (NormalFeaturePermission link : links) {
            Permission permission = permissionById.get(link.getPermissionId());
            if (permission == null || permission.getCode() == null || permission.getCode().isBlank()) {
                continue;
            }
            result.add(permission.getCode().trim());
        }
        return Set.copyOf(result);
    }

    public NormalFeatureUserManagementView getUserManagement(Long userId) {
        WebUser user = requireExternalUser(userId);
        List<NormalFeature> features = allFeaturesInOrder();
        Map<Long, List<String>> permissionCodesByFeatureId = permissionCodesByFeatureId(features.stream()
                .map(NormalFeature::getId)
                .filter(Objects::nonNull)
                .toList());
        List<String> groupIds = normalFeatureGroupMemberRepository.findByUserId(userId).stream()
                .map(NormalFeatureGroupMember::getGroupId)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .toList();
        Set<Long> groupFeatureIds = new LinkedHashSet<>(groupFeatureIdsForUser(userId));
        Map<Long, NormalFeatureOverrideType> overrideByFeatureId = normalFeatureUserOverrideRepository.findByUserId(userId)
                .stream()
                .filter(override -> override.getFeatureId() != null && override.getOverrideType() != null)
                .collect(Collectors.toMap(NormalFeatureUserOverride::getFeatureId, NormalFeatureUserOverride::getOverrideType,
                        (left, right) -> right, LinkedHashMap::new));
        List<NormalFeatureUserFeatureView> featureViews = features.stream().map(feature -> {
            Long featureId = feature.getId();
            boolean globallyEnabled = Boolean.TRUE.equals(feature.getEnabled());
            boolean groupEnabled = featureId != null && groupFeatureIds.contains(featureId);
            NormalFeatureOverrideType overrideType = featureId == null
                    ? NormalFeatureOverrideType.NONE
                    : overrideByFeatureId.getOrDefault(featureId, NormalFeatureOverrideType.NONE);
            boolean effectiveEnabled = switch (overrideType) {
                case ENABLE -> true;
                case DISABLE -> false;
                case NONE -> groupEnabled;
            };
            return new NormalFeatureUserFeatureView(feature.getId(), feature.getCode(), feature.getName(),
                    feature.getDescription(), globallyEnabled,
                    permissionCodesByFeatureId.getOrDefault(featureId, List.of()), groupEnabled, effectiveEnabled,
                    overrideType);
        }).toList();
        return new NormalFeatureUserManagementView(user.getId(), resolveAccount(user), groupIds, featureViews);
    }

    @Transactional
    public boolean updateGlobalFeatures(UpdateNormalFeatureCommand cmd) {
        Set<Long> desiredFeatureIds = resolveFeatureIds(cmd == null ? null : cmd.featureIds());
        Long operatorId = operatorId();
        List<NormalFeature> features = normalFeatureRepository.findAll();
        for (NormalFeature feature : features) {
            if (feature.getId() == null) {
                continue;
            }
            if (desiredFeatureIds.contains(feature.getId())) {
                feature.enable(operatorId);
            } else {
                feature.disable(operatorId);
            }
        }
        normalFeatureRepository.saveAll(features);
        apiPermissionCache.clearAll();
        return true;
    }

    @Transactional
    public boolean updateFeatureStatus(Long featureId, boolean enabled) {
        NormalFeature feature = normalFeatureRepository.findById(featureId).orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
        Long operatorId = operatorId();
        if (enabled) {
            feature.enable(operatorId);
        } else {
            feature.disable(operatorId);
        }
        normalFeatureRepository.save(feature);
        apiPermissionCache.clearAll();
        return true;
    }

    @Transactional
    public boolean updateUserFeatures(Long userId, UpdateNormalFeatureCommand cmd) {
        requireExternalUser(userId);
        Set<Long> desiredFeatureIds = resolveFeatureIds(cmd == null ? null : cmd.featureIds());
        Set<Long> globalEnabledFeatureIds = new LinkedHashSet<>(enabledFeatureIds());
        List<NormalFeature> features = allFeaturesInOrder();
        Long operatorId = operatorId();

        normalFeatureUserOverrideRepository.deleteByUserId(userId);
        List<NormalFeatureUserOverride> next = new ArrayList<>();
        for (NormalFeature feature : features) {
            Long featureId = feature.getId();
            if (featureId == null) {
                continue;
            }
            boolean globalEnabled = globalEnabledFeatureIds.contains(featureId);
            boolean desiredEnabled = desiredFeatureIds.contains(featureId);
            if (globalEnabled == desiredEnabled) {
                continue;
            }
            NormalFeatureOverrideType overrideType =
                    desiredEnabled ? NormalFeatureOverrideType.ENABLE : NormalFeatureOverrideType.DISABLE;
            next.add(new NormalFeatureUserOverride(userId, featureId, overrideType, null, operatorId));
        }
        normalFeatureUserOverrideRepository.saveAll(next);
        apiPermissionCache.clearAll();
        return true;
    }

    @Transactional
    public boolean saveUserManagement(Long userId, SaveNormalFeatureUserManagementCommand cmd) {
        requireExternalUser(userId);
        Set<Long> groupIds = resolveGroupIds(cmd == null ? null : cmd.groupIds());
        normalFeatureGroupMemberRepository.deleteByUserId(userId);
        normalFeatureGroupMemberRepository.saveAll(groupIds.stream()
                .map(groupId -> new NormalFeatureGroupMember(groupId, userId, operatorId()))
                .toList());

        normalFeatureUserOverrideRepository.deleteByUserId(userId);
        List<SaveNormalFeatureUserManagementCommand.FeatureOverrideCommand> overrides = cmd == null || cmd.overrides() == null
                ? List.of()
                : cmd.overrides();
        Set<Long> allowedFeatureIds = allFeaturesInOrder().stream().map(NormalFeature::getId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<NormalFeatureUserOverride> next = new ArrayList<>();
        for (SaveNormalFeatureUserManagementCommand.FeatureOverrideCommand override : overrides) {
            if (override == null || override.featureId() == null || override.featureId().isBlank()) {
                continue;
            }
            Long featureId = parseFeatureId(override.featureId());
            BizAssert.state(allowedFeatureIds.contains(featureId), BaseError.INVALID_PARAMETER);
            NormalFeatureOverrideType overrideType = override.overrideType() == null ? NormalFeatureOverrideType.NONE : override.overrideType();
            if (overrideType == NormalFeatureOverrideType.NONE) {
                continue;
            }
            next.add(new NormalFeatureUserOverride(userId, featureId, overrideType, null, operatorId()));
        }
        normalFeatureUserOverrideRepository.saveAll(next);
        apiPermissionCache.clearAll();
        return true;
    }

    private List<Long> effectiveFeatureIdsForUserInternal(Long userId) {
        LinkedHashSet<Long> result = new LinkedHashSet<>(groupFeatureIdsForUser(userId));
        List<NormalFeatureUserOverride> overrides = normalFeatureUserOverrideRepository.findByUserId(userId);
        for (NormalFeatureUserOverride override : overrides) {
            Long featureId = override.getFeatureId();
            if (featureId == null || override.getOverrideType() == null) {
                continue;
            }
            if (override.getOverrideType() == NormalFeatureOverrideType.ENABLE) {
                result.add(featureId);
                continue;
            }
            if (override.getOverrideType() == NormalFeatureOverrideType.DISABLE) {
                result.remove(featureId);
            }
        }
        return List.copyOf(result);
    }

    private List<Long> enabledFeatureIds() {
        return allFeaturesInOrder().stream()
                .filter(feature -> Boolean.TRUE.equals(feature.getEnabled()))
                .map(NormalFeature::getId)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<Long> groupFeatureIdsForUser(Long userId) {
        List<Long> groupIds = normalFeatureGroupMemberRepository.findByUserId(userId).stream()
                .map(NormalFeatureGroupMember::getGroupId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (groupIds.isEmpty()) {
            return List.of();
        }
        Set<Long> enabledGroupIds = normalFeatureGroupRepository.findByIdIn(groupIds).stream()
                .filter(group -> Boolean.TRUE.equals(group.getEnabled()))
                .map(NormalFeatureGroup::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (enabledGroupIds.isEmpty()) {
            return List.of();
        }
        Set<Long> enabledFeatureIds = new LinkedHashSet<>(enabledFeatureIds());
        return normalFeatureGroupGrantRepository.findByGroupIdIn(enabledGroupIds).stream()
                .map(NormalFeatureGroupGrant::getFeatureId)
                .filter(Objects::nonNull)
                .filter(enabledFeatureIds::contains)
                .distinct()
                .toList();
    }

    private Set<Long> resolveGroupIds(List<String> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return Set.of();
        }
        Set<Long> allowedIds = normalFeatureGroupRepository.findByIdIn(groupIds.stream().map(this::parseFeatureId).toList()).stream()
                .filter(group -> Boolean.TRUE.equals(group.getEnabled()))
                .map(NormalFeatureGroup::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        LinkedHashSet<Long> normalized = new LinkedHashSet<>();
        for (String groupId : groupIds) {
            if (groupId == null || groupId.isBlank()) {
                continue;
            }
            Long parsed = parseFeatureId(groupId);
            BizAssert.state(allowedIds.contains(parsed), BaseError.INVALID_PARAMETER);
            normalized.add(parsed);
        }
        return Set.copyOf(normalized);
    }

    private Set<Long> resolveFeatureIds(List<String> featureIds) {
        List<NormalFeature> features = allFeaturesInOrder();
        if (features.isEmpty() || featureIds == null || featureIds.isEmpty()) {
            return Set.of();
        }
        Set<Long> allowedIds = features.stream()
                .map(NormalFeature::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        LinkedHashSet<Long> normalized = new LinkedHashSet<>();
        for (String featureId : featureIds) {
            if (featureId == null || featureId.isBlank()) {
                continue;
            }
            Long parsedId = parseFeatureId(featureId);
            BizAssert.state(allowedIds.contains(parsedId), BaseError.INVALID_PARAMETER);
            normalized.add(parsedId);
        }
        return Set.copyOf(normalized);
    }

    private List<String> toFeatureIdStrings(Collection<Long> featureIds) {
        if (featureIds == null || featureIds.isEmpty()) {
            return List.of();
        }
        Set<Long> selected = featureIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return allFeaturesInOrder().stream()
                .map(NormalFeature::getId)
                .filter(Objects::nonNull)
                .filter(selected::contains)
                .map(String::valueOf)
                .toList();
    }

    private Map<Long, List<String>> permissionCodesByFeatureId(List<Long> featureIds) {
        if (featureIds.isEmpty()) {
            return Map.of();
        }
        List<NormalFeaturePermission> links = normalFeaturePermissionRepository.findByFeatureIdIn(featureIds);
        if (links.isEmpty()) {
            return Map.of();
        }

        LinkedHashSet<Long> permissionIds = links.stream()
                .map(NormalFeaturePermission::getPermissionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, String> permissionCodeById = permissionRepository.findAllById(permissionIds).stream()
                .filter(permission -> permission.getCode() != null && !permission.getCode().isBlank())
                .collect(Collectors.toMap(Permission::getId, permission -> permission.getCode().trim(),
                        (left, right) -> left, LinkedHashMap::new));

        Map<Long, LinkedHashSet<String>> buffer = new LinkedHashMap<>();
        for (NormalFeaturePermission link : links) {
            String permissionCode = permissionCodeById.get(link.getPermissionId());
            if (permissionCode == null) {
                continue;
            }
            buffer.computeIfAbsent(link.getFeatureId(), __ -> new LinkedHashSet<>()).add(permissionCode);
        }

        LinkedHashMap<Long, List<String>> result = new LinkedHashMap<>();
        for (Map.Entry<Long, LinkedHashSet<String>> entry : buffer.entrySet()) {
            result.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return result;
    }

    private List<NormalFeature> allFeaturesInOrder() {
        return normalFeatureRepository.findAll().stream()
                .sorted(Comparator.comparing(NormalFeature::getCode, Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(NormalFeature::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    private WebUser requireExternalUser(Long userId) {
        BizAssert.notNull(userId, BaseError.INVALID_PARAMETER);
        return webUserRepository.findById(userId).orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
    }

    private String resolveAccount(WebUser user) {
        Long primaryIdentityId = user.getPrimaryIdentityId();
        if (primaryIdentityId != null) {
            return webUserIdentityRepository.findById(primaryIdentityId).map(WebUserIdentity::getIdentityValue)
                    .orElse(String.valueOf(user.getId()));
        }
        return webUserIdentityRepository.findFirstByUserIdAndIdentityType(user.getId(), WebUserIdentityType.USERNAME)
                .map(WebUserIdentity::getIdentityValue)
                .orElse(String.valueOf(user.getId()));
    }

    private Long operatorId() {
        Long operatorId = CtxUtil.getPrincipal().userId();
        BizAssert.notNull(operatorId, BaseError.FORBIDDEN);
        return operatorId;
    }

    private Long parseFeatureId(String featureId) {
        try {
            return Long.parseLong(featureId.trim());
        } catch (NumberFormatException ex) {
            throw new BizException(BaseError.INVALID_PARAMETER);
        }
    }
}
