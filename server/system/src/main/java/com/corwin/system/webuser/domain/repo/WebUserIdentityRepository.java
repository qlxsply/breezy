package com.corwin.system.webuser.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.webuser.domain.model.WebUserIdentity;
import com.corwin.system.webuser.domain.model.WebUserIdentityType;
import java.util.List;
import java.util.Optional;

/**
 * Domain repository for {@link WebUserIdentity} entity. Provides queries for finding identities by
 * user and type.
 *
 * @author Corwin 2026/5/11
 */
public interface WebUserIdentityRepository extends DomainRepository<WebUserIdentity, Long> {

  /** Find all identities belonging to a user. */
  List<WebUserIdentity> findByUserId(Long userId);

  /** Find the first identity matching user ID and identity type. */
  Optional<WebUserIdentity> findFirstByUserIdAndIdentityType(
      Long userId, WebUserIdentityType identityType);
}
