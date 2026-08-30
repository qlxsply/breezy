package com.corwin.system.webuser.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.webuser.domain.model.WebUserCurrentIdentity;
import com.corwin.system.webuser.domain.model.WebUserIdentityType;
import java.util.Optional;

/**
 * Domain repository for {@link WebUserCurrentIdentity} entity. Provides lookups by identity hash or
 * OAuth provider details.
 *
 * @author Corwin 2026/5/11
 */
public interface WebUserCurrentIdentityRepository
    extends DomainRepository<WebUserCurrentIdentity, Long> {

  /** Find current identity by identity type and hash value. */
  Optional<WebUserCurrentIdentity> findByIdentityTypeAndIdentityHash(
      WebUserIdentityType identityType, String identityHash);

  /** Find current identity by OAuth provider code and subject. */
  Optional<WebUserCurrentIdentity> findByProviderCodeAndProviderSubject(
      String providerCode, String providerSubject);
}
