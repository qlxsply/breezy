package com.corwin.system.webuser.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.webuser.domain.model.WebUserCurrentIdentity;
import com.corwin.system.webuser.domain.model.WebUserIdentityType;

import java.util.Optional;

/**
 * @author Corwin 2026/5/11
 */
public interface WebUserCurrentIdentityRepository extends DomainRepository<WebUserCurrentIdentity, Long> {

    Optional<WebUserCurrentIdentity> findByIdentityTypeAndIdentityHash(WebUserIdentityType identityType,
            String identityHash);

    Optional<WebUserCurrentIdentity> findByProviderCodeAndProviderSubject(String providerCode, String providerSubject);
}
