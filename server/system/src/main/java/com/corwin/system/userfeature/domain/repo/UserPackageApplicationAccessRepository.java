package com.corwin.system.userfeature.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.userfeature.domain.model.UserPackageApplicationAccess;

import java.util.List;

/**
 * Domain repository for package-level application access grants.
 *
 * @author Corwin 2026/6/14
 */
public interface UserPackageApplicationAccessRepository extends DomainRepository<UserPackageApplicationAccess, Long> {

    List<UserPackageApplicationAccess> findByPackageId(Long packageId);

    List<UserPackageApplicationAccess> findByPackageIdIn(Iterable<Long> packageIds);

    void deleteByPackageId(Long packageId);
}
