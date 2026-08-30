package com.corwin.system.webuser.infrastructure.persistence;

import com.corwin.system.webuser.domain.model.WebUserCurrentIdentity;
import com.corwin.system.webuser.domain.model.WebUserIdentityType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link WebUserCurrentIdentity} entity.
 *
 * @author Corwin 2026/5/11
 */
public interface WebUserCurrentIdentityJpaRepository
    extends JpaRepository<WebUserCurrentIdentity, Long> {

  /** Find current identity by identity type and hash. */
  Optional<WebUserCurrentIdentity> findByIdentityTypeAndIdentityHash(
      WebUserIdentityType identityType, String identityHash);

  /** Find current identity by OAuth provider code and subject. */
  Optional<WebUserCurrentIdentity> findByProviderCodeAndProviderSubject(
      String providerCode, String providerSubject);
}
