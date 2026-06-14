package com.corwin.system.userfeature.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.userfeature.domain.model.UserPackageFeatureAccess;

import java.util.List;

/**
 * @author Corwin 2026/6/14
 */
public interface UserPackageFeatureAccessRepository extends DomainRepository<UserPackageFeatureAccess, Long> {

    List<UserPackageFeatureAccess> findByPackageId(Long packageId);

    List<UserPackageFeatureAccess> findByPackageIdIn(Iterable<Long> packageIds);

    void deleteByPackageId(Long packageId);
}
