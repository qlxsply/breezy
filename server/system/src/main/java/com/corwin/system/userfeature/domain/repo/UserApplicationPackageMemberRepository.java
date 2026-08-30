package com.corwin.system.userfeature.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.userfeature.domain.model.UserApplicationPackageMember;
import java.util.List;

/**
 * Domain repository for user-package membership relations.
 *
 * @author Corwin 2026/6/14
 */
public interface UserApplicationPackageMemberRepository
    extends DomainRepository<UserApplicationPackageMember, Long> {

  List<UserApplicationPackageMember> findByUserId(Long userId);

  List<UserApplicationPackageMember> findByPackageId(Long packageId);

  void deleteByUserId(Long userId);

  void deleteByPackageId(Long packageId);
}
