package com.corwin.system.userfeature.infrastructure.persistence;

import com.corwin.system.userfeature.domain.model.UserApplicationPackage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA repository for {@link UserApplicationPackage} entity.
 *
 * @author Corwin 2026/6/14
 */
public interface UserApplicationPackageJpaRepository
    extends JpaRepository<UserApplicationPackage, Long> {

  List<UserApplicationPackage> findByIdIn(Iterable<Long> ids);

  List<UserApplicationPackage> findByDefaultPackageTrueAndEnabledTrue();

  boolean existsByPackageCode(String packageCode);
}
