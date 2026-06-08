package com.corwin.system.normalfeature.application.service;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.framework.error.BizException;
import com.corwin.framework.web.ctx.CtxUtil;
import com.corwin.system.normalfeature.application.command.SaveNormalFeatureGroupCommand;
import com.corwin.system.normalfeature.application.view.NormalFeatureItemView;
import com.corwin.system.normalfeature.application.view.NormalFeatureGroupView;
import com.corwin.system.normalfeature.domain.model.NormalFeature;
import com.corwin.system.normalfeature.domain.model.NormalFeatureGroup;
import com.corwin.system.normalfeature.domain.model.NormalFeatureGroupGrant;
import com.corwin.system.normalfeature.domain.model.NormalFeatureGroupMember;
import com.corwin.system.normalfeature.domain.model.NormalFeatureGroupType;
import com.corwin.system.normalfeature.domain.repo.NormalFeatureGroupGrantRepository;
import com.corwin.system.normalfeature.domain.repo.NormalFeatureGroupMemberRepository;
import com.corwin.system.normalfeature.domain.repo.NormalFeatureGroupRepository;
import com.corwin.system.normalfeature.domain.repo.NormalFeatureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Corwin 2026/5/21
 */
@Service
@RequiredArgsConstructor
public class NormalFeatureGroupAdminService {

    private final NormalFeatureGroupRepository groupRepository;
    private final NormalFeatureGroupGrantRepository groupGrantRepository;
    private final NormalFeatureGroupMemberRepository groupMemberRepository;
    private final NormalFeatureRepository featureRepository;

    public PageData<NormalFeatureGroupView> page(String keyword, Boolean enabled, PageSpec spec) {
        PageData<NormalFeatureGroup> page = groupRepository.page(keyword, enabled, spec);
        return new PageData<>(page.pageNo(), page.pageSize(), page.numberOfElements(), page.totalPages(),
                page.totalElements(), page.elements().stream().map(this::toView).toList());
    }

    public NormalFeatureGroupView get(Long id) {
        return toView(requireGroup(id));
    }

    @Transactional
    public NormalFeatureGroupView create(SaveNormalFeatureGroupCommand cmd) {
        BizAssert.notNull(cmd, BaseError.INVALID_PARAMETER);
        String code = normalizeCode(cmd.code());
        BizAssert.state(!groupRepository.existsByCode(code), BaseError.CONFLICT);
        String operator = operator();
        NormalFeatureGroup entity = new NormalFeatureGroup(code, normalizeName(cmd.name()),
                defaultGroupType(cmd.groupType()), trimToNull(cmd.description()), cmd.defaultGroup(), false, 0L);
        if (!cmd.enabled()) {
            entity.disable(0L);
        }
        entity = groupRepository.save(entity);
        saveGroupFeatures(entity.getId(), resolveFeatureIds(cmd.featureIds()));
        return toView(entity);
    }

    @Transactional
    public NormalFeatureGroupView update(Long id, SaveNormalFeatureGroupCommand cmd) {
        BizAssert.notNull(cmd, BaseError.INVALID_PARAMETER);
        NormalFeatureGroup entity = requireGroup(id);
        String code = normalizeCode(cmd.code());
        if (!entity.getCode().equals(code)) {
            BizAssert.state(!groupRepository.existsByCode(code), BaseError.CONFLICT);
        }
        entity.update(code, normalizeName(cmd.name()), defaultGroupType(cmd.groupType()), trimToNull(cmd.description()),
                cmd.defaultGroup(), 0L);
        if (cmd.enabled()) {
            entity.enable(0L);
        } else {
            entity.disable(0L);
        }
        entity = groupRepository.save(entity);
        saveGroupFeatures(entity.getId(), resolveFeatureIds(cmd.featureIds()));
        return toView(entity);
    }

    @Transactional
    public boolean updateStatus(Long id, boolean enabled) {
        NormalFeatureGroup entity = requireGroup(id);
        if (enabled) {
          entity.enable(0L);
        } else {
          entity.disable(0L);
        }
        groupRepository.save(entity);
        return true;
    }

    @Transactional
    public boolean delete(Long id) {
        NormalFeatureGroup entity = requireGroup(id);
        groupGrantRepository.deleteByGroupId(id);
        groupMemberRepository.deleteByGroupId(id);
        groupRepository.delete(entity);
        return true;
    }

    public List<NormalFeatureGroup> defaultEnabledGroups() {
        return groupRepository.findByDefaultGroupTrueAndEnabledTrue();
    }

    @Transactional
    public void assignDefaultGroupsToUser(Long userId) {
        if (userId == null) {
            return;
        }
        for (NormalFeatureGroup group : defaultEnabledGroups()) {
            groupMemberRepository.save(new NormalFeatureGroupMember(group.getId(), userId, 0L));
        }
    }

    private void saveGroupFeatures(Long groupId, Set<Long> featureIds) {
        groupGrantRepository.deleteByGroupId(groupId);
        List<NormalFeatureGroupGrant> next = featureIds.stream()
                .map(featureId -> new NormalFeatureGroupGrant(groupId, featureId, 0L))
                .toList();
        groupGrantRepository.saveAll(next);
    }

    private Set<Long> resolveFeatureIds(List<String> featureIds) {
        if (featureIds == null || featureIds.isEmpty()) {
            return Set.of();
        }
        Set<Long> allowedIds = featureRepository.findAll().stream().map(NormalFeature::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        LinkedHashSet<Long> normalized = new LinkedHashSet<>();
        for (String featureId : featureIds) {
            if (featureId == null || featureId.isBlank()) {
                continue;
            }
            Long parsed = parseId(featureId);
            BizAssert.state(allowedIds.contains(parsed), BaseError.INVALID_PARAMETER);
            normalized.add(parsed);
        }
        return Set.copyOf(normalized);
    }

    private NormalFeatureGroup requireGroup(Long id) {
        return groupRepository.findById(id).orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
    }

    private NormalFeatureGroupView toView(NormalFeatureGroup entity) {
        List<Long> featureIdValues = groupGrantRepository.findByGroupId(entity.getId()).stream()
                .map(NormalFeatureGroupGrant::getFeatureId)
                .filter(Objects::nonNull)
                .toList();
        List<String> featureIds = featureIdValues.stream()
                .map(String::valueOf)
                .toList();

        Map<Long, NormalFeature> featureMap = featureRepository.findByIdIn(featureIdValues).stream()
                .collect(Collectors.toMap(NormalFeature::getId, item -> item));
        List<NormalFeatureItemView> features = featureIdValues.stream()
                .map(featureMap::get)
                .filter(Objects::nonNull)
                .map(item -> new NormalFeatureItemView(item.getId(), item.getCode(), item.getName(),
                        item.getDescription(), Boolean.TRUE.equals(item.getEnabled())))
                .toList();

        return new NormalFeatureGroupView(entity.getId(), entity.getCode(), entity.getName(), entity.getGroupType(),
                entity.getDescription(), Boolean.TRUE.equals(entity.getEnabled()),
                Boolean.TRUE.equals(entity.getDefaultGroup()), featureIds, features);
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

    private Long parseId(String raw) {
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ex) {
            throw new BizException(BaseError.INVALID_PARAMETER);
        }
    }

    private NormalFeatureGroupType defaultGroupType(NormalFeatureGroupType groupType) {
        return groupType == null ? NormalFeatureGroupType.WHITELIST : groupType;
    }

    private String operator() {
        var principal = CtxUtil.getPrincipal();
        return principal == null ? "system" : principal.username();
    }
}
