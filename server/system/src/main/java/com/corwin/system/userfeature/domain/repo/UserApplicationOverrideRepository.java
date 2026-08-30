package com.corwin.system.userfeature.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.userfeature.domain.model.UserApplicationOverride;
import java.util.List;

/**
 * Domain repository for per-user application access overrides.
 *
 * @author Corwin 2026/6/14
 */
public interface UserApplicationOverrideRepository
    extends DomainRepository<UserApplicationOverride, Long> {

  List<UserApplicationOverride> findByUserId(Long userId);

  void deleteByUserId(Long userId);
}
