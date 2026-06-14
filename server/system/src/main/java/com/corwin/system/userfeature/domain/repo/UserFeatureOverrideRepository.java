package com.corwin.system.userfeature.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.userfeature.domain.model.UserFeatureOverride;

import java.util.List;

/**
 * @author Corwin 2026/6/14
 */
public interface UserFeatureOverrideRepository extends DomainRepository<UserFeatureOverride, Long> {

    List<UserFeatureOverride> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
