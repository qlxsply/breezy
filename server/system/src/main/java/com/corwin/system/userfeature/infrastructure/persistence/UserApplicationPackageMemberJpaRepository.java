package com.corwin.system.userfeature.infrastructure.persistence;

import com.corwin.system.userfeature.domain.model.UserApplicationPackageMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * JPA repository for {@link UserApplicationPackageMember} entity.
 *
 * @author Corwin 2026/6/14
 */
public interface UserApplicationPackageMemberJpaRepository extends JpaRepository<UserApplicationPackageMember, Long> {

    List<UserApplicationPackageMember> findByUserId(Long userId);

    List<UserApplicationPackageMember> findByPackageId(Long packageId);

    void deleteByUserId(Long userId);

    void deleteByPackageId(Long packageId);
}
