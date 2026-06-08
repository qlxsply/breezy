package com.corwin.system.webuser.infrastructure.persistence;

import com.corwin.system.webuser.domain.model.WebUserCurrentIdentity;
import com.corwin.system.webuser.domain.model.WebUserIdentityType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * @author Corwin 2026/5/11
 */
public interface WebUserCurrentIdentityJpaRepository extends JpaRepository<WebUserCurrentIdentity, Long> {

    Optional<WebUserCurrentIdentity> findByIdentityTypeAndIdentityHash(WebUserIdentityType identityType,
            String identityHash);

    Optional<WebUserCurrentIdentity> findByProviderCodeAndProviderSubject(String providerCode, String providerSubject);
}
