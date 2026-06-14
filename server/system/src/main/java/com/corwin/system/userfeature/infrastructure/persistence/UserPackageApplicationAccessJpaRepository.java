package com.corwin.system.userfeature.infrastructure.persistence;

import com.corwin.system.userfeature.domain.model.UserPackageApplicationAccess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author Corwin 2026/6/14
 */
public interface UserPackageApplicationAccessJpaRepository extends JpaRepository<UserPackageApplicationAccess, Long> {

    List<UserPackageApplicationAccess> findByPackageId(Long packageId);

    List<UserPackageApplicationAccess> findByPackageIdIn(Iterable<Long> packageIds);

    void deleteByPackageId(Long packageId);
}
