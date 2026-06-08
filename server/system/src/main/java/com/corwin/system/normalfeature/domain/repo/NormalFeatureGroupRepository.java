package com.corwin.system.normalfeature.domain.repo;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.normalfeature.domain.model.NormalFeatureGroup;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/5/21
 */
public interface NormalFeatureGroupRepository extends DomainRepository<NormalFeatureGroup, Long> {

    PageData<NormalFeatureGroup> page(String keyword, Boolean enabled, PageSpec spec);

    Optional<NormalFeatureGroup> findByCode(String code);

    boolean existsByCode(String code);

    List<NormalFeatureGroup> findByDefaultGroupTrueAndEnabledTrue();

    List<NormalFeatureGroup> findByIdIn(Iterable<Long> ids);
}
