package com.corwin.system.normalfeature.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.normalfeature.domain.model.NormalFeatureGroupMember;

import java.util.List;

/**
 * @author Corwin 2026/5/21
 */
public interface NormalFeatureGroupMemberRepository extends DomainRepository<NormalFeatureGroupMember, Long> {

    List<NormalFeatureGroupMember> findByUserId(Long userId);

    List<NormalFeatureGroupMember> findByGroupId(Long groupId);

    void deleteByUserId(Long userId);

    void deleteByGroupId(Long groupId);
}
