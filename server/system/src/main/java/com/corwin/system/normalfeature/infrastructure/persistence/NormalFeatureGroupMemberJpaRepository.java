package com.corwin.system.normalfeature.infrastructure.persistence;

import com.corwin.system.normalfeature.domain.model.NormalFeatureGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author Corwin 2026/5/21
 */
public interface NormalFeatureGroupMemberJpaRepository extends JpaRepository<NormalFeatureGroupMember, Long> {

    List<NormalFeatureGroupMember> findByUserId(Long userId);

    List<NormalFeatureGroupMember> findByGroupId(Long groupId);

    void deleteByUserId(Long userId);

    void deleteByGroupId(Long groupId);
}
