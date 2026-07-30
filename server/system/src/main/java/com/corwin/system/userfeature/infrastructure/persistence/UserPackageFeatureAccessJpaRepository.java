package com.corwin.system.userfeature.infrastructure.persistence;

import com.corwin.system.userfeature.domain.model.UserPackageFeatureAccess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * JPA repository for {@link UserPackageFeatureAccess} entity.
 *
 * @author Corwin 2026/6/14
 */
public interface UserPackageFeatureAccessJpaRepository extends JpaRepository<UserPackageFeatureAccess, Long> {

    List<UserPackageFeatureAccess> findByPackageId(Long packageId);

    List<UserPackageFeatureAccess> findByPackageIdIn(Iterable<Long> packageIds);

    void deleteByPackageId(Long packageId);
}
